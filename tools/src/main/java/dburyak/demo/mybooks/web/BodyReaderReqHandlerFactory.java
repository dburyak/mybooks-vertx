package dburyak.demo.mybooks.web;

import io.micronaut.context.annotation.Factory;
import io.vertx.reactivex.ext.web.handler.BodyHandler;

import javax.inject.Singleton;

@Factory
public class BodyReaderReqHandlerFactory {

    @Singleton
    public BodyHandler bodyHandler() {
        return BodyHandler.create();
    }
}
