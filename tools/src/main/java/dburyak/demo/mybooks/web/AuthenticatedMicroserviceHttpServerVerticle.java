package dburyak.demo.mybooks.web;

import dburyak.demo.mybooks.AboutVerticle;
import dburyak.demo.mybooks.HealthVerticle;
import dburyak.demo.mybooks.auth.AuthenticationException;
import io.reactivex.Single;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.eventbus.EventBus;
import io.vertx.reactivex.core.eventbus.Message;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.ext.auth.AuthProvider;
import io.vertx.reactivex.ext.auth.User;
import io.vertx.reactivex.ext.web.Route;
import io.vertx.reactivex.ext.web.Router;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.Map;

import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpResponseStatus.SERVICE_UNAVAILABLE;
import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;
import static io.vertx.core.http.HttpHeaders.AUTHORIZATION;
import static io.vertx.core.http.HttpMethod.GET;

/**
 * Defines three endpoints: "about", "health" and "ready".
 */
@Singleton
public abstract class AuthenticatedMicroserviceHttpServerVerticle extends AuthenticatedHttpServerVerticle {

    @Inject
    private AuthProvider authProvider;

    @Inject
    private Endpoints endpoints;

    @Inject
    private Provider<AboutVerticle> aboutVerticle;

    @Inject
    private Provider<HealthVerticle> healthVerticle;

    @Inject
    EventBus eventBus;

    protected final void buildPublicEndpoints(Router router) {
        buildHealth(router.route(GET, endpoints.getHealth()).produces("application/json"));
        buildReady(router.route(GET, endpoints.getReady()).produces("application/json"));
        buildAbout(router.route(GET, endpoints.getAbout()).produces("application/json"));

        doBuildPublicEndpoints(router);
    }

    protected final void buildProtectedEndpoints(Router router) {
        checkNeededVerticles();
        doBuildProtectedEndpoints(router);
    }

    protected void doBuildPublicEndpoints(Router router) {
    }

    protected void doBuildProtectedEndpoints(Router router) {
    }

    protected void buildHealth(Route route) {
        checkNeededVerticles();
        var addr = healthVerticle.get().getHealthAddr();
        buildHealthVerticleReq(route, addr);
    }

    protected void buildReady(Route route) {
        checkNeededVerticles();
        var addr = healthVerticle.get().getReadyAddr();
        buildHealthVerticleReq(route, addr);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    protected void buildAbout(Route route) {
        checkNeededVerticles();
        var verticle = aboutVerticle.get();
        var briefAddr = verticle.getBriefInfoAddr();
        var detailedAddr = verticle.getDetailedInfoAddr();
        route.handler(ctx -> {
            var jwtStr = getAuthJwtStrFromRequest(ctx.request());
            var isAuth = jwtStr != null;
            var doAuth = isAuth
                    ? authProvider.rxAuthenticate(new JsonObject().put("jwt", jwtStr))
                    : Single.just(new User(null));
            doAuth
                    .onErrorResumeNext(err -> Single.error(new AuthenticationException(err.getMessage(), err)))
                    .flatMap(user -> eventBus.rxRequest(isAuth ? detailedAddr : briefAddr, null))
                    .map(Message::body).cast(Map.class)
                    .subscribe(aboutInfo -> {
                        ctx.response().setStatusCode(OK.code())
                                .putHeader("content-type", "application/json")
                                .end(Json.encode(aboutInfo));
                    }, err -> {
                        var isAuthErr = err instanceof AuthenticationException;
                        var status = isAuthErr ? UNAUTHORIZED : INTERNAL_SERVER_ERROR;
                        ctx.response().setStatusCode(status.code())
                                .putHeader("content-type", "text/plain")
                                .end(status.reasonPhrase());
                    });
        });
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void buildHealthVerticleReq(Route route, String addr) {
        route.handler(ctx -> {
            var jwtStr = getAuthJwtStrFromRequest(ctx.request());
            var isAuth = jwtStr != null;
            var doAuth = isAuth
                    ? authProvider.rxAuthenticate(new JsonObject().put("jwt", jwtStr))
                    : Single.just(new User(null));
            doAuth
                    .onErrorResumeNext(err -> Single.error(new AuthenticationException(err.getMessage(), err)))
                    .flatMap(user -> eventBus.rxRequest(addr, null))
                    .map(Message::body).cast(Map.class)
                    .subscribe(healthInfo -> {
                        var isUp = "UP".equals(healthInfo.get("outcome"));
                        var status = isUp ? OK : SERVICE_UNAVAILABLE;
                        var isJson = isAuth && isUp;
                        var respText = (isAuth && isUp) ? Json.encode(healthInfo) : status.reasonPhrase();
                        ctx.response().setStatusCode(status.code())
                                .putHeader("content-type", isJson ? "application/json" : "text/plain")
                                .end(respText);
                    }, err -> {
                        var isAuthErr = err instanceof AuthenticationException;
                        var status = isAuthErr ? UNAUTHORIZED : INTERNAL_SERVER_ERROR;
                        ctx.response().setStatusCode(status.code())
                                .putHeader("content-type", "text/plain")
                                .end(status.reasonPhrase());
                    });
        });
    }

    private void checkNeededVerticles() {
        if (aboutVerticle.get() == null || healthVerticle.get() == null) {
            throw new IllegalStateException("About and Health verticles must be defined in application");
        }
    }

    private String getAuthJwtStrFromRequest(HttpServerRequest req) {
        var authHeader = req.getHeader(AUTHORIZATION);
        String jwtStr = null;
        if (authHeader != null) {
            jwtStr = "";
            var parts = authHeader.split("\\s+");
            if (parts.length == 2) {
                jwtStr = parts[1];
            }
        }
        return jwtStr;
    }
}
