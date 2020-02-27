package dburyak.demo.mybooks.auth;

import io.micronaut.context.annotation.Factory;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.auth.jwt.JWTAuth;

import javax.inject.Singleton;

@Factory
public class JwtAuthProviderFactory {

    @Singleton
    public JWTAuth jwtAuth(Vertx vertx) {
        return JWTAuth.create(vertx, new JWTAuthOptions());
    }
}
