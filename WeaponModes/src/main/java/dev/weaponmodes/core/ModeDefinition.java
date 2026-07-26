package dev.weaponmodes.core;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;

import java.util.List;
import java.util.Map;

/** Immutable definition of a single weapon mode, loaded from config. */
public record ModeDefinition(
        String key,
        String displayName,
        List<String> lore,
        Map<Attribute, AttributeModifier> attributes,
        double lifesteal,
        List<ModeEffect> onHitEffects,
        List<ModeEffect> whileHeldEffects
) {

    public static EquipmentSlotGroup parseSlotGroup(String name) {
        return switch (name.toUpperCase()) {
            case "MAINHAND", "HAND" -> EquipmentSlotGroup.MAINHAND;
            case "OFFHAND", "OFF_HAND" -> EquipmentSlotGroup.OFFHAND;
            case "HEAD" -> EquipmentSlotGroup.HEAD;
            case "CHEST" -> EquipmentSlotGroup.CHEST;
            case "LEGS" -> EquipmentSlotGroup.LEGS;
            case "FEET" -> EquipmentSlotGroup.FEET;
            default -> EquipmentSlotGroup.ANY;
        };
    }
}
