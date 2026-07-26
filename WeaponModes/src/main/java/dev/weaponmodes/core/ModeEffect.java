package dev.weaponmodes.core;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** Parsed potion effect from config (e.g. "SLOWNESS:1:60"). */
public record ModeEffect(PotionEffectType type, int amplifier, int durationTicks) {

    public PotionEffect toBukkit() {
        return new PotionEffect(type, durationTicks, amplifier - 1, true, false);
    }

    public static ModeEffect parse(String spec) {
        String[] parts = spec.split(":");
        var type = PotionEffectType.getByName(parts[0]);
        if (type == null) throw new IllegalArgumentException("Unknown effect: " + parts[0]);
        int amp = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
        int dur = parts.length > 2 ? Integer.parseInt(parts[2]) : 100;
        return new ModeEffect(type, amp, dur);
    }
}
