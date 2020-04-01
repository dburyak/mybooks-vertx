package dburyak.demo.mybooks.user.app;

import dburyak.demo.mybooks.MicronautVerticle;
import dburyak.demo.mybooks.MicronautVerticleProducer;
import dburyak.demo.mybooks.domain.Permission;
import dburyak.demo.mybooks.user.domain.Role;
import dburyak.demo.mybooks.user.domain.User;
import dburyak.demo.mybooks.user.repository.RolesRepository;
import dburyak.demo.mybooks.user.repository.UsersRepository;
import dburyak.demo.mybooks.user.service.RoleService;
import dburyak.demo.mybooks.user.service.UserService;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.IndexOptions;
import io.vertx.reactivex.core.eventbus.EventBus;
import io.vertx.reactivex.ext.mongo.MongoClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;

@Singleton
public class DbInitVerticle extends MicronautVerticle {
    private static final Logger log = LogManager.getLogger(DbInitVerticle.class);
    private static final String EB_ADDR_IS_INITIALIZED = DbInitVerticle.class + ".isInitialized";

    private boolean isInitialized = false;

    @Inject
    private EventBus eventBus;

    @Inject
    private MongoClient mongoClient;

    @Inject
    private UserService userService;

    @Inject
    private RoleService roleService;

    public static String getIsInitializedAddr() {
        return EB_ADDR_IS_INITIALIZED;
    }

    @Override
    protected Completable doStart() {
        return Completable
                .fromAction(() -> log.info("init database"))
                .andThen(registerIsInitializedEbConsumer())
                .andThen(createUsersCollection())
                .andThen(createUserIdIndex())
                .andThen(createUsersLoginIndex())
                .andThen(createUsersEmailIndex())
                .andThen(createUsersRolesIndex())
                .andThen(createRolesNameIndex())
                .andThen(createAdminRole())
                .andThen(createAdminUser())
                .doOnComplete(() -> {
                    log.info("done database init");
                    isInitialized = true;
                })
                .doOnError(err -> log.error("failed to init database", err));
    }

    private Completable registerIsInitializedEbConsumer() {
        return Completable
                .fromAction(() -> eventBus.consumer(getIsInitializedAddr(), msg -> msg.reply(isInitialized)));
    }

    private Completable createUsersCollection() {
        var collectionName = UsersRepository.getCollectionName();
        return mongoClient.rxCreateCollection(collectionName)
                .doOnSubscribe(ignr -> log.debug("creating collection: collectionName={}", collectionName))
                .doOnComplete(() -> log.debug("collection created: collectionName={}", collectionName))
                .doOnError(err -> log.debug("collection already exists: collectionName={}", collectionName))
                .onErrorComplete();
    }

    private Completable createUserIdIndex() {
        var indexName = "users_userId_index";
        var indexOpts = new IndexOptions().name(indexName).unique(true);
        var indexKeys = new JsonObject()
                .put(User.KEY_USER_ID, 1);
        return createIndex(UsersRepository.getCollectionName(), indexName, indexKeys, indexOpts)
                .onErrorComplete();
    }

    private Completable createUsersLoginIndex() {
        var indexName = "users_login_index";
        var indexOpts = new IndexOptions().name(indexName).unique(true);
        var indexKeys = new JsonObject()
                .put(User.KEY_LOGIN, 1);
        return createIndex(UsersRepository.getCollectionName(), indexName, indexKeys, indexOpts)
                .onErrorComplete();
    }

    private Completable createUsersEmailIndex() {
        var indexName = "users_email_index";
        var indexOpts = new IndexOptions().name(indexName).unique(true);
        var indexKeys = new JsonObject()
                .put(User.KEY_EMAIL, 1);
        return createIndex(UsersRepository.getCollectionName(), indexName, indexKeys, indexOpts)
                .onErrorComplete();
    }

    private Completable createUsersRolesIndex() {
        var indexName = "users_roles_index";
        var indexOpts = new IndexOptions().name(indexName).unique(false);
        var indexKeys = new JsonObject()
                .put(User.KEY_ROLES, 1);
        return createIndex(UsersRepository.getCollectionName(), indexName, indexKeys, indexOpts)
                .onErrorComplete();
    }

    private Completable createRolesNameIndex() {
        var indexName = "roles_name_index";
        var indexOpts = new IndexOptions().name(indexName).unique(true);
        var indexKeys = new JsonObject()
                .put(Role.KEY_NAME, 1);
        return createIndex(RolesRepository.getCollectionName(), indexName, indexKeys, indexOpts);
    }

    private Completable createIndex(String collectionName, String indexName, JsonObject indexKeys,
            IndexOptions indexOpts) {
        return mongoClient.rxCreateIndexWithOptions(collectionName, indexKeys, indexOpts)
                .doOnSubscribe(ignr -> log.debug("creating index: indexName={}", indexName))
                .doOnComplete(() -> log.debug("index created: indexName={}", indexName))
                .doOnError(err -> log.error("failed to create index: indexName={}", indexName, err));
    }

    private Completable createAdminRole() {
        var roleName = "admin";
        return Maybe
                .fromCallable(() -> new HashSet<>(Arrays.asList(Permission.values())))
                .flatMap(allPermissions ->
                        roleService.save(roleName, true, allPermissions))
                .doOnSubscribe(ignr -> log.debug("creating role: roleName={}", roleName))
                .doOnComplete(() -> log.debug("role created: roleName={}", roleName))
                .doOnError(err -> log.error("failed to create role: roleName={}", roleName, err))
                .ignoreElement();
    }

    private Completable createAdminUser() {
        var login = "admin";
        var password = "admin";
        return roleService
                .allRoles()
                .toList()
                .flatMapMaybe(allRoles -> {
                    var allPermissions = new HashSet<>(Arrays.asList(Permission.values()));
                    var allRoleNames = allRoles.stream().map(Role::getName).collect(Collectors.toSet());
                    return userService.saveNewUserOrUpdateExistingWithoutPassword(login, password,
                            allRoleNames, allPermissions);
                })
                .doOnSubscribe(ignr -> log.debug("creating admin user: login={}", login))
                .doOnSuccess(ignr -> log.debug("admin user created: login={}", login))
                .doOnError(err -> log.error("failed to create admin user: login={}", login, err))
                .ignoreElement();
    }

    public static class Producer extends MicronautVerticleProducer<Producer> {

        @Override
        protected MicronautVerticle doCreateVerticle() {
            return new DbInitVerticle();
        }
    }
}
