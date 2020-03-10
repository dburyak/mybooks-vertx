package dburyak.demo.mybooks.auth.app;

import dburyak.demo.mybooks.MicronautVerticle;
import dburyak.demo.mybooks.MicronautVerticleProducer;
import io.vertx.reactivex.core.eventbus.EventBus;
import io.vertx.reactivex.ext.healthchecks.HealthChecks;
import io.vertx.reactivex.ext.mongo.MongoClient;

import javax.inject.Inject;
import javax.inject.Singleton;

import static io.vertx.ext.healthchecks.Status.KO;
import static io.vertx.ext.healthchecks.Status.OK;

@Singleton
public class HealthVerticle extends dburyak.demo.mybooks.HealthVerticle {

    @Inject
    private MongoClient mongoClient;

    @Inject
    private EventBus eventBus;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    protected void registerHealthProcedures(HealthChecks healthChecks) {
        healthChecks.register("db/alive", f ->
                mongoClient.rxGetCollections()
                        .subscribe(ignr -> f.complete(OK()), f::fail));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    protected void registerReadyProcedures(HealthChecks readyChecks) {
        readyChecks.register("db/initialized", f ->
                eventBus.<Boolean>rxRequest(DbInitVerticle.getIsInitializedAddr(), null)
                        .subscribe(resp -> f.complete(resp.body() ? OK() : KO()), f::fail));
    }

    public static final class Producer extends MicronautVerticleProducer<Producer> {

        @Override
        protected MicronautVerticle doCreateVerticle() {
            return new HealthVerticle();
        }
    }
}
