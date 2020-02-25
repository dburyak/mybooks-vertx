package dburyak.demo.mybooks.di;

import io.micronaut.context.annotation.Requires;

import javax.inject.Qualifier;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Verticle local bean scope.
 * Similar to the concept of "ThreadLocal" but works per-verticle instead of per-thread.
 * Should be used when different verticles should have different instances of the annotated bean.
 */
@Qualifier
@Vertx
@Requires(property = "vertx.app.bean.ctx.main", notEquals = "true")
@Retention(RUNTIME)
@Documented
public @interface VerticleBean {
}
