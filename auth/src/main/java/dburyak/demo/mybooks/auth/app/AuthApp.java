package dburyak.demo.mybooks.auth.app;

import dburyak.demo.mybooks.MicronautVerticleProducer;
import dburyak.demo.mybooks.MicronautVertxApplication;

import java.util.List;

public class AuthApp extends MicronautVertxApplication {
    public static void main(String[] args) {
        var app = new AuthApp();
        app.start().subscribe();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> app.stop().subscribe()));
    }

    @Override
    public List<MicronautVerticleProducer> getVerticlesProducers() {
        return List.of(new AuthAboutVerticle.Producer());
    }
}