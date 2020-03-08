package dburyak.demo.mybooks.auth.service;

import io.micronaut.context.annotation.Property;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jwt.JWTOptions;
import io.vertx.reactivex.ext.auth.User;
import io.vertx.reactivex.ext.auth.jwt.JWTAuth;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.UUID;

@Singleton
public class UserTokenService {

    @Property(name = "jwt.issuer")
    private String jwtIssuer;

    @Property(name = "service.user.jwt.issuer")
    private String userServiceJwtIssuer;

    @Property(name = "jwt.user-token.access.expires-in-minutes")
    private int jwtUserAccessExpMin;

    @Property(name = "jwt.user-token.refresh.expires-in-minutes")
    private int jwtUserRefreshExpMin;

    @Inject
    private JWTAuth jwtAuth;

    public String generateAccessToken(JsonObject userClaims) {
        return jwtAuth.generateToken(userClaims, buildBaseUserJwtOptions()
                .setExpiresInMinutes(jwtUserAccessExpMin));
    }

    public String generateRefreshToken(JsonObject userClaims) {
        return jwtAuth.generateToken(userClaims
                        .put("jti", UUID.randomUUID().toString()),
                buildBaseUserJwtOptions()
                        .setAudience(List.of(jwtIssuer))
                        .setExpiresInMinutes(jwtUserRefreshExpMin));
    }

    public Single<JsonObject> refreshToken(JsonObject refreshToken) {
        // TODO: implement
        return Single.just(new JsonObject());
    }

    public boolean isRequestFromAllowedService(JsonObject principal) {
        // only "user" service can request user-token generation
        var iss = principal.getString("iss");
        return iss != null && iss.startsWith(userServiceJwtIssuer);
    }

    public Single<Boolean> hasPermissionToGenerateToken(User user) {
        return user.rxIsAuthorized(Permissions.USER_TOKEN_GENERATE.toString());
    }

    private JWTOptions buildBaseUserJwtOptions() {
        return new JWTOptions().setIssuer(jwtIssuer);
    }
}
