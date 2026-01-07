package net.nitrado.hytale.plugins.webserver;

import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.PluginBase;
import net.nitrado.hytale.plugins.webserver.authentication.AuthFilter;
import net.nitrado.hytale.plugins.webserver.authentication.AuthProvider;
import net.nitrado.hytale.plugins.webserver.servlets.AuthorizationWrapperServlet;
import net.nitrado.hytale.plugins.webserver.cert.CertificateProvider;
import net.nitrado.hytale.plugins.webserver.config.WebServerConfig;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.ee10.servlet.FilterHolder;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.SessionTrackingMode;
import jakarta.servlet.http.HttpServlet;
import javax.net.ssl.SSLContext;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;

public class WebServer {
    private final ServletContextHandler context;
    private final Server server;
    private final HytaleLogger logger;
    private final Map<PluginIdentifier, List<String>> pluginToPathSpecs =  new HashMap<>();
    private AuthProvider[] defaultAuthProviders;

    public WebServer(HytaleLogger logger, WebServerConfig config, Path dataDir) {
        this.logger = logger;

        this.context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        this.context.setContextPath("/");

        // Use only cookies for session tracking, preventing jsessionid URL parameters
        this.context.getSessionHandler().setSessionTrackingModes(EnumSet.of(SessionTrackingMode.COOKIE));

        var tlsConfig = config.getTls();

        // Configure session cookie - disable Secure flag when running over HTTP
        var sessionHandler = this.context.getSessionHandler();
        if (tlsConfig.isInsecure()) {
            sessionHandler.setSecureRequestOnly(false);
        }

        var addr = new InetSocketAddress(config.getBindHost(), config.getBindPort());

        this.logger.atInfo().log("Binding WebServer to " + addr);

        this.server = new Server();

        ServerConnector connector;
        if (tlsConfig.isInsecure()) {
            this.logger.atWarning().log("TLS is disabled - using insecure plain HTTP!");
            connector = new ServerConnector(this.server);
        } else {
            connector = this.createTLSConnector(config, dataDir);
        }

        connector.setHost(addr.getHostName());
        connector.setPort(addr.getPort());

        this.server.addConnector(connector);
        this.server.setHandler(this.context);
    }

    /**
     * Adds a servlet at the specified path.
     */
    protected void addServlet(HttpServlet servlet, String pathSpec) {
        this.context.addServlet(new ServletHolder(servlet), pathSpec);
        this.logger.atInfo().log("Added servlet at path: %s", pathSpec);
    }

    protected void removeServlet(String pathSpec) {
        var servletHandler = this.context.getServletHandler();
        var mappings = servletHandler.getServletMappings();
        var holders = servletHandler.getServlets();

        if (mappings == null) return;

        // Find the servlet name(s) mapped to this path
        var servletNamesToRemove = java.util.Arrays.stream(mappings)
                .filter(m -> java.util.Arrays.asList(m.getPathSpecs()).contains(pathSpec))
                .map(org.eclipse.jetty.ee10.servlet.ServletMapping::getServletName)
                .collect(java.util.stream.Collectors.toSet());

        // Remove mappings
        var filteredMappings = java.util.Arrays.stream(mappings)
                .filter(m -> !java.util.Arrays.asList(m.getPathSpecs()).contains(pathSpec))
                .toArray(org.eclipse.jetty.ee10.servlet.ServletMapping[]::new);
        servletHandler.setServletMappings(filteredMappings);

        // Remove holders (only if they have no remaining mappings)
        if (holders != null) {
            var remainingMappedNames = java.util.Arrays.stream(filteredMappings)
                    .map(org.eclipse.jetty.ee10.servlet.ServletMapping::getServletName)
                    .collect(java.util.stream.Collectors.toSet());

            var filteredHolders = java.util.Arrays.stream(holders)
                    .filter(h -> !servletNamesToRemove.contains(h.getName()) || remainingMappedNames.contains(h.getName()))
                    .toArray(org.eclipse.jetty.ee10.servlet.ServletHolder[]::new);
            servletHandler.setServlets(filteredHolders);
        }

        this.logger.atInfo().log("Removed servlet at path: %s", pathSpec);
    }

