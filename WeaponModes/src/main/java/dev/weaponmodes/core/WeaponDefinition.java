package dev.weaponmodes.core;

import org.bukkit.Material;

import java.util.List;

/** A weapon type with an ordered list of switchable modes. */
public record WeaponDefinition(
        String key,
        String displayName,
        Material baseMaterial,
        List<ModeDefinition> modes
) {

    /** Get the next mode after the given index, wrapping around. */
    public int nextModeIndex(int current) {
        return (current + 1) % modes.size();
    }

    public ModeDefinition getMode(int index) {
        return modes.get(index % modes.size());
    }
}
