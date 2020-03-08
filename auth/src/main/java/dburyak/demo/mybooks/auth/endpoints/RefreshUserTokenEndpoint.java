package dburyak.demo.mybooks.auth.endpoints;

import dburyak.demo.mybooks.web.Endpoint;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.reactivex.ext.web.Route;
import io.vertx.reactivex.ext.web.RoutingContext;

import javax.inject.Singleton;

import static io.vertx.core.http.HttpMethod.PUT;

@Singleton
public class RefreshUserTokenEndpoint implements Endpoint {

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
    public Handler<RoutingContext> reqAccessHandler() {
        return (RoutingContext ctx) -> {
            var user = ctx.user();
            var principal = user.principal();
            var refreshTokenId = principal.getString("jti");
        };
    }

    @Override
    public Handler<RoutingContext> reqHandler() {
        return (RoutingContext ctx) -> {
            var bodyJson = ctx.getBodyAsJson();

        };
    }
}
