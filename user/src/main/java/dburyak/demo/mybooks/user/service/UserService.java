package dburyak.demo.mybooks.user.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import dburyak.demo.mybooks.domain.Permission;
import dburyak.demo.mybooks.user.domain.User;
import dburyak.demo.mybooks.user.repository.UsersRepository;
import io.micronaut.context.annotation.Value;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.reactivex.core.Vertx;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UserService {

    @Inject
    private UsersRepository usersRepository;

    @Inject
    private RoleService roleService;

    @Inject
    private Vertx vertx;

    @Inject
    private BCrypt.Hasher bcryptHasher;

    @Inject
    private BCrypt.Verifyer bcryptVerifier;

    @Value("${password.bcrypt-hash-cost:15}")
    private int bcryptHashCost;

    public Flowable<Permission> getAllPermissionsOfUserId(String userId) {
        return usersRepository.findByUserId(userId)
                .flatMapPublisher(this::getAllPermissionsOfUser);
    }

    public Flowable<Permission> getAllPermissionsOfUser(User user) {
        return Flowable.fromIterable(user.getExplicitPermissions())
                .concatWith(roleService.getPermissionsOfRoleNames(user.getRoles()))
                .distinct();
    }

    public Single<String> hashPassword(String plainPassword) {
        return vertx
                .<byte[]>rxExecuteBlocking(p ->
                        p.complete(bcryptHasher.hash(bcryptHashCost, plainPassword.toCharArray())), false)
                .map(String::new)
                .toSingle();
    }

    public Single<Boolean> verifyPassword(String plainPassword, String hash) {
        return vertx
                .<BCrypt.Result>rxExecuteBlocking(p ->
                        bcryptVerifier.verify(plainPassword.toCharArray(), hash.toCharArray()), false)
                .map(res -> res.verified)
                .toSingle();
    }

    public Single<Boolean> verifyPassword(String plainPassword, User user) {
        return verifyPassword(plainPassword, user.getPasswordHash());
    }

    public Single<User> hashAndSetPassword(String plainPassword, User user) {
        return hashPassword(plainPassword)
                .map(user::withPasswordHash);
    }
}
