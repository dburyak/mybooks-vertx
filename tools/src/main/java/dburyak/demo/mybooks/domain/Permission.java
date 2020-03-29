package dburyak.demo.mybooks.domain;

import java.util.Arrays;
import java.util.Objects;

public enum Permission {
    USER_TOKEN_GENERATE(":user-token:generate"),
    LIST_USERS_OF_SAME_ROLES(":user:list:same-roles"),
    LIST_NON_SYSTEM_USERS(":user:list:non-system"),
    LIST_ALL_USERS(":user:list:all")
    ;

    private String permissionName;

    Permission(String permissionName) {
        Objects.requireNonNull(permissionName);
        this.permissionName = permissionName;
    }

    public String getPermissionName() {
        return permissionName;
    }

    public static Permission fromPermissionName(String permissionName) {
        return Arrays.stream(values())
                .filter(p -> p.permissionName.equals(permissionName))
                .findFirst()
                .orElseThrow();
    }


    @Override
    public String toString() {
        return permissionName;
    }
}
