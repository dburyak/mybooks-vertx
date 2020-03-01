package dburyak.demo.mybooks.auth.app;

import dburyak.demo.mybooks.MicronautVerticle;
import dburyak.demo.mybooks.MicronautVerticleProducer;
import dburyak.demo.mybooks.web.AuthenticatedMicroserviceHttpServerVerticle;
import io.vertx.reactivex.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;

@Singleton
public class HttpServerVerticle extends AuthenticatedMicroserviceHttpServerVerticle {
    private static final Logger log = LoggerFactory.getLogger(HttpServerVerticle.class);

    @Override
    protected void doBuildPublicEndpoints(Router router) {

    }

    @Override
    protected void doBuildProtectedEndpoints(Router router) {

    }

    public static class Producer extends MicronautVerticleProducer {

        @Override
        protected MicronautVerticle doCreateVerticle() {
            return new HttpServerVerticle();
        }
    }
}
