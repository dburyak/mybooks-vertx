package dburyak.demo.mybooks.user.domain;

import dburyak.demo.mybooks.domain.DomainObject;
import dburyak.demo.mybooks.domain.Permission;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static dburyak.demo.mybooks.dal.MongoUtil.KEY_DB_ID;

public class Role implements DomainObject<Role> {
    public static final String KEY_NAME = "name";
    public static final String KEY_PERMISSIONS = "permissions";
    public static final String KEY_IS_SYSTEM = "isSystem";

    private String dbId;
    private String name;
    private boolean isSystem;
    private Set<Permission> permissions;

    public Role() {
    }

    public Role(JsonObject roleJson) {
        setAllFromJson(roleJson);
    }

    @Override
    public Role setAllFromJson(JsonObject json) {
        this.dbId = json.getString(KEY_DB_ID);
        this.name = json.getString(KEY_NAME);
        this.isSystem = json.getBoolean(KEY_IS_SYSTEM, false);
        var permissionsJson = json.getJsonArray(KEY_PERMISSIONS);
        if (permissionsJson != null && !permissionsJson.isEmpty()) {
            this.permissions = permissionsJson.stream()
                    .map(n -> Permission.fromPermissionName((String) n))
                    .collect(Collectors.toSet());;
        }
        return this;
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public JsonObject toJson() {
        var json = new JsonObject()
                .put(KEY_NAME, name)
                .put(KEY_IS_SYSTEM, isSystem);
        if (dbId != null && !dbId.isEmpty()) {
            json.put(KEY_DB_ID, dbId);
        }
        if (permissions != null && !permissions.isEmpty()) {
            json.put(KEY_PERMISSIONS, new JsonArray(permissions.stream()
                    .map(Permission::getPermissionName)
                    .collect(Collectors.toList())));
        }
        return json;
    }

    @Override
    public String getDbId() {
        return dbId;
    }

    @Override
    public void setDbId(String newDbId) {
        Objects.requireNonNull(newDbId);
        dbId = newDbId;
    }

    public Role withDbId(String newDbId) {
        setDbId(newDbId);
        return this;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    public Role withPermissions(Set<Permission> permissions) {
        setPermissions(permissions);
        return this;
    }

    public void setPermissionsByNames(Set<String> permissionNames) {
        this.permissions = permissionNames.stream()
                .map(Permission::fromPermissionName)
                .collect(Collectors.toSet());;
    }

    public Role withPermissionsByName(Set<String> permissionNames) {
        setPermissionsByNames(permissionNames);
        return this;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Role withName(String name) {
        setName(name);
        return this;
    }

    public boolean isSystem() {
        return isSystem;
    }

    public void setSystem(boolean system) {
        isSystem = system;
    }

    public Role withSystem(boolean isSystem) {
        setSystem(isSystem);
        return this;
    }

    @Override
    public String toString() {
        return name;
    }
}
