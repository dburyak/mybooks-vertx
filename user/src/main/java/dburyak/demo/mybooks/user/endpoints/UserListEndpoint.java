package dburyak.demo.mybooks.user.endpoints;

import dburyak.demo.mybooks.dal.MongoUtil;
import dburyak.demo.mybooks.user.domain.Role;
import dburyak.demo.mybooks.user.domain.User;
import dburyak.demo.mybooks.user.service.RoleService;
import dburyak.demo.mybooks.user.service.UserService;
import dburyak.demo.mybooks.web.Endpoint;
import io.micronaut.context.annotation.Value;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.api.RequestParameters;
import io.vertx.ext.web.api.validation.ValidationException;
import io.vertx.reactivex.ext.web.Route;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.api.validation.HTTPRequestValidationHandler;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

import static dburyak.demo.mybooks.domain.Permission.LIST_ALL_USERS;
import static dburyak.demo.mybooks.domain.Permission.LIST_NON_SYSTEM_ROLES;
import static dburyak.demo.mybooks.domain.Permission.LIST_USERS_OF_SAME_ROLES;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.ext.web.api.validation.ParameterType.INT;

@Singleton
public class UserListEndpoint implements Endpoint {
    private static final String KEY_CAN_LIST_SAME_ROLES = "canListSameRoles";
    private static final String KEY_CAN_LIST_NON_SYS_ROLES = "canListNonSysRoles";
    private static final String KEY_CAN_LIST_ALL_USERS = "canListAllUsers";
    private static final String PARAM_NAME_OFFSET = "offset";
    private static final String PARAM_NAME_LIMIT = "limit";

    @Value("${list.max-limit.list-all-users:200}")
    private int listAllUsersMaxLimit;

    @Value("${list.default-limit.list-all-users:25}")
    private int listAllUsersDefaultLimit;

    @Inject
    private UserService userService;

    @Inject
    private RoleService roleService;

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
            var canListSameRoles = u.rxIsAuthorised(LIST_USERS_OF_SAME_ROLES.toString());
            var canListNonSystemUsers = u.rxIsAuthorised(LIST_NON_SYSTEM_ROLES.toString());
            var canListAllUsers = u.rxIsAuthorised(LIST_ALL_USERS.toString());
            Single
                    .zip(canListSameRoles, canListNonSystemUsers, canListAllUsers,
                            (same, nonSys, all) -> {
                                ctx.put(KEY_CAN_LIST_SAME_ROLES, same);
                                ctx.put(KEY_CAN_LIST_NON_SYS_ROLES, nonSys);
                                ctx.put(KEY_CAN_LIST_ALL_USERS, all);
                                return same || nonSys || all;
                            })
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
        return List.of(
                HTTPRequestValidationHandler.create()
                        .addQueryParam(PARAM_NAME_OFFSET, INT, false)
                        .addQueryParam(PARAM_NAME_LIMIT, INT, false),
                (RoutingContext ctx) -> {
                    var parsedParams = ctx.<RequestParameters>get("parsedParameters");
                    var offsetParam = parsedParams.queryParameter(PARAM_NAME_OFFSET);
                    var limitParam = parsedParams.queryParameter(PARAM_NAME_LIMIT);
                    var offset = (offsetParam != null) ? offsetParam.getInteger() : null;
                    var limit = (limitParam != null) ? limitParam.getInteger() : null;
                    if (offset != null) {
                        if (offset < 0) {
                            throw new ValidationException(PARAM_NAME_OFFSET + " must be non-negative");
                        }
                    }
                    if (limit != null) {
                        if (limit <= 0) {
                            throw new ValidationException(PARAM_NAME_LIMIT + " must be positive");
                        }
                    }
                    ctx.put(PARAM_NAME_OFFSET, offset);
                    ctx.put(PARAM_NAME_LIMIT, limit);
                    ctx.next();
                }
        );
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored", "Convert2MethodRef"})
    @Override
    public List<Handler<RoutingContext>> reqHandlers() {
        return List.of((RoutingContext ctx) -> {
            var offsetReq = ctx.<Integer>get(PARAM_NAME_OFFSET);
            var limitReq = ctx.<Integer>get(PARAM_NAME_LIMIT);
            var offset = (offsetReq != null) ? offsetReq : 0;
            var limit = (limitReq != null) ? Math.min(limitReq, listAllUsersMaxLimit) : listAllUsersDefaultLimit;
            var canListSame = ctx.<Boolean>get(KEY_CAN_LIST_SAME_ROLES);
            var canListNonSys = ctx.<Boolean>get(KEY_CAN_LIST_NON_SYS_ROLES);
            var canListAll = ctx.<Boolean>get(KEY_CAN_LIST_ALL_USERS);
            var userId = ctx.user().principal().getString(User.KEY_USER_ID);
            Flowable<User> usersList = Flowable.empty();
            if (canListAll) {
                usersList = userService.list(offset, limit);
            } else if (canListNonSys) {
                usersList = roleService.allNonSystemRoles()
                        .map(Role::getName)
                        .toList()
                        .flatMapPublisher(nonSysRoles -> userService.findWithRoles(nonSysRoles, offset, limit));
            } else if (canListSame) {
                usersList = userService.findRolesOfUserByUserId(userId)
                        .toList()
                        .flatMapPublisher(userRoles -> userService.findWithRoles(userRoles, offset, limit));
            } else {
                ctx.fail(new AssertionError("must have any permission to list users: " + List.of(
                        LIST_ALL_USERS, LIST_NON_SYSTEM_ROLES, LIST_USERS_OF_SAME_ROLES)));
                return;
            }
            usersList
                    .map(u -> {
                        var userJson = u.toJson();
                        userJson.remove(MongoUtil.KEY_DB_ID);
                        userJson.remove(User.KEY_PASSWORD_HASH);
                        return userJson;
                    })
                    .toList()
                    .map(JsonArray::new)
                    .subscribe(
                            usersJsonArray -> ctx.response()
                                    .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                                    .end(usersJsonArray.encode()),
                            err -> ctx.fail(err));
        });
    }
}
