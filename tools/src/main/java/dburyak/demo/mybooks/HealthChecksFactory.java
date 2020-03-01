package dburyak.demo.mybooks;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Prototype;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.healthchecks.HealthChecks;

@Factory
public class HealthChecksFactory {

    @Prototype
    public HealthChecks healthChecks(Vertx vertx) {
        return HealthChecks.create(vertx);
    }
}
