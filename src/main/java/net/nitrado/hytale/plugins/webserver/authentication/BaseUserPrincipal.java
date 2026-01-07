package net.nitrado.hytale.plugins.webserver.authentication;

import java.security.Principal;

public class BaseUserPrincipal implements Principal {

    private final String identity;

    public BaseUserPrincipal(String identity) {
        this.identity = identity;
    }

    @Override
    public String getName() {
        return this.identity;
    }
}
