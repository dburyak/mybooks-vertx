package dburyak.demo.mybooks.eb;

import javax.inject.Singleton;

@Singleton
public class LocalAwareJsonUnsafeMessageCodec extends JsonMessageCodec {

    @Override
    public Object transform(Object data) {
        return data;
    }
}
