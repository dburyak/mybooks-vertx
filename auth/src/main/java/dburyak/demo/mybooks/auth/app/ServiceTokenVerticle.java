package dburyak.demo.mybooks.auth.app;

import dburyak.demo.mybooks.MicronautVerticle;
import dburyak.demo.mybooks.MicronautVerticleProducer;

import javax.inject.Singleton;

@Singleton
public class ServiceTokenVerticle extends dburyak.demo.mybooks.ServiceTokenVerticle {
    private static final String SERVICE_TOKEN_ADDR = ServiceTokenVerticle.class + ADDR_GET_SERVICE_TOKEN_SUFFIX;

    @Override
    public String getServiceTokenAddr() {
        return SERVICE_TOKEN_ADDR;
    }

    public static class Producer extends MicronautVerticleProducer<Producer> {

        @Override
        protected MicronautVerticle doCreateVerticle() {
            return new ServiceTokenVerticle();
        }
    }
}
