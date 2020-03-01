package dburyak.demo.mybooks;

import dburyak.demo.mybooks.di.VerticleBean;
import dburyak.demo.mybooks.di.Vertx;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Primary;
import io.reactivex.Completable;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;

import static io.micronaut.inject.qualifiers.Qualifiers.byQualifiers;
import static io.micronaut.inject.qualifiers.Qualifiers.byStereotype;

@Singleton
@Vertx
public class MicronautVerticle extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(MicronautVerticle.class);

    private volatile Context vertxContext;
    protected volatile ApplicationContext verticleBeanCtx;

    @Override
    public final Completable rxStart() {
        vertxContext = new Context(super.context);
        return Completable
                .fromAction(() -> {
                    log.info("starting verticle: {}", this);
                    log.info("starting verticle bean context: verticle={}, verticleCtx={}", this, verticleBeanCtx);
                    verticleBeanCtx.registerSingleton(ApplicationContext.class, verticleBeanCtx,
                            byQualifiers(byStereotype(Primary.class), byStereotype(VerticleBean.class)));
                    verticleBeanCtx.registerSingleton(this);
                    verticleBeanCtx.refreshBean(verticleBeanCtx.findBeanRegistration(verticleBeanCtx).orElseThrow()
                            .getIdentifier());
                    verticleBeanCtx.refreshBean(verticleBeanCtx.findBeanRegistration(this).orElseThrow()
                            .getIdentifier());
                })
                .andThen(Completable.defer(this::doStart))
                .doOnComplete(() -> log.info("verticle started: {}", this))
                .doOnError(e -> log.error("failed to start verticle: {}", this, e));
    }

    @Override
    public final Completable rxStop() {
        return Completable
                .fromAction(() -> log.info("stopping verticle: {}", this))
                .andThen(Completable.defer(this::doStop))
                .doOnComplete(() -> log.info("verticle stopped: {}", this))
                .doOnError(e -> log.error("failed to stop verticle: {}", this, e));
    }

    protected Completable doStart() {
        return Completable.complete();
    }

    protected Completable doStop() {
        return Completable.complete();
    }

    public final Context getVertxContext() {
        return vertxContext;
    }
}
