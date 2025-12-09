package net.nitrado.hytale.plugins.webserver.commands;

import com.hypixel.hytale.server.core.command.newcmdsystem.commandtypes.CommandCollectionBase;
import net.nitrado.hytale.plugins.webserver.WebServer;

import javax.annotation.Nonnull;

public class WebServerCommand extends CommandCollectionBase {

    public WebServerCommand(WebServer webServer) {
        super("webserver", "Manage webserver-related configuration, such as user credentials and service accounts");
        addAliases("web");

        addSubCommand(new UserPasswordCommand(webServer.getUserCredentialStore()));
    }
}
