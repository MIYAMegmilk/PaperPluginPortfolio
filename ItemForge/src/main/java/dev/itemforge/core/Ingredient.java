package dev.itemforge.core;

import org.bukkit.Material;

/** A required material and its count, parsed from "MATERIAL:amount". */
public record Ingredient(Material material, int amount) {

    public static Ingredient parse(String spec) {
        String[] parts = spec.split(":");
        Material mat = Material.matchMaterial(parts[0]);
        if (mat == null) throw new IllegalArgumentException("Unknown material: " + parts[0]);
        int amt = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
        return new Ingredient(mat, amt);
    }
}
