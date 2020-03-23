package dburyak.demo.mybooks.user.app;

import dburyak.demo.mybooks.MicronautVerticle;
import dburyak.demo.mybooks.MicronautVerticleProducer;
import dburyak.demo.mybooks.domain.Permission;
import dburyak.demo.mybooks.user.domain.User;
import dburyak.demo.mybooks.user.repository.UsersRepository;
import dburyak.demo.mybooks.user.service.RoleService;
import dburyak.demo.mybooks.user.service.UserService;
import io.reactivex.Completable;
import io.reactivex.Single;
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
                .andThen(createLoginIndex())
                .andThen(createEmailIndex())
                .andThen(createRolesIndex())
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

    private Completable createLoginIndex() {
        var indexName = "users_login_index";
        var indexOpts = new IndexOptions().name(indexName).unique(true);
        var indexKeys = new JsonObject()
                .put(User.KEY_LOGIN, 1);
        return createIndex(UsersRepository.getCollectionName(), indexName, indexKeys, indexOpts)
                .onErrorComplete();
    }

    private Completable createEmailIndex() {
        var indexName = "users_email_index";
        var indexOpts = new IndexOptions().name(indexName).unique(true);
        var indexKeys = new JsonObject()
                .put(User.KEY_EMAIL, 1);
        return createIndex(UsersRepository.getCollectionName(), indexName, indexKeys, indexOpts)
                .onErrorComplete();
    }

    private Completable createRolesIndex() {
        var indexName = "users_roles_index";
        var indexOpts = new IndexOptions().name(indexName).unique(true);
        var indexKeys = new JsonObject()
                .put(User.KEY_ROLES, 1);
        return createIndex(UsersRepository.getCollectionName(), indexName, indexKeys, indexOpts)
                .onErrorComplete();
    }

    private Completable createIndex(String collectionName, String indexName, JsonObject indexKeys,
            IndexOptions indexOpts) {
        return mongoClient.rxCreateIndexWithOptions(collectionName, indexKeys, indexOpts)
                .doOnSubscribe(ignr -> log.debug("creating index: indexName={}", indexName))
                .doOnComplete(() -> log.debug("index created: indexName={}", indexName))
                .doOnError(err -> log.error("failed to create index: indexName={}", indexName, err));
    }

    private Completable createAdminUser() {
        return Single
                .fromCallable(() -> {
                    log.debug("creating admin user");
                    var allPermissions = new HashSet<>(Arrays.asList(Permission.values()));
                    return new User()
                            .withLogin("admin")
                            .withExplicitPermissions(allPermissions);
                })
                .flatMap(user -> userService.saveUserWithPasswordOrUpdateWithoutPassword(user, "admin"))
                .ignoreElement()
                .doOnComplete(() -> log.debug("admin user created"))
                .doOnError(err -> log.error("failed to create admin user", err));
    }

    public static class Producer extends MicronautVerticleProducer<Producer> {

        @Override
        protected MicronautVerticle doCreateVerticle() {
            return new DbInitVerticle();
        }
    }
}
