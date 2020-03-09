package dburyak.demo.mybooks.web;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.api.validation.ValidationException;
import io.vertx.reactivex.ext.web.Route;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.BodyHandler;

import javax.inject.Inject;
import javax.inject.Singleton;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

/**
 * Defines API endpoint.
 * <p>Path has precedence over RegexPath. So in order to define RegexPath, implementation should explicitly return null
 * for "getPath" and non-null for "getPathRegex".
 * <p>Handlers are set up in the following order:
 * <ul>
 *     <li>"reqAccessHandler" - checks if the user is allowed to perform endpoint action</li>
 *     <li>"reqValidator" - validates request parameters, body, etc.</li>
 *     <li>"reqHandler" - handler to perform the main action of this endpoint</li>
 * </ul>
 */
@Singleton
public interface Endpoint {

    /**
     * Endpoint path.
     *
     * @return endpoint path
     */
    String getPath();

    /**
     * Endpoint regex path. Is used only when {@link #getPath()} returns null.
     *
     * @return path matching regular expression
     */
    default String getRegexPath() {
        return null;
    }

    /**
     * Http method of this endpoint.
     *
     * @return http method
     */
    HttpMethod getHttpMethod();

    /**
     * Defines whether endpoint needs to handle request body.
     *
     * @return whether request body is processed by this endpoint
     */
    default boolean hasReqBody() {
        return false;
    }

    /**
     * Called to configure the route.
     * Mime types of the route may be configured here.
     *
     * @param route route to be configured
     * @return configured route
     */
    default Route configureRoute(Route route) {
        return route;
    }

    /**
     * Build handler for checking authentication of the user: whether the user is allowed to perform the action of this
     * endpoint.
     *
     * @return request handler
     */
    default Handler<RoutingContext> reqAccessHandler() {
        return null;
    }

    /**
     * Build handler for checking whether request params are valid.
     *
     * @return request handler
     */
    default Handler<RoutingContext> reqValidator() {
        return null;
    }

    /**
     * The request handler that performs the main action of this endpoint.
     *
     * @return request handler
     */
    default Handler<RoutingContext> reqHandler() {
        return null;
    }

    /**
     * Mount handlers of this endpoint.
     *
     * @param router web api router
     */
    @Inject
    default void registerEndpoint(Router router, BodyHandler bodyHandler) {
        var reqHandler = reqHandler();
        if (reqHandler == null) {
            return;
        }

        var path = getPath();
        Route route = null;
        if (path != null) {
            route = router.route(getHttpMethod(), path);
        } else {
            var regexPath = getRegexPath();
            if (regexPath != null) {
                route = router.routeWithRegex(getHttpMethod(), regexPath);
            }
        }
        if (route != null) {
            if (hasReqBody()) {
                route.handler(bodyHandler);
            }
            route = configureRoute(route);
            var accessHandler = reqAccessHandler();
            var validator = reqValidator();
            if (accessHandler != null) {
                route.handler(accessHandler);
            }
            if (validator != null) {
                route.handler(validator);
            }
            route.handler(reqHandler);
            route.failureHandler(ctx -> {
                var err = ctx.failure();
                if (err instanceof ValidationException) {
                    var validationErr = (ValidationException) err;
                    var errDescr = "message: " + validationErr.getMessage() + "\n" +
                            "type: " + validationErr.type() + "\n" +
                            "param: " + validationErr.parameterName() + "\n" +
                            "value: " + validationErr.value() + "\n" +
                            "rule: " + validationErr.validationRule();
                    ctx.response()
                            .setChunked(true)
                            .setStatusCode(BAD_REQUEST.code())
                            .setStatusMessage(BAD_REQUEST.reasonPhrase())
                            .write(errDescr);
                }
                ctx.next();
            });
        }
    }
}
