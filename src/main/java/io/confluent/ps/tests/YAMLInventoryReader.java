package io.confluent.ps.tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.confluent.security.auth.provider.ldap.LdapConfig;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Properties;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.Callable;
import java.util.stream.StreamSupport;

@CommandLine.Command(name = "YAMLInventoryReader", mixinStandardHelpOptions = true, version = "1.0")
public class YAMLInventoryReader implements Callable<Integer>  {

    @CommandLine.Option(names = {"-i", "--ansible-inventory"}, description = "Ansible inventory file", required = true ) String inventoryFile;

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    private JsonNode findLocation(JsonNode root, String[] possibilities) {
        for (String location : possibilities) {
            var node = root.at(location);
            if (! node.isMissingNode())
                return node;
        }

        throw new IllegalArgumentException("Cannot find 'kafka_broker_custom_properties' under " + Arrays.toString(possibilities));
    }

    private LdapConfig loadInventoryFile(Path inventoryPath) throws IOException {
        var tree = mapper.readTree(Files.readAllBytes(inventoryPath));
        var kafka_broker_custom_properties = findLocation(tree,
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
        ldapEntries.forEach((entry) -> System.out.printf("%s : %s%n", entry.getKey(), entry.getValue().asText()));
        ldapEntries.forEach((entry) -> properties.put(entry.getKey(), entry.getValue().asText()));

        System.out.println(properties);

        return new LdapConfig(properties);
    }
   @Override
    public Integer call() throws Exception {
        LdapConfig config = loadInventoryFile(Path.of(inventoryFile));

        return null;
    }

    public static void main(String... args) {
        new CommandLine(new YAMLInventoryReader()).execute(args);
    }
}
