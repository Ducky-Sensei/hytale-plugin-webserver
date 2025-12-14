package net.nitrado.hytale.plugins.webserver.cert;

import javax.net.ssl.SSLContext;

public interface CertificateProvider {
    /**
     * Creates an SSLContext configured with this provider's certificate(s).
     * @return a configured SSLContext ready for use
     */
    SSLContext createSSLContext() throws Exception;

    /**
     * Optional: Check if certificates need refresh/renewal.
     * @return true if the certificate has been updated
     */
    default boolean refresh() throws Exception {
        return false;
    }
}
