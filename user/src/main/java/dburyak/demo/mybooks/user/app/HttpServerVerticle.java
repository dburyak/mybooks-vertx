package dburyak.demo.mybooks.user.app;

import dburyak.demo.mybooks.MicronautVerticle;
import dburyak.demo.mybooks.MicronautVerticleProducer;
import dburyak.demo.mybooks.user.endpoints.UserListEndpoint;
import dburyak.demo.mybooks.user.endpoints.UserLoginEndpoint;
import dburyak.demo.mybooks.web.AuthenticatedMicroserviceHttpServerVerticle;
import io.micronaut.context.annotation.Property;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.handler.BodyHandler;
import io.vertx.reactivex.servicediscovery.ServiceDiscovery;
import io.vertx.reactivex.servicediscovery.types.HttpEndpoint;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class HttpServerVerticle extends AuthenticatedMicroserviceHttpServerVerticle {

    @Inject
    private UserLoginEndpoint userLoginEndpoint;

    @Inject
    private UserListEndpoint userListEndpoint;

    @Inject
    private BodyHandler bodyReaderReqHandler;

    @Inject
    private ServiceDiscovery serviceDiscovery;

    @Property(name = "service.user.discovery.base-name")
    private String discoveryBaseName;

    @Property(name = "service.user.discovery.user-login")
    private String discoveryUserLoginName;

    @Property(name = "service.user.discovery.user-logout")
    private String discoveryUserLogoutName;

    @Property(name = "http.host")
    private String httpHost;

    @Property(name = "http.port")
    private int httpPort;

    @Override
    protected void doBuildPublicEndpoints(Router router) {
        userLoginEndpoint.registerEndpoint(router, bodyReaderReqHandler);
        serviceDiscovery.rxPublish(HttpEndpoint
                .createRecord(discoveryBaseName + discoveryUserLoginName, httpHost, httpPort, null))
                .subscribe();
    }

    @Override
    protected void doBuildProtectedEndpoints(Router router) {
        userListEndpoint.registerEndpoint(router, null);
    }

    public static class Producer extends MicronautVerticleProducer<Producer> {

        @Override
        protected MicronautVerticle doCreateVerticle() {
            return new HttpServerVerticle();
        }
    }
}
