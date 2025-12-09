package net.nitrado.hytale.plugins.webserver.filters;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class AuthRequiredFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        var req = (HttpServletRequest) servletRequest;
        var res = (HttpServletResponse) servletResponse;

        if (req.getUserPrincipal() == null) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(req, res);
    }

}
