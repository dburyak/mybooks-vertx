package dburyak.demo.mybooks.auth;

import io.vertx.core.json.JsonObject;

public class BadUserClaimsException extends TokenException {
    private JsonObject userClaims;

    public BadUserClaimsException() {
        super();
    }

    public BadUserClaimsException(JsonObject userClaims) {
        super(userClaims.toString());
        this.userClaims = userClaims;
    }

    public BadUserClaimsException(String message, JsonObject userClaims) {
        super(message + " : " + userClaims.toString());
        this.userClaims = userClaims;
    }

    public BadUserClaimsException(String message) {
        super(message);
    }

    public BadUserClaimsException(String message, Throwable cause) {
        super(message, cause);
    }

    public BadUserClaimsException(Throwable cause) {
        super(cause);
    }

    public JsonObject getUserClaims() {
        return userClaims;
    }

    public void setUserClaims(JsonObject userClaims) {
        this.userClaims = userClaims;
    }
}
