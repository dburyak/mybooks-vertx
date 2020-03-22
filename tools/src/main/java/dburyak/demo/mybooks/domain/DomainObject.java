package dburyak.demo.mybooks.domain;

import io.vertx.core.json.JsonObject;

public interface DomainObject<T extends DomainObject<T>> {
    T setAllFromJson(JsonObject json);

    JsonObject toJson();

    String getDbId();

    void setDbId(String newDbId);
}
