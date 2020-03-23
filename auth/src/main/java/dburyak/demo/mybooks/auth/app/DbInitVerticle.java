package dburyak.demo.mybooks.auth.app;

import dburyak.demo.mybooks.MicronautVerticle;
import dburyak.demo.mybooks.MicronautVerticleProducer;
import dburyak.demo.mybooks.auth.repository.RefreshTokensRepository;
import io.reactivex.Completable;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.IndexOptions;
import io.vertx.reactivex.core.eventbus.EventBus;
import io.vertx.reactivex.ext.mongo.MongoClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DbInitVerticle extends MicronautVerticle {
    private static final Logger log = LogManager.getLogger(DbInitVerticle.class);
    private static final String EB_ADDR_IS_INITIALIZED = DbInitVerticle.class + ".isInitialized";

    private boolean isInitialized = false;

    @Inject
    private MongoClient mongoClient;

    @Inject
    private EventBus eventBus;

    public static String getIsInitializedAddr() {
        return EB_ADDR_IS_INITIALIZED;
    }

    @Override
    protected Completable doStart() {
        return Completable
                .fromAction(() -> log.info("init database"))
                .andThen(registerIsInitializedEbConsumer())
                .andThen(createRefreshTokenCollection())
                .andThen(createRefreshTokenJtiIndex())
                .andThen(createRefreshTokensSubAndDeviceIdIndex())
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

    private Completable createRefreshTokenCollection() {
        var collectionName = RefreshTokensRepository.getCollectionName();
        return mongoClient.rxCreateCollection(collectionName)
                .doOnSubscribe(ignr -> log.debug("creating collection: collectionName={}", collectionName))
                .doOnComplete(() -> log.debug("collection created: collectionName={}", collectionName))
                .doOnError(err -> log.debug("collection already exists: collectionName={}", collectionName))
                .onErrorComplete();
    }

    private Completable createRefreshTokenJtiIndex() {
        var indexName = "refreshTokens_jti_index";
        var indexOpts = new IndexOptions().name(indexName).unique(true);
        var indexKeys = new JsonObject()
                .put("jti", 1);
        return createIndex(RefreshTokensRepository.getCollectionName(), indexName, indexKeys, indexOpts)
                .onErrorComplete();
    }

    private Completable createRefreshTokensSubAndDeviceIdIndex() {
        var indexName = "refreshTokens_sub_deviceId_index";
        var indexOpts = new IndexOptions().name(indexName).unique(true);
        var indexKeys = new JsonObject()
                .put("sub", 1)
                .put("device_id", 1);
        return createIndex(RefreshTokensRepository.getCollectionName(), indexName, indexKeys, indexOpts)
                .onErrorComplete();
    }

    private Completable createIndex(String collectionName, String indexName, JsonObject indexKeys,
            IndexOptions indexOpts) {
        return mongoClient.rxCreateIndexWithOptions(collectionName, indexKeys, indexOpts)
                .doOnSubscribe(ignr -> log.debug("creating index: indexName={}", indexName))
                .doOnComplete(() -> log.debug("index created: indexName={}", indexName))
                .doOnError(err -> log.error("failed to create index: indexName={}", indexName, err));
    }

    public static class Producer extends MicronautVerticleProducer<Producer> {

        @Override
        protected MicronautVerticle doCreateVerticle() {
            return new DbInitVerticle();
        }
    }
}
