package dburyak.demo.mybooks.auth.app;

import dburyak.demo.mybooks.MicronautVerticle;
import dburyak.demo.mybooks.MicronautVerticleProducer;
import dburyak.demo.mybooks.auth.service.UserTokenService;
import io.micronaut.context.annotation.Value;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

import static java.util.concurrent.TimeUnit.MINUTES;

@Singleton
public class DbCleanupVerticle extends MicronautVerticle {
    private static final Logger log = LogManager.getLogger(DbCleanupVerticle.class);

    private Disposable refreshTokenCleanupSubscription;

    @Value("${user-token.refresh.cleanup-period-min:5}")
    private int refreshTokenCleanupPeriodMin;

    @Inject
    private UserTokenService userTokenService;

    @Override
    protected Completable doStart() {
        return startExpiredRefreshTokensCleanupJob();
    }

    @Override
    protected Completable doStop() {
        return Completable.fromAction(() -> refreshTokenCleanupSubscription.dispose());
    }

    private Completable startExpiredRefreshTokensCleanupJob() {
        return Single
                .fromCallable(() -> Observable.interval(0, refreshTokenCleanupPeriodMin, MINUTES))
                .map(ticker -> ticker
                        .flatMapSingle(tick -> userTokenService.deleteAllExpiredRefreshTokens())
                        .doOnNext(n -> log.debug("expired refresh tokens deleted: numDeleted={}", n))
                        .ignoreElements()
                        .subscribe())
                .doOnSuccess(s -> refreshTokenCleanupSubscription = s)
                .ignoreElement();
    }

    public static class Producer extends MicronautVerticleProducer<Producer> {
        @Override
        protected MicronautVerticle doCreateVerticle() {
            return new DbCleanupVerticle();
        }
    }
}
