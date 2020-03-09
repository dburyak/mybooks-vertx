package dburyak.demo.mybooks.auth.endpoints;

import dburyak.demo.mybooks.auth.service.UserTokenService;
import dburyak.demo.mybooks.web.Endpoint;
import io.micronaut.context.ApplicationContext;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.api.validation.ValidationException;
import io.vertx.reactivex.ext.web.Route;
import io.vertx.reactivex.ext.web.RoutingContext;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.PrintWriter;
import java.io.StringWriter;

import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.vertx.core.http.HttpMethod.PUT;
import static io.vertx.ext.web.api.validation.ValidationException.ErrorType.JSON_INVALID;

@Singleton
public class RefreshUserTokenEndpoint implements Endpoint {
    private boolean isErrReportEnabled;

    @Inject
    private UserTokenService userTokenService;

    @Inject
    private ApplicationContext appCtx;

    @Override
    public String getPath() {
        return "/user-token";
    }

    @Override
    public HttpMethod getHttpMethod() {
        return PUT;
    }

    @Override
    public Route configureRoute(Route route) {
        return route.produces("application/json");
    }

    @Override
    public Handler<RoutingContext> reqValidator() {
        return (RoutingContext ctx) -> {
            var user = ctx.user();
            var principal = user.principal();
            var jti = principal.getString(UserTokenService.KEY_JTI);
            var sub = principal.getString(UserTokenService.KEY_SUB);
            var deviceId = principal.getString(UserTokenService.KEY_DEVICE_ID);
            if (jti == null || sub == null || deviceId == null || jti.isBlank() || sub.isBlank()
                    || deviceId.isBlank()) {
                var err = new ValidationException("bad refresh token", JSON_INVALID);
                err.setParameterName("Authorization header");
                throw err;
            }
            ctx.next();
        };
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public Handler<RoutingContext> reqHandler() {
        return (RoutingContext ctx) -> {
            userTokenService.refreshTokens(ctx.user().principal())
                    .subscribe(newTokensJson -> {
                        ctx.response().putHeader("content-type", "application/json")
                                .end(newTokensJson.encode());
                    }, err -> {
                        var resp = ctx.response();
                        resp.setStatusCode(INTERNAL_SERVER_ERROR.code());
                        if (isErrReportEnabled) {
                            resp.setChunked(true);
                            resp.write("err type: " + err.getClass().getCanonicalName() + "\n");
                            resp.write("err msg: " + err.getMessage() + "\n");
                            resp.write(err.toString() + "\n");
                            var stackTraceStr = new StringWriter();
                            err.printStackTrace(new PrintWriter(stackTraceStr));
                            resp.write(stackTraceStr.toString() + "\n");
                        }
                        resp.end(INTERNAL_SERVER_ERROR.reasonPhrase());
                    });
        };
    }

    @PostConstruct
    private void init() {
        var envs = appCtx.getEnvironment().getActiveNames();
        var isProd = envs.contains("prod");
        var isDev = envs.contains("dev");
        var isTest = envs.contains("test");
        isErrReportEnabled = !isProd && (isDev || isTest);
    }
}