package dev.weaponmodes.command;

import dev.weaponmodes.core.WeaponDefinition;
import dev.weaponmodes.listener.ModeSwitchListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public final class WeaponModesCommand implements CommandExecutor, TabCompleter {

    private final Map<String, WeaponDefinition> weapons;
    private final ModeSwitchListener switchListener;

    public WeaponModesCommand(Map<String, WeaponDefinition> weapons,
                               ModeSwitchListener switchListener) {
        this.weapons = weapons;
        this.switchListener = switchListener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /wm <give <weapon>|list>", NamedTextColor.YELLOW));
            return true;
        }
        return switch (args[0].toLowerCase()) {
            case "give" -> give(sender, args);
            case "list" -> list(sender);
            default -> {
                sender.sendMessage(Component.text("Unknown subcommand.", NamedTextColor.RED));
                yield true;
            }
        };
    }

    private boolean give(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /wm give <weapon>", NamedTextColor.YELLOW));
            return true;
        }
        WeaponDefinition weapon = weapons.get(args[1].toLowerCase());
        if (weapon == null) {
            sender.sendMessage(Component.text("Unknown weapon: " + args[1], NamedTextColor.RED));
            return true;
        }

        ItemStack item = new ItemStack(weapon.baseMaterial());
        switchListener.applyMode(item, weapon, 0);
        player.getInventory().addItem(item);
        player.sendMessage(Component.text("Gave " + weapon.displayName() + ".", NamedTextColor.GREEN));
        return true;
    }

    private boolean list(CommandSender sender) {
        sender.sendMessage(Component.text("--- Weapons ---", NamedTextColor.GOLD));
        for (var weapon : weapons.values()) {
            String modes = weapon.modes().stream()
                    .map(m -> m.key())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("none");
            sender.sendMessage(Component.text("  " + weapon.key() + " (" + modes + ")", NamedTextColor.WHITE));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            return List.of("give", "list").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return weapons.keySet().stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase())).toList();
        }
        return List.of();
    }
}
