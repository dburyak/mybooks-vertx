package dburyak.demo.mybooks;

import dburyak.demo.mybooks.di.AppBean;
import dburyak.demo.mybooks.di.EventLoop;
import dburyak.demo.mybooks.di.Worker;
import io.micronaut.context.annotation.Context;
import io.reactivex.Scheduler;
import io.reactivex.plugins.RxJavaPlugins;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

@Context
@AppBean
public class AppInitialization {
    private static final Logger log = LoggerFactory.getLogger(AppInitialization.class);

    @Inject
    @EventLoop
    private Scheduler vertxRxScheduler;

    @Inject
    @Worker
    private Scheduler vertxRxBlockingScheduler;

    @PostConstruct
    void init() {
        log.info("initializing application");
        registerRxJavaVertxSchedulers();
        log.info("application initialized");
    }

    private void registerRxJavaVertxSchedulers() {
        log.debug("register vertx rx schedulers");
        RxJavaPlugins.setComputationSchedulerHandler(ignr -> vertxRxScheduler);
        RxJavaPlugins.setIoSchedulerHandler(ignr -> vertxRxBlockingScheduler);
        RxJavaPlugins.setNewThreadSchedulerHandler(ignr -> vertxRxScheduler);
    }
}
