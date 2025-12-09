package net.nitrado.hytale.plugins.webserver.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.security.Principal;

public interface AuthProvider {
    record AuthResult(AuthResultType type, Principal principal) {}

    /**
     * AuthResultType is used as return value for authentication methods.
     */
    enum AuthResultType {
        /**
         * NONE signals that the user did not attempt to authenticate with the given method, no authentication
         * check was performed. This allows subsequent methods to be attempted.
         */
        NONE,
        /**
         * FAILURE signals that the user did attempt to authenticate with the given method, but that authentication
         * failed. This results in a 401 Unauthorized to be returned.
         */
        FAILURE,
        /**
         * CHALLENGE signals that additional steps need to be taken to complete authentication. When using this return
         * value, the method itself has set appropriate data on the HTTP response (such as headers and status codes)
         * and the response will be sent out as-is. This may be used to send a WWW-Authenticate header for basic auth,
         * or a redirect for OAuth implementations.
         */
        CHALLENGE,
        /**
         * SUCCESS indicates that authentication with the given method was successful and the user identity has been
         * returned.
         */
        SUCCESS,
    }

    AuthResult authenticate(HttpServletRequest req, HttpServletResponse res);
    default boolean challenge(HttpServletRequest req, HttpServletResponse res) {
        return false;
    }
}
