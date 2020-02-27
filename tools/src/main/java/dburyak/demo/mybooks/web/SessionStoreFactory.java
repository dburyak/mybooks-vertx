package dburyak.demo.mybooks.web;

import io.micronaut.context.annotation.Factory;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.sstore.LocalSessionStore;
import io.vertx.reactivex.ext.web.sstore.SessionStore;

import javax.inject.Singleton;

@Factory
public class SessionStoreFactory {

    @Singleton
    public SessionStore localSessionStore(Vertx vertx) {
        return LocalSessionStore.create(vertx);
    }
}
