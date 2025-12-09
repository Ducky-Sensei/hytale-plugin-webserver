package net.nitrado.hytale.plugins.webserver.auth;

import com.hypixel.hytale.server.core.permissions.PermissionHolder;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;

import javax.annotation.Nonnull;
import java.security.Principal;
import java.util.UUID;

public class HytaleUserPrincipal implements Principal, PermissionHolder {

    protected UUID uuid;

    public HytaleUserPrincipal(UUID uuid) {
        this.uuid = uuid;
    }

    protected UUID getUuid() {
        return uuid;
    }

    @Override
    public String getName() {
        return this.uuid.toString();
    }

    @Override
    public boolean hasPermission(@Nonnull String s) {
        return PermissionsModule.get().hasPermission(uuid, s);
    }

    @Override
    public boolean hasPermission(@Nonnull String s, boolean b) {
        return PermissionsModule.get().hasPermission(uuid, s, b);
    }
}
