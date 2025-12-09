package net.nitrado.hytale.plugins.webserver.commands;

import com.hypixel.hytale.server.core.command.newcmdsystem.commandtypes.CommandCollectionBase;
import net.nitrado.hytale.plugins.webserver.auth.store.UserCredentialStore;

import javax.annotation.Nonnull;

public class UserPasswordCommand extends CommandCollectionBase {

    public UserPasswordCommand(UserCredentialStore store) {
        super("userpassword", "Manage personal credentials for web access");
        addAliases("userpw");

        addSubCommand(new UserPasswordSetCommand(store));
        addSubCommand(new UserPasswordDeleteCommand(store));
    }


}
