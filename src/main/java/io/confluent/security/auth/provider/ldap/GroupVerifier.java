package io.confluent.security.auth.provider.ldap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.confluent.ps.ldap_check.ContextAutoClosingWrapper;
import org.apache.kafka.common.utils.Time;
import org.jetbrains.annotations.NotNull;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@CommandLine.Command(name = "GroupVerifier")
public class GroupVerifier extends LdapVerifierBase implements Callable<Integer> {

    private static final Logger log = LoggerFactory.getLogger(GroupVerifier.class.getName());

    @Option(names = "--yaml", description="Enable YAML output") boolean yamlOutput = false;

    public LdapGroupManager build(LdapConfig config) {
        return new LdapGroupManager(config, Time.SYSTEM);
    }

    private Map<String, Set<String>> listAllGroupsAndUsers(LdapGroupManager groupManager, Set<String> users) {
        Map<String, Set<String>> allGroups = new HashMap<>();

        for (var user: users) {
            var groups = groupManager.groups(user);
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

    private record ResultEntryConfig(String nameAttribute,
                                     Pattern nameAttributePattern,
                                     String memberAttribute,
                                     Pattern memberAttributePattern) {

    }

    private record ResultEntry(String name, Set<String> members) {
    }

    private Map<String, Set<String>> findGroupsByQuery(LdapConfig config) throws NamingException {
        Map<String, Set<String>> result = new HashMap<>();
        var env = config.ldapContextEnvironment;

        try (var wrapper = new ContextAutoClosingWrapper<>(new InitialLdapContext(env, null))) {
            LdapContext context = wrapper.get();

            var searchControls = new SearchControls();

            searchControls.setReturningAttributes(new String[]{
                    config.groupNameAttribute,
                    config.groupMemberAttribute
            });
            var resultEntryConfig = new ResultEntryConfig(
                    config.groupNameAttribute,
                    config.groupNameAttributePattern,
                    config.groupMemberAttribute,
                    config.groupMemberAttributePattern);

            searchControls.setSearchScope(config.groupSearchScope);

            // TODO maybe set page size?
            NamingEnumeration<SearchResult> enumeration = context.search(config.groupSearchBase, config.groupSearchFilter, searchControls);

            while (enumeration.hasMore()) {
                SearchResult searchResult = enumeration.next();
                ResultEntry entry = searchResultEntry(searchResult, resultEntryConfig, config);

                assert entry != null;
                result.put(entry.name, entry.members);
            }

        }
        return result;
    }

    static String attributeValue(Object value, Pattern pattern, String parent, String attrDesc, LdapConfig.SearchMode searchMode) {
        if (value == null) {
            log.error("Ignoring null {} in LDAP {} {}", attrDesc, searchMode, parent);
            return null;
        }
        if (pattern == null) {
            return String.valueOf(value);
        }
        Matcher matcher = pattern.matcher(value.toString());
        if (!matcher.matches()) {
            log.debug("Ignoring {} in LDAP {} {} that doesn't match pattern: {}", attrDesc, searchMode, parent, value);
            return null;
        }
        return matcher.group(1);
    }

    private ResultEntry searchResultEntry(SearchResult searchResult, ResultEntryConfig resultEntryConfig, LdapConfig config) throws NamingException {
        Attributes attributes = searchResult.getAttributes();
        Attribute nameAttr = attributes.get(resultEntryConfig.nameAttribute);
        if (nameAttr != null) {
            String name = attributeValue(nameAttr.get(), resultEntryConfig.nameAttributePattern,
                    "", "search result", config.searchMode);
            if (name == null) {
                return null;
            }
            Set<String> members = new HashSet<>();
            Attribute memberAttr = attributes.get(resultEntryConfig.memberAttribute);
            if (memberAttr != null) {
                NamingEnumeration<?> attrs = memberAttr.getAll();
                while (attrs.hasMore()) {
                    Object member = attrs.next();
                    String memberName = attributeValue(member, resultEntryConfig.memberAttributePattern,
                            name, "member", config.searchMode);
                    if (memberName != null) {
                        members.add(memberName);
                    }
                }
            }
            return new ResultEntry(name, members);
        }
        return null;
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

        Map<String, Set<String>> groups;

        if (config.searchMode == LdapConfig.SearchMode.USERS) {
            LdapGroupManager groupManager = build(config);
            var users = groupManager.searchAndProcessResults();
            groups = listAllGroupsAndUsers(groupManager, users);
        }
        else if (config.searchMode == LdapConfig.SearchMode.GROUPS) {
            groups = findGroupsByQuery(config);
        }
        else {
            throw new IllegalArgumentException("Unsupported search mode " + config.searchMode);
        }

        assert groups != null;
        if (yamlOutput) {
            printYAML(groups);
        }
        else {
            printPlain(groups);
        }

        return 0;
    }

    public static void main(String... args) {
        new CommandLine(new GroupVerifier()).
                setUsageHelpLongOptionsMaxWidth(40).
                execute(args);
    }
}
