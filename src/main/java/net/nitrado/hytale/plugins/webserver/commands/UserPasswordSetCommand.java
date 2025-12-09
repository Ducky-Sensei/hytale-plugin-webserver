package net.nitrado.hytale.plugins.webserver.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.CommandUtil;
import com.hypixel.hytale.server.core.command.newcmdsystem.AbstractCommand;
import com.hypixel.hytale.server.core.command.newcmdsystem.CommandContext;
import com.hypixel.hytale.server.core.command.newcmdsystem.argument.RequiredArg;
import com.hypixel.hytale.server.core.command.newcmdsystem.values.premade.ArgTypes;
import net.nitrado.hytale.plugins.webserver.auth.store.UserCredentialStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;


public class UserPasswordSetCommand extends AbstractCommand {

    private final UserCredentialStore userCredentialStore;
    private final RequiredArg<String> passwordArg = withRequiredArg("password", "The new password", ArgTypes.STRING);

    public UserPasswordSetCommand(UserCredentialStore store) {
        super("set", "Set a personal password to log into the server's web apps.");

        this.userCredentialStore = store;

    }

    @Nullable
    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
        final var password = passwordArg.get(context);

        CommandUtil.requirePermission(context.sender(), "nitrado.webserver.userpassword.set");

        try {
            userCredentialStore.setUserCredential(context.sender().getUuid(), context.sender().getDisplayName(), password);
            context.sendMessage(Message.raw("Password set successfully."));
        } catch (IOException e) {
            context.sendMessage(Message.raw("Password set failed: Could not write to credential store."));
        }

        return CompletableFuture.completedFuture(null);
    }
}
