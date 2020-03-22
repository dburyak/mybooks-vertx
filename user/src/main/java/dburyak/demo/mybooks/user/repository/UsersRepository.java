package dburyak.demo.mybooks.user.repository;

import dburyak.demo.mybooks.dal.MongoUtil;
import dburyak.demo.mybooks.user.domain.User;
import io.reactivex.Maybe;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.ext.mongo.MongoClient;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.function.Supplier;

import static dburyak.demo.mybooks.user.domain.User.KEY_EMAIL;
import static dburyak.demo.mybooks.user.domain.User.KEY_LOGIN;
import static dburyak.demo.mybooks.user.domain.User.KEY_USER_ID;

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

    private Maybe<User> findOneByQuery(Supplier<JsonObject> querySupplier) {
        return Maybe
                .fromCallable(querySupplier::get)
                .flatMap(q -> mongoClient.rxFindOne(getCollectionName(), q, null))
                .map(User::new);
    }
}
