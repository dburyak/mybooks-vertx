package dburyak.demo.mybooks.web;

import javax.inject.Singleton;

@Singleton
public class RequestCtxKeys {
    private static final String KEY_IS_AUTHENTICATED = "authenticated";

    public String getIsAuthenticated() {
        return KEY_IS_AUTHENTICATED;
    }
}
