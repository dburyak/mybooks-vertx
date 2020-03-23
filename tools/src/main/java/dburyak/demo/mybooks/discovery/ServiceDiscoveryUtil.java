package dburyak.demo.mybooks.discovery;

import io.micronaut.context.annotation.Property;
import io.reactivex.Observable;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.eventbus.EventBus;
import io.vertx.reactivex.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.Record;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.function.Predicate;

@Singleton
public class ServiceDiscoveryUtil {

    @Property(name = "service.discovery.announce-addr")
    private String discoveryAnnounceAddr;

    @Property(name = "service.discovery.usage-addr")
    private String discoveryUsageAddr;

    @Inject
    private ServiceDiscovery discovery;

    @Inject
    private EventBus eventBus;

    public Observable<Record> discover(String serviceName, Predicate<Record> filter) {
        return discovery
                .rxGetRecords(filter::test)
                .flatMapObservable(Observable::fromIterable)
                .concatWith(Observable
                        .<JsonObject>create(emitter -> {
                            var ebConsumer = eventBus.<JsonObject>consumer(discoveryAnnounceAddr,
                                    msg -> emitter.onNext(msg.body()));
                            emitter.setCancellable(ebConsumer::unregister);
                        })
                        .map(Record::new)
                        .filter(filter::test));
    }
}
