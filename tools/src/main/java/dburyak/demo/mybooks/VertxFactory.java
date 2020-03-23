package dburyak.demo.mybooks;

import dburyak.demo.mybooks.di.AppBean;
import io.micronaut.context.annotation.Factory;
import io.vertx.core.VertxOptions;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.reactivex.core.Vertx;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;

@AppBean
@Factory
public class VertxFactory {
    private static final Logger log = LoggerFactory.getLogger(VertxFactory.class);

    @Singleton
    @AppBean
    public Vertx vertx(VertxOptions vertxOptions) {
        var vertx = Vertx.rxClusteredVertx(vertxOptions)
                .blockingGet();
        log.info("create vertx : {}", vertx);
        return vertx;
    }

    @Singleton
    @AppBean
    public VertxOptions vertxOptions(ClusterManager clusterManager) {
        return new VertxOptions().setClusterManager(clusterManager);
    }

    @Singleton
    @AppBean
    public ClusterManager clusterManager() {
        return new HazelcastClusterManager();
    }
}

