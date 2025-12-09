package net.nitrado.hytale.plugins.webserver.filters;

import com.hypixel.hytale.server.core.permissions.PermissionHolder;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class PermissionsRequiredFilter implements Filter {

    protected String[] permissions;

    public PermissionsRequiredFilter(String ...permissions) {
        this.permissions = permissions;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        var req = (HttpServletRequest) servletRequest;
        var res = (HttpServletResponse) servletResponse;

        var user = req.getUserPrincipal();

        if (user == null) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        if (!(user instanceof PermissionHolder holder)) {
            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        for  (String permission : this.permissions) {
            if (!holder.hasPermission(permission)) {
                res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }
}
