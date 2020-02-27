package dburyak.demo.mybooks.web;

import io.micronaut.context.annotation.Factory;
import io.vertx.reactivex.ext.auth.AuthProvider;
import io.vertx.reactivex.ext.web.handler.SessionHandler;
import io.vertx.reactivex.ext.web.sstore.SessionStore;

import javax.inject.Singleton;

@Factory
public class SessionHandlerFactory {

    @Singleton
    public SessionHandler authSessionHandler(SessionStore sessionStore, AuthProvider authProvider) {
        return SessionHandler.create(sessionStore).setAuthProvider(authProvider);
    }
}
