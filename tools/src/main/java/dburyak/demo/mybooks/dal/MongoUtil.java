package dburyak.demo.mybooks.dal;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.impl.codec.json.JsonObjectCodec;
import org.bson.BsonBinarySubType;

import javax.inject.Singleton;
import java.nio.ByteBuffer;
import java.util.UUID;

@Singleton
public class MongoUtil {
    public byte[] toUuidBytes(String uuid) {
        return toUuidBytes(UUID.fromString(uuid));
    }

    public byte[] toUuidBytes(UUID uuid) {
        return ByteBuffer.allocate(16)
                .putLong(uuid.getMostSignificantBits())
                .putLong(uuid.getLeastSignificantBits())
                .array();
    }

    public UUID fromUuidBytes(byte[] uuidBytes) {
        var buf = ByteBuffer.wrap(uuidBytes);
        return new UUID(buf.getLong(), buf.getLong());
    }

    public JsonObject putUuid(UUID uuid, String key, JsonObject mongoObject) {
        return mongoObject.put(key, new JsonObject()
                .put(JsonObjectCodec.BINARY_FIELD, toUuidBytes(uuid))
                .put(JsonObjectCodec.TYPE_FIELD, BsonBinarySubType.UUID_STANDARD.getValue()));
    }

    public JsonObject putUuid(String uuid, String key, JsonObject mongoObject) {
        return putUuid(UUID.fromString(uuid), key, mongoObject);
    }

    public UUID readUuid(String key, JsonObject mongoObject) {
        var bytes = mongoObject.getJsonObject(key).getBinary(JsonObjectCodec.BINARY_FIELD);
        var buf = ByteBuffer.wrap(bytes);
        return new UUID(buf.getLong(), buf.getLong());
    }
}
