package dburyak.demo.mybooks.auth.app.endpoints;

import dburyak.demo.mybooks.web.Endpoint;
import io.micronaut.context.annotation.Property;
import io.reactivex.Single;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jwt.JWTOptions;
import io.vertx.ext.web.api.validation.ValidationException;
import io.vertx.reactivex.ext.auth.User;
import io.vertx.reactivex.ext.auth.jwt.JWTAuth;
import io.vertx.reactivex.ext.web.Route;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.api.validation.CustomValidator;
import io.vertx.reactivex.ext.web.api.validation.HTTPRequestValidationHandler;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.ext.web.api.validation.ParameterType.BASE64;
import static io.vertx.ext.web.api.validation.ValidationException.ErrorType.JSON_NOT_PARSABLE;

@Singleton
public class GetUserTokenEndpoint implements Endpoint {
    public static final String PARAM_NAME_DATA = "claims";

    @Property(name = "jwt.issuer")
    private String jwtIssuer;

    @Property(name = "jwt.user-token.access.expires-in-minutes")
    private int jwtUserAccessExpMin;

    @Property(name = "jwt.user-token.refresh.expires-in-minutes")
    private int jwtUserRefreshExpMin;

    @Inject
    private JWTAuth jwtAuth;

    @Inject
    private Base64.Decoder base64Decoder;

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
                    .zip(Single.just(isRequestFromAllowedService(principal)),
                            hasPermissionToGenerateToken(user),
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
                        ctx.put("claimsJson", Json.decodeValue(jsonStr));
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
            var userClaimsJson = ctx.<JsonObject>get("claimsJson");
            Single.zip(
                    Single.fromCallable(() -> jwtAuth.generateToken(userClaimsJson, buildBaseUserJwtOptions()
                            .setExpiresInMinutes(jwtUserAccessExpMin))),
                    Single.fromCallable(() -> jwtAuth.generateToken(userClaimsJson
                                    .put("jti", UUID.randomUUID().toString()),
                            buildBaseUserJwtOptions()
                                    .setAudience(List.of(jwtIssuer))
                                    .setExpiresInMinutes(jwtUserRefreshExpMin))),
                    (accessToken, refreshToken) -> new JsonObject()
                            .put("access-token", accessToken)
                            .put("refresh-token", refreshToken))
                    .subscribe(tokensJson -> {
                        ctx.response().putHeader("content-type", "application/json")
                                .end(tokensJson.encode());
                    }, err -> {
                        ctx.response().setStatusCode(INTERNAL_SERVER_ERROR.code())
                                .end(INTERNAL_SERVER_ERROR.reasonPhrase());
                    });
        };
    }

    private boolean isRequestFromAllowedService(JsonObject principal) {
        // only "user" service can request user-token generation
        var iss = principal.getString("iss");
        return iss != null && iss.startsWith("mybooks.service.user");
    }

    private Single<Boolean> hasPermissionToGenerateToken(User user) {
        return user.rxIsAuthorized(":user-token:generate");
    }

    private JWTOptions buildBaseUserJwtOptions() {
        return new JWTOptions().setIssuer(jwtIssuer);
    }
}
