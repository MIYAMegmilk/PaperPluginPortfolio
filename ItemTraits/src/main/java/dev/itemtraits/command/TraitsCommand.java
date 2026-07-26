package dev.itemtraits.command;

import dev.itemtraits.api.ItemTraitsAPI;
import dev.itemtraits.core.TraitRegistry;
import dev.itemtraits.gui.TraitInspectGui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class TraitsCommand implements CommandExecutor, TabCompleter {

    private final ItemTraitsAPI api;
    private final TraitRegistry registry;
    private final TraitInspectGui inspectGui;

    public TraitsCommand(ItemTraitsAPI api, TraitRegistry registry, TraitInspectGui inspectGui) {
        this.api = api;
        this.registry = registry;
        this.inspectGui = inspectGui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /traits <inspect|generate>", NamedTextColor.YELLOW));
            return true;
        }
        return switch (args[0].toLowerCase()) {
            case "inspect" -> inspect(sender);
            case "gui" -> openGui(sender);
            case "generate" -> generate(sender);
            default -> {
                sender.sendMessage(Component.text("Unknown subcommand.", NamedTextColor.RED));
                yield true;
            }
        };
    }

    private boolean inspect(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return true;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.isEmpty()) {
            player.sendMessage(Component.text("Hold an item first.", NamedTextColor.RED));
            return true;
        }
        var traits = api.getTraits(hand);
        if (traits.isEmpty()) {
            player.sendMessage(Component.text("No traits on this item.", NamedTextColor.GRAY));
            return true;
        }
        player.sendMessage(Component.text("--- Traits ---", NamedTextColor.GOLD));
        for (var entry : traits.get().values().entrySet()) {
            var def = registry.get(entry.getKey());
            String line = def
                    .map(d -> d.displayName() + ": " + d.formatValue(entry.getValue()))
                    .orElse(entry.getKey() + ": " + entry.getValue());
            player.sendMessage(Component.text("  " + line, NamedTextColor.WHITE));
        }
        return true;
    }

    private boolean openGui(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return true;
        }
        inspectGui.open(player);
        return true;
    }

    private boolean generate(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return true;
        }
        if (!player.hasPermission("itemtraits.generate")) {
            player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.isEmpty()) {
            player.sendMessage(Component.text("Hold an item first.", NamedTextColor.RED));
            return true;
        }
        api.generateTraits(hand);
        player.sendMessage(Component.text("Traits generated.", NamedTextColor.GREEN));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            return List.of("inspect", "gui", "generate").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return List.of();
    }
}
