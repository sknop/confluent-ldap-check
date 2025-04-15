#!/bin/sh

echo "Running LDAP script"

CONFIG_FILE=/home/app/config.properties

touch $CONFIG_FILE

echo "ldap.java.naming.provider.url=${ldap_java_naming_provider_url}" >> $CONFIG_FILE
echo "ldap.java.naming.security.credentials=${ldap_java_naming_security_credentials}" >> $CONFIG_FILE
echo "ldap.java.naming.security.principal=${ldap_java_naming_security_principal}" >> $CONFIG_FILE
echo "ldap.java.naming.security.authentication=${ldap_java_naming_security_authentication}" >> $CONFIG_FILE
echo "ldap.search.mode=${ldap_search_mode}" >> $CONFIG_FILE
echo "ldap.group.search.scope=${ldap_group_search_scope}" >> $CONFIG_FILE
echo "ldap.group.search.base=${ldap_group_search_base}" >> $CONFIG_FILE
echo "ldap.group.object.class=${ldap_group_object_class}" >> $CONFIG_FILE
echo "ldap.group.name.attribute=${ldap_group_name_attribute}" >> $CONFIG_FILE
echo "ldap.group.member.attribute.pattern=${ldap_group_member_attribute_pattern}" >> $CONFIG_FILE
echo "ldap.group.member.attribute=${ldap_group_member_attribute}" >> $CONFIG_FILE
echo "ldap.user.search.scope=${ldap_user_search_scope}" >> $CONFIG_FILE
echo "ldap.user.search.base=${ldap_user_search_base}" >> $CONFIG_FILE
echo "ldap.user.object.class=${ldap_user_object_class}" >> $CONFIG_FILE
echo "ldap.user.name.attribute=${ldap_user_name_attribute}" >> $CONFIG_FILE
echo "ldap.user.memberof.attribute=${ldap_user_memberof_attribute}" >> $CONFIG_FILE
echo "ldap.user.memberof.attribute.pattern=${ldap_user_memberof_attribute_pattern}" >> $CONFIG_FILE

echo "Verifier Type: $verifier_type"
echo "Config File:"
cat $CONFIG_FILE

echo "Command Result:"

if [ $verifier_type = "io.confluent.security.auth.provider.ldap.GroupVerifier" ]; then
  java -cp /home/app/target/confluent-ldap-check-1.0.0.jar \
      $verifier_type -c \
      $CONFIG_FILE
else
  java -cp /home/app/target/confluent-ldap-check-1.0.0.jar \
      $verifier_type -c \
      $CONFIG_FILE -u $username -p $password
fi
