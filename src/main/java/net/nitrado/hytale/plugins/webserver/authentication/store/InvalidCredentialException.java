package net.nitrado.hytale.plugins.webserver.authentication.store;

public class InvalidCredentialException extends RuntimeException {
    public InvalidCredentialException(String message) {
        super(message);
    }
}
