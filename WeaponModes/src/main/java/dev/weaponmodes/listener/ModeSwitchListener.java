package dev.weaponmodes.listener;

import dev.weaponmodes.api.event.WeaponModeSwitchEvent;
import dev.weaponmodes.core.ModeDefinition;
import dev.weaponmodes.core.WeaponDefinition;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Map;

public final class ModeSwitchListener implements Listener {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final Map<String, WeaponDefinition> weapons;
    private final NamespacedKey weaponKey;
    private final NamespacedKey modeKey;

    public ModeSwitchListener(Plugin plugin, Map<String, WeaponDefinition> weapons) {
        this.weapons = weapons;
        this.weaponKey = new NamespacedKey(plugin, "weapon");
        this.modeKey = new NamespacedKey(plugin, "mode");
    }

    public NamespacedKey getWeaponKey() { return weaponKey; }
    public NamespacedKey getModeKey() { return modeKey; }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        String weaponId = meta.getPersistentDataContainer().get(weaponKey, PersistentDataType.STRING);
        if (weaponId == null) return;

        WeaponDefinition weapon = weapons.get(weaponId);
        if (weapon == null) return;

        event.setCancelled(true);

        int currentIndex = meta.getPersistentDataContainer()
                .getOrDefault(modeKey, PersistentDataType.INTEGER, 0);
        int nextIndex = weapon.nextModeIndex(currentIndex);

        ModeDefinition fromMode = weapon.getMode(currentIndex);
        ModeDefinition toMode = weapon.getMode(nextIndex);

        var switchEvent = new WeaponModeSwitchEvent(
                event.getPlayer(), item, weapon, fromMode, toMode);
        Bukkit.getPluginManager().callEvent(switchEvent);
        if (switchEvent.isCancelled()) return;

        applyMode(item, weapon, nextIndex);
    }

    /** Applies a mode to an item: updates PDC, display name, lore, and attributes. */
    public void applyMode(ItemStack item, WeaponDefinition weapon, int modeIndex) {
        ModeDefinition mode = weapon.getMode(modeIndex);
        ItemMeta meta = item.getItemMeta();

        meta.getPersistentDataContainer().set(weaponKey, PersistentDataType.STRING, weapon.key());
        meta.getPersistentDataContainer().set(modeKey, PersistentDataType.INTEGER, modeIndex);

        meta.displayName(LEGACY.deserialize(mode.displayName())
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));

        meta.lore(mode.lore().stream()
                .map(line -> (Component) LEGACY.deserialize(line)
                        .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false))
                .toList());

        // Clear old and apply new attribute modifiers
        var existing = meta.getAttributeModifiers();
        if (existing != null) {
            existing.forEach((attr, mod) -> {
                if (mod.getKey().getNamespace().equals(weaponKey.getNamespace())) {
                    meta.removeAttributeModifier(attr, mod);
                }
            });
        }
        mode.attributes().forEach(meta::addAttributeModifier);

        item.setItemMeta(meta);
    }
}
