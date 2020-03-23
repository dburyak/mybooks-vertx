package dburyak.demo.mybooks.auth.app;

import dburyak.demo.mybooks.MicronautVerticle;
import dburyak.demo.mybooks.MicronautVerticleProducer;
import dburyak.demo.mybooks.auth.endpoints.GetUserTokenEndpoint;
import dburyak.demo.mybooks.auth.endpoints.RefreshUserTokenEndpoint;
import dburyak.demo.mybooks.web.AuthenticatedMicroserviceHttpServerVerticle;
import io.micronaut.context.annotation.Property;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.servicediscovery.ServiceDiscovery;
import io.vertx.reactivex.servicediscovery.types.HttpEndpoint;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class HttpServerVerticle extends AuthenticatedMicroserviceHttpServerVerticle {

    @Inject
    private GetUserTokenEndpoint getUserTokenEndpoint;

    @Inject
    private RefreshUserTokenEndpoint refreshUserTokenEndpoint;

    @Inject
    private ServiceDiscovery serviceDiscovery;

    @Property(name = "service.auth.discovery.base-name")
    private String discoveryBaseName;

    @Property(name = "service.auth.discovery.get-user-token")
    private String discoveryGetUserTokenName;

    @Property(name = "service.auth.discovery.refresh-user-token")
    private String discoveryRefreshUserTokenName;

    @Property(name = "http.host")
    private String httpHost;

    @Property(name = "http.port")
    private int httpPort;

    @Override
    protected void doBuildPublicEndpoints(Router router) {
        refreshUserTokenEndpoint.registerEndpoint(router, null);
        serviceDiscovery.rxPublish(HttpEndpoint
                .createRecord(discoveryBaseName + discoveryRefreshUserTokenName, httpHost, httpPort, null))
                .subscribe();
    }

    @Override
    protected void doBuildProtectedEndpoints(Router router) {
        getUserTokenEndpoint.registerEndpoint(router, null);
        serviceDiscovery.rxPublish(HttpEndpoint
                .createRecord(discoveryBaseName + discoveryGetUserTokenName, httpHost, httpPort, null))
                .subscribe();
    }

    public static class Producer extends MicronautVerticleProducer<Producer> {

        @Override
        protected MicronautVerticle doCreateVerticle() {
            return new HttpServerVerticle();
        }
    }
}
