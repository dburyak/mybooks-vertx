package dburyak.demo.mybooks.user.endpoints;

import dburyak.demo.mybooks.user.service.UserService;
import dburyak.demo.mybooks.web.Endpoint;
import io.reactivex.Single;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.api.RequestParameters;
import io.vertx.reactivex.ext.web.Route;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.api.validation.HTTPRequestValidationHandler;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

import static dburyak.demo.mybooks.domain.Permission.LIST_ALL_USERS;
import static dburyak.demo.mybooks.domain.Permission.LIST_NON_SYSTEM_USERS;
import static dburyak.demo.mybooks.domain.Permission.LIST_USERS_OF_SAME_ROLES;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.ext.web.api.validation.ParameterType.INT;

@Singleton
public class UserListEndpoint implements Endpoint {

    @Inject
    private UserService userService;

    @Override
    public String getPath() {
        return "/user";
    }

    @Override
    public HttpMethod getHttpMethod() {
        return GET;
    }

    @Override
    public Route configureRoute(Route route) {
        return route.produces("application/json");
    }

    @SuppressWarnings({"Convert2MethodRef", "ResultOfMethodCallIgnored"})
    @Override
    public List<Handler<RoutingContext>> reqAccessHandlers() {
        return List.of((RoutingContext ctx) -> {
            var u = ctx.user();
            u.rxIsAuthorised(LIST_USERS_OF_SAME_ROLES.toString())
                    .flatMap(canListSameRoles ->
                            canListSameRoles ? Single.just(true) : u.rxIsAuthorised(LIST_NON_SYSTEM_USERS.toString()))
                    .flatMap(canListNonSystem ->
                            canListNonSystem ? Single.just(true) : u.rxIsAuthorised(LIST_ALL_USERS.toString()))
                    .subscribe(canList -> {
                        if (canList) {
                            ctx.next();
                        } else {
                            ctx.fail(FORBIDDEN.code());
                        }
                    }, err -> ctx.fail(err));
        });
    }

    @Override
    public List<Handler<RoutingContext>> reqValidators() {
        return List.of(HTTPRequestValidationHandler.create()
                .addQueryParam("offset", INT, false)
                .addQueryParam("limit", INT, false)
        );
    }

    @Override
    public List<Handler<RoutingContext>> reqHandlers() {
        return List.of((RoutingContext ctx) -> {
            var parsedParams = ctx.<RequestParameters>get("parsedParameters");
            var offsetParsed = parsedParams.queryParameter("offset");
            var u = ctx.user();
            var req = ctx.request();
            var offsetStr = ctx.request().getParam("offset");
            var offset = (offsetStr != null) ? Integer.parseInt(offsetStr) : 0;
//            u.rxIsAuthorized(LIST_ALL_USERS.toString())
//                    .flatMap(canListAll -> {
//                        userService.listAll();
//                    })
            ctx.response().end("done");
        });
    }
}
