package dburyak.demo.mybooks.user.service;

import io.vertx.core.json.JsonObject;

import static dburyak.demo.mybooks.user.endpoints.UserLoginEndpoint.KEY_PASSWORD;

public class BadUserLoginInfoException extends RuntimeException {
    private JsonObject userLoginInfo;

    public BadUserLoginInfoException(JsonObject userLoginInfo) {
        super();
        this.userLoginInfo = withClearedPassword(userLoginInfo);
    }

    public BadUserLoginInfoException(String message, JsonObject userLoginInfo) {
        super(message);
        this.userLoginInfo = withClearedPassword(userLoginInfo);
    }

    public BadUserLoginInfoException(String message, JsonObject userLoginInfo, Throwable cause) {
        super(message, cause);
        this.userLoginInfo = withClearedPassword(userLoginInfo);
    }

    public BadUserLoginInfoException(JsonObject userLoginInfo, Throwable cause) {
        super(cause);
        this.userLoginInfo = withClearedPassword(userLoginInfo);
    }

    private static JsonObject withClearedPassword(JsonObject userLoginInfo) {
        var res = userLoginInfo.copy();
        var password = userLoginInfo.getString(KEY_PASSWORD);
        if (password == null) {
            res.putNull(KEY_PASSWORD);
        } else if (password.isEmpty()) {
            res.put(KEY_PASSWORD, "EMPTY");
        } else {
            res.put(KEY_PASSWORD, "********");
        }
        return res;
    }
}
