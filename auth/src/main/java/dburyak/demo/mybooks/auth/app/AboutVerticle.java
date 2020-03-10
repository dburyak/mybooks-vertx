package dburyak.demo.mybooks.auth.app;

import dburyak.demo.mybooks.MicronautVerticle;
import dburyak.demo.mybooks.MicronautVerticleProducer;
import dburyak.demo.mybooks.auth.repository.RefreshTokensRepository;
import io.reactivex.Single;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

@Singleton
public class AboutVerticle extends dburyak.demo.mybooks.AboutVerticle {

    @Inject
    private RefreshTokensRepository refreshTokensRepository;

    @Override
    public Single<Map<String, Object>> buildDetailedInfo() {
        return refreshTokensRepository.count()
                .map(numRefreshTokensRegistered -> Map.of("refresh_tokens", numRefreshTokensRegistered));
    }

    public static class Producer extends MicronautVerticleProducer<Producer> {

        @Override
        protected MicronautVerticle doCreateVerticle() {
            return new AboutVerticle();
        }
    }
}
