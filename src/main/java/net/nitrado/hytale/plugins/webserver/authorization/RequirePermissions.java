package net.nitrado.hytale.plugins.webserver.authorization;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(RequirePermissionsRepeated.class)
public @interface RequirePermissions {
    String[] value();              // required permissions
    Mode mode() default Mode.ALL;  // ALL = must have all, ANY = must have at least one

    enum Mode { ALL, ANY }
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface RequirePermissionsRepeated {
    RequirePermissions[] value();
}