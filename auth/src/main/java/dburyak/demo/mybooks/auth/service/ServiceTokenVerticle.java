package dburyak.demo.mybooks.auth.service;

import dburyak.demo.mybooks.MicronautVerticle;
import dburyak.demo.mybooks.MicronautVerticleProducer;
import io.micronaut.context.annotation.Property;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jwt.JWTOptions;
import io.vertx.reactivex.core.eventbus.EventBus;
import io.vertx.reactivex.ext.auth.jwt.JWTAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

import static java.util.concurrent.TimeUnit.SECONDS;

@Singleton
public class ServiceTokenVerticle extends MicronautVerticle {
    private static final Logger log = LoggerFactory.getLogger(ServiceTokenVerticle.class);
    private static final String ADDR_GET_SERVICE_TOKEN = ServiceTokenVerticle.class + ".getServiceToken";

    @Property(name = "jwt.service-token.expires-in-seconds")
    private int jwtServiceExpSec;

    @Inject
    private JWTAuth authProvider;

    @Inject
    private EventBus eventBus;

    private Disposable tokenRefreshJob;
    private String serviceToken;

    public String getServiceTokenAddr() {
        return ADDR_GET_SERVICE_TOKEN;
    }

    @Override
    protected Completable doStart() {
        return Completable
                .fromAction(() -> {
                    serviceToken = generateServiceToken(); // initial value
                    tokenRefreshJob = Observable
                            .interval(jwtServiceExpSec > 2 ? jwtServiceExpSec - 2 : jwtServiceExpSec, SECONDS)
                            .subscribe(
                                    tick -> serviceToken = generateServiceToken(),
                                    err -> log.error("failed to generate service token", err));
                })
                .andThen(Completable.fromAction(() -> {
                    eventBus.consumer(getServiceTokenAddr(), msg -> msg.reply(serviceToken));
                }));
    }

    @Override
    protected Completable doStop() {
        return Completable.fromAction(() -> tokenRefreshJob.dispose());
    }

    private String generateServiceToken() {
        var jwtOpts = new JWTOptions()
                .setExpiresInSeconds(jwtServiceExpSec);
        return authProvider.generateToken(new JsonObject(), jwtOpts);
    }

    public static class Producer extends MicronautVerticleProducer<Producer> {

        @Override
        protected MicronautVerticle doCreateVerticle() {
            return new ServiceTokenVerticle();
        }
    }
}
