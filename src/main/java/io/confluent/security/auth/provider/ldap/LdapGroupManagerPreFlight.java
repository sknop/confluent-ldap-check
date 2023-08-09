package io.confluent.security.auth.provider.ldap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.confluent.ps.preflight.Check;
import io.confluent.ps.preflight.CheckResult;
import io.confluent.ps.preflight.PreFlight;
import io.confluent.ps.preflight.PreFlightReport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import javax.naming.NamingException;

import org.apache.kafka.common.utils.Time;

public class LdapGroupManagerPreFlight
        implements PreFlight<LdapConfig, LdapGroupManager> {
    static ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    public static void main(String[] args) throws IOException {
        if (args.length != 2) throw new IllegalArgumentException("<config path> is expected as argument.");
        var configPath = Path.of(args[0]);
        var checksPath = Path.of(args[1]);
        var preFlight = new LdapGroupManagerPreFlight();
        var config = preFlight.loadConfig(configPath);
        var component = preFlight.build(config);
        var checkList = loadChecks(checksPath);
        PreFlightReport report = preFlight.run(component, checkList);
        component.close();
        report.print();
    }

    public LdapConfig loadConfig(Path configPath) throws IOException {
        Properties props = new Properties();
        try (final var inStream = Files.newInputStream(configPath)) {
            props.load(inStream);
            return new LdapConfig(props);
        }
    }

    public static List<Check<LdapGroupManager>> loadChecks(Path path) throws IOException {
        var tree = mapper.readTree(Files.readAllBytes(path));
        var checks = new ArrayList<Check<LdapGroupManager>>();
        final var check = tree.get("ldap-group-manager");
        for (var user : check.get("users")) {
            var name = user.fieldNames().next();
            var groups = new HashSet<String>();
            for (var group : user.get(name).get("groups")) {
                groups.add(group.textValue());
            }
            checks.add(new UserGroupCheck(name, groups));
        }
        return checks;
    }

    public LdapGroupManager build(LdapConfig config) {
        return new LdapGroupManager(config, Time.SYSTEM);
    }

    public PreFlightReport run(
            LdapGroupManager component,
            List<Check<LdapGroupManager>> checkList
    ) {
        PreFlightReport report = new PreFlightReport();
        for (var check : checkList) {
            List<CheckResult> results = check.apply(component);
            report.addAll(results);
        }
        return report;
    }

    static class UserGroupCheck extends Check<LdapGroupManager> {

        final String username;
        final Set<String> groups;

        UserGroupCheck(String username, Set<String> groups) {
            this.username = username;
            this.groups = groups;
        }

        @Override
        public List<CheckResult> apply(LdapGroupManager component) {
            var results = new ArrayList<CheckResult>();
            final var title = "ldap-group-manager: find groups for [" + username + "]";
            try {
                var result = component.searchAndProcessResults();

                System.out.println("**************************");
                for (var entry: result) {
                  System.out.println(entry);
                }
                System.out.println("**************************");

                var found = component.groups(username);
                final var expected = groups;
                if (found.containsAll(expected)) {
                    results.add(new CheckResult(title));
                } else {
                    results.add(
                            new CheckResult(
                                    title,
                                    String.format(
                                            "Not all groups found for User %s: " +
                                                    "Group expected: %s " +
                                                    "Groups found: %s",
                                            username,
                                            expected,
                                            found
                                    )
                            )
                    );
                }
            } catch (NamingException | IOException e) {
                results.add(new CheckResult(title, e));
            }
            return results;
        }
    }
}
