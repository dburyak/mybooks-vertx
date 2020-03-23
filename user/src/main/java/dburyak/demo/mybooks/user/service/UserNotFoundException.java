package dburyak.demo.mybooks.user.service;

import dburyak.demo.mybooks.user.endpoints.UserLoginEndpoint;
import io.vertx.core.json.JsonObject;

public class UserNotFoundException extends RuntimeException {
    private JsonObject userLoginInfo;

    public UserNotFoundException(JsonObject userLoginInfo) {
        super();
        this.userLoginInfo = withoutPassword(userLoginInfo);
    }

    public UserNotFoundException(String message, JsonObject userLoginInfo) {
        super(message);
        this.userLoginInfo = withoutPassword(userLoginInfo);
    }

    public UserNotFoundException(String message, JsonObject userLoginInfo, Throwable cause) {
        super(message, cause);
        this.userLoginInfo = withoutPassword(userLoginInfo);
    }

    public UserNotFoundException(JsonObject userLoginInfo, Throwable cause) {
        super(cause);
        this.userLoginInfo = withoutPassword(userLoginInfo);
    }

    private static JsonObject withoutPassword(JsonObject userLoginInfo) {
        var res = userLoginInfo.copy();
        res.remove(UserLoginEndpoint.KEY_PASSWORD);
        return res;
    }
}
