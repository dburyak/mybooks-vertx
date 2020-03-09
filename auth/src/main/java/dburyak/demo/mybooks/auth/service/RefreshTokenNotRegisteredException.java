package dburyak.demo.mybooks.auth.service;

import dburyak.demo.mybooks.auth.TokenException;
import io.vertx.core.json.JsonObject;

public class RefreshTokenNotRegisteredException extends TokenException {
    private JsonObject refreshToken;

    public RefreshTokenNotRegisteredException() {
        super();
    }

    public RefreshTokenNotRegisteredException(JsonObject refreshToken) {
        super(refreshToken.toString());
        this.refreshToken = refreshToken;
    }

    public RefreshTokenNotRegisteredException(String message, JsonObject refreshToken) {
        super(message + " : " + refreshToken.toString());
        this.refreshToken = refreshToken;
    }

    public RefreshTokenNotRegisteredException(String message) {
        super(message);
    }

    public RefreshTokenNotRegisteredException(String message, Throwable cause) {
        super(message, cause);
    }

    public RefreshTokenNotRegisteredException(Throwable cause) {
        super(cause);
    }

    public JsonObject getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(JsonObject refreshToken) {
        this.refreshToken = refreshToken;
    }
}
