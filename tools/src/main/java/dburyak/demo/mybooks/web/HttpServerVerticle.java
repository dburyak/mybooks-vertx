package dburyak.demo.mybooks.web;

import dburyak.demo.mybooks.MicronautVerticle;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.ext.web.Router;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public abstract class HttpServerVerticle extends MicronautVerticle {

    @Inject
    protected HttpServer httpServer;

    @Override
    protected final Completable doStart() {
        return Single
                .fromCallable(() -> {
                    var router = Router.router(vertx);
                    buildEndpoints(router);
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

    protected abstract void buildEndpoints(Router router);
}
