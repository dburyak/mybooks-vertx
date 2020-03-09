package dburyak.demo.mybooks.auth.service;

import dburyak.demo.mybooks.auth.BadUserClaimsException;
import dburyak.demo.mybooks.auth.repository.RefreshTokensRepository;
import io.micronaut.context.annotation.Property;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jwt.JWT;
import io.vertx.ext.jwt.JWTOptions;
import io.vertx.reactivex.ext.auth.User;
import io.vertx.reactivex.ext.auth.jwt.JWTAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.UUID;

@Singleton
public class UserTokenService {
    private static final Logger log = LoggerFactory.getLogger(UserTokenService.class);

    public static final String KEY_ISS = "iss";
    public static final String KEY_SUB = "sub";
    public static final String KEY_DEVICE_ID = "device_id";
    public static final String KEY_JTI = "jti";
    public static final String KEY_IAT = "iat";
    public static final String KEY_EXP = "exp";
    public static final String KEY_ACCESS_TOKEN = "access_token";
    public static final String KEY_REFRESH_TOKEN = "refresh_token";

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

    @Inject
    private JWT jwt;

    @Inject
    private RefreshTokensRepository refreshTokensRepository;

    public Single<JsonObject> generateTokens(JsonObject userClaims) {
        validateUserClaims(userClaims);
        var sub = userClaims.getString(KEY_SUB);
        var deviceId = userClaims.getString(KEY_DEVICE_ID);
        // before generating new access+refresh tokens pair we should invalidate previous refresh token if it exists
        return refreshTokensRepository.deleteBySubAndDeviceId(sub, deviceId)
                .map(isPreviousDeleted -> {
                    if (isPreviousDeleted) {
                        log.debug("previous refresh token was removed by new generate request: userId={}, deviceId={}",
                                sub, deviceId);
                    }
                    return new JsonObject()
                            .put("access_token", generateAccessToken(userClaims))
                            .put("refresh_token", generateRefreshToken(userClaims));
                });
    }

    public Single<JsonObject> refreshTokens(JsonObject refreshToken) {
        var userClaims = refreshToken.copy();
        userClaims.remove(KEY_JTI);
        userClaims.remove(KEY_IAT);
        userClaims.remove(KEY_EXP);
        return Single
                .fromCallable(() -> {
                    validateUserClaims(userClaims);
                    return generateRefreshToken(userClaims);
                })
                .flatMap(newRefreshTokenStr -> {
                    var newRefreshToken = jwt.decode(newRefreshTokenStr);
                    var oldJti = refreshToken.getString(KEY_JTI);
                    return refreshTokensRepository.findAndReplaceByJti(oldJti, newRefreshToken)
                            .switchIfEmpty(Single.error(new RefreshTokenNotRegisteredException(refreshToken)))
                            .doOnSuccess(t -> log.debug("token refreshed: userId={}, deviceId={}, jti={}",
                                    t.getString(KEY_SUB), t.getString(KEY_DEVICE_ID), t.getString(KEY_JTI)))
                            .map(ignr -> newRefreshTokenStr);
                })
                .map(newRefreshTokenStr -> new JsonObject()
                        .put(KEY_ACCESS_TOKEN, generateAccessToken(userClaims))
                        .put(KEY_REFRESH_TOKEN, newRefreshTokenStr));
    }

    public boolean isRequestFromAllowedService(JsonObject principal) {
        // only "user" service can request user-token generation
        var iss = principal.getString(KEY_ISS);
        return iss != null && iss.startsWith(userServiceJwtIssuer);
    }

    public Single<Boolean> hasPermissionToGenerateToken(User user) {
        return user.rxIsAuthorized(Permissions.USER_TOKEN_GENERATE.toString());
    }

    private String generateAccessToken(JsonObject userClaims) {
        validateUserClaims(userClaims);
        return jwtAuth.generateToken(userClaims, buildBaseUserJwtOptions()
                .setExpiresInMinutes(jwtUserAccessExpMin));
    }

    private String generateRefreshToken(JsonObject userClaims) {
        validateUserClaims(userClaims);
        return jwtAuth.generateToken(userClaims
                        .put(KEY_JTI, UUID.randomUUID()),
                buildBaseUserJwtOptions()
                        .setAudience(List.of(jwtIssuer))
                        .setExpiresInMinutes(jwtUserRefreshExpMin));
    }

    private JWTOptions buildBaseUserJwtOptions() {
        return new JWTOptions().setIssuer(jwtIssuer);
    }

    private void validateUserClaims(JsonObject userClaims) {
        if (!userClaims.containsKey(KEY_SUB)) {
            throw new BadUserClaimsException("must contain \"" + KEY_SUB + "\" with user id", userClaims);
        }
        if (!userClaims.containsKey(KEY_DEVICE_ID)) {
            throw new BadUserClaimsException("must contain \"" + KEY_DEVICE_ID + "\"", userClaims);
        }
        if (userClaims.containsKey(KEY_JTI)) {
            throw new BadUserClaimsException("must NOT contain \"" + KEY_JTI + "\"", userClaims);
        }
    }
}
