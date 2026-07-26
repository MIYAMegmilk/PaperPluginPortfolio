package dev.weaponmodes.listener;

import dev.weaponmodes.core.ModeDefinition;
import dev.weaponmodes.core.WeaponDefinition;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;

/** Handles on-hit effects (lifesteal, debuffs) for mode weapons. */
public final class CombatListener implements Listener {

    private final Map<String, WeaponDefinition> weapons;
    private final NamespacedKey weaponKey;
    private final NamespacedKey modeKey;

    public CombatListener(Map<String, WeaponDefinition> weapons,
                           NamespacedKey weaponKey, NamespacedKey modeKey) {
        this.weapons = weapons;
        this.weaponKey = weaponKey;
        this.modeKey = modeKey;
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity victim)) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!item.hasItemMeta()) return;

        var pdc = item.getItemMeta().getPersistentDataContainer();
        String weaponId = pdc.get(weaponKey, PersistentDataType.STRING);
        if (weaponId == null) return;

        WeaponDefinition weapon = weapons.get(weaponId);
        if (weapon == null) return;

        int modeIndex = pdc.getOrDefault(modeKey, PersistentDataType.INTEGER, 0);
        ModeDefinition mode = weapon.getMode(modeIndex);

        // Lifesteal
        if (mode.lifesteal() > 0) {
            double heal = event.getFinalDamage() * mode.lifesteal();
            player.setHealth(Math.min(player.getHealth() + heal,
                    player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue()));
        }

        // On-hit debuffs
        for (var effect : mode.onHitEffects()) {
            victim.addPotionEffect(effect.toBukkit());
        }
    }
}
