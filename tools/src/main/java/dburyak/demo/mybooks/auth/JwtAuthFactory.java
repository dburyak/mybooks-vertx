package dburyak.demo.mybooks.auth;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Prototype;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.auth.AuthProvider;
import io.vertx.reactivex.ext.auth.jwt.JWTAuth;
import io.vertx.reactivex.ext.web.handler.JWTAuthHandler;

import javax.inject.Singleton;

@Factory
public class JwtAuthFactory {

    @Property(name = "jwt.private-key")
    private String jwtPrivateKey;

    @Property(name = "jwt.algorithm")
    private String jwtAlgorithm;

    @Property(name = "jwt.is-symmetric")
    private boolean isSymmetric;

    @Singleton
    public JWTAuth jwtAuth(Vertx vertx, JWTAuthOptions jwtAuthOptions) {
        return JWTAuth.create(vertx, jwtAuthOptions);
    }

    @Prototype
    public JWTAuthOptions jwtAuthOptions() {
        var cryptKeysOpts = new PubSecKeyOptions()
                .setAlgorithm(jwtAlgorithm)
                .setSymmetric(isSymmetric);
        if (isSymmetric) {
            cryptKeysOpts.setPublicKey(jwtPrivateKey);
        } else {
            cryptKeysOpts.setSecretKey(jwtPrivateKey);
        }
        return new JWTAuthOptions().addPubSecKey(cryptKeysOpts);
    }

    @Singleton
    public JWTAuthHandler jwtAuthRouterHandler(AuthProvider jwtAuth) {
        return JWTAuthHandler.create((JWTAuth) jwtAuth);
    }
}
