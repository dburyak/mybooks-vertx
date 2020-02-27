package dburyak.demo.mybooks.auth.app;

import dburyak.demo.mybooks.MicronautVerticle;
import dburyak.demo.mybooks.MicronautVerticleProducer;
import dburyak.demo.mybooks.web.Endpoints;
import io.vertx.reactivex.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class HttpServerVerticle extends dburyak.demo.mybooks.HttpServerVerticle {
    private static final Logger log = LoggerFactory.getLogger(HttpServerVerticle.class);

    @Inject
    private Endpoints endpoints;

    @Override
    protected void registerHttpHandlers(Router router) {
        super.registerHttpHandlers(router);
        router.get(endpoints.getHealth()).handler(ctx -> {
            var user = ctx.user();
            log.info("user : {}", user);
            log.info("test ....");
            ctx.response().setStatusCode(200).end();
        });
    }

    public static class Producer extends MicronautVerticleProducer {

        @Override
        protected MicronautVerticle doCreateVerticle() {
            return new HttpServerVerticle();
        }
    }
}
