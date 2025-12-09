package net.nitrado.hytale.plugins.webserver.auth.store;

import java.io.IOException;
import java.util.UUID;

public interface UserCredentialStore {
    void setUserCredential(UUID uuid, String username, String password) throws IOException;
    default void setUserCredential(UUID uuid, String password) throws IOException {
        setUserCredential(uuid, null, password);
    }
    void deleteUserCredential(String username) throws IOException;
    void deleteUserCredential(UUID uuid) throws IOException;
}
