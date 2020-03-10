package dburyak.demo.mybooks.auth.app;

import dburyak.demo.mybooks.MicronautVerticle;
import dburyak.demo.mybooks.MicronautVerticleProducer;
import dburyak.demo.mybooks.auth.endpoints.GetUserTokenEndpoint;
import dburyak.demo.mybooks.auth.endpoints.RefreshUserTokenEndpoint;
import dburyak.demo.mybooks.web.AuthenticatedMicroserviceHttpServerVerticle;
import io.vertx.reactivex.ext.web.Router;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class HttpServerVerticle extends AuthenticatedMicroserviceHttpServerVerticle {

    @Inject
    private GetUserTokenEndpoint getUserTokenEndpoint;

    @Inject
    private RefreshUserTokenEndpoint refreshUserTokenEndpoint;

    @Override
    protected void doBuildPublicEndpoints(Router router) {
        refreshUserTokenEndpoint.registerEndpoint(router, null);
    }

    @Override
    protected void doBuildProtectedEndpoints(Router router) {
        getUserTokenEndpoint.registerEndpoint(router, null);
    }

    public static class Producer extends MicronautVerticleProducer<Producer> {

        @Override
        protected MicronautVerticle doCreateVerticle() {
            return new HttpServerVerticle();
        }
    }
}
