package dburyak.demo.mybooks.auth.app;

import dburyak.demo.mybooks.MicronautVerticleProducer;
import dburyak.demo.mybooks.MicronautVertxApplication;

import java.util.Collections;
import java.util.List;

public class AuthApp extends MicronautVertxApplication {
    public static void main(String[] args) {
        new AuthApp().start().subscribe();
    }

    @Override
    public List<MicronautVerticleProducer> getVerticlesProducers() {
        return Collections.emptyList();
    }
}
