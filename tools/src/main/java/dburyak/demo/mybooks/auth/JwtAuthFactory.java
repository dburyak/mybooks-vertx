package dburyak.demo.mybooks.auth;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Prototype;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.auth.AuthProvider;
import io.vertx.reactivex.ext.auth.jwt.JWTAuth;
import io.vertx.reactivex.ext.web.handler.AuthHandler;
import io.vertx.reactivex.ext.web.handler.JWTAuthHandler;

import javax.inject.Singleton;

@Factory
public class JwtAuthFactory {

    @Singleton
    public AuthProvider jwtAuth(Vertx vertx, JWTAuthOptions jwtAuthOptions) {
        return JWTAuth.create(vertx, jwtAuthOptions);
    }

    @Prototype
    public JWTAuthOptions jwtAuthOptions() {
        return new JWTAuthOptions().addPubSecKey(new PubSecKeyOptions()
                .setAlgorithm("HS256")
                // FIXME: read it from env
                .setPublicKey("public key")
                .setSymmetric(true));
    }

    @Singleton
    public AuthHandler jwtAuthRouterHandler(AuthProvider jwtAuth) {
        return JWTAuthHandler.create((JWTAuth) jwtAuth);
    }
}
