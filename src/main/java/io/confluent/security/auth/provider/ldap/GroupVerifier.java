package io.confluent.security.auth.provider.ldap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.kafka.common.utils.Time;
2import org.jetbrains.annotations.NotNull;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "GroupVerifier")
public class GroupVerifier extends LdapVerifierBase implements Callable<Integer> {

    @Option(names = "--yaml", description="Enable YAML output") boolean yamlOutput = false;

    public LdapGroupManager build(LdapConfig config) {
        return new LdapGroupManager(config, Time.SYSTEM);
    }

    private Map<String, Set<String>> listAllGroupsAndUsers(LdapGroupManager component, Set<String> users) {
        Map<String, Set<String>> allGroups = new HashMap<>();

        for (var user: users) {
            var groups = component.groups(user);
            for (var group: groups) {
                if (allGroups.containsKey(group)) {
                    allGroups.get(group).add(user);
                }
                else {
                    Set<String> userForGroup = new HashSet<>();
                    userForGroup.add(user);
                    allGroups.put(group, userForGroup);
                }
            }
        }

        return allGroups;
    }

    private void printYAML(@NotNull Map<String, Set<String>> groups) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.writeValue(System.out, groups);
    }

    private void printPlain(@NotNull Map<String, Set<String>> groups) {
        for (var group : groups.entrySet()) {
            System.out.printf("%s:%n", group.getKey());
            for (var user : group.getValue()) {
                System.out.printf("\t%s%n", user);
            }
        }
    }

    @Override
    public Integer call() throws Exception {
        LdapConfig config = loadProperties();
        LdapGroupManager groupManager = build(config);

        var users = groupManager.searchAndProcessResults();
        var groups = listAllGroupsAndUsers(groupManager, users);

        if (yamlOutput) {
            printYAML(groups);
        }
        else {
            printPlain(groups);
        }

        return null;
    }

    public static void main(String... args) {
        new CommandLine(new GroupVerifier()).
                setUsageHelpLongOptionsMaxWidth(40).
                execute(args);
    }
}
