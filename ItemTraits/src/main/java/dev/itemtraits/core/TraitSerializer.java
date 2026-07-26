package dev.itemtraits.core;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.itemtraits.api.TraitHolder;

import java.util.LinkedHashMap;
import java.util.Optional;

/**
 * Converts {@link TraitHolder} to/from a versioned JSON string for PDC storage.
 *
 * <p>Schema version is embedded in the JSON as {@code "v"} so future format
 * changes can be deserialized without data loss.
 */
public final class TraitSerializer {

    private static final int CURRENT_VERSION = 1;
    private static final Gson GSON = new Gson();

    public String serialize(TraitHolder holder) {
        var root = new JsonObject();
        root.addProperty("v", CURRENT_VERSION);
        var traits = new JsonObject();
        holder.values().forEach(traits::addProperty);
        root.add("traits", traits);
        return GSON.toJson(root);
    }

    public Optional<TraitHolder> deserialize(String json) {
        try {
            var root = JsonParser.parseString(json).getAsJsonObject();
            int version = root.get("v").getAsInt();
            return switch (version) {
                case 1 -> parseV1(root);
                default -> Optional.empty();
            };
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private Optional<TraitHolder> parseV1(JsonObject root) {
        var traits = root.getAsJsonObject("traits");
        if (traits == null) return Optional.empty();
        var values = new LinkedHashMap<String, Double>();
        for (var entry : traits.entrySet()) {
            values.put(entry.getKey(), entry.getValue().getAsDouble());
        }
        return Optional.of(new TraitHolder(values));
    }
}
