package dev.weaponmodes.listener;

import dev.weaponmodes.core.ModeDefinition;
import dev.weaponmodes.core.WeaponDefinition;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;

/**
 * Applies continuous potion effects while a mode weapon is held.
 * Uses PlayerItemHeldEvent to start/stop a per-player repeating task
 * instead of polling all players every N ticks.
 */
public final class HeldEffectListener implements Listener {

    private final Plugin plugin;
    private final Map<String, WeaponDefinition> weapons;
    private final NamespacedKey weaponKey;
    private final NamespacedKey modeKey;
    private final Map<java.util.UUID, Integer> activeTasks = new java.util.HashMap<>();

    public HeldEffectListener(Plugin plugin, Map<String, WeaponDefinition> weapons,
                               NamespacedKey weaponKey, NamespacedKey modeKey) {
        this.plugin = plugin;
        this.weapons = weapons;
        this.weaponKey = weaponKey;
        this.modeKey = modeKey;
    }

    @EventHandler
    public void onSlotChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();

        // Cancel existing task
        Integer taskId = activeTasks.remove(player.getUniqueId());
        if (taskId != null) {
            plugin.getServer().getScheduler().cancelTask(taskId);
        }

        // Check new slot (run next tick so the slot switch has completed)
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            ItemStack item = player.getInventory().getItemInMainHand();
            ModeDefinition mode = resolveMode(item);
            if (mode == null || mode.whileHeldEffects().isEmpty()) return;

            int id = new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) { cancel(); activeTasks.remove(player.getUniqueId()); return; }
                    ItemStack held = player.getInventory().getItemInMainHand();
                    ModeDefinition current = resolveMode(held);
                    if (current == null || current.whileHeldEffects().isEmpty()) {
                        cancel();
                        activeTasks.remove(player.getUniqueId());
                        return;
                    }
                    for (var effect : current.whileHeldEffects()) {
                        player.addPotionEffect(effect.type().createEffect(80, effect.amplifier() - 1));
                    }
                }
            }.runTaskTimer(plugin, 0L, 40L).getTaskId();

            activeTasks.put(player.getUniqueId(), id);
        });
    }

    private ModeDefinition resolveMode(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        var pdc = item.getItemMeta().getPersistentDataContainer();
        String weaponId = pdc.get(weaponKey, PersistentDataType.STRING);
        if (weaponId == null) return null;
        WeaponDefinition weapon = weapons.get(weaponId);
        if (weapon == null) return null;
        int modeIndex = pdc.getOrDefault(modeKey, PersistentDataType.INTEGER, 0);
        return weapon.getMode(modeIndex);
    }
}
