package dburyak.demo.mybooks.user.endpoints;

import dburyak.demo.mybooks.user.service.UserService;
import dburyak.demo.mybooks.util.RegexUtil;
import dburyak.demo.mybooks.web.Endpoint;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.api.RequestParameters;
import io.vertx.ext.web.api.validation.ValidationException;
import io.vertx.reactivex.ext.web.Route;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.api.validation.HTTPRequestValidationHandler;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.ext.web.api.validation.ValidationException.ErrorType.JSON_INVALID;

@Singleton
public class UserLoginEndpoint implements Endpoint {
    private static final String KEY_CTX_BODY_JSON = "bodyJson";

    public static final String KEY_USER_ID = "user_id";
    public static final String KEY_LOGIN = "login";
    public static final String KEY_EMAIL = "email";
    public static final String KEY_PASSWORD = "password";
    public static final String KEY_DEVICE_ID = "device_id";

    @Inject
    private RegexUtil regexUtil;

    @Inject
    private UserService userService;

    @Override
    public String getPath() {
        return "/user/login";
    }

    @Override
    public HttpMethod getHttpMethod() {
        return POST;
    }

    @Override
    public Route configureRoute(Route route) {
        return route.consumes("application/json")
                .produces("application/json");
    }

    @Override
    public boolean hasReqBody() {
        return true;
    }

    @Override
    public List<Handler<RoutingContext>> reqValidators() {
        var hJsonSchema = HTTPRequestValidationHandler.create()
                .addJsonBodySchema(getJsonBodySchema());
        Handler<RoutingContext> hJsonUserId = (RoutingContext ctx) -> {
            var bodyJson = ctx.<RequestParameters>get("parsedParameters").body().getJsonObject();
            if (bodyJson == null) {
                var err = new ValidationException("json body must be provided",
                        ValidationException.ErrorType.EMPTY_VALUE);
                err.setParameterName(KEY_CTX_BODY_JSON);
                throw err;
            }
            var hasUserId = bodyJson.containsKey(KEY_USER_ID);
            var hasLogin = bodyJson.containsKey(KEY_LOGIN);
            var hasEmail = bodyJson.containsKey(KEY_EMAIL);
            if (!hasUserId && !hasLogin && !hasEmail) {
                var err = new ValidationException("any of the following json fields must be provided: " +
                        List.of(KEY_USER_ID, KEY_LOGIN, KEY_EMAIL), JSON_INVALID);
                err.setParameterName(KEY_CTX_BODY_JSON);
                throw err;
            }
            ctx.put(KEY_CTX_BODY_JSON, bodyJson);
            ctx.next();
        };
        return List.of(hJsonSchema, hJsonUserId);
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored", "Convert2MethodRef"})
    @Override
    public List<Handler<RoutingContext>> reqHandlers() {
        Handler<RoutingContext> h = (RoutingContext ctx) -> {
            var userLoginInfo = ctx.<JsonObject>get(KEY_CTX_BODY_JSON);
            userService.loginUser(userLoginInfo)
                    .subscribe(
                            tokensJson -> ctx.response()
                                    .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                                    .end(tokensJson.encode()),
                            err -> ctx.fail(err));
        };
        return List.of(h);
    }

    private String getJsonBodySchema() {
        var uuid = regexUtil.getUuidPatternString();
        return "{" +
                "  \"type\": \"object\"," +
                "  \"properties\": {" +
                "    \"user_id\": {\"type\": \"string\", \"pattern\": \"" + uuid + "\"}," +
                "    \"login\": {\"type\": \"string\"}," +
                "    \"email\": {\"type\": \"string\", \"format\": \"email\"}," +
                "    \"password\": {\"type\": \"string\"}" +
                "  }," +
                "  \"required\": [\"password\"]" +
                "}";
    }
}
