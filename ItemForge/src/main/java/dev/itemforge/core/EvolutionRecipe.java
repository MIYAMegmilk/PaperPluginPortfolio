package dev.itemforge.core;

import org.bukkit.Material;

import java.util.List;

/** An evolution recipe: input material + ingredients → output item. */
public record EvolutionRecipe(
        String key,
        Material inputMaterial,
        Material outputMaterial,
        String outputName,
        List<String> outputLore,
        List<Ingredient> ingredients
) {}
