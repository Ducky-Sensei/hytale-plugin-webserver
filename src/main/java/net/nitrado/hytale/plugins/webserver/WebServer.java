package net.nitrado.hytale.plugins.webserver;

import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.permissions.provider.HytalePermissionsProvider;
import com.hypixel.hytale.server.core.permissions.provider.PermissionProvider;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.plugin.PluginBase;
import com.hypixel.hytale.server.core.util.Config;
import net.nitrado.hytale.plugins.webserver.auth.AuthProvider;
import net.nitrado.hytale.plugins.webserver.auth.BasicAuthProvider;
import net.nitrado.hytale.plugins.webserver.auth.store.CombinedCredentialValidator;
import net.nitrado.hytale.plugins.webserver.auth.store.CredentialValidator;
import net.nitrado.hytale.plugins.webserver.auth.store.JsonPasswordStore;
import net.nitrado.hytale.plugins.webserver.auth.store.UserCredentialStore;
import net.nitrado.hytale.plugins.webserver.cert.CertificateProvider;
import net.nitrado.hytale.plugins.webserver.commands.WebServerCommand;
import net.nitrado.hytale.plugins.webserver.config.TlsConfig;
import net.nitrado.hytale.plugins.webserver.config.WebServerConfig;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import javax.annotation.Nonnull;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.logging.Level;

public class WebServer extends JavaPlugin {

    public WebServer(@Nonnull JavaPluginInit init) {
        super(init);

        this.contexts = new ContextHandlerCollection();
    }

    private final Config<WebServerConfig> config = withConfig(WebServerConfig.CODEC);
    private final ContextHandlerCollection contexts;
    private Server server;

    private CredentialValidator userCredentialValidator;
    private CredentialValidator serviceAccountCredentialValidator;

    private UserCredentialStore userCredentialStore;
    private UserCredentialStore serviceAccountCredentialStore;

