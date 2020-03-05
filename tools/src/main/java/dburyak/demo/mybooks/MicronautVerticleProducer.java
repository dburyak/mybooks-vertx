package dburyak.demo.mybooks;

import io.micronaut.context.ApplicationContext;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Verticle;

import java.util.Objects;
import java.util.function.Supplier;

public abstract class MicronautVerticleProducer<I extends MicronautVerticleProducer<I>> implements Supplier<Verticle> {
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

    public I setName(String name) {
        this.name = name;
        return (I) this;
    }

    public I setVerticleBeanCtx(ApplicationContext verticleBeanCtx) {
        Objects.requireNonNull(verticleBeanCtx);
        this.verticleBeanCtx = verticleBeanCtx;
        return (I) this;
    }

    public DeploymentOptions getDeploymentOptions() {
        return deploymentOptions;
    }

    public I setDeploymentOptions(DeploymentOptions deploymentOptions) {
        Objects.requireNonNull(deploymentOptions);
        this.deploymentOptions = deploymentOptions;
        return (I) this;
    }
}
