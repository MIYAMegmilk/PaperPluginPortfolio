package dev.weaponmodes.api.event;

import dev.weaponmodes.core.ModeDefinition;
import dev.weaponmodes.core.WeaponDefinition;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

/** Fired when a player right-clicks to switch weapon mode. Cancellable. */
public class WeaponModeSwitchEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final ItemStack item;
    private final WeaponDefinition weapon;
    private final ModeDefinition fromMode;
    private final ModeDefinition toMode;
    private boolean cancelled;

    public WeaponModeSwitchEvent(Player player, ItemStack item,
                                 WeaponDefinition weapon,
                                 ModeDefinition fromMode, ModeDefinition toMode) {
        this.player = player;
        this.item = item;
        this.weapon = weapon;
        this.fromMode = fromMode;
        this.toMode = toMode;
    }

    public Player getPlayer() { return player; }
    public ItemStack getItem() { return item; }
    public WeaponDefinition getWeapon() { return weapon; }
    public ModeDefinition getFromMode() { return fromMode; }
    public ModeDefinition getToMode() { return toMode; }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
