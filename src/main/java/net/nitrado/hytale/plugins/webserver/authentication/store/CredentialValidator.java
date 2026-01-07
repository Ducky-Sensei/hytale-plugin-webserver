package net.nitrado.hytale.plugins.webserver.authentication.store;

import java.util.UUID;

public interface CredentialValidator {
    public boolean hasUser(String username);
    public boolean hasUser(UUID uuid);
    public UUID validateCredential(String username, String credential);
    public UUID validateCredential(UUID uuid, String credential);

}