package dburyak.demo.mybooks.user.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import dburyak.demo.mybooks.discovery.ServiceDiscoveryUtil;
import dburyak.demo.mybooks.domain.Permission;
import dburyak.demo.mybooks.user.app.ServiceTokenVerticle;
import dburyak.demo.mybooks.user.domain.User;
import dburyak.demo.mybooks.user.endpoints.UserLoginEndpoint;
import dburyak.demo.mybooks.user.repository.UsersRepository;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Value;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.eventbus.EventBus;
import io.vertx.reactivex.core.eventbus.Message;
import io.vertx.reactivex.ext.web.client.HttpRequest;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.servicediscovery.ServiceDiscovery;
import io.vertx.reactivex.servicediscovery.types.HttpEndpoint;
import io.vertx.servicediscovery.Record;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;

@Singleton
public class UserService {
    private static final Logger log = LogManager.getLogger(UserService.class);

    private Disposable discoverySubscriptionGetUserToken;
    private String discoveryAuthGetUserTokenAddr;
    private WebClient authGetUserClient;

    @Property(name = "service.auth.discovery.base-name")
    private String discoveryAuthBase;

    @Property(name = "service.auth.discovery.get-user-token")
    private String discoveryAuthGetUserToken;

    @Value("${password.bcrypt-hash-cost:15}")
    private int bcryptHashCost;

    @Inject
    private ServiceDiscovery discovery;

    @Inject
    private ServiceDiscoveryUtil discoveryUtil;

    @Inject
    private UsersRepository usersRepository;

    @Inject
    private RoleService roleService;

    @Inject
    private Vertx vertx;

    @Inject
    private EventBus eventBus;

    @Inject
    private BCrypt.Hasher bcryptHasher;

    @Inject
    private BCrypt.Verifyer bcryptVerifier;

    @Inject
    private Base64.Encoder base64Encoder;

    @Inject
    private Provider<ServiceTokenVerticle> serviceTokenVerticle;

    public Flowable<Permission> getAllPermissionsOfUserId(String userId) {
        return usersRepository.findByUserId(userId)
                .flatMapPublisher(this::getAllPermissionsOfUser);
    }

    public Flowable<Permission> getAllPermissionsOfUser(User user) {
        var userRoles = user.getRoles() != null ? user.getRoles() : Collections.<String>emptySet();
        return Flowable.fromIterable(user.getExplicitPermissions())
                .concatWith(roleService.getPermissionsOfRoleNames(userRoles))
                .distinct();
    }

    public Single<String> hashPassword(String plainPassword) {
        return Single
                .create(emitter -> {
                    var stillNeedToCompute = new AtomicBoolean(true);
                    vertx.<byte[]>executeBlocking(p -> {
                        if (stillNeedToCompute.get()) {
                            p.complete(bcryptHasher.hash(bcryptHashCost, plainPassword.toCharArray()));
                        }
                    }, res -> {
                        if (res.succeeded()) {
                            emitter.onSuccess(new String(res.result()));
                        } else {
                            emitter.onError(res.cause());
                        }
                    });
                    emitter.setCancellable(() -> stillNeedToCompute.set(false));
                });
    }

    public Single<Boolean> verifyPassword(String plainPassword, String hash) {
        return vertx
                .<BCrypt.Result>rxExecuteBlocking(p ->
                        p.complete(bcryptVerifier.verify(plainPassword.toCharArray(), hash.toCharArray())), false)
                .map(res -> res.verified)
                .toSingle();
    }

    public Single<Boolean> verifyPassword(String plainPassword, User user) {
        return verifyPassword(plainPassword, user.getPasswordHash());
    }

    public Single<User> hashAndSetPassword(String plainPassword, User user) {
        return hashPassword(plainPassword)
                .map(user::withPasswordHash);
    }

    public Single<User> saveUserWithPassword(User user, String password) {
        var resUser = user.copy();
        return hashAndSetPassword(password, resUser)
                .flatMapMaybe(u -> usersRepository.save(u))
                .map(resUser::withDbId)
                .toSingle(resUser);
    }

    public Single<User> updateUser(User user) {
        return Single.error(() -> new AssertionError("not implemented"));
    }

    public Maybe<User> saveNewUserOrUpdateExistingWithoutPassword(String login, String plainPassword,
            Set<String> roles, Set<Permission> explicitPermissions) {
        return usersRepository
                .findByLogin(login)
                .toSingle(new User().withLogin(login))
                .flatMap(u -> { // generate password hash if this is new user
                    if (u.getDbId() == null) {
                        return hashAndSetPassword(plainPassword, u);
                    } else {
                        return Single.just(u);
                    }
                })
                .flatMapMaybe(u -> usersRepository
                        .findOneAndReplaceUpsert(u.withRoles(roles).withExplicitPermissions(explicitPermissions)));
    }

    public Flowable<User> list() {
        return list(0, -1);
    }

    public Flowable<User> list(int offset, int limit) {
        return usersRepository.list(offset, limit);
    }

    public Flowable<User> findWithRoles(Set<String> roles) {
        return findWithRoles(roles, 0, -1);
    }

    public Flowable<User> findWithRoles(List<String> roles) {
        return findWithRoles(new HashSet<>(roles), 0, -1);
    }

