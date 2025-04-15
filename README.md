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

accordingly, please let me know so I can adjust it (or/and file a pull request).

## Modules

- confluent-ldap-check

## How to run

### Run with Docker Compose

Build with docker compose from source

```sh
docker-compose build
```

Update the environmental variables in docker-compose.yml. The LDAP env vars with dots are replaced with underscores.

Bring up the instance

```sh
> docker-compose up

confluent-ldap-check-ldap-check-1  | Running LDAP script
confluent-ldap-check-ldap-check-1  | Verifier Type: 
confluent-ldap-check-ldap-check-1  | Config File:
confluent-ldap-check-ldap-check-1  | ldap.java.naming.provider.url=ldap://host.docker.internal:9389
confluent-ldap-check-ldap-check-1  | ldap.java.naming.security.credentials=Developer!
confluent-ldap-check-ldap-check-1  | ldap.java.naming.security.principal=cn=mds,dc=test,dc=com
confluent-ldap-check-ldap-check-1  | ldap.java.naming.security.authentication=simple
confluent-ldap-check-ldap-check-1  | ldap.search.mode=GROUPS
confluent-ldap-check-ldap-check-1  | ldap.group.search.scope=2
confluent-ldap-check-ldap-check-1  | ldap.group.search.base=dc=test,dc=com
confluent-ldap-check-ldap-check-1  | ldap.group.object.class=posixGroup
confluent-ldap-check-ldap-check-1  | ldap.group.name.attribute=cn
confluent-ldap-check-ldap-check-1  | ldap.group.member.attribute.pattern=cn=([^,]*)(?:.*)dc=test,dc=com
confluent-ldap-check-ldap-check-1  | ldap.group.member.attribute=member
confluent-ldap-check-ldap-check-1  | ldap.user.search.scope=2
confluent-ldap-check-ldap-check-1  | ldap.user.search.base=dc=test,dc=com
confluent-ldap-check-ldap-check-1  | ldap.user.object.class=organizationalRole
confluent-ldap-check-ldap-check-1  | ldap.user.name.attribute=cn
confluent-ldap-check-ldap-check-1  | Command Result:
confluent-ldap-check-ldap-check-1  | User 'kafka' has been authenticated
confluent-ldap-check-ldap-check-1 exited with code 0
```

### Run with Docker

Build with docker from source

```sh
docker-compose build . -t ldap-image-test
```

Update the environmental variables in docker-compose.yml. The LDAP env vars with dots are replaced with underscores.

Run the verifier

```sh
docker run \
-e verifier_type=io.confluent.security.auth.provider.ldap.AuthenticationVerifier \
-e username=kafka \
-e password=kafka-secret \
-e ldap_java_naming_provider_url=ldap://host.docker.internal:9389 \
-e ldap_java_naming_security_principal=cn=mds,dc=test,dc=com \
-e ldap_java_naming_security_authentication=simple \
-e ldap_java_naming_security_credentials=Developer! \
-e ldap_search_mode=GROUPS \
-e ldap_group_search_scope=2 \
-e ldap_group_search_base=dc=test,dc=com \
-e ldap_group_object_class=posixGroup \
-e ldap_group_name_attribute=cn \
-e ldap_group_member_attribute_pattern="cn=([^,]*)(?:.*)dc=test,dc=com" \
-e ldap_group_member_attribute=member \
-e ldap_user_search_scope=2 \
-e ldap_user_search_base=dc=test,dc=com \
-e ldap_user_object_class=organizationalRole \
-e ldap_user_name_attribute=cn \
-e ldap_user_memberof_attribute=member \
-e ldap_user_memberof_attribute_pattern=a \
ldap-image-test
```

### Run locally

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
