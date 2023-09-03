# Confluent LDAP checks

Verify that the LDAP configuration for RBAC performs as expected - without the need to spin up a cluster.
Two checks can be performed:

- Test that individual users can be identified, that is, their DN be found and their password checked
- List all LDAP groups and their associated users as detected by the LDAP queries

These are implemented in two separate classes:

- io.confluent.security.auth.provider.ldap.GroupVerifier
- io.confluent.security.auth.provider.ldap.AuthenticationVerifier

You can provide your LDAP configuration in two different ways. 

- Via a configuration file in INIT format (key=value)
- Via an Ansible inventory file in YAML format

In the latter case, the tool will look at two different locations for the ldap configurations:

    /all/vars/kafka_broker_custom_properties
    /kafka_broker/vars/kafka_broker_custom_properties

If your configuration is different from these two locations, you can adjust the code in

    LdapVerifierBase::loadInventoryFile

accordingly and please let me know so that I can adjust it (or/and file a pull request).

## Modules

- confluent-ldap-check

## How to run

Package JAR:

```shell
> mvn clean package
```


Start LDAP locally for testing:

```shell
> docker-compose up -d
```

Alternatively, configure or use your own LDAP service, for example, the Samba service for the Bootcamp environments.

To list all matching groups and their associated users, run

```shell
> java -cp target/confluent-ldap-check-1.0.0.jar io.confluent.security.auth.provider.ldap.GroupVerifier -c \
      configs/confluent-ldap-check.properties
```

You will see output like this:
```shell
ops:
	alice
dev:
	barnie
	charlie
```

Note that you can change the output to YAML if you prefer.

Help:

```shell
> java  -cp target/confluent-ldap-check-1.0.0.jar io.confluent.security.auth.provider.ldap.GroupVerifier -h
Usage:
GroupVerifier [-hV] [--yaml] [-r=<replacementFile>] (-c=<configFile> | -i=<inventoryFile>)

Description:


Options:

  -c, --config=<configFile>                  LDAP configuration properties file
  -h, --help                                 Show this help message and exit.
  -i, --ansible-inventory=<inventoryFile>    Ansible inventory file
  -r, --replacement-file=<replacementFile>   File that contains Jinja2 replacements in property format
  -V, --version                              Print version information and exit.
      --yaml                                 Enable YAML output

```

LDAP Configuration file and Ansible inventory file are mutually exclusive.

To check if you can authenticate a user, use 

```shell
> java -cp target/confluent-ldap-check-1.0.0.jar  io.confluent.security.auth.provider.ldap.AuthenticationVerifier -i  ../rbac-kraft.hosts.yml -r configs/replacement.properties -u alice -p alice-secret
User 'alice' has been authenticated
```

You can leave out the password, you will then interactively be prompted for it.

The replacement file is useful if your Ansible inventory YAML file contains Jinja2 variables. 
You can then use the replacement file to populate these variables. Here is an example from the CP bootcamp:

```shell
{{kafka_broker_truststore_path}}=configs/kafka-truststore.jks
{{kafka_broker_truststore_storepass}}=changeme
{{region}}=emea
{{region\u0020|\u0020upper}}=EMEA
```

This is very useful if you use LDAPS and need to provide a truststore file. 
You probably do not want to hardcode the location in your inventory file, but you need to provide the location of
a local copy. Rather than having to copy the inventory file and replace the entries, just add the location
to your replacement file.

This code was tested with the model answers from https://github.com/sknop/bootcamp-terraform and the docker-compose configuration provided here.

Happy hacking!

(C) Sven Erik Knop
