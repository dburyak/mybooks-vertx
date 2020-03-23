package dburyak.demo.mybooks;

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
public abstract class ServiceTokenVerticle extends MicronautVerticle {
    private static final Logger log = LoggerFactory.getLogger(ServiceTokenVerticle.class);
    private static final int REFRESH_IN_SECONDS_BEFORE_EXPIRATION = 3;
    protected static final String ADDR_GET_SERVICE_TOKEN_SUFFIX = ".getServiceToken";

    @Property(name = "jwt.service-token.expires-in-seconds")
    private int jwtServiceExpSec;

    @Property(name = "jwt.issuer")
    private String issuer;

    @Inject
    private JWTAuth jwtAuth;

    @Inject
    private EventBus eventBus;

    private Disposable tokenRefreshJob;
    private String serviceToken;

    public String getServiceTokenAddr() {
        return getClass() + ADDR_GET_SERVICE_TOKEN_SUFFIX;
    }

    @Override
    protected Completable doStart() {
        return Completable
                .fromAction(() -> {
                    serviceToken = generateServiceToken(); // initial value
                    tokenRefreshJob = Observable
                            .interval(jwtServiceExpSec > REFRESH_IN_SECONDS_BEFORE_EXPIRATION
                                    ? jwtServiceExpSec - REFRESH_IN_SECONDS_BEFORE_EXPIRATION
                                    : jwtServiceExpSec - 1, SECONDS)
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

    protected void configureJwtOptions(JWTOptions jwtOptions) {
    }

    private String generateServiceToken() {
        var jwtOpts = new JWTOptions()
                .setIssuer(issuer)
                .setSubject(issuer);
        configureJwtOptions(jwtOpts);
        jwtOpts.setExpiresInSeconds(jwtServiceExpSec);
        return jwtAuth.generateToken(new JsonObject(), jwtOpts);
    }
}
