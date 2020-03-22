package dburyak.demo.mybooks.user.app;

import dburyak.demo.mybooks.MicronautVerticle;
import dburyak.demo.mybooks.MicronautVerticleProducer;

import javax.inject.Singleton;

@Singleton
public class AboutVerticle extends dburyak.demo.mybooks.AboutVerticle {

    public static class Producer extends MicronautVerticleProducer<Producer> {

        @Override
        protected MicronautVerticle doCreateVerticle() {
            return new AboutVerticle();
        }
    }
}
