package dev.weaponmodes;

import dev.weaponmodes.command.WeaponModesCommand;
import dev.weaponmodes.core.ModeDefinition;
import dev.weaponmodes.core.ModeEffect;
import dev.weaponmodes.core.WeaponDefinition;
import dev.weaponmodes.listener.CombatListener;
import dev.weaponmodes.listener.HeldEffectListener;
import dev.weaponmodes.listener.ModeSwitchListener;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WeaponModesPlugin extends JavaPlugin {

    private final Map<String, WeaponDefinition> weapons = new LinkedHashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadWeapons();

        var switchListener = new ModeSwitchListener(this, weapons);
        var combatListener = new CombatListener(weapons,
                switchListener.getWeaponKey(), switchListener.getModeKey());
        var heldListener = new HeldEffectListener(this, weapons,
                switchListener.getWeaponKey(), switchListener.getModeKey());

        getServer().getPluginManager().registerEvents(switchListener, this);
        getServer().getPluginManager().registerEvents(combatListener, this);
        getServer().getPluginManager().registerEvents(heldListener, this);

        var cmd = getCommand("weaponmodes");
        var handler = new WeaponModesCommand(weapons, switchListener);
        cmd.setExecutor(handler);
        cmd.setTabCompleter(handler);

        getLogger().info("Loaded " + weapons.size() + " weapon(s).");
    }

    private void loadWeapons() {
        weapons.clear();
        ConfigurationSection section = getConfig().getConfigurationSection("weapons");
        if (section == null) return;

        for (String weaponKey : section.getKeys(false)) {
            var wSec = section.getConfigurationSection(weaponKey);
            String displayName = wSec.getString("display-name", weaponKey);
            Material material = Material.matchMaterial(wSec.getString("base-material", "IRON_SWORD"));
            if (material == null) {
                getLogger().warning("Unknown material for weapon '" + weaponKey + "', skipping.");
                continue;
            }

            var modesSec = wSec.getConfigurationSection("modes");
            if (modesSec == null) continue;

            List<ModeDefinition> modes = new ArrayList<>();
            for (String modeKey : modesSec.getKeys(false)) {
                var mSec = modesSec.getConfigurationSection(modeKey);
                modes.add(parseMode(weaponKey, modeKey, mSec));
            }

            if (!modes.isEmpty()) {
                weapons.put(weaponKey, new WeaponDefinition(weaponKey, displayName, material, modes));
            }
        }
    }

    private ModeDefinition parseMode(String weaponKey, String modeKey, ConfigurationSection sec) {
        String displayName = sec.getString("display-name", modeKey);
        List<String> lore = sec.getStringList("lore");

        // Attributes
        Map<Attribute, AttributeModifier> attributes = new HashMap<>();
        var attrSec = sec.getConfigurationSection("attributes");
        if (attrSec != null) {
            for (String attrKey : attrSec.getKeys(false)) {
                var aSec = attrSec.getConfigurationSection(attrKey);
                try {
                    Attribute attr = Attribute.valueOf(aSec.getString("attribute",
                            attrKey.toUpperCase()));
                    var op = AttributeModifier.Operation.valueOf(
                            aSec.getString("operation", "ADD_NUMBER"));
                    var slot = ModeDefinition.parseSlotGroup(
                            aSec.getString("slot", "ANY"));
                    double value = aSec.getDouble("value", 0);
                    var key = new NamespacedKey(this, weaponKey + "_" + attrKey);
                    attributes.put(attr, new AttributeModifier(key, value, op, slot));
                } catch (IllegalArgumentException e) {
                    getLogger().warning("Invalid attribute '" + attrKey
                            + "' in " + weaponKey + "." + modeKey + ": " + e.getMessage());
                }
            }
        }

        // On-hit
        double lifesteal = 0;
        List<ModeEffect> onHitEffects = new ArrayList<>();
        var onHitSec = sec.getConfigurationSection("on-hit");
        if (onHitSec != null) {
            lifesteal = onHitSec.getDouble("lifesteal", 0);
            for (String spec : onHitSec.getStringList("effects")) {
                try { onHitEffects.add(ModeEffect.parse(spec)); }
                catch (Exception e) { getLogger().warning("Bad on-hit effect: " + spec); }
            }
        }

        // While-held
        List<ModeEffect> whileHeldEffects = new ArrayList<>();
        var heldSec = sec.getConfigurationSection("while-held");
        if (heldSec != null) {
            for (String spec : heldSec.getStringList("effects")) {
                try { whileHeldEffects.add(ModeEffect.parse(spec)); }
                catch (Exception e) { getLogger().warning("Bad held effect: " + spec); }
            }
        }

        return new ModeDefinition(modeKey, displayName, lore,
                attributes, lifesteal, onHitEffects, whileHeldEffects);
    }
}
