package dburyak.demo.mybooks.auth.service;

import dburyak.demo.mybooks.auth.TokenException;

public class RefreshTokenNotRegisteredException extends TokenException {
    private String jti;

    public RefreshTokenNotRegisteredException() {
        super();
    }

    public RefreshTokenNotRegisteredException(String message, String jti) {
        super(message + " : " + jti);
        this.jti = jti;
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

    public String getJti() {
        return jti;
    }

    public void setJti(String jti) {
        this.jti = jti;
    }
}
