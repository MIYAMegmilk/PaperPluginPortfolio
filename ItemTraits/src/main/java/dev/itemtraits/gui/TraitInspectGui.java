package dev.itemtraits.gui;

import dev.itemtraits.api.ItemTraitsAPI;
import dev.itemtraits.api.TraitHolder;
import dev.itemtraits.core.TraitDefinition;
import dev.itemtraits.core.TraitRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Chest GUI that displays trait values of items in the player's inventory.
 * Layout (3 rows):
 *   Row 0: decorative glass panes
 *   Row 1: up to 7 item slots (slots 10-16) showing items with traits
 *   Row 2: decorative glass panes
 *
 * Clicking an item shows a detail view with bar-style stat visualization.
 */
public final class TraitInspectGui implements Listener {

    private static final Component TITLE = Component.text("Trait Inspector", NamedTextColor.DARK_PURPLE);
    private static final Component DETAIL_TITLE = Component.text("Trait Detail", NamedTextColor.GOLD);
    private static final int GUI_SIZE = 27;
    private static final int DETAIL_SIZE = 27;

    private final ItemTraitsAPI api;
    private final TraitRegistry registry;

    public TraitInspectGui(ItemTraitsAPI api, TraitRegistry registry) {
        this.api = api;
        this.registry = registry;
    }

    /** Open the inspector GUI for a player, scanning their inventory for trait items. */
    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, GUI_SIZE, TITLE);

        // Fill border with glass
        ItemStack border = createBorder();
        for (int i = 0; i < GUI_SIZE; i++) {
            gui.setItem(i, border);
        }

        // Scan player inventory for items with traits (up to 7)
        int slot = 10;
        for (ItemStack item : player.getInventory().getContents()) {
            if (slot > 16) break;
            if (item == null || item.isEmpty()) continue;
            Optional<TraitHolder> traits = api.getTraits(item);
            if (traits.isEmpty()) continue;
            gui.setItem(slot, item.clone());
            slot++;
        }

        // Fill empty item slots with info
        for (int i = slot; i <= 16; i++) {
            gui.setItem(i, createPlaceholder());
        }

        player.openInventory(gui);
    }

    /** Open a detail view for a specific item's traits. */
    private void openDetail(Player player, ItemStack item, TraitHolder traits) {
        Inventory gui = Bukkit.createInventory(null, DETAIL_SIZE, DETAIL_TITLE);

        ItemStack border = createBorder();
        for (int i = 0; i < DETAIL_SIZE; i++) {
            gui.setItem(i, border);
        }

        // Slot 4: the item itself
        gui.setItem(4, item.clone());

        // Slots 10-16: one stat indicator per trait
        int slot = 10;
        for (var entry : traits.values().entrySet()) {
            if (slot > 16) break;
            gui.setItem(slot, createStatIndicator(entry.getKey(), entry.getValue()));
            slot++;
        }

        // Slot 22: back button
        gui.setItem(22, createBackButton());

        player.openInventory(gui);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Component title = event.getView().title();

        if (title.equals(TITLE)) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.isEmpty()) return;
            if (clicked.getType() == Material.GRAY_STAINED_GLASS_PANE
                    || clicked.getType() == Material.LIGHT_GRAY_STAINED_GLASS_PANE) return;

            api.getTraits(clicked).ifPresent(traits ->
                    openDetail(player, clicked, traits));

        } else if (title.equals(DETAIL_TITLE)) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked != null && clicked.getType() == Material.ARROW) {
                open(player);
            }
        }
    }

    private ItemStack createStatIndicator(String traitKey, double value) {
        Optional<TraitDefinition> defOpt = registry.get(traitKey);
        String label = defOpt.map(TraitDefinition::displayName).orElse(traitKey);
        String formatted = defOpt.map(d -> d.formatValue(value)).orElse(String.valueOf(value));

        // Bar visualization: fill level based on position within [min, max]
        double min = defOpt.map(TraitDefinition::min).orElse(0.0);
        double max = defOpt.map(TraitDefinition::max).orElse(100.0);
        double ratio = max > min ? (value - min) / (max - min) : 0;
        int bars = (int) Math.round(ratio * 10);
        String bar = "§a" + "|".repeat(bars) + "§7" + "|".repeat(10 - bars);

        Material icon = selectIcon(ratio);
        ItemStack indicator = new ItemStack(icon);
        ItemMeta meta = indicator.getItemMeta();
        meta.displayName(Component.text(label, NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Value: " + formatted, NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(bar, NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));
        if (defOpt.isPresent()) {
            var def = defOpt.get();
            lore.add(Component.text("Range: " + def.formatValue(def.min())
                    + " ~ " + def.formatValue(def.max()), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        indicator.setItemMeta(meta);
        return indicator;
    }

    private static Material selectIcon(double ratio) {
        if (ratio >= 0.8) return Material.EMERALD;
        if (ratio >= 0.5) return Material.DIAMOND;
        if (ratio >= 0.3) return Material.GOLD_INGOT;
        return Material.IRON_INGOT;
    }

    private static ItemStack createBorder() {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.displayName(Component.empty());
        pane.setItemMeta(meta);
        return pane;
    }

    private static ItemStack createPlaceholder() {
        ItemStack pane = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.displayName(Component.text("No item", NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        pane.setItemMeta(meta);
        return pane;
    }

    private static ItemStack createBackButton() {
        ItemStack arrow = new ItemStack(Material.ARROW);
        ItemMeta meta = arrow.getItemMeta();
        meta.displayName(Component.text("Back", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        arrow.setItemMeta(meta);
        return arrow;
    }
}
