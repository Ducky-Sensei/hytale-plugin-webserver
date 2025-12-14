package net.nitrado.hytale.plugins.webserver.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandUtil;
import net.nitrado.hytale.plugins.webserver.Permissions;
import net.nitrado.hytale.plugins.webserver.auth.store.UserCredentialStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;


public class UserPasswordDeleteCommand extends AbstractCommand {

    private final UserCredentialStore userCredentialStore;

    public UserPasswordDeleteCommand(UserCredentialStore store) {
        super("delete", "Delete a personal password.");

        this.userCredentialStore = store;

    }

    @Nullable
    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
        CommandUtil.requirePermission(context.sender(), Permissions.USERPASSWORD_DELETE);

        try {
            userCredentialStore.deleteUserCredential(context.sender().getUuid());
            context.sendMessage(Message.raw("Password set successfully."));
        } catch (IOException e) {
            context.sendMessage(Message.raw("Password set failed: Could not write to credential store."));
        }

        return CompletableFuture.completedFuture(null);
    }
}
