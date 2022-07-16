package io.confluent.security.auth.provider.ldap;

import io.confluent.ps.preflight.Check;
import io.confluent.ps.preflight.CheckResult;
import io.confluent.ps.preflight.PreFlight;
import io.confluent.ps.preflight.PreFlightReport;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.naming.NamingException;
import org.apache.kafka.common.utils.Time;

public class LdapGroupManagerPreFlight
  implements PreFlight<LdapConfig, LdapGroupManager> {

  public static void main(String[] args) throws IOException {
    LdapGroupManagerPreFlight preFlight = new LdapGroupManagerPreFlight();
    LdapConfig config = preFlight.loadConfig(
      Paths.get("configs/ldap-group.properties")
    );
    // explain config??
    LdapGroupManager component = preFlight.build(config);
    var checkList = List.<Check<LdapGroupManager>>of(
      new UserGroupCheck(
        Map.of("cn=alice,ou=it,ou=users,dc=confluent,dc=io", Set.of("ops1"))
      )
    );
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

    final Map<String, Set<String>> userGroups;

    UserGroupCheck(Map<String, Set<String>> userGroups) {
      this.userGroups = userGroups;
    }

    @Override
    public List<CheckResult> apply(LdapGroupManager component) {
      var results = new ArrayList<CheckResult>();
      try {
        component.searchAndProcessResults();
        for (var user : userGroups.keySet()) {
          var found = component.groups(user);
          final var expected = userGroups.get(user);
          if (found.containsAll(expected)) {
            results.add(new CheckResult());
          } else {
            results.add(
              new CheckResult(
                String.format(
                  "Not all groups found for User %s: " +
                  "Group expected: %s " +
                  "Groups found: %s",
                  user,
                  expected,
                  found
                )
              )
            );
          }
        }
      } catch (NamingException | IOException e) {
        results.add(new CheckResult(e));
      }
      results.add(new CheckResult());
      return results;
    }
  }
}
