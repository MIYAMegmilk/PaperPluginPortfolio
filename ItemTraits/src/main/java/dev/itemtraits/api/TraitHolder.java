package dev.itemtraits.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable snapshot of trait values attached to a single item.
 * All mutation methods return a new instance.
 */
public final class TraitHolder {

    private final Map<String, Double> values;

    public TraitHolder(Map<String, Double> values) {
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    /** All trait key-value pairs. */
    public Map<String, Double> values() {
        return values;
    }

    /** Get a trait value, or 0 if absent. */
    public double get(String key) {
        return values.getOrDefault(key, 0.0);
    }

    public boolean has(String key) {
        return values.containsKey(key);
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    /** Return a new holder with the given key set (or replaced). */
    public TraitHolder with(String key, double value) {
        var copy = new LinkedHashMap<>(values);
        copy.put(key, value);
        return new TraitHolder(copy);
    }

    /** Return a new holder with the given key multiplied by a factor. No-op if key absent. */
    public TraitHolder multiply(String key, double factor) {
        if (!values.containsKey(key)) return this;
        return with(key, values.get(key) * factor);
    }
}
