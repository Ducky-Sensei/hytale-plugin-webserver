package net.nitrado.hytale.plugins.webserver.filters;


import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.nitrado.hytale.plugins.webserver.auth.AuthProvider;
import net.nitrado.hytale.plugins.webserver.auth.UserPrincipalRequestWrapper;

import java.io.IOException;

public class AuthFilter implements Filter {

    private final AuthProvider[] authProviders;

    public AuthFilter(AuthProvider... authProviders) {
        this.authProviders = authProviders;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        for  (AuthProvider authProvider : authProviders) {
            var result = authProvider.authenticate(req, res);

            switch (result.type()) {
                case AuthProvider.AuthResultType.NONE:
                    continue;

                case AuthProvider.AuthResultType.SUCCESS:
                    var wrapped = new UserPrincipalRequestWrapper(req, result.principal());
                    filterChain.doFilter(wrapped, response);
                    return;

                case AuthProvider.AuthResultType.FAILURE:
                    res.setStatus(401);
                    return;

                case AuthProvider.AuthResultType.CHALLENGE:
                    return;
            }
        }

        // forward without user principal (i.e. anonymous user)
        filterChain.doFilter(request, response);

        if (res.getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
            for (AuthProvider authProvider : authProviders) {
                if (authProvider.challenge(req, res)) {
                    return;
                }
            }
        }
    }
}
