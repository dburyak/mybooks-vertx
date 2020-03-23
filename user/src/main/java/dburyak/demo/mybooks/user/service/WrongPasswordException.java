package dburyak.demo.mybooks.user.service;

import dburyak.demo.mybooks.user.endpoints.UserLoginEndpoint;
import io.vertx.core.json.JsonObject;

public class WrongPasswordException extends RuntimeException {
    private JsonObject userLoginInfo;

    public WrongPasswordException(JsonObject userLoginInfo) {
        super();
        this.userLoginInfo = withoutPassword(userLoginInfo);
    }

    public WrongPasswordException(String message, JsonObject userLoginInfo) {
        super(message);
        this.userLoginInfo = withoutPassword(userLoginInfo);
    }

    public WrongPasswordException(String message, JsonObject userLoginInfo, Throwable cause) {
        super(message, cause);
        this.userLoginInfo = withoutPassword(userLoginInfo);
    }

    public WrongPasswordException(JsonObject userLoginInfo, Throwable cause) {
        super(cause);
        this.userLoginInfo = withoutPassword(userLoginInfo);
    }

    private static JsonObject withoutPassword(JsonObject userLoginInfo) {
        var res = userLoginInfo.copy();
        res.remove(UserLoginEndpoint.KEY_PASSWORD);
        return res;
    }
}
