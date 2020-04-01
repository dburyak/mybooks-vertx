package dburyak.demo.mybooks.auth.endpoints;

import dburyak.demo.mybooks.auth.service.RefreshTokenNotRegisteredException;
import dburyak.demo.mybooks.auth.service.UserTokenService;
import dburyak.demo.mybooks.web.Endpoint;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.reactivex.ext.web.Route;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.api.validation.HTTPRequestValidationHandler;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.List;

import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.vertx.core.http.HttpMethod.PUT;
import static io.vertx.ext.web.api.validation.ParameterType.UUID;

@Singleton
public class RefreshUserTokenEndpoint implements Endpoint {
    private static final String PATH_PARAM_REFRESH_TOKEN_ID = "refreshTokenId";

    @Inject
    private UserTokenService userTokenService;

    @Override
    public String getPath() {
        return "/user-token/:" + PATH_PARAM_REFRESH_TOKEN_ID;
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
    public List<Handler<RoutingContext>> reqValidators() {
        var hTokenId = HTTPRequestValidationHandler.create()
                .addPathParam(PATH_PARAM_REFRESH_TOKEN_ID, UUID);
        return List.of(hTokenId);
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    @Override
    public List<Handler<RoutingContext>> reqHandlers() {
        Handler<RoutingContext> h = (RoutingContext ctx) -> {
            userTokenService.refreshTokens(ctx.pathParam(PATH_PARAM_REFRESH_TOKEN_ID))
                    .subscribe(newTokensJson -> {
                        ctx.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                                .end(newTokensJson.encode());
                    }, err -> {
                        if (err instanceof RefreshTokenNotRegisteredException) {
                            ctx.fail(NOT_FOUND.code(), err);
                        } else {
                            ctx.fail(err);
                        }
                    });
        };
        return List.of(h);
    }
}
