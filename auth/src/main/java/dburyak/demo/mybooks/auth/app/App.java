package dburyak.demo.mybooks.auth.app;

import dburyak.demo.mybooks.MicronautVerticleProducer;
import dburyak.demo.mybooks.MicronautVertxApplication;

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
    public List<MicronautVerticleProducer> getVerticlesProducers() {
        return List.of(
                new AboutVerticle.Producer(),
                new HealthVerticle.Producer(),
                new HttpServerVerticle.Producer());
    }
}
