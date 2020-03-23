package dburyak.demo.mybooks.user.repository;

import com.mongodb.MongoWriteException;
import dburyak.demo.mybooks.dal.MongoUtil;
import dburyak.demo.mybooks.user.domain.User;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.UpdateOptions;
import io.vertx.reactivex.ext.mongo.MongoClient;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static dburyak.demo.mybooks.user.domain.User.KEY_EMAIL;
import static dburyak.demo.mybooks.user.domain.User.KEY_LOGIN;
import static dburyak.demo.mybooks.user.domain.User.KEY_USER_ID;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Singleton
public class UsersRepository {
    private static final String USER_COLLECTION_NAME = "users";

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

    public Maybe<User> save(User user) {
        return Maybe
                .fromCallable(() -> buildQueryForUser(user))
                .flatMap(q -> {
                    if (isBlank(user.getUserId())) {
                        user.setUserId(UUID.randomUUID().toString());
                    }
                    var u = toDbFormat(user.toJson());
                    var findOpts = new FindOptions();
                    var updOpts = new UpdateOptions().setUpsert(true);
                    return mongoClient.rxFindOneAndUpdateWithOptions(getCollectionName(), q, u, findOpts, updOpts);
                })
                .map(this::fromDbFormat)
                .map(User::new);
    }

    public Maybe<User> updateWithoutPassword(User user) {
        return Maybe
                .fromCallable(() -> {
                    var q = buildQueryForUser(user);
                    if (isBlank(user.getUserId())) {
                        user.setUserId(UUID.randomUUID().toString());
                    }
                    var u = user.toJson();
                    u.remove(User.KEY_PASSWORD_HASH);
                    u = toDbFormat(u);
                    return List.of(q, u);
                })
                .flatMap(t -> {
                    var q = t.get(0);
                    var u = t.get(1);
                    return mongoClient.rxFindOneAndUpdate(getCollectionName(), q, new JsonObject()
                            .put("$set", u));
                })
                .map(this::fromDbFormat)
                .map(User::new);
    }

    public Single<User> insertWithPasswordOrUpdateWithoutPassword(User user, String passwordHash) {
        var oldPasswordHash = user.getPasswordHash();
        return Maybe
                // first, try to insert it as a brand new document with password hash
                .fromCallable(() -> {
                    if (isBlank(user.getUserId())) {
                        user.setUserId(UUID.randomUUID().toString());
                    }
                    user.setPasswordHash(passwordHash);
                    return toDbFormat(user.toJson());
                })
                .flatMap(userJson -> mongoClient.rxInsert(getCollectionName(), userJson))
                .map(user::withDbId)

                // if failed, then update existing user without updating password hash
                .onErrorResumeNext(err -> {
                    if (!(err instanceof MongoWriteException)) {
                        return Maybe.error(err);
                    }
                    return updateWithoutPassword(user.withPasswordHash(oldPasswordHash));
                })
                .toSingle(user);
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
}
