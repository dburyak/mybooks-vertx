package dburyak.demo.mybooks.discovery;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.servicediscovery.ServiceDiscovery;

import javax.inject.Singleton;

@Factory
public class ServiceDiscoveryFactory {

    @Singleton
    @Bean(preDestroy = "close")
    public ServiceDiscovery serviceDiscovery(Vertx vertx) {
        return ServiceDiscovery.create(vertx);
    }
}
