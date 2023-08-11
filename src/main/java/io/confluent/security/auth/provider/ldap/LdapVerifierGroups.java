package io.confluent.security.auth.provider.ldap;

import org.apache.kafka.common.utils.Time;
import picocli.CommandLine;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "LdapVerifierGroups")
public class LdapVerifierGroups extends LdapVerifierBase implements Callable<Integer> {

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

    @Override
    public Integer call() throws Exception {
        LdapConfig config = loadProperties();
        LdapGroupManager groupManager = build(config);

        var users = groupManager.searchAndProcessResults();
        var groups = listAllGroupsAndUsers(groupManager, users);

        for (var group : groups.entrySet()) {
            System.out.println(group);
        }

        return null;
    }

    public static void main(String... args) {
        new CommandLine(new LdapVerifierGroups()).execute(args);
    }
}
