package dburyak.demo.mybooks.dal;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.impl.codec.json.JsonObjectCodec;
import org.bson.BsonBinarySubType;

import javax.inject.Singleton;
import java.nio.ByteBuffer;
import java.util.UUID;

@Singleton
public class MongoUtil {
    public static final String KEY_DB_ID = "_id";
    public static final String KEY_OID = "$oid";
    public static final String OPERATOR_IN = "$in";
    public static final String OPERATOR_NOT = "$not";

    /**
     * Greater or equal to.
     */
    public static final String OPERATOR_GTE = "$gte";

    public String getKeyDbId() {
        return KEY_DB_ID;
    }

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

    public JsonObject withDecodedObjectIdInPlace(JsonObject mongoObject) {
        if (mongoObject.containsKey(KEY_DB_ID)) {
            var dbId = mongoObject.getValue(KEY_DB_ID);
            if (dbId instanceof JsonObject) {
                mongoObject.put(KEY_DB_ID, ((JsonObject) dbId).getString(KEY_OID));
            }
        }
        return mongoObject;
    }

    public JsonObject withEncodedObjectIdInPlace(JsonObject mongoObject) {
        if (mongoObject.containsKey(KEY_DB_ID)) {
            var dbId = mongoObject.getValue(KEY_DB_ID);
            if (dbId instanceof CharSequence) {
                mongoObject.put(KEY_DB_ID, new JsonObject().put(KEY_OID, dbId));
            }
        }
        return mongoObject;
    }

    public JsonObject withDecodedObjectId(JsonObject mongoObject) {
        var copy = mongoObject.copy();
        return withDecodedObjectIdInPlace(copy);
    }

    public JsonObject withEncodedObjectId(JsonObject mongoObject) {
        var copy = mongoObject.copy();
        return withEncodedObjectIdInPlace(copy);
    }
}
