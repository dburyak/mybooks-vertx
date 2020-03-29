package dburyak.demo.mybooks.web;

import dburyak.demo.mybooks.MicronautVerticle;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Value;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.ext.web.api.validation.ValidationException;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Set;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

@Singleton
public abstract class HttpServerVerticle extends MicronautVerticle {
    private static final Logger log = LoggerFactory.getLogger(HttpServerVerticle.class);

    @Value("${http.err-details-envs:[]}")
    private Set<String> errDetailsEnvs;

    @Inject
    protected HttpServer httpServer;

    @Inject
    private ApplicationContext appCtx;

    private boolean isErrDetailsEnabled;

    @Override
    protected final Completable doStart() {
        return Single
                .fromCallable(() -> {
                    var router = Router.router(vertx);
                    buildEndpoints(router);
                    buildErrHandlers(router);
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

    private void buildErrHandlers(Router router) {
        if (isErrDetailsEnabled) {
            router.route().failureHandler(ctx -> {
                var resp = ctx.response();
                var err = ctx.failure();
                var isMainStatusSet = ctx.statusCode() >= 0;
                var isRespStatusSet = resp.getStatusCode() != OK.code();
                var isRespCodeExplicitlySet = isMainStatusSet || isRespStatusSet;
                if (!isRespCodeExplicitlySet) {
                    if (err instanceof ValidationException) {
                        resp.setStatusCode(BAD_REQUEST.code())
                                .setStatusMessage(BAD_REQUEST.reasonPhrase());
                    } else {
                        resp.setStatusCode(INTERNAL_SERVER_ERROR.code())
                                .setStatusMessage(INTERNAL_SERVER_ERROR.reasonPhrase());
                    }
                } else {
                    if (isMainStatusSet && !isRespStatusSet) {
                        resp.setStatusCode(ctx.statusCode());
                    }
                }
                resp.setChunked(true);
                if (err != null) {
                    var stackTraceStr = new StringWriter();
                    err.printStackTrace(new PrintWriter(stackTraceStr));
                    resp.write(stackTraceStr.toString() + "\n");
                } else {
                    resp.write("err: null\n");
                }
                ctx.next();
            });
        }
    }

    @PostConstruct
    private void init() {
        var currentEnvs = appCtx.getEnvironment().getActiveNames();
        isErrDetailsEnabled = !Collections.disjoint(currentEnvs, errDetailsEnvs);
        if (isErrDetailsEnabled) {
            log.debug("server errors details enabled");
        }
    }
}
