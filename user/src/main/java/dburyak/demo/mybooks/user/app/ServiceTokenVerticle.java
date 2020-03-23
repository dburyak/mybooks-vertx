package dburyak.demo.mybooks.user.app;

import dburyak.demo.mybooks.MicronautVerticle;
import dburyak.demo.mybooks.MicronautVerticleProducer;
import io.vertx.ext.jwt.JWTOptions;

import javax.inject.Singleton;

import java.util.List;

import static dburyak.demo.mybooks.domain.Permission.USER_TOKEN_GENERATE;

@Singleton
public class ServiceTokenVerticle extends dburyak.demo.mybooks.ServiceTokenVerticle {
    private static final String ADDR_SERVICE_TOKEN = ServiceTokenVerticle.class + ".getServiceToken";

    @Override
    public String getServiceTokenAddr() {
        return ADDR_SERVICE_TOKEN;
    }

    @Override
    protected void configureJwtOptions(JWTOptions jwtOptions) {
        jwtOptions.setPermissions(List.of(USER_TOKEN_GENERATE.toString()));
    }

    public static class Producer extends MicronautVerticleProducer<Producer> {

        @Override
        protected MicronautVerticle doCreateVerticle() {
            return new ServiceTokenVerticle();
        }
    }
}
