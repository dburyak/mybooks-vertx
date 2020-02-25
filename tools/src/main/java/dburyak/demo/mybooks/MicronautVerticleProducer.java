package dburyak.demo.mybooks;

import io.micronaut.context.ApplicationContext;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Verticle;

import java.util.Objects;
import java.util.function.Supplier;

public abstract class MicronautVerticleProducer implements Supplier<Verticle> {
    private String name = getClass().getCanonicalName();
    private ApplicationContext verticleBeanCtx;
    private DeploymentOptions deploymentOptions = new DeploymentOptions();

    @Override
    public final MicronautVerticle get() {
        var verticle = doCreateVerticle();
        if (verticleBeanCtx == null) {
            throw new IllegalStateException("target verticle bean ctx must be specified for micronaut verticle");
        }
        verticle.verticleBeanCtx = verticleBeanCtx;
        return verticle;
    }

    protected abstract MicronautVerticle doCreateVerticle();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setVerticleBeanCtx(ApplicationContext verticleBeanCtx) {
        Objects.requireNonNull(verticleBeanCtx);
        this.verticleBeanCtx = verticleBeanCtx;
    }

    public DeploymentOptions getDeploymentOptions() {
        return deploymentOptions;
    }

    public void setDeploymentOptions(DeploymentOptions deploymentOptions) {
        Objects.requireNonNull(deploymentOptions);
        this.deploymentOptions = deploymentOptions;
    }
}
