package dburyak.demo.mybooks.auth.repository;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Property;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.mongo.MongoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;

@Factory
public class MongoClientFactory {
    private static final Logger log = LoggerFactory.getLogger(MongoClientFactory.class);

    @Property(name = "db.mongo.db-name")
    private String dbName;

    @Property(name = "db.mongo.host")
    private String dbHost;

    @Property(name = "db.mongo.port")
    private int dbPort;

    @Property(name = "db.mongo.max-pool-size")
    private int maxPoolSize;

    @Property(name = "db.mongo.min-pool-size")
    private int minPoolSize;

    @Property(name = "db.mongo.max-idle-time-ms")
    private long maxIdleTimeMs;

    @Singleton
    public MongoClient mongoClient(Vertx vertx) {
        log.debug("creating mongo client conn pool");
        return MongoClient.create(vertx, new JsonObject()
                .put("db_name", dbName)
                .put("host", dbHost)
                .put("port", dbPort)
                .put("minPoolSize", minPoolSize)
                .put("maxPoolSize", maxPoolSize)
                .put("maxIdleTimeMS", maxIdleTimeMs)
        );
    }
}
