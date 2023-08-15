package io.confluent.ps.ldap_check;

import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;
import java.util.function.Supplier;

public class ContextAutoClosingWrapper<T extends InitialDirContext> implements AutoCloseable, Supplier<InitialDirContext> {
    private final T context;

    public ContextAutoClosingWrapper(T s) {
        context = s;
    }

    @Override
    public T get() {
        return context;
    }

    @Override
    public void close() throws NamingException {
        context.close();
    }
}
