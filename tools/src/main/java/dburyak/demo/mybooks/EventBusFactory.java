package dburyak.demo.mybooks;

import io.micronaut.context.annotation.Factory;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.eventbus.EventBus;

import javax.inject.Singleton;

@Factory
public class EventBusFactory {

    @Singleton
    public EventBus eventBus(Vertx vertx) {
        // rx.EventBus is not thread safe, but the wrapped core.EventBus is thread safe,
        // so per-verticle singleton thin wrapper should be used that wraps single-in-app thread safe instance
        return EventBus.newInstance(vertx.getDelegate().eventBus());
    }
}
