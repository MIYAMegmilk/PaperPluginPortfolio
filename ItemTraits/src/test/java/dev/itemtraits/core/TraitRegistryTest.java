package dev.itemtraits.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TraitRegistryTest {

    @Test
    void registerAndRetrieve() {
        var reg = new TraitRegistry();
        var def = new TraitDefinition("atk", "Attack", 1, 10, 0, "{value}");
        reg.register(def);

        assertTrue(reg.get("atk").isPresent());
        assertEquals("Attack", reg.get("atk").get().displayName());
    }

    @Test
    void duplicateKeyThrows() {
        var reg = new TraitRegistry();
        var def = new TraitDefinition("atk", "Attack", 1, 10, 0, "{value}");
        reg.register(def);
        assertThrows(IllegalArgumentException.class, () -> reg.register(def));
    }

    @Test
    void unknownKeyReturnsEmpty() {
        var reg = new TraitRegistry();
        assertTrue(reg.get("nonexistent").isEmpty());
    }

    @Test
    void clearRemovesAll() {
        var reg = new TraitRegistry();
        reg.register(new TraitDefinition("a", "A", 0, 1, 0, "{value}"));
        reg.clear();

        assertTrue(reg.get("a").isEmpty());
        assertEquals(0, reg.all().size());
    }

    @Test
    void definitionValidation() {
        assertThrows(IllegalArgumentException.class,
                () -> new TraitDefinition("bad", "Bad", 10, 1, 0, "{value}"));
        assertThrows(IllegalArgumentException.class,
                () -> new TraitDefinition("bad", "Bad", 0, 10, -1, "{value}"));
    }

    @Test
    void formatValue() {
        var def = new TraitDefinition("atk", "Attack", 0, 20, 1, "+{value}");
        assertEquals("+5.5", def.formatValue(5.5));
        assertEquals("+0.0", def.formatValue(0.0));

        var intDef = new TraitDefinition("def", "Defense", 0, 10, 0, "{value}pts");
        assertEquals("3pts", intDef.formatValue(3.7));
    }
}
