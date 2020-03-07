package dburyak.demo.mybooks.auth.app;

import dburyak.demo.mybooks.MicronautVerticle;
import dburyak.demo.mybooks.MicronautVerticleProducer;
import dburyak.demo.mybooks.auth.app.endpoints.GetUserTokenEndpoint;
import dburyak.demo.mybooks.web.AuthenticatedMicroserviceHttpServerVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jwt.JWTOptions;
import io.vertx.reactivex.ext.auth.jwt.JWTAuth;
import io.vertx.reactivex.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Base64;
import java.util.List;

@Singleton
public class HttpServerVerticle extends AuthenticatedMicroserviceHttpServerVerticle {
    private static final Logger log = LoggerFactory.getLogger(HttpServerVerticle.class);

    @Inject
    private GetUserTokenEndpoint getUserTokenEndpoint;

    @Inject
    private JWTAuth jwtAuth;

    @Inject
    private Base64.Encoder base64Encoder;

    @Override
    protected void doBuildProtectedEndpoints(Router router) {
        getUserTokenEndpoint.registerEndpoint(router);
    }

    public static class Producer extends MicronautVerticleProducer<Producer> {

        @Override
        protected MicronautVerticle doCreateVerticle() {
            return new HttpServerVerticle();
        }
    }

    // FIXME: delete this vvvv
    @PostConstruct
    private void init() {
        var sampleToken = jwtAuth.generateToken(new JsonObject(), new JWTOptions()
                .setIssuer(":mybooks:service:user")
                .setExpiresInMinutes(10)
                .setPermissions(List.of(":user-token:generate"))
        );
        log.info("sample token: {}", sampleToken);
        jwtAuth.authenticate(new JsonObject().put("jwt", sampleToken), ar -> {
            log.info("sample token : \n{}", ar.result().principal().encodePrettily());
        });

        var sampleJsonData = new JsonObject().put("k1", "v1").put("k2", "v2").encode();
        var encodedJsonData = base64Encoder.encodeToString(sampleJsonData.getBytes());
        log.info("base64 json data: {}", encodedJsonData);
    }
}
