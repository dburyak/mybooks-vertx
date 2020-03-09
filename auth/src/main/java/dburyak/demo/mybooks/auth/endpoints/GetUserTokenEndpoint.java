package dburyak.demo.mybooks.auth.endpoints;

import dburyak.demo.mybooks.auth.service.UserTokenService;
import dburyak.demo.mybooks.web.Endpoint;
import io.micronaut.context.ApplicationContext;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.api.validation.ValidationException;
import io.vertx.reactivex.ext.web.Route;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.api.validation.CustomValidator;
import io.vertx.reactivex.ext.web.api.validation.HTTPRequestValidationHandler;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Base64;

import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.ext.web.api.validation.ParameterType.BASE64;
import static io.vertx.ext.web.api.validation.ValidationException.ErrorType.JSON_NOT_PARSABLE;

@Singleton
public class GetUserTokenEndpoint implements Endpoint {
    public static final String PARAM_NAME_CLAIMS = "claims";

    private static final String CTX_KEY_USER_CLAIMS_JSON = "userClaimsJson";

    private boolean isErrReportEnabled;

    @Inject
    private Base64.Decoder base64Decoder;

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
        return GET;
    }

    @Override
    public Route configureRoute(Route route) {
        return route.produces("application/json");
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public Handler<RoutingContext> reqAccessHandler() {
        return (RoutingContext ctx) -> userTokenService.hasPermissionToGenerateToken(ctx.user())
                .subscribe(canAccess -> {
                    if (!canAccess) {
                        ctx.response().setStatusCode(FORBIDDEN.code())
                                .end(FORBIDDEN.reasonPhrase());
                    } else { // has access, handle request further
                        ctx.next();
                    }
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
    }

    @Override
    public Handler<RoutingContext> reqValidator() {
        return HTTPRequestValidationHandler.create()
                .addQueryParam(PARAM_NAME_CLAIMS, BASE64, true)
                .addCustomValidatorFunction(new CustomValidator(ctx -> {
                    try {
                        var dataBase64 = ctx.queryParams().get(PARAM_NAME_CLAIMS);
                        var jsonStr = new String(base64Decoder.decode(dataBase64));
                        ctx.put(CTX_KEY_USER_CLAIMS_JSON, Json.decodeValue(jsonStr));
                    } catch (Exception cause) {
                        var err = new ValidationException(PARAM_NAME_CLAIMS + " must be valid base64 encoded json",
                                JSON_NOT_PARSABLE, cause);
                        err.setParameterName(PARAM_NAME_CLAIMS);
                        err.setValue(ctx.queryParams().get(PARAM_NAME_CLAIMS));
                        throw err;
                    }
                }))
                .addCustomValidatorFunction(new CustomValidator(ctx -> {
                    var userClaims = ctx.<JsonObject>get(CTX_KEY_USER_CLAIMS_JSON);
                    var sub = userClaims.getString(UserTokenService.KEY_SUB);
                    var deviceId = userClaims.getString(UserTokenService.KEY_DEVICE_ID);
                    if (sub == null || sub.isBlank()) {
                        var err = new ValidationException("\"sub\" (user id) must be specified in claims");
                        err.setParameterName(PARAM_NAME_CLAIMS);
                        err.setValue(userClaims.toString());
                        throw err;
                    }
                    if (deviceId == null || deviceId.isBlank()) {
                        var err = new ValidationException("\"device_id\" must be specified in claims");
                        err.setParameterName(PARAM_NAME_CLAIMS);
                        err.setValue(userClaims.toString());
                        throw err;
                    }
                    userClaims.remove(UserTokenService.KEY_JTI);
                    userClaims.remove(UserTokenService.KEY_EXP);
                    userClaims.remove(UserTokenService.KEY_IAT);
                    ctx.put(CTX_KEY_USER_CLAIMS_JSON, userClaims);
                }));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public Handler<RoutingContext> reqHandler() {
        return (RoutingContext ctx) -> {
            var userClaims = ctx.<JsonObject>get(CTX_KEY_USER_CLAIMS_JSON);
            userTokenService.generateTokens(userClaims)
                    .subscribe(tokensJson -> {
                        ctx.response().putHeader("content-type", "application/json")
                                .end(tokensJson.encode());
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
