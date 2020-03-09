package dburyak.demo.mybooks.auth;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Property;
import io.vertx.ext.jwt.JWK;
import io.vertx.ext.jwt.JWT;

import javax.inject.Singleton;

@Factory
public class JwtFactory {

    @Property(name = "jwt.private-key")
    private String jwtPrivateKey;

    @Property(name = "jwt.algorithm")
    private String jwtAlgorithm;

    @Property(name = "jwt.is-symmetric")
    private boolean isSymmetric;

    @Singleton
    public JWT jwt(JWK jwk) {
        return new JWT().addJWK(jwk);
    }

    @Singleton
    public JWK jwk() {
        if (!isSymmetric) {
            throw new AssertionError("asymmetric keys are not implemented");
        }
        return new JWK(jwtAlgorithm, jwtPrivateKey);
    }
}
