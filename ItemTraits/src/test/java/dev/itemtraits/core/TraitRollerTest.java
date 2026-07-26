package dev.itemtraits.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class TraitRollerTest {

    @Test
    void valuesStayWithinBounds() {
        var roller = new TraitRoller(new Random(42));
        var def = new TraitDefinition("atk", "Attack", 1.0, 10.0, 1, "+{value}");
        for (int i = 0; i < 200; i++) {
            double val = roller.rollSingle(def);
            assertTrue(val >= 1.0 && val <= 10.0, "Out of range: " + val);
        }
    }

    @Test
    void precisionZeroProducesIntegers() {
        var roller = new TraitRoller(new Random(42));
        var def = new TraitDefinition("def", "Defense", 1.0, 100.0, 0, "{value}");
        for (int i = 0; i < 100; i++) {
            double val = roller.rollSingle(def);
            assertEquals(val, Math.floor(val), 0.001, "Not integer: " + val);
        }
    }

    @Test
    void rollAllDefinitions() {
        var roller = new TraitRoller(new Random(42));
        var defs = List.of(
                new TraitDefinition("a", "A", 0, 10, 0, "{value}"),
                new TraitDefinition("b", "B", 5, 20, 1, "{value}")
        );
        var holder = roller.roll(defs);
        assertTrue(holder.has("a"));
        assertTrue(holder.has("b"));
        assertEquals(2, holder.values().size());
    }

    @Test
    void sameSeedProducesSameResult() {
        var def = new TraitDefinition("x", "X", 0, 100, 2, "{value}");
        double v1 = new TraitRoller(new Random(123)).rollSingle(def);
        double v2 = new TraitRoller(new Random(123)).rollSingle(def);
        assertEquals(v1, v2, 0.0001);
    }

    @Test
    void roundFunction() {
        assertEquals(3.1, TraitRoller.round(3.14, 1), 0.0001);
        assertEquals(3.0, TraitRoller.round(3.14, 0), 0.0001);
        assertEquals(3.14, TraitRoller.round(3.14, 2), 0.0001);
    }
}
