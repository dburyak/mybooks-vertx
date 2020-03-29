package dburyak.demo.mybooks.auth.repository;

import dburyak.demo.mybooks.dal.MongoUtil;
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
    private static final int LIST_BATCH_SIZE = 20;
    private static final String KEY_SUB = "sub";
    private static final String KEY_DEVICE_ID = "device_id";
    private static final String KEY_JTI = "jti";

    @Property(name = "db.mongo.cache.cache-1Lvl-enabled")
    boolean isCache1LvlEnabled;

    @Property(name = "db.mongo.cache.cache-2Lvl-enabled")
    boolean isCache2LvlEnabled;

    @Inject
    private MongoClient mongoClient;

    @Inject
    private MongoUtil mongoUtil;

    public static String getCollectionName() {
        return COLLECTION_NAME;
    }

    public Maybe<JsonObject> get(String dbId) {
        return Maybe
                .fromCallable(() -> new JsonObject().put(mongoUtil.getKeyDbId(), dbId))
                .flatMap(q -> mongoClient.rxFindOne(getCollectionName(), q, null))
                .map(this::fromDbFormat);
    }

    public Maybe<JsonObject> findAndDeleteByJti(String jti) {
        return Maybe
                .fromCallable(() -> mongoUtil.putUuid(jti, KEY_JTI, new JsonObject()))
                .flatMap(q -> mongoClient.rxFindOneAndDelete(getCollectionName(), q))
                .map(this::fromDbFormat);
    }

    public Maybe<JsonObject> findAndReplaceByJti(String oldJti, JsonObject newRefreshToken) {
        return Single
                .fromCallable(() -> toDbFormat(newRefreshToken))
                .flatMapMaybe(newRefreshTokenDb -> {
                    var q = mongoUtil.putUuid(oldJti, KEY_JTI, new JsonObject());
                    return mongoClient.rxFindOneAndReplace(getCollectionName(), q, newRefreshTokenDb);
                })
                .map(this::fromDbFormat);
    }

    public Maybe<String> insert(JsonObject refreshToken) {
        return Maybe
                .fromCallable(() -> toDbFormat(refreshToken))
                .flatMap(refreshTokenDb -> mongoClient.rxInsert(getCollectionName(), refreshTokenDb));
    }

    public Single<Boolean> existsWithJti(String jti) {
        return Single
                .fromCallable(() -> mongoUtil.putUuid(jti, KEY_JTI, new JsonObject()))
                .flatMap(q -> mongoClient.rxCount(getCollectionName(), q))
                .map(num -> num > 0);
    }

    public Single<Boolean> deleteByJti(String jti) {
        return findAndDeleteByJti(jti)
                .map(found -> true)
                .toSingle(false);
    }

    public Maybe<JsonObject> findAndDeleteBySubAndDeviceId(String sub, String deviceId) {
        return Maybe
                .fromCallable(() -> {
                    var q = new JsonObject();
                    mongoUtil.putUuid(sub, KEY_SUB, q);
                    mongoUtil.putUuid(deviceId, KEY_DEVICE_ID, q);
                    return q;
                })
                .flatMap(q -> mongoClient.rxFindOneAndDelete(getCollectionName(), q))
                .map(this::fromDbFormat);
    }

    public Maybe<JsonObject> findAndReplaceUpsertBySubAndDeviceId(String sub, String deviceId,
            JsonObject newRefreshToken) {
        return Maybe
                .fromCallable(() -> {
                    var q = new JsonObject();
                    mongoUtil.putUuid(sub, KEY_SUB, q);
                    mongoUtil.putUuid(deviceId, KEY_DEVICE_ID, q);
                    return q;
                })
                .flatMap(q -> {
                    var newRefreshTokenDb = toDbFormat(newRefreshToken);
                    return mongoClient.rxFindOneAndReplaceWithOptions(getCollectionName(), q, newRefreshTokenDb,
                            new FindOptions(),
                            new UpdateOptions().setUpsert(true));
                })
                .map(this::fromDbFormat);
    }

    public Single<Boolean> existsWithSub(String sub) {
        return Single
                .fromCallable(() -> mongoUtil.putUuid(sub, KEY_SUB, new JsonObject()))
                .flatMap(q -> mongoClient.rxCount(getCollectionName(), q))
                .map(num -> num > 0);
    }

    public Single<Boolean> existsWithSubAndDeviceId(String sub, String deviceId) {
        return Single
                .fromCallable(() -> {
                    var q = new JsonObject();
                    mongoUtil.putUuid(sub,KEY_SUB, q);
                    mongoUtil.putUuid(deviceId, KEY_DEVICE_ID, q);
                    return q;
                })
                .flatMap(q -> mongoClient.rxCount(getCollectionName(), q))
                .map(num -> num > 0);
    }

    public Single<Boolean> deleteBySubAndDeviceId(String sub, String deviceId) {
        return findAndDeleteBySubAndDeviceId(sub, deviceId)
                .map(found -> true)
                .toSingle(false);
    }

    public Single<Long> deleteAllBySub(String sub) {
        return Maybe
                .fromCallable(() -> mongoUtil.putUuid(sub, KEY_SUB, new JsonObject()))
                .flatMap(q -> mongoClient.rxRemoveDocuments(getCollectionName(), q))
                .map(MongoClientDeleteResult::getRemovedCount)
                .toSingle(0L);
    }

    public Single<Long> count() {
        return mongoClient.rxCount(getCollectionName(), new JsonObject());
    }

    public Flowable<JsonObject> list() {
        return list(0, -1);
    }

    public Flowable<JsonObject> list(int offset, int limit) {
        var opts = new FindOptions().setBatchSize(getListBatchSize())
                .setSkip(offset)
                .setLimit(limit);
        return mongoClient.findBatchWithOptions(getCollectionName(), new JsonObject(), opts)
                .toFlowable()
                .map(this::fromDbFormat);
    }

    private JsonObject toDbFormat(JsonObject refreshTokenApp) {
        var refreshTokenDb = refreshTokenApp.copy();
        mongoUtil.putUuid(refreshTokenApp.getString(KEY_JTI), KEY_JTI, refreshTokenDb);
        mongoUtil.putUuid(refreshTokenApp.getString(KEY_DEVICE_ID), KEY_DEVICE_ID, refreshTokenDb);
        mongoUtil.putUuid(refreshTokenApp.getString(KEY_SUB), KEY_SUB, refreshTokenDb);
        return refreshTokenDb;
    }

    private JsonObject fromDbFormat(JsonObject refreshTokenDb) {
        var refreshTokenApp = refreshTokenDb.copy();
        refreshTokenApp.put(KEY_JTI, mongoUtil.readUuid(KEY_JTI, refreshTokenDb).toString());
        refreshTokenApp.put(KEY_DEVICE_ID, mongoUtil.readUuid(KEY_DEVICE_ID, refreshTokenDb).toString());
        refreshTokenApp.put(KEY_SUB, mongoUtil.readUuid(KEY_SUB, refreshTokenDb).toString());
        return refreshTokenApp;
    }

    private static int getListBatchSize() {
        return LIST_BATCH_SIZE;
    }
}
