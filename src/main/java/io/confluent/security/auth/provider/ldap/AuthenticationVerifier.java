package io.confluent.security.auth.provider.ldap;

import org.jetbrains.annotations.NotNull;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import javax.naming.CommunicationException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import java.util.Hashtable;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "AuthenticationVerifier")
public class AuthenticationVerifier extends LdapVerifierBase implements Callable<Integer> {

    @Option(names = {"-u", "--username"}, required = true, description = "Username") String userName;
    @Option(names = {"-p", "--password"}, required = true, arity = "0..1", interactive = true, description = "Password") String password;

    private AutoCloseable autoCloseable(InitialDirContext context) {
        return context::close;
    }

    private String getUserDn(@NotNull LdapConfig config) throws NamingException {
        var env = config.ldapContextEnvironment;
        LdapContext context = new InitialLdapContext(env, null);

        SearchControls searchControls = new SearchControls();
        searchControls.setReturningAttributes(new String[]{config.userNameAttribute});
        searchControls.setSearchScope(config.userSearchScope); // TODO think about userPasswordAttribute

        Object[] filterArgs = new Object[] {userName};
        NamingEnumeration<SearchResult> enumeration = context.search(config.userSearchBase, config.userDnSearchFilter, filterArgs,searchControls);

        if (!enumeration.hasMore()) {
            System.err.printf("User '%s' not found %n", userName);
            System.exit(-1);
        }

        SearchResult result = enumeration.next();

        if (enumeration.hasMore()) {
            System.err.printf("Search found multiple entries for user '%s'", userName);
            System.exit(-2);
        }

        context.close();

        return result.getNameInNamespace();
    }

    private void verifyPassword(String userDn, @NotNull LdapConfig config) {
        Hashtable<String, String> env = new Hashtable<>(config.ldapContextEnvironment);
        env.put(LdapContext.SECURITY_AUTHENTICATION, "simple");
        env.put(LdapContext.SECURITY_PRINCIPAL, userDn);
        env.put(LdapContext.SECURITY_CREDENTIALS, password);

        try (var ignored = autoCloseable(new InitialDirContext(env))) {
            System.out.printf("User %s has been authenticated%n", userName);
        } catch (Exception e) {
            if (e instanceof CommunicationException) {
                System.err.printf("LDAP bind failed for user DN %s", userDn);
            } else {
                System.err.printf("LDAP bind failed for user DN %s with specified password", userDn);
            }

            System.exit(3);
        }
    }

    @Override
    public Integer call() throws Exception {
        LdapConfig config = loadProperties();

        String userDn = getUserDn(config);

        verifyPassword(userDn, config);

        return 0;
    }

    public static void main(String... args) {
        new CommandLine(new AuthenticationVerifier()).
                setUsageHelpLongOptionsMaxWidth(40).
                execute(args);
    }
}
