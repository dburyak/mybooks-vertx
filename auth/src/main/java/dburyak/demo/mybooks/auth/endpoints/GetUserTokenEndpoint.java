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
    public static final String PARAM_NAME_DATA = "claims";

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
                .addQueryParam(PARAM_NAME_DATA, BASE64, true)
                .addCustomValidatorFunction(new CustomValidator(ctx -> {
                    try {
                        var dataBase64 = ctx.queryParams().get(PARAM_NAME_DATA);
                        var jsonStr = new String(base64Decoder.decode(dataBase64));
                        ctx.put("userClaimsJson", Json.decodeValue(jsonStr));
                    } catch (Exception cause) {
                        var err = new ValidationException(PARAM_NAME_DATA + " must be valid base64 encoded json",
                                JSON_NOT_PARSABLE, cause);
                        err.setParameterName(PARAM_NAME_DATA);
                        err.setValue(ctx.queryParams().get(PARAM_NAME_DATA));
                        throw err;
                    }
                }));
    }

    @Override
    public Handler<RoutingContext> reqHandler() {
        return (RoutingContext ctx) -> {
            var userClaimsJson = ctx.<JsonObject>get("userClaimsJson");
            Single.zip(
                    Single.fromCallable(() -> userTokenService.generateAccessToken(userClaimsJson)),
                    Single.fromCallable(() -> userTokenService.generateRefreshToken(userClaimsJson)),
                    (accessToken, refreshToken) -> new JsonObject()
                            .put("access_token", accessToken)
                            .put("refresh_token", refreshToken))
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
