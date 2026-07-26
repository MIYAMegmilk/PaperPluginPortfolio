package dev.itemtraits.persistence;

import dev.itemtraits.api.TraitHolder;
import dev.itemtraits.core.TraitSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Optional;

/** Reads and writes {@link TraitHolder} to an item's PersistentDataContainer. */
public final class PdcTraitStore {

    private final NamespacedKey key;
    private final TraitSerializer serializer;

    public PdcTraitStore(Plugin plugin, TraitSerializer serializer) {
        this.key = new NamespacedKey(plugin, "data");
        this.serializer = serializer;
    }

    public Optional<TraitHolder> read(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return Optional.empty();
        String json = item.getItemMeta()
                .getPersistentDataContainer()
                .get(key, PersistentDataType.STRING);
        if (json == null) return Optional.empty();
        return serializer.deserialize(json);
    }

    public void write(ItemStack item, TraitHolder holder) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer()
                .set(key, PersistentDataType.STRING, serializer.serialize(holder));
        item.setItemMeta(meta);
    }
}
