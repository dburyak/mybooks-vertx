package dburyak.demo.mybooks;

import dburyak.demo.mybooks.di.AppBean;
import io.micronaut.context.annotation.Factory;
import io.vertx.reactivex.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;

@AppBean
@Factory
public class VertxFactory {
    private static final Logger log = LoggerFactory.getLogger(VertxFactory.class);

    @Singleton
    @AppBean
    public Vertx vertx() {
        var vertx = Vertx.vertx();
        log.info("create vertx : {}", vertx);
        return vertx;
    }
}

