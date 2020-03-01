package dburyak.demo.mybooks.web;

import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.handler.AuthHandler;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public abstract class AuthenticatedHttpServerVerticle extends HttpServerVerticle {

    @Inject
    private AuthHandler authHandler;

    @Override
    protected final void buildEndpoints(Router router) {
        buildPublicEndpoints(router);
        router.route().handler(authHandler);
        buildProtectedEndpoints(router);
    }

    protected abstract void buildPublicEndpoints(Router router);

    protected abstract void buildProtectedEndpoints(Router router);
}