    public void addServlet(PluginBase plugin, String pathSpec, HttpServlet servlet) throws IllegalPathSpecException {
        if (!pathSpec.isEmpty() && !pathSpec.startsWith("/")) {
            throw new IllegalPathSpecException();
        }

        var identifier = plugin.getIdentifier();
        var prefix = String.format("/%s/%s", identifier.getGroup(), identifier.getName());
        var fullPathSpec = prefix + pathSpec;

        this.addServlet(new AuthorizationWrapperServlet(this.logger, servlet), fullPathSpec);

        var authFilter = new AuthFilter(this.getDefaultAuthProviders());

        if (!this.pluginToPathSpecs.containsKey(identifier)) {
            this.pluginToPathSpecs.put(identifier, new ArrayList<>());

            this.context.addFilter(authFilter, prefix, EnumSet.of(DispatcherType.REQUEST));
            this.context.addFilter(authFilter, prefix + "/*", EnumSet.of(DispatcherType.REQUEST));
        }

        this.pluginToPathSpecs.computeIfAbsent(identifier, k -> new ArrayList<>());
        this.pluginToPathSpecs.get(identifier).add(pathSpec);
    }

    public AuthProvider[] getDefaultAuthProviders() {
        return this.defaultAuthProviders;
    }

    protected void setDefaultAuthProviders(AuthProvider[] defaultAuthProviders) {
        this.defaultAuthProviders = defaultAuthProviders;
    }

    public void removeServlet(PluginBase plugin, String pathSpec) throws IllegalPathSpecException {
        if (!pathSpec.isEmpty() && !pathSpec.startsWith("/")) {
            throw new IllegalPathSpecException();
        }

        var identifier = plugin.getIdentifier();

        var prefix = String.format("/%s/%s", identifier.getGroup(), identifier.getName());
        var fullPathSpec = prefix + pathSpec;

        this.removeServlet(fullPathSpec);
        this.pluginToPathSpecs.computeIfAbsent(identifier, k -> new ArrayList<>());
        this.pluginToPathSpecs.get(identifier).remove(pathSpec);
    }

    public void removeServlets(PluginBase plugin) {
        var toRemove =  Set.copyOf(this.pluginToPathSpecs.get(plugin.getIdentifier()));

        for (var pathSpec : toRemove) {
            try {
                this.removeServlet(plugin, pathSpec);
            } catch (IllegalPathSpecException e) {
                // this can't occur in practice
            }
        }
    }

    /**
     * Adds a filter at the specified path.
     */
    public void addFilter(Filter filter, String pathSpec) {
        this.context.addFilter(new FilterHolder(filter), pathSpec, EnumSet.of(DispatcherType.REQUEST));
    }

    protected void start() throws Exception {
        for (var connector : this.server.getConnectors()) {
            if (connector instanceof ServerConnector sc) {
                this.logger.atInfo().log("WebServer listening on %s:%d", sc.getHost(), sc.getPort());
            }
        }
        this.server.start();
    }

    protected void stop() {
        try {
            this.server.stop();
        } catch (Exception e) {
            this.logger.atSevere().withCause(e).log("Failed to stop WebServer");
        }
    }

    protected ServerConnector createTLSConnector(WebServerConfig config, Path dataDir) {
        SSLContext sslContext;
        try {
            sslContext = this.createSSLContext(config, dataDir);
        } catch (Exception e) {
            this.logger.atSevere().log("Failed to create SSL context: " + e.getMessage());
            throw new RuntimeException(e);
        }

        SslContextFactory.Server ssl = new SslContextFactory.Server();
        ssl.setSslContext(sslContext);
        ssl.setSniRequired(false);

        HttpConfiguration httpsConfig = new HttpConfiguration();
        SecureRequestCustomizer secureRequestCustomizer = new SecureRequestCustomizer();
        secureRequestCustomizer.setSniRequired(false);
        secureRequestCustomizer.setSniHostCheck(false);
        httpsConfig.addCustomizer(secureRequestCustomizer);

        return new ServerConnector(this.server,
                new SslConnectionFactory(ssl, HttpVersion.HTTP_1_1.asString()),
                new HttpConnectionFactory(httpsConfig));
    }

    protected SSLContext createSSLContext(WebServerConfig config, Path dataDir) throws Exception {
        var tlsConfig = config.getTls();

        CertificateProvider provider = tlsConfig.createCertificateProvider(
                config.getBindHost(),
                dataDir,
                msg -> this.logger.atInfo().log(msg)
        );
        this.logger.at(Level.INFO).log("Using certificate provider: " + tlsConfig.getCertificateProvider());
        return provider.createSSLContext();
    }
}
