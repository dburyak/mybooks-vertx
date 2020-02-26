package dburyak.demo.mybooks.eb;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.json.Json;

import javax.inject.Singleton;
import java.util.List;

@Singleton
public abstract class JsonMessageCodec implements MessageCodec<Object, Object> {

    @Override
    public final void encodeToWire(Buffer buffer, Object data) {
        var className = data.getClass().getCanonicalName();
        var jsonStr = Json.encode(data);
        buffer.appendInt(className.length());
        buffer.appendString(className);
        buffer.appendInt(jsonStr.length());
        buffer.appendString(jsonStr);
    }

    @Override
    public final Object decodeFromWire(int pos, Buffer buffer) {
        var decodedObjects = readMsgObjectsFromWire(pos, buffer);
        Class<?> dataClass = (Class<?>) decodedObjects.get(0);
        String jsonStr = (String) decodedObjects.get(1);
        return Json.decodeValue(jsonStr, dataClass);
    }

    @Override
    public final String name() {
        return getClass().getCanonicalName();
    }

    @Override
    public final byte systemCodecID() {
        return -1;
    }

    private List<Object> readMsgObjectsFromWire(int pos, Buffer buffer) {
        var p = pos;
        var classNameSize = buffer.getInt(p);
        p += Integer.BYTES;
        var className = buffer.getString(p, p + classNameSize);
        p += classNameSize;
        var jsonStrSize = buffer.getInt(p);
        p += Integer.BYTES;
        var jsonStr = buffer.getString(p, p += jsonStrSize);
        try {
            return List.of(Class.forName(className), jsonStr);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("unknown class of encoded data: " + className, e);
        }
    }
}
