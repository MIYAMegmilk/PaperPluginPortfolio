package dev.itemtraits;

import dev.itemtraits.api.ItemTraitsAPI;
import dev.itemtraits.api.TraitHolder;
import dev.itemtraits.api.event.TraitsGenerateEvent;
import dev.itemtraits.api.event.TraitsTransformEvent;
import dev.itemtraits.command.TraitsCommand;
import dev.itemtraits.core.TraitDefinition;
import dev.itemtraits.core.TraitRegistry;
import dev.itemtraits.core.TraitRoller;
import dev.itemtraits.core.TraitSerializer;
import dev.itemtraits.gui.TraitInspectGui;
import dev.itemtraits.persistence.PdcTraitStore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public final class ItemTraitsPlugin extends JavaPlugin implements ItemTraitsAPI {

    private final TraitRegistry registry = new TraitRegistry();
    private final TraitSerializer serializer = new TraitSerializer();
    private NamespacedKey loreCountKey;
    private TraitRoller roller;
    private PdcTraitStore store;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadDefinitions();
        loreCountKey = new NamespacedKey(this, "lore-lines");
        roller = new TraitRoller(new Random());
        store = new PdcTraitStore(this, serializer);

        Bukkit.getServicesManager().register(
                ItemTraitsAPI.class, this, this, ServicePriority.Normal);

        var inspectGui = new TraitInspectGui(this, registry);
        getServer().getPluginManager().registerEvents(inspectGui, this);

        var command = getCommand("traits");
        var handler = new TraitsCommand(this, registry, inspectGui);
        command.setExecutor(handler);
        command.setTabCompleter(handler);

        getLogger().info("Loaded " + registry.all().size() + " trait definition(s).");
    }

    private void loadDefinitions() {
        registry.clear();
        ConfigurationSection section = getConfig().getConfigurationSection("traits");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            var sub = section.getConfigurationSection(key);
            registry.register(new TraitDefinition(
                    key,
                    sub.getString("display-name", key),
                    sub.getDouble("min", 0),
                    sub.getDouble("max", 10),
                    sub.getInt("precision", 0),
                    sub.getString("format", "{value}")
            ));
        }
    }

    @Override
    public Optional<TraitHolder> getTraits(ItemStack item) {
        return store.read(item);
    }

    @Override
    public ItemStack applyTraits(ItemStack item, TraitHolder traits) {
        store.write(item, traits);
        updateLore(item, traits);
        return item;
    }

    @Override
    public ItemStack generateTraits(ItemStack item) {
        TraitHolder rolled = roller.roll(registry.all());
        var event = new TraitsGenerateEvent(item, rolled);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return item;
        store.write(item, event.getTraits());
        updateLore(item, event.getTraits());
        return item;
    }

    @Override
    public ItemStack transformTraits(ItemStack source, ItemStack target) {
        var existing = store.read(source);
        if (existing.isEmpty()) return target;
        var event = new TraitsTransformEvent(source, target, existing.get());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return target;
        store.write(target, event.getTraits());
        updateLore(target, event.getTraits());
        return target;
    }

    @Override
    public Collection<TraitDefinition> getDefinitions() {
        return registry.all();
    }

    private void updateLore(ItemStack item, TraitHolder holder) {
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = meta.lore();
        lore = lore == null ? new ArrayList<>() : new ArrayList<>(lore);

        // Remove old trait lines tracked by count
        int oldCount = meta.getPersistentDataContainer()
                .getOrDefault(loreCountKey, PersistentDataType.INTEGER, 0);
        for (int i = 0; i < oldCount && !lore.isEmpty(); i++) {
            lore.removeLast();
        }

        // Build new trait lines
        List<Component> traitLines = new ArrayList<>();
        traitLines.add(Component.empty());
        for (var entry : holder.values().entrySet()) {
            var def = registry.get(entry.getKey());
            String label = def.map(TraitDefinition::displayName).orElse(entry.getKey());
            String value = def.map(d -> d.formatValue(entry.getValue()))
                    .orElse(String.valueOf(entry.getValue()));
            traitLines.add(
                    Component.text(label + " ", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
                            .append(Component.text(value, NamedTextColor.WHITE))
            );
        }

        lore.addAll(traitLines);
        meta.lore(lore);
        meta.getPersistentDataContainer()
                .set(loreCountKey, PersistentDataType.INTEGER, traitLines.size());
        item.setItemMeta(meta);
    }
}