    public Flowable<User> findWithoutRoles(Set<String> roles) {
        return findWithoutRoles(roles, 0, -1);
    }

    public Flowable<User> findWithoutRoles(List<String> roles) {
        return findWithoutRoles(new HashSet<>(roles), 0, -1);
    }

    public Flowable<User> findWithRoles(Set<String> roles, int offset, int limit) {
        return usersRepository.listWithRoles(roles, offset, limit);
    }

    public Flowable<User> findWithRoles(List<String> roles, int offset, int limit) {
        return usersRepository.listWithRoles(new HashSet<>(roles), offset, limit);
    }

    public Flowable<User> findWithoutRoles(Set<String> roles, int offset, int limit) {
        return usersRepository.listWithoutRoles(roles, offset, limit);
    }

    public Flowable<User> findWithoutRoles(List<String> roles, int offset, int limit) {
        return usersRepository.listWithoutRoles(new HashSet<>(roles), offset, limit);
    }

    public Single<Long> count() {
        return usersRepository.count();
    }

    public Maybe<User> findByUserId(String userId) {
        return usersRepository.findByUserId(userId);
    }

    public Maybe<User> findByAnyOf(String userId, String login, String email) {
        return usersRepository.findByAnyOf(userId, login, email);
    }

    public Observable<String> findRolesOfUserByAnyOf(String userId, String login, String email) {
        return usersRepository.findRolesOfUserByAnyOf(userId, login, email);
    }

    public Observable<String> findRolesOfUserByUserId(String userId) {
        return findRolesOfUserByAnyOf(userId, null, null);
    }

    public Single<JsonObject> loginUser(JsonObject userLoginInfo) {
        var userId = userLoginInfo.getString(User.KEY_USER_ID);
        var login = userLoginInfo.getString(User.KEY_LOGIN);
        var email = userLoginInfo.getString(User.KEY_EMAIL);
        var deviceId = userLoginInfo.getString(UserLoginEndpoint.KEY_DEVICE_ID);

        // find user
        var user = (userId != null && !userId.isEmpty()) ? usersRepository.findByUserId(userId)
                : (login != null && !login.isEmpty()) ? usersRepository.findByLogin(login)
                : (email != null && !email.isEmpty()) ? usersRepository.findByEmail(email)
                : Maybe.<User>error(() -> new BadUserLoginInfoException(userLoginInfo));
        return user.toSingle()
                .onErrorResumeNext(err -> {
                    if (err instanceof NoSuchElementException) {
                        return Single.error(() -> new UserNotFoundException(userLoginInfo));
                    } else {
                        return Single.error(err);
                    }
                })

                // check password
                .flatMap(u -> verifyPassword(userLoginInfo.getString(UserLoginEndpoint.KEY_PASSWORD), u)
                        .map(passwordOk -> {
                            if (!passwordOk) {
                                throw new WrongPasswordException(userLoginInfo);
                            }
                            return u;
                        }))

                // get new tokens
                .flatMap(u -> loginUser(u, deviceId));
    }

    private Single<JsonObject> loginUser(User user, String deviceId) {
        var authUrl = "/user-token";
        return getAllPermissionsOfUser(user)
                .map(Permission::toString)
                .toList()
                .map(permissions -> new JsonObject()
                        .put("sub", user.getUserId())
                        .put("device_id", deviceId)
                        .put("permissions", new JsonArray(permissions)))
                .zipWith(eventBus.<String>rxRequest(serviceTokenVerticle.get().getServiceTokenAddr(), null)
                                .map(Message::body),
                        (claims, authToken) -> {
                            var claimsBase64 = base64Encoder.encodeToString(claims.encode().getBytes());
                            return authGetUserClient.get(authUrl)
                                    .putHeader(HttpHeaders.ACCEPT.toString(), "application/json")
                                    .bearerTokenAuthentication(authToken)
                                    .setQueryParam("claims", claimsBase64);
                        })
                .flatMap(HttpRequest::rxSend)
                .map(resp -> {
                    if (resp.statusCode() == OK.code()) {
                        var respJson = resp.bodyAsJsonObject();
                        return respJson;
                    } else {
                        throw new RuntimeException("failed to call auth service: url={" + authUrl + "}, respCode={" +
                                resp.statusCode() + "}, respBody={" + resp.bodyAsString() + "}");
                    }
                });
    }

    @PostConstruct
    private void init() {
        discoveryAuthGetUserTokenAddr = discoveryAuthBase + discoveryAuthGetUserToken;
        discoverySubscriptionGetUserToken = discoveryUtil
                .discover(discoveryAuthGetUserTokenAddr, this::isGetUserToken)
                .doOnNext(r -> log.debug("discovered auth/get-user-token service: {}", r::toJson))
                .flatMapSingle(rec -> HttpEndpoint
                        .rxGetWebClient(discovery, new JsonObject().put("name", discoveryAuthGetUserTokenAddr)))
                .subscribe(
                        cl -> authGetUserClient = cl,
                        err -> log.error("discovery failure for auth/get-user-token service", err));
    }

    @PreDestroy
    private void dispose() {
        discoverySubscriptionGetUserToken.dispose();
    }

    private boolean isGetUserToken(Record record) {
        return discoveryAuthGetUserTokenAddr.equals(record.getName()) && HttpEndpoint.TYPE.equals(record.getType());
    }
}
