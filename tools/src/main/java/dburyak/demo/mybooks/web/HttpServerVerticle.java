package dburyak.demo.mybooks.web;

import dburyak.demo.mybooks.MicronautVerticle;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Value;
import io.reactivex.Completable;
import io.reactivex.Single;
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
                resp.setChunked(true);
                resp.write("err type: " + err.getClass().getCanonicalName() + "\n");
                var stackTraceStr = new StringWriter();
                err.printStackTrace(new PrintWriter(stackTraceStr));
                resp.write(stackTraceStr.toString() + "\n");
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
