package dburyak.demo.mybooks.eb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.lang.reflect.InvocationTargetException;

@Singleton
public class LocalAwareJsonCopyingMessageCodec extends JsonMessageCodec {
    private static final Logger log = LoggerFactory.getLogger(LocalAwareJsonCopyingMessageCodec.class);

    @Override
    public Object transform(Object data) {
        // copy object via copy constructor
        var dataClass = data.getClass();
        try {
            return dataClass.getDeclaredConstructor(data.getClass()).newInstance(data);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException
                | NoSuchMethodException e) {
            log.error("failed to call copy constructor of data object", e);
            throw new IllegalArgumentException("data object must implement copy constructor: " + data, e);
        }
    }
}
