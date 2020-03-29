package dburyak.demo.mybooks.user.repository;

import dburyak.demo.mybooks.dal.MongoUtil;
import dburyak.demo.mybooks.user.domain.User;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.UpdateOptions;
import io.vertx.reactivex.ext.mongo.MongoClient;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.UUID;
import java.util.function.Supplier;

import static dburyak.demo.mybooks.user.domain.User.KEY_EMAIL;
import static dburyak.demo.mybooks.user.domain.User.KEY_LOGIN;
import static dburyak.demo.mybooks.user.domain.User.KEY_PASSWORD_HASH;
import static dburyak.demo.mybooks.user.domain.User.KEY_USER_ID;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Singleton
public class UsersRepository {
    private static final String USER_COLLECTION_NAME = "users";
    private static final int LIST_BATCH_SIZE = 20;

    @Inject
    private MongoClient mongoClient;

    @Inject
    private MongoUtil mongoUtil;

    public static String getCollectionName() {
        return USER_COLLECTION_NAME;
    }

    public Maybe<User> get(String dbId) {
        return findOneByQuery(() -> new JsonObject().put(mongoUtil.getKeyDbId(), dbId));
    }

    public Maybe<User> findByUserId(String userId) {
        return findOneByQuery(() -> new JsonObject().put(KEY_USER_ID, userId));
    }

    public Maybe<User> findByLogin(String login) {
        return findOneByQuery(() -> new JsonObject().put(KEY_LOGIN, login));
    }

    public Maybe<User> findByEmail(String email) {
        return findOneByQuery(() -> new JsonObject().put(KEY_EMAIL, email));
    }

    /**
     * Insert brand new user or update previously retrieved one (with {@code _id} defined).
     *
     * @param user user to be saved
     * @return dbId of the user
     */
    public Maybe<String> save(User user) {
        return Maybe
                .fromCallable(() -> {
                    generateUserIdIfAbsent(user);
                    return toDbFormat(user.toJson());
                })
                .flatMap(u -> mongoClient.rxSave(getCollectionName(), u));
    }

    /**
     * Find user by {@code userId} and/or {@code login} and/or {@code email} and replace it. Insert as new user if it
     * doesn't exist yet.
     */
    public Maybe<User> findOneAndReplaceUpsert(User user) {
        return Maybe
                .fromCallable(() -> buildQueryForUser(user))
                .flatMap(q -> {
                    generateUserIdIfAbsent(user);
                    var u = toDbFormat(user.toJson());
                    var findOpts = new FindOptions();
                    var updOpts = new UpdateOptions().setUpsert(true);
                    return mongoClient.rxFindOneAndReplaceWithOptions(getCollectionName(), q, u, findOpts, updOpts);
                })
                .map(this::fromDbFormat)
                .map(User::new);
    }

    public Single<User> saveNewWithPasswordOrReplaceExistingWithoutPassword(User user, String passwordHash) {
        return Single
                .fromCallable(() -> buildQueryForUser(user))
                .flatMap(q -> mongoClient
                        .rxFindOne(getCollectionName(), q, new JsonObject()
                                .put(mongoUtil.getKeyDbId(), 1)
                                .put(User.KEY_USER_ID, 1)
                                .put(User.KEY_PASSWORD_HASH, 1))
                        .toSingle(new JsonObject())
                )
                .flatMap(existing -> {
                    var userJson = user.toJson();
                    generateUserIdIfAbsent(userJson);
                    var upd = toDbFormat(userJson);
                    var dbId = existing.getString(mongoUtil.getKeyDbId());
                    if (dbId != null) {
                        upd.put(mongoUtil.getKeyDbId(), dbId);
                        upd.put(KEY_PASSWORD_HASH, existing.getString(KEY_PASSWORD_HASH));
                    } else {
                        upd.put(KEY_PASSWORD_HASH, passwordHash);
                    }
                    return mongoClient.rxSave(getCollectionName(), upd)
                            .map(newDbId -> upd.put(mongoUtil.getKeyDbId(), newDbId))
                            .toSingle(upd);
                })
                .map(this::fromDbFormat)
                .map(User::new);
    }

    public Single<Long> count() {
        return mongoClient.rxCount(getCollectionName(), new JsonObject());
    }

    public Flowable<User> list() {
        return list(0, -1);
    }

    public Flowable<User> list(int offset, int limit) {
        var opts = new FindOptions().setBatchSize(getListBatchSize())
                .setSkip(offset)
                .setLimit(limit);
        return mongoClient.findBatchWithOptions(getCollectionName(), new JsonObject(), opts)
                .toFlowable()
                .map(this::fromDbFormat)
                .map(User::new);
    }

    private Maybe<User> findOneByQuery(Supplier<JsonObject> querySupplier) {
        return Maybe
                .fromCallable(querySupplier::get)
                .flatMap(q -> mongoClient.rxFindOne(getCollectionName(), q, null))
                .map(this::fromDbFormat)
                .map(User::new);
    }

    private JsonObject buildQueryForUser(User user) {
        var q = new JsonObject();
        if (isNotBlank(user.getUserId())) {
            mongoUtil.putUuid(user.getUserId(), KEY_USER_ID, q);
        } else if (isNotBlank(user.getLogin())) {
            q.put(KEY_LOGIN, user.getLogin());
        } else if (isNotBlank(user.getEmail())) {
            q.put(KEY_EMAIL, user.getEmail());
        } else {
            throw new IllegalArgumentException("can't save user without required fields");
        }
        return q;
    }

    private JsonObject toDbFormat(JsonObject userJson) {
        var userJsonDb = userJson.copy();
        mongoUtil.putUuid(userJson.getString(KEY_USER_ID), KEY_USER_ID, userJsonDb);
        return userJsonDb;
    }

    private JsonObject fromDbFormat(JsonObject userJsonDb) {
        var userJson = userJsonDb.copy();
        userJson.put(KEY_USER_ID, mongoUtil.readUuid(KEY_USER_ID, userJsonDb).toString());
        return userJson;
    }

    private void generateUserIdIfAbsent(User user) {
        if (isBlank(user.getUserId())) {
            user.setUserId(UUID.randomUUID().toString());
        }
    }

    private void generateUserIdIfAbsent(JsonObject userJson) {
        var existingUserId = userJson.getString(KEY_USER_ID);
        if (isBlank(existingUserId)) {
            userJson.put(KEY_USER_ID, UUID.randomUUID().toString());
        }
    }

    private static int getListBatchSize() {
        return LIST_BATCH_SIZE;
    }
}
