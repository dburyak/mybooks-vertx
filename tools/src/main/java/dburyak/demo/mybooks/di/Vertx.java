package dburyak.demo.mybooks.di;

import javax.inject.Qualifier;
import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Vertx specific bean implementation.
 */
@Qualifier
@Retention(RUNTIME)
public @interface Vertx {
}
