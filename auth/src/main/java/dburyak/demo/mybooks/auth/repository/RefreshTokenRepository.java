package dburyak.demo.mybooks.auth.repository;

import io.micronaut.context.annotation.Property;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.reactivex.ext.mongo.MongoClient;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Objects;

@Singleton
public class RefreshTokenRepository {
    private static final String COLLECTION_NAME = "refreshTokens";
    private static final int LIST_ALL_BATCH_SIZE = 20;

    @Property(name = "db.mongo.cache.cache-1Lvl-enabled")
    boolean isCache1LvlEnabled;

    @Property(name = "db.mongo.cache.cache-2Lvl-enabled")
    boolean isCache2LvlEnabled;

    @Inject
    private MongoClient mongoClient;

    public static String getCollectionName() {
        return COLLECTION_NAME;
    }

    public Maybe<JsonObject> get(String id) {
        Objects.requireNonNull(id);
        var q = new JsonObject().put("_id", id);
        return mongoClient.rxFindOne(getCollectionName(), q, null);
    }

    public Maybe<JsonObject> findAndDelete(String jti) {
        Objects.requireNonNull(jti);
        var q = new JsonObject().put("jti", jti);
        return mongoClient.rxFindOneAndDelete(getCollectionName(), q);
    }

    public Single<Long> count() {
        return mongoClient.rxCount(getCollectionName(), new JsonObject());
    }

    public Flowable<JsonObject> list() {
        return list(0, -1);
    }

    public Flowable<JsonObject> list(int skip, int limit) {
        var opts = new FindOptions().setBatchSize(getListAllBatchSize())
                .setSkip(skip)
                .setLimit(limit);
        return mongoClient.findBatchWithOptions(getCollectionName(), new JsonObject(), opts)
                .toFlowable();
    }

    private static int getListAllBatchSize() {
        return LIST_ALL_BATCH_SIZE;
    }
}
