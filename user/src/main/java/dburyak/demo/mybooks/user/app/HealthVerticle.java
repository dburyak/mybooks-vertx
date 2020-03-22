package dburyak.demo.mybooks.user.app;

import dburyak.demo.mybooks.MicronautVerticle;
import dburyak.demo.mybooks.MicronautVerticleProducer;

import javax.inject.Singleton;

@Singleton
public class HealthVerticle extends dburyak.demo.mybooks.HealthVerticle {

    public static class Producer extends MicronautVerticleProducer<Producer> {

        @Override
        protected MicronautVerticle doCreateVerticle() {
            return new HealthVerticle();
        }
    }
}
