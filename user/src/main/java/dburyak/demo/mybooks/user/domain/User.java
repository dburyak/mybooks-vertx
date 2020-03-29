package dburyak.demo.mybooks.user.domain;

import dburyak.demo.mybooks.domain.DomainObject;
import dburyak.demo.mybooks.domain.Permission;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static dburyak.demo.mybooks.dal.MongoUtil.KEY_DB_ID;

public class User implements DomainObject<User> {
    public static final String KEY_USER_ID = "user_id";
    public static final String KEY_LOGIN = "login";
    public static final String KEY_EMAIL = "email";
    public static final String KEY_PASSWORD_HASH = "password_hash";
    public static final String KEY_EXPLICIT_PERMISSIONS = "explicit_permissions";
    public static final String KEY_ROLES = "roles";

    /**
     * Db id.
     * Used only to identify records within the database.
     */
    private String dbId;

    /**
     * Global user identifier.
     * Used to identify user globally.
     */
    private String userId;

    private String login;
    private String email;
    private String passwordHash;
    private Set<String> roles;
    private Set<Permission> explicitPermissions;

    public User() {
    }

    public User(JsonObject userJson) {
        setAllFromJson(userJson);
    }

    public User(User copyFrom) {
        this.dbId = copyFrom.getDbId();
        this.userId = copyFrom.getUserId();
        this.login = copyFrom.getLogin();
        this.email = copyFrom.getEmail();
        this.passwordHash = copyFrom.getPasswordHash();
        this.explicitPermissions = copyFrom.getExplicitPermissions();
        this.roles = copyFrom.getRoles();
    }

    public static User copy(User copyFrom) {
        return new User(copyFrom);
    }

    public User copy() {
        return new User(this);
    }

    @Override
    public User setAllFromJson(JsonObject userJson) {
        dbId = userJson.getString(KEY_DB_ID);
        userId = userJson.getString(KEY_USER_ID);
        login = userJson.getString(KEY_LOGIN);
        email = userJson.getString(KEY_EMAIL);
        passwordHash = userJson.getString(KEY_PASSWORD_HASH);
        var explicitPermissionsJson = userJson.getJsonArray(KEY_EXPLICIT_PERMISSIONS);
        if (explicitPermissionsJson != null) {
            explicitPermissions = explicitPermissionsJson.stream()
                    .map(p -> Permission.fromPermissionName((String) p))
                    .collect(Collectors.toSet());
        } else {
            explicitPermissions = Collections.emptySet();
        }
        var rolesJson = userJson.getJsonArray(KEY_ROLES);
        if (rolesJson != null) {
            roles = rolesJson.stream()
                    .map(r -> (String) r)
                    .collect(Collectors.toSet());
        }
        return this;
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public JsonObject toJson() {
        var json = new JsonObject()
                .put(KEY_USER_ID, userId)
                .put(KEY_LOGIN, login)
                .put(KEY_EMAIL, email)
                .put(KEY_PASSWORD_HASH, passwordHash);
        if (dbId != null && !dbId.isEmpty()) {
            json.put(KEY_DB_ID, dbId);
        }
        if (explicitPermissions != null && !explicitPermissions.isEmpty()) {
            json.put(KEY_EXPLICIT_PERMISSIONS, new JsonArray(explicitPermissions.stream()
                    .map(Permission::getPermissionName)
                    .collect(Collectors.toList())));
        }
        if (roles != null && !roles.isEmpty()) {
            json.put(KEY_ROLES, new JsonArray(new ArrayList<>(roles)));
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

    public User withDbId(String newDbId) {
        setDbId(newDbId);
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        Objects.requireNonNull(userId);
        this.userId = userId;
    }

    public User withUserId(String userId) {
        setUserId(userId);
        return this;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public User withLogin(String login) {
        setLogin(login);
        return this;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public User withEmail(String email) {
        setEmail(email);
        return this;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public User withPasswordHash(String passwordHash) {
        setPasswordHash(passwordHash);
        return this;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public User withRoles(Set<String> roles) {
        setRoles(roles);
        return this;
    }

    public Set<Permission> getExplicitPermissions() {
        return explicitPermissions;
    }

    public void setExplicitPermissions(Set<Permission> explicitPermissions) {
        this.explicitPermissions = explicitPermissions;
    }

    public User withExplicitPermissions(Set<Permission> explicitPermissions) {
        setExplicitPermissions(explicitPermissions);
        return this;
    }

    public void setExplicitPermissionsByNames(Set<String> explicitPermissionNames) {
        this.explicitPermissions = explicitPermissionNames.stream()
                .map(Permission::fromPermissionName)
                .collect(Collectors.toSet()); ;
    }

    public User withExplicitPermissionsByNames(Set<String> explicitPermissionNames) {
        setExplicitPermissionsByNames(explicitPermissionNames);
        return this;
    }

    @Override
    public String toString() {
        return toJson().toString();
    }
}