    @Override
    protected void setup() {
        var l = getLogger();
        var cfg = config.get();
        var addr = new InetSocketAddress(cfg.getBindHost(), cfg.getBindPort());
        var tlsConfig = cfg.getTls();

        l.at(Level.INFO).log("Binding WebServer to " + addr);

        this.server = new Server();

        ServerConnector connector;
        if (tlsConfig.isInsecure()) {
            l.at(Level.WARNING).log("TLS is disabled - using insecure plain HTTP!");
            connector = new ServerConnector(this.server);
        } else {
            SSLContext sslContext;
            try {
                sslContext = this.createSSLContext(tlsConfig);
            } catch (Exception e) {
                l.at(Level.SEVERE).log("Failed to create SSL context: " + e.getMessage());
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

            connector = new ServerConnector(this.server,
                    new SslConnectionFactory(ssl, HttpVersion.HTTP_1_1.asString()),
                    new HttpConnectionFactory(httpsConfig));
        }

        connector.setHost(addr.getHostName());
        connector.setPort(addr.getPort());

        this.server.addConnector(connector);
        this.server.setHandler(this.contexts);

        try {
            this.setupAuthStores();
        } catch (IOException e) {
            l.at(Level.SEVERE).log("failed to setup stores for webserver credentials: %s", e.getMessage());
            return;
        }

        this.setupCommands();

        try {
            this.server.start();
        } catch (Exception e) {
            l.at(Level.SEVERE).log(e.getMessage());
        }
    }

    protected SSLContext createSSLContext(TlsConfig tlsConfig) throws Exception {
        var l = getLogger();
        CertificateProvider provider = tlsConfig.createCertificateProvider(
                config.get().getBindHost(),
                getDataDirectory(),
                msg -> l.at(Level.INFO).log(msg)
        );
        l.at(Level.INFO).log("Using certificate provider: " + tlsConfig.getCertificateProvider());
        return provider.createSSLContext();
    }

    protected void setupAnonymousUser() {
        PermissionsModule.get().addUserToGroup(new UUID(0,0), "ANONYMOUS");
    }

    protected void setupCommands() {
        CommandManager.get().register(new WebServerCommand(this));
    }

    protected void setupAuthStores() throws IOException {
        // TODO: Make implementation configurable somehow?

        var dataDir = getDataDirectory();

        var serviceAccountStore = new JsonPasswordStore(dataDir.resolve("serviceaccounts.json"), getLogger().getSubLogger("ServiceAccountCredentialStore"));
        serviceAccountStore.load();

        var userStore = new JsonPasswordStore(dataDir.resolve("users.json"), getLogger().getSubLogger("UserCredentialStore"));
        userStore.load();

        this.serviceAccountCredentialStore = serviceAccountStore;
        this.serviceAccountCredentialValidator = serviceAccountStore;

        this.userCredentialStore = userStore;
        this.userCredentialValidator = userStore;
    }

    @Override
    protected void start() {
        this.setupAnonymousUser();
    }

    @Override
    protected void shutdown() {
        try {
            this.server.stop();
        } catch (Exception e) {
            getLogger().at(Level.SEVERE).log("Failed to stop WebServer: " + e.getMessage());
        }
    }

    public HandlerBuilder createHandlerBuilder(PluginBase plugin) throws PrefixAlreadyRegisteredException{
        var id = plugin.getIdentifier();
        var prefix = String.format("/%s/%s", id.getGroup().toLowerCase(), id.getName().toLowerCase());

        if (this.hasHandlerFor(prefix)) {
            throw new PrefixAlreadyRegisteredException();
        }

        return new HandlerBuilder(this, prefix);
    }

    private boolean hasHandlerFor(String prefix) {
        var handlers = this.contexts.getHandlers();

        if (handlers == null) {
            return false;
        }

        for (Handler handler : handlers) {
            if (handler instanceof ContextHandler contextHandler) {
                String contextPath = contextHandler.getContextPath();

                if (contextPath != null && (contextPath.equals(prefix) || contextPath.startsWith(prefix + "/") || prefix.startsWith(contextPath + "/"))) {
                    return true;
                }
            }
        }

        return false;
    }

    protected void register(Handler handler, Object... beans) throws Exception {
        if (handler instanceof ContextHandler contextHandler) {
            getLogger().atInfo().log("registered handler at prefix: %s", contextHandler.getContextPath());
        }

        contexts.addHandler(handler);
        for (var o : beans) {
            this.server.addBean(o);
        }
    }

    public UserCredentialStore getUserCredentialStore() {
        return this.userCredentialStore;
    }

    public UserCredentialStore getServiceAccountCredentialStore() {
        return this.serviceAccountCredentialStore;
    }

    public AuthProvider[] getDefaultAuthProviders() {
        var combined = new CombinedCredentialValidator();
        combined.add(this.userCredentialValidator);
        combined.add(this.serviceAccountCredentialValidator);

        return new AuthProvider[]{
            new BasicAuthProvider(combined),
        };
    }

    public UUID createServiceAccount(String name, String password) throws IOException {
        UUID uuid = UUID.randomUUID();

        if (!name.startsWith("serviceaccount.")) {
            name = "serviceaccount." + name;
        }

        try {
            this.serviceAccountCredentialStore.setUserCredential(uuid, name, password);
            PermissionsModule.get().addUserToGroup(uuid, "SERVICE_ACCOUNT");
            return uuid;

        } catch (IOException e) {
            getLogger().at(Level.SEVERE).log("failed to create service account credentials: %s", e.getMessage());
            throw e;
        }
    }

    public void setServiceAccountPassword(String name, String password) throws IOException {
        var uuid = this.serviceAccountCredentialStore.getUUIDByName(name);
        if (uuid == null) {
            throw new IOException("no UUID found for service account: " + name);
        }

        this.serviceAccountCredentialStore.setUserCredential(uuid, password);
    }

    public void setServiceAccountPassword(UUID uuid, String password) throws IOException {
        this.serviceAccountCredentialStore.setUserCredential(uuid, password);
    }

    public void deleteServiceAccount(UUID uuid) throws IOException {
        try {
            this.serviceAccountCredentialStore.deleteUserCredential(uuid);
            var perm = PermissionsModule.get();

            for (PermissionProvider provider : perm.getProviders()) {
                var groups = provider.getGroupsForUser(uuid);

                for (var group : groups) {
                    provider.removeUserFromGroup(uuid, group);
                }

                var permissions = provider.getUserPermissions(uuid);
                provider.removeUserPermissions(uuid, permissions);
            }

        } catch (IOException e) {
            getLogger().at(Level.SEVERE).log("failed to delete service account: %s", e.getMessage());
            throw e;
        }
    }

    public void deleteServiceAccount(String name) throws IOException {
        var uuid = this.serviceAccountCredentialStore.getUUIDByName(name);
        if (uuid == null) {
            return;
        }

        this.deleteServiceAccount(uuid);
    }
}
