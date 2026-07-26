package dev.itemforge.gui;

import dev.itemforge.core.EvolutionRecipe;
import dev.itemforge.core.Ingredient;
import dev.itemtraits.api.ItemTraitsAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Anvil-style evolution GUI.
 * <pre>
 * Row 0:  border
 * Row 1:  [border] [INPUT slot 10] [border] [arrow 12] [border] [OUTPUT preview 14] [border] [border] [border]
 * Row 2:  border
 * Row 3:  [border] [mat0 slot 28] [mat1 slot 29] [mat2 slot 30] [border] [CONFIRM 32] [border] [border] [border]
 * Row 4:  border
 * </pre>
 */
public final class ForgeGui implements Listener {

    private static final Component TITLE = Component.text("Evolution Forge", NamedTextColor.DARK_GREEN);
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final int SIZE = 45;

    private static final int SLOT_INPUT = 10;
    private static final int SLOT_ARROW = 12;
    private static final int SLOT_OUTPUT = 14;
    private static final int SLOT_CONFIRM = 32;
    private static final int[] SLOT_MATERIALS = {28, 29, 30};

    private final Plugin plugin;
    private final Map<String, EvolutionRecipe> recipes;
    private final ItemTraitsAPI traitsApi;

    public ForgeGui(Plugin plugin, Map<String, EvolutionRecipe> recipes, ItemTraitsAPI traitsApi) {
        this.plugin = plugin;
        this.recipes = recipes;
        this.traitsApi = traitsApi;
    }

    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, SIZE, TITLE);
        fillBorder(gui);
        gui.setItem(SLOT_ARROW, createDecor(Material.ARROW, "&7Place input + materials"));
        gui.setItem(SLOT_OUTPUT, createDecor(Material.BARRIER, "&cNo recipe matched"));
        gui.setItem(SLOT_CONFIRM, createDecor(Material.GRAY_DYE, "&7Waiting for recipe..."));
        player.openInventory(gui);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().title().equals(TITLE)) return;

        int slot = event.getRawSlot();

        // Allow interaction with input and material slots
        if (slot == SLOT_INPUT || isMatSlot(slot)) {
            // Let the click through, then update preview next tick
            Bukkit.getScheduler().runTask(plugin, () -> updatePreview(player));
            return;
        }

        // Allow shift-click from player inventory into forge
        if (slot >= SIZE) {
            Bukkit.getScheduler().runTask(plugin, () -> updatePreview(player));
            return;
        }

        event.setCancelled(true);

        // Confirm button
        if (slot == SLOT_CONFIRM) {
            tryEvolve(player);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!event.getView().title().equals(TITLE)) return;

        Inventory gui = event.getInventory();
        // Return items in input and material slots
        returnItem(player, gui, SLOT_INPUT);
        for (int s : SLOT_MATERIALS) {
            returnItem(player, gui, s);
        }
    }

    private void updatePreview(Player player) {
        Inventory gui = player.getOpenInventory().getTopInventory();
        if (gui.getSize() != SIZE) return;

        ItemStack input = gui.getItem(SLOT_INPUT);
        EvolutionRecipe match = findRecipe(input, gui);

        if (match != null) {
            gui.setItem(SLOT_OUTPUT, createOutputPreview(match));
            gui.setItem(SLOT_ARROW, createDecor(Material.LIME_DYE, "&aRecipe matched!"));
            gui.setItem(SLOT_CONFIRM, createConfirmButton());
        } else {
            gui.setItem(SLOT_OUTPUT, createDecor(Material.BARRIER, "&cNo recipe matched"));
            gui.setItem(SLOT_ARROW, createDecor(Material.ARROW, "&7Place input + materials"));
            gui.setItem(SLOT_CONFIRM, createDecor(Material.GRAY_DYE, "&7Waiting for recipe..."));
        }
    }

    private EvolutionRecipe findRecipe(ItemStack input, Inventory gui) {
        if (input == null || input.isEmpty()) return null;

        for (var recipe : recipes.values()) {
            if (input.getType() != recipe.inputMaterial()) continue;
            if (hasIngredients(gui, recipe.ingredients())) return recipe;
        }
        return null;
    }

    private boolean hasIngredients(Inventory gui, List<Ingredient> required) {
        for (int i = 0; i < required.size(); i++) {
            if (i >= SLOT_MATERIALS.length) return false;
            ItemStack slot = gui.getItem(SLOT_MATERIALS[i]);
            Ingredient ing = required.get(i);
            if (slot == null || slot.getType() != ing.material() || slot.getAmount() < ing.amount()) {
                return false;
            }
        }
        return true;
    }

    private void tryEvolve(Player player) {
        Inventory gui = player.getOpenInventory().getTopInventory();
        ItemStack input = gui.getItem(SLOT_INPUT);
        EvolutionRecipe recipe = findRecipe(input, gui);
        if (recipe == null) return;

        // Consume materials
        for (int i = 0; i < recipe.ingredients().size(); i++) {
            ItemStack matSlot = gui.getItem(SLOT_MATERIALS[i]);
            Ingredient ing = recipe.ingredients().get(i);
            matSlot.setAmount(matSlot.getAmount() - ing.amount());
        }

        // Build output
        ItemStack output = new ItemStack(recipe.outputMaterial());
        ItemMeta meta = output.getItemMeta();
        meta.displayName(LEGACY.deserialize(recipe.outputName())
                .decoration(TextDecoration.ITALIC, false));
        if (!recipe.outputLore().isEmpty()) {
            meta.lore(recipe.outputLore().stream()
                    .map(l -> (Component) LEGACY.deserialize(l)
                            .decoration(TextDecoration.ITALIC, false))
                    .toList());
        }
        output.setItemMeta(meta);

        // Transfer traits via ItemTraits API
        traitsApi.transformTraits(input, output);

        // Replace input with output
        gui.setItem(SLOT_INPUT, output);

        // Effects
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.2f);
        player.sendMessage(Component.text("Evolution complete!", NamedTextColor.GREEN));

        updatePreview(player);
    }

    private void returnItem(Player player, Inventory gui, int slot) {
        ItemStack item = gui.getItem(slot);
        if (item != null && !item.isEmpty()) {
            var leftover = player.getInventory().addItem(item);
            leftover.values().forEach(i -> player.getWorld().dropItem(player.getLocation(), i));
            gui.setItem(slot, null);
        }
    }

    private boolean isMatSlot(int slot) {
        for (int s : SLOT_MATERIALS) if (s == slot) return true;
        return false;
    }

    private ItemStack createOutputPreview(EvolutionRecipe recipe) {
        ItemStack preview = new ItemStack(recipe.outputMaterial());
        ItemMeta meta = preview.getItemMeta();
        meta.displayName(LEGACY.deserialize(recipe.outputName())
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        for (String l : recipe.outputLore()) {
            lore.add(LEGACY.deserialize(l).decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.empty());
        lore.add(Component.text("(Preview — click Confirm to forge)", NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        preview.setItemMeta(meta);
        return preview;
    }

    private static ItemStack createConfirmButton() {
        ItemStack item = new ItemStack(Material.LIME_DYE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Confirm Evolution", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createDecor(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LEGACY.deserialize(name).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private static void fillBorder(Inventory gui) {
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.displayName(Component.empty());
        pane.setItemMeta(meta);
        for (int i = 0; i < gui.getSize(); i++) {
            gui.setItem(i, pane);
        }
        // Clear interactive slots
        gui.setItem(SLOT_INPUT, null);
        for (int s : SLOT_MATERIALS) gui.setItem(s, null);
    }
}
