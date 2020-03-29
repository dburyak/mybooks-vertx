package dburyak.demo.mybooks.user.repository;

import dburyak.demo.mybooks.dal.MongoUtil;
import dburyak.demo.mybooks.user.domain.Role;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.reactivex.ext.mongo.MongoClient;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Set;
import java.util.function.Supplier;

import static dburyak.demo.mybooks.dal.MongoUtil.OPERATOR_IN;

@Singleton
public class RolesRepository {
    private static final int LIST_BATCH_SIZE = 20;
    private static final int FIND_ALL_BY_NAMES_BATCH_SIZE = 20;

    public static final String COLLECTION_NAME = "roles";

    @Inject
    private MongoClient mongoClient;

    @Inject
    private MongoUtil mongoUtil;

    public static String getCollectionName() {
        return COLLECTION_NAME;
    }

    public Maybe<Role> get(String dbId) {
        return findOneByQuery(() -> new JsonObject().put(mongoUtil.getKeyDbId(), dbId));
    }

    public Maybe<Role> findByName(String roleName) {
        return findOneByQuery(() -> new JsonObject().put(Role.KEY_NAME, roleName));
    }

    public Flowable<Role> findAllByNames(Set<String> roleNames) {
        return Single
                .fromCallable(() -> new JsonObject().put(Role.KEY_NAME, new JsonObject()
                        .put(OPERATOR_IN, new JsonArray(new ArrayList<>(roleNames)))))
                .flatMapPublisher(q -> {
                    var opts = new FindOptions().setBatchSize(getFindAllByNamesBatchSize());
                    return mongoClient.findBatchWithOptions(getCollectionName(), q, opts)
                            .toFlowable();
                })
                .map(Role::new);
    }

    public Flowable<Role> list() {
        return list(0, -1);
    }

    public Flowable<Role> list(int offset, int limit) {
        var opts = new FindOptions().setBatchSize(getListBatchSize())
                .setSkip(offset)
                .setLimit(limit);
        return mongoClient.findBatchWithOptions(getCollectionName(), new JsonObject(), opts)
                .toFlowable()
                .map(Role::new);
    }

    public Maybe<String> save(Role role) {
        return Maybe
                .fromCallable(role::toJson)
                .flatMap(roleJson -> mongoClient.rxSave(getCollectionName(), roleJson));
    }

    private Maybe<Role> findOneByQuery(Supplier<JsonObject> querySupplier) {
        return Maybe
                .fromCallable(querySupplier::get)
                .flatMap(q -> mongoClient.rxFindOne(getCollectionName(), q, null))
                .map(Role::new);
    }

    private int getListBatchSize() {
        return LIST_BATCH_SIZE;
    }

    private int getFindAllByNamesBatchSize() {
        return FIND_ALL_BY_NAMES_BATCH_SIZE;
    }
}
