package dburyak.demo.mybooks;

import dburyak.demo.mybooks.di.AppBean;
import dburyak.demo.mybooks.di.EventLoop;
import dburyak.demo.mybooks.di.Worker;
import io.micronaut.context.annotation.Factory;
import io.reactivex.Scheduler;
import io.vertx.reactivex.core.RxHelper;
import io.vertx.reactivex.core.Vertx;

import javax.inject.Singleton;

@AppBean
@Factory
public class VertxRxSchedulerFactory {

    @Singleton
    @AppBean
    @EventLoop
    public Scheduler vertxRxScheduler(Vertx vertx) {
        return RxHelper.scheduler(vertx);
    }

    @Singleton
    @AppBean
    @Worker
    public Scheduler vertxRxBlockingScheduler(Vertx vertx) {
        return RxHelper.blockingScheduler(vertx);
    }
}
