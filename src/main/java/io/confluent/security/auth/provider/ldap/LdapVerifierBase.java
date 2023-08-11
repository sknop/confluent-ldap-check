package io.confluent.security.auth.provider.ldap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Properties;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

@CommandLine.Command(
        scope = CommandLine.ScopeType.INHERIT,
        synopsisHeading = "%nUsage:%n",
        descriptionHeading   = "%nDescription:%n%n",
        parameterListHeading = "%nParameters:%n%n",
        optionListHeading    = "%nOptions:%n%n",
        mixinStandardHelpOptions = true,
        version = "1.0")
abstract public class LdapVerifierBase {

    @CommandLine.ArgGroup(multiplicity = "1")
    Exclusive exclusive;

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    static class Exclusive {
        @Option(names = {"-c", "--config"}, description = "LDAP configuration properties file" ) String configFile;
        @Option(names = {"-i", "--ansible-inventory"}, description = "Ansible inventory file" ) String inventoryFile;
    }

    @Option(names = {"-r", "--replacement-file"},
            description = "File that contains Jinja2 replacements in property format") String replacementFile;

    protected LdapConfig loadProperties() throws IOException {
        if (exclusive.configFile != null) {
            return loadConfigFile(Path.of(exclusive.configFile));
        }
        else if (exclusive.inventoryFile != null) {
            return loadInventoryFile(Path.of(exclusive.inventoryFile));
        }
        else {
            // Should not be able to happen, but a bit defensive programming does not hurt
            System.err.println("Need to choose either config or inventory file");
            System.exit(1);
        }
        return null;
    }

    private @NotNull JsonNode findLocation(JsonNode root, String @NotNull [] possibilities) {
        for (String location : possibilities) {
            var node = root.at(location);
            if (! node.isMissingNode())
                return node;
        }

        throw new IllegalArgumentException("Cannot find 'kafka_broker_custom_properties' under " + Arrays.toString(possibilities));
    }

    @Contract("_ -> new")
    private @NotNull LdapConfig createLdapConfigWithPotentialReplacement(Properties properties) throws IOException {
        if (replacementFile != null) {
            Properties replacements = new Properties();

            try (final var inStream = Files.newInputStream(Path.of(replacementFile))) {
                replacements.load(inStream);
            }

            Pattern pattern = Pattern.compile(".*(\\{\\{.*}}).*");
            for (var entry : properties.entrySet()) {
                String key = (String) entry.getKey();
                String value = (String) entry.getValue();

                var matcher = pattern.matcher(value);
                if (matcher.matches()) {
                    // var line = matcher.group(0);
                    var match = matcher.group(1);

                    if (replacements.containsKey(match)) {
                        properties.put(key, value.replace(match, replacements.getProperty(match)));
                    }
                }
            }

        }
        return new LdapConfig(properties);
    }

    /**
        Read from a standard config file

        @return LdapConfig - a fully configured LdapConfig object
     */
    @Contract("_ -> new")
    private @NotNull LdapConfig loadConfigFile(Path configPath) throws IOException {
        Properties properties = new Properties();
        try (final var inStream = Files.newInputStream(configPath)) {
            properties.load(inStream);
            return createLdapConfigWithPotentialReplacement(properties);
        }
    }

    /**
     Read from an Ansible inventory file as used by cp-ansible

     @return LdapConfig - a fully configured LdapConfig object
     */
    @Contract("_ -> new")
    private @NotNull LdapConfig loadInventoryFile(Path inventoryPath) throws IOException {
        var tree = mapper.readTree(Files.readAllBytes(inventoryPath));

        final var kafka_broker_custom_properties = findLocation(tree,
                new String[]
                        {
                                "/all/vars/kafka_broker_custom_properties",
                                "/kafka_broker/vars/kafka_broker_custom_properties"
                        }
        );

        var ldapEntries = StreamSupport.stream(Spliterators.spliteratorUnknownSize(
                kafka_broker_custom_properties.fields(), Spliterator.ORDERED),false)
                .filter(entry -> entry.getKey().startsWith("ldap."))
                .toList();

        Properties properties = new Properties();
        ldapEntries.forEach((entry) -> properties.put(entry.getKey(), entry.getValue().asText()));

        return createLdapConfigWithPotentialReplacement(properties);
    }
}
