package dburyak.demo.mybooks.auth.endpoints;

import dburyak.demo.mybooks.auth.service.UserTokenService;
import dburyak.demo.mybooks.web.Endpoint;
import io.reactivex.Single;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.api.validation.ValidationException;
import io.vertx.reactivex.ext.web.Route;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.api.validation.CustomValidator;
import io.vertx.reactivex.ext.web.api.validation.HTTPRequestValidationHandler;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Base64;

import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.ext.web.api.validation.ParameterType.BASE64;
import static io.vertx.ext.web.api.validation.ValidationException.ErrorType.JSON_NOT_PARSABLE;

@Singleton
public class GetUserTokenEndpoint implements Endpoint {
    private static final String CTX_KEY_USER_CLAIMS_JSON = "userClaimsJson";

    public static final String PARAM_NAME_CLAIMS = "claims";

    @Inject
    private Base64.Decoder base64Decoder;

    @Inject
    private UserTokenService userTokenService;

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

    @Override
    public Handler<RoutingContext> reqAccessHandler() {
        return (RoutingContext ctx) -> {
            var user = ctx.user();
            var principal = user.principal();
            Single
                    .zip(Single.just(userTokenService.isRequestFromAllowedService(principal)),
                            userTokenService.hasPermissionToGenerateToken(user),
                            (allowedService, canGenerate) -> allowedService && canGenerate)
                    .subscribe(canAccess -> {
                        if (!canAccess) {
                            ctx.response().setStatusCode(FORBIDDEN.code())
                                    .end(FORBIDDEN.reasonPhrase());
                        } else { // has access, handle request further
                            ctx.next();
                        }
                    }, err -> {
                        ctx.response().setStatusCode(INTERNAL_SERVER_ERROR.code())
                                .end(INTERNAL_SERVER_ERROR.reasonPhrase());
                    });
        };
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
                        var err = new ValidationException("\"deviceId\" must be specified in claims");
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

    @Override
    public Handler<RoutingContext> reqHandler() {
        return (RoutingContext ctx) -> {
            var userClaims = ctx.<JsonObject>get(CTX_KEY_USER_CLAIMS_JSON);
            userTokenService.generateTokens(userClaims)
                    .subscribe(tokensJson -> {
                        ctx.response().putHeader("content-type", "application/json")
                                .end(tokensJson.encode());
                    }, err -> {
                        ctx.response().setStatusCode(INTERNAL_SERVER_ERROR.code())
                                .end(INTERNAL_SERVER_ERROR.reasonPhrase());
                    });
        };
    }
}
