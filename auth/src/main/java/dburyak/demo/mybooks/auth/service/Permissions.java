package dburyak.demo.mybooks.auth.service;

import java.util.Objects;

public enum Permissions {
    USER_TOKEN_GENERATE(":user-token:generate");

    private final String permissionName;

    Permissions(String permissionName) {
        Objects.requireNonNull(permissionName);
        this.permissionName = permissionName;
    }

    @Override
    public String toString() {
        return permissionName;
    }
}
