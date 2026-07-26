package dev.itemtraits.core;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Thread-safe registry of trait definitions loaded from config. */
public final class TraitRegistry {

    private final Map<String, TraitDefinition> definitions = new LinkedHashMap<>();

    public void register(TraitDefinition def) {
        if (definitions.containsKey(def.key())) {
            throw new IllegalArgumentException("Duplicate trait key: " + def.key());
        }
        definitions.put(def.key(), def);
    }

    public Optional<TraitDefinition> get(String key) {
        return Optional.ofNullable(definitions.get(key));
    }

    public Collection<TraitDefinition> all() {
        return Collections.unmodifiableCollection(definitions.values());
    }

    public void clear() {
        definitions.clear();
    }
}
