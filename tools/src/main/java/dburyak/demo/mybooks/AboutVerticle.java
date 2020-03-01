package dburyak.demo.mybooks;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.eventbus.EventBus;
import io.vertx.reactivex.core.file.FileSystem;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Singleton
public abstract class AboutVerticle extends MicronautVerticle {
    private String version;
    private String revision;
    private String builtAt;

    @Inject
    private EventBus eventBus;

    @Inject
    private MessageCodec<Object, Object> ebMsgCodec;

    @Inject
    private FileSystem fs;

    public final Single<Map<String, Object>> buildBaseBriefInfo() {
        return Single.fromCallable(() -> {
            var baseInfo = new LinkedHashMap<String, Object>();
            baseInfo.put("version", version);
            return baseInfo;
        });
    }

    public Single<Map<String, Object>> buildBriefInfo() {
        return Single.just(Collections.emptyMap());
    }

    public final Single<Map<String, Object>> buildBaseDetailedInfo() {
        return Single.fromCallable(() -> {
            var baseInfo = new LinkedHashMap<String, Object>();
            baseInfo.put("revision", revision);
            baseInfo.put("built_at", builtAt);
            baseInfo.put("server_time", Instant.now());
            return baseInfo;
        });
    }

    public Single<Map<String, Object>> buildDetailedInfo() {
        return Single.just(Collections.emptyMap());
    }

    public String getBriefInfoAddr() {
        return getClass() + ".briefInfo";
    }

    public String getDetailedInfoAddr() {
        return getClass() + ".detailedInfo";
    }

    @Override
    protected final Completable doStart() {
        return readAndCacheInfoFromFs()
                .andThen(Completable.mergeArray(registerBriefInfoEbConsumer(), registerDetailedInfoEbConsumer()));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private Completable registerBriefInfoEbConsumer() {
        return Completable
                .fromAction(() -> eventBus.consumer(getBriefInfoAddr(), msg -> Single
                        .zip(buildBaseBriefInfo(), buildBriefInfo(), (baseBrief, brief) -> {
                            var info = new LinkedHashMap<String, Object>();
                            info.putAll(baseBrief);
                            info.putAll(brief);
                            return info;
                        })
                        .subscribe(info -> msg.reply(info, new DeliveryOptions().setCodecName(ebMsgCodec.name())))));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private Completable registerDetailedInfoEbConsumer() {
        return Completable.fromAction(() -> eventBus.consumer(getDetailedInfoAddr(), msg -> Single
                .zip(buildBaseBriefInfo(), buildBriefInfo(), buildBaseDetailedInfo(), buildDetailedInfo(),
                        (baseBrief, brief, baseDetailed, detailed) -> {
                    var info = new LinkedHashMap<String, Object>();
                    info.putAll(baseBrief);
                    info.putAll(brief);
                    info.putAll(baseDetailed);
                    info.putAll(detailed);
                    return info;
                })
                .subscribe(info -> msg.reply(info, new DeliveryOptions().setCodecName(ebMsgCodec.name())))));
    }

    private Completable readAndCacheInfoFromFs() {
        return Single
                .zip(readVersionFromFs(), readRevisionFromFs(), readBuiltAtFromFs(), (v, r, b) -> {
                    version = v;
                    revision = r;
                    builtAt = b;
                    return "ignored";
                })
                .ignoreElement();
    }

    private Single<String> readSingleLineFromFsFile(String path) {
        return fs.rxReadFile(path)
                .map(Buffer::toString);
    }

    private Single<String> readVersionFromFs() {
        return readSingleLineFromFsFile("version.txt");
    }

    private Single<String> readRevisionFromFs() {
        return readSingleLineFromFsFile("revision.txt");
    }

    private Single<String> readBuiltAtFromFs() {
        return readSingleLineFromFsFile("built_at.txt");
    }
}
