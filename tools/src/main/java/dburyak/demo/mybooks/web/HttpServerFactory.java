package dburyak.demo.mybooks.web;

import io.micronaut.context.annotation.Factory;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.http.HttpServer;

import javax.inject.Singleton;

@Factory
public class HttpServerFactory {

    @Singleton
    public HttpServer httpServer(Vertx vertx, HttpServerOptions httpServerOptions) {
        return vertx.createHttpServer(httpServerOptions);
    }

    @Singleton
    public HttpServerOptions httpServerOptions() {
        return new HttpServerOptions().setPort(8097);
    }
}
