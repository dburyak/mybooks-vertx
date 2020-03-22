package dburyak.demo.mybooks.user.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import io.micronaut.context.annotation.Factory;

import javax.inject.Singleton;

@Factory
public class BcryptFactory {

    @Singleton
    public BCrypt.Hasher bcryptHasher() {
        return BCrypt.withDefaults();
    }

    @Singleton
    public BCrypt.Verifyer bcryptVerifier() {
        return BCrypt.verifyer();
    }
}
