package dburyak.demo.mybooks.user.app;

import dburyak.demo.mybooks.MicronautVerticleProducer;
import dburyak.demo.mybooks.MicronautVertxApplication;

import java.util.Collections;
import java.util.List;

public class App extends MicronautVertxApplication {
    public static void main(String[] args) {
        var app = new App();
        app.start().subscribe();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            app.stop().blockingAwait();
            System.out.println("application stopped");
        }));
    }

    @Override
    public List<MicronautVerticleProducer<?>> getVerticlesProducers() {
        return Collections.emptyList();
    }
}
