package dburyak.demo.mybooks.auth.repository;

import io.micronaut.context.annotation.Property;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClientDeleteResult;
import io.vertx.ext.mongo.UpdateOptions;
import io.vertx.reactivex.ext.mongo.MongoClient;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RefreshTokensRepository {
    private static final String COLLECTION_NAME = "refreshTokens";
    private static final int LIST_ALL_BATCH_SIZE = 20;
    private static final String KEY_SUB = "sub";
    private static final String KEY_DEVICE_ID = "device_id";
    private static final String KEY_JTI = "jti";

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
        var q = new JsonObject().put("_id", id);
        return mongoClient.rxFindOne(getCollectionName(), q, null);
    }

    public Maybe<JsonObject> findAndDeleteByJti(String jti) {
        var q = new JsonObject().put(KEY_JTI, jti);
        return mongoClient.rxFindOneAndDelete(getCollectionName(), q);
    }

    public Maybe<JsonObject> findAndReplaceByJti(String oldJti, JsonObject newRefreshToken) {
        var q = new JsonObject().put(KEY_JTI, oldJti);
        return mongoClient.rxFindOneAndReplace(getCollectionName(), q, newRefreshToken);
    }

    public Maybe<String> insert(JsonObject refreshToken) {
        return mongoClient.rxInsert(getCollectionName(), refreshToken);
    }

    public Single<Boolean> existsWithJti(String jti) {
        var q = new JsonObject().put(KEY_JTI, jti);
        return mongoClient.rxCount(getCollectionName(), q)
                .map(num -> num > 0);
    }

    public Single<Boolean> deleteByJti(String jti) {
        return findAndDeleteByJti(jti)
                .map(found -> true)
                .toSingle(false);
    }

    public Maybe<JsonObject> findAndDeleteBySubAndDeviceId(String sub, String deviceId) {
        var q = new JsonObject().put(KEY_SUB, sub).put(KEY_DEVICE_ID, deviceId);
        return mongoClient.rxFindOneAndDelete(getCollectionName(), q);
    }

    public Maybe<JsonObject> findAndReplaceUpsertBySubAndDeviceId(String sub, String deviceId, JsonObject newRefreshToken) {
        var q = new JsonObject().put(KEY_SUB, sub).put(KEY_DEVICE_ID, deviceId);
        return mongoClient.rxFindOneAndReplaceWithOptions(getCollectionName(), q, newRefreshToken,
                new FindOptions(),
                new UpdateOptions().setUpsert(true));
    }

    public Single<Boolean> existsWithSub(String sub) {
        var q = new JsonObject().put(KEY_SUB, sub);
        return mongoClient.rxCount(getCollectionName(), q)
                .map(num -> num > 0);
    }

    public Single<Boolean> existsWithSubAndDeviceId(String sub, String deviceId) {
        var q = new JsonObject().put(KEY_SUB, sub).put(KEY_DEVICE_ID, deviceId);
        return mongoClient.rxCount(getCollectionName(), q)
                .map(num -> num > 0);
    }

    public Single<Boolean> deleteBySubAndDeviceId(String sub, String deviceId) {
        return findAndDeleteBySubAndDeviceId(sub, deviceId)
                .map(found -> true)
                .toSingle(false);
    }

    public Single<Long> deleteAllBySub(String sub) {
        var q = new JsonObject().put(KEY_SUB, sub);
        return mongoClient.rxRemoveDocuments(getCollectionName(), q)
                .map(MongoClientDeleteResult::getRemovedCount)
                .toSingle(0L);
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
