package dburyak.demo.mybooks.web;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Prototype;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.http.HttpServer;

import javax.inject.Singleton;

@Factory
public class HttpServerFactory {

    @Property(name = "http.port")
    int httpServerPort;

    @Singleton
    public HttpServer httpServer(Vertx vertx, HttpServerOptions httpServerOptions) {
        return vertx.createHttpServer(httpServerOptions);
    }

    @Prototype
    public HttpServerOptions httpServerOptions() {
        return new HttpServerOptions().setPort(httpServerPort);
    }
}
