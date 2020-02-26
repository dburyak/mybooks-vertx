package dburyak.demo.mybooks.eb;

import io.micronaut.context.annotation.Primary;

import javax.inject.Singleton;

@Singleton
@Primary
public class LocalAwareKryoUnsafeMessageCodec extends KryoMessageCodec {

    @Override
    public Object transform(Object data) {
        return data;
    }
}
