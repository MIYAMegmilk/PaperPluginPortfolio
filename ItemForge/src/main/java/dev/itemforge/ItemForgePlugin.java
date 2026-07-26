package dev.itemforge;

import dev.itemforge.core.EvolutionRecipe;
import dev.itemforge.core.Ingredient;
import dev.itemforge.gui.ForgeGui;
import dev.itemtraits.api.ItemTraitsAPI;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ItemForgePlugin extends JavaPlugin {

    private final Map<String, EvolutionRecipe> recipes = new LinkedHashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        RegisteredServiceProvider<ItemTraitsAPI> provider =
                getServer().getServicesManager().getRegistration(ItemTraitsAPI.class);
        if (provider == null) {
            getLogger().severe("ItemTraits API not found — disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        ItemTraitsAPI traitsApi = provider.getProvider();

        loadRecipes();

        ForgeGui gui = new ForgeGui(this, recipes, traitsApi);
        getServer().getPluginManager().registerEvents(gui, this);
        getCommand("forge").setExecutor(new ForgeCommand(gui));

        getLogger().info("ItemForge loaded with " + recipes.size() + " recipes.");
    }

    private void loadRecipes() {
        ConfigurationSection section = getConfig().getConfigurationSection("recipes");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection r = section.getConfigurationSection(key);
            Material input = Material.matchMaterial(r.getString("input", ""));
            ConfigurationSection out = r.getConfigurationSection("output");
            Material outputMat = Material.matchMaterial(out.getString("material", ""));
            String outputName = out.getString("name", "&f" + key);
            List<String> outputLore = out.getStringList("lore");
            List<Ingredient> ingredients = r.getStringList("materials").stream()
                    .map(Ingredient::parse)
                    .toList();

            if (input == null || outputMat == null) {
                getLogger().warning("Skipping invalid recipe: " + key);
                continue;
            }
            recipes.put(key, new EvolutionRecipe(key, input, outputMat, outputName, outputLore, ingredients));
        }
    }
}
