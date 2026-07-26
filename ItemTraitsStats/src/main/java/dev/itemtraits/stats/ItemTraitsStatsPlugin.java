package dev.itemtraits.stats;

import dev.itemtraits.api.TraitHolder;
import dev.itemtraits.api.event.TraitsGenerateEvent;
import dev.itemtraits.api.event.TraitsTransformEvent;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ItemTraitsStatsPlugin extends JavaPlugin implements Listener {

    private record Binding(NamespacedKey key, Attribute attribute,
                           AttributeModifier.Operation operation,
                           EquipmentSlotGroup slotGroup, double scale) {}

    private final Map<String, Binding> bindings = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadBindings();
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("Loaded " + bindings.size() + " stat binding(s).");
    }

    private void loadBindings() {
        bindings.clear();
        ConfigurationSection section = getConfig().getConfigurationSection("bindings");
        if (section == null) return;
        for (String traitKey : section.getKeys(false)) {
            var sub = section.getConfigurationSection(traitKey);
            try {
                var attr = Attribute.valueOf(sub.getString("attribute"));
                var op = AttributeModifier.Operation.valueOf(sub.getString("operation"));
                var slot = parseSlotGroup(sub.getString("slot", "any"));
                double scale = sub.getDouble("scale", 1.0);
                bindings.put(traitKey, new Binding(new NamespacedKey(this, traitKey), attr, op, slot, scale));
            } catch (IllegalArgumentException e) {
                getLogger().warning("Invalid binding for '" + traitKey + "': " + e.getMessage());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGenerate(TraitsGenerateEvent event) {
        ItemStack item = event.getItem();
        TraitHolder traits = event.getTraits();
        // Main plugin writes PDC+lore after event returns; apply modifiers next tick
        Bukkit.getScheduler().runTask(this, () -> applyModifiers(item, traits));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTransform(TraitsTransformEvent event) {
        ItemStack item = event.getTarget();
        TraitHolder traits = event.getTraits();
        Bukkit.getScheduler().runTask(this, () -> applyModifiers(item, traits));
    }

    private void applyModifiers(ItemStack item, TraitHolder traits) {
        ItemMeta meta = item.getItemMeta();

        for (var binding : bindings.values()) {
            var existing = meta.getAttributeModifiers(binding.attribute());
            if (existing != null) {
                for (var mod : List.copyOf(existing)) {
                    if (mod.getKey().equals(binding.key())) {
                        meta.removeAttributeModifier(binding.attribute(), mod);
                    }
                }
            }
        }

        for (var entry : traits.values().entrySet()) {
            var binding = bindings.get(entry.getKey());
            if (binding == null) continue;
            double value = entry.getValue() * binding.scale();
            meta.addAttributeModifier(binding.attribute(),
                    new AttributeModifier(binding.key(), value, binding.operation(), binding.slotGroup()));
        }

        item.setItemMeta(meta);
    }

    private static EquipmentSlotGroup parseSlotGroup(String name) {
        return switch (name.toUpperCase()) {
            case "HAND", "MAINHAND" -> EquipmentSlotGroup.MAINHAND;
            case "OFF_HAND", "OFFHAND" -> EquipmentSlotGroup.OFFHAND;
            case "HEAD" -> EquipmentSlotGroup.HEAD;
            case "CHEST" -> EquipmentSlotGroup.CHEST;
            case "LEGS" -> EquipmentSlotGroup.LEGS;
            case "FEET" -> EquipmentSlotGroup.FEET;
            default -> EquipmentSlotGroup.ANY;
        };
    }
}
