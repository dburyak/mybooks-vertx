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

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Base64;
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

    @Inject
    private Base64.Encoder base64Encoder;

    public Single<JsonObject> generateTokens(JsonObject userClaims) {
        validateUserClaims(userClaims);
        var sub = userClaims.getString(KEY_SUB);
        var deviceId = userClaims.getString(KEY_DEVICE_ID);

        return Single
                .fromCallable(() -> new JsonObject()
                        .put(KEY_ACCESS_TOKEN, generateAccessToken(userClaims))
                        .put(KEY_REFRESH_TOKEN, generateRefreshToken(userClaims)))
                .flatMap(newTokensJson -> {
                    var newRefreshTokenStr = newTokensJson.getString(KEY_REFRESH_TOKEN);
                    var newRefreshToken = jwt.decode(newRefreshTokenStr);
                    // before generating new access+refresh tokens pair we should invalidate previous refresh token
                    // if it exists
                    return refreshTokensRepository.findAndReplaceUpsertBySubAndDeviceId(sub, deviceId, newRefreshToken)
                            .map(ignr -> true)
                            .toSingle(false)
                            .doOnSuccess(isPreviousDeleted -> {
                                if (isPreviousDeleted) {
                                    log.debug("previous refresh token was removed by new generate tokens request: " +
                                            "userId={}, deviceId={}", sub, deviceId);
                                }
                            })
                            .map(ignr -> newTokensJson);
                });
    }

    public Single<JsonObject> refreshTokens(JsonObject refreshToken) {
        var userClaims = refreshToken.copy();
        var oldJti = (String) userClaims.remove(KEY_JTI);
        userClaims.remove(KEY_IAT);
        userClaims.remove(KEY_EXP);
        return Single
                .fromCallable(() -> generateRefreshToken(userClaims))
                .flatMap(newRefreshTokenStr -> {
                    var newRefreshToken = jwt.decode(newRefreshTokenStr);
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

    public Single<Boolean> hasPermissionToGenerateToken(User user) {
        var principal = user.principal();
        var iss = principal.getString(KEY_ISS);
        var isRequestFromUserService = iss != null && iss.startsWith(userServiceJwtIssuer);
        return isRequestFromUserService
                ? user.rxIsAuthorized(Permissions.USER_TOKEN_GENERATE.toString())
                : Single.just(false);
    }

    private String generateAccessToken(JsonObject userClaims) {
        validateUserClaims(userClaims);
        return jwtAuth.generateToken(userClaims, buildBaseUserJwtOptions()
                .setExpiresInMinutes(jwtUserAccessExpMin));
    }

    private String generateRefreshToken(JsonObject userClaims) {
        validateUserClaims(userClaims);
        return jwtAuth.generateToken(userClaims
                        .put(KEY_JTI, UUID.randomUUID().toString()),
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

    // FIXME: delete this vvvv
    @PostConstruct
    private void init() {
        var postmanUserServiceToken = jwtAuth.generateToken(new JsonObject()
                        .put("description", "postman user service token"),
                new JWTOptions()
                        .setIssuer(userServiceJwtIssuer)
                        .setSubject(userServiceJwtIssuer)
                        .setExpiresInMinutes(100 * 365 * 24 * 60)
                        .setPermissions(List.of(Permissions.USER_TOKEN_GENERATE.toString())));
        log.info("postman user service token: {}", postmanUserServiceToken);
        jwtAuth.authenticate(new JsonObject().put("jwt", postmanUserServiceToken), ar -> {
            log.info("postman user service token data: \n{}", ar.result().principal().encodePrettily());
        });

        var sampleUserClaims = new JsonObject()
                .put(UserTokenService.KEY_SUB, UUID.randomUUID().toString())
                .put(UserTokenService.KEY_DEVICE_ID, UUID.randomUUID().toString())
                .put("k1", "v1")
                .put("k2", "v2").encode();
        var encodedUserClaims = base64Encoder.encodeToString(sampleUserClaims.getBytes());
        log.info("base64 encoded user claims: {}", encodedUserClaims);
    }
}
