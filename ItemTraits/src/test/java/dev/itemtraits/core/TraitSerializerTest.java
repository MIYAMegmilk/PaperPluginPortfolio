package dev.itemtraits.core;

import dev.itemtraits.api.TraitHolder;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TraitSerializerTest {

    private final TraitSerializer serializer = new TraitSerializer();

    @Test
    void roundTripPreservesValues() {
        var original = new TraitHolder(Map.of("attack", 5.5, "defense", 3.0));
        String json = serializer.serialize(original);
        var result = serializer.deserialize(json);

        assertTrue(result.isPresent());
        assertEquals(5.5, result.get().get("attack"), 0.001);
        assertEquals(3.0, result.get().get("defense"), 0.001);
    }

    @Test
    void emptyHolderRoundTrips() {
        var original = new TraitHolder(Map.of());
        String json = serializer.serialize(original);
        var result = serializer.deserialize(json);

        assertTrue(result.isPresent());
        assertTrue(result.get().isEmpty());
    }

    @Test
    void invalidJsonReturnsEmpty() {
        assertTrue(serializer.deserialize("not json").isEmpty());
        assertTrue(serializer.deserialize("").isEmpty());
    }

    @Test
    void unknownVersionReturnsEmpty() {
        assertTrue(serializer.deserialize("{\"v\":999,\"traits\":{}}").isEmpty());
    }

    @Test
    void jsonContainsVersionField() {
        var holder = new TraitHolder(Map.of("x", 1.0));
        String json = serializer.serialize(holder);
        assertTrue(json.contains("\"v\":1"));
    }
}
