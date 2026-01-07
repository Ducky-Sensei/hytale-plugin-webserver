package net.nitrado.hytale.plugins.webserver.authentication;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.security.Principal;

public class UserPrincipalRequestWrapper extends HttpServletRequestWrapper {
    private final Principal principal;

    public UserPrincipalRequestWrapper(HttpServletRequest request, Principal principal) {
        super(request);
        this.principal = principal;
    }

    @Override
    public Principal getUserPrincipal() {
        return this.principal;
    }

    @Override
    public String getAuthType() {
        return "CUSTOM";
    }
}