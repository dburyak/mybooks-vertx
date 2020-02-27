package dburyak.demo.mybooks;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.handler.SessionHandler;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public abstract class HttpServerVerticle extends MicronautVerticle {

    @Inject
    protected HttpServer httpServer;

    @Inject
    protected SessionHandler sessionHandler;

    @Override
    protected final Completable doStart() {
        return Single
                .fromCallable(() -> {
                    var router = Router.router(vertx);
                    registerHttpHandlers(router);
                    return router;
                })
                .flatMap(router -> httpServer
                        .requestHandler(router)
                        .rxListen())
                .ignoreElement();
    }

    @Override
    protected final Completable doStop() {
        return httpServer.rxClose();
    }

    protected void registerHttpHandlers(Router router) {
        // set session auth handler first in default implementation
        router.route().handler(sessionHandler);
    }
}
