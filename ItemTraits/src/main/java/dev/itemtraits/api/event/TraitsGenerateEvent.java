package dev.itemtraits.api.event;

import dev.itemtraits.api.TraitHolder;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

/**
 * Fired after trait values are rolled but before they are written to PDC.
 * Cancelling prevents any traits from being applied.
 * Listeners may replace the holder to adjust rolled values.
 */
public class TraitsGenerateEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final ItemStack item;
    private TraitHolder traits;
    private boolean cancelled;

    public TraitsGenerateEvent(ItemStack item, TraitHolder traits) {
        this.item = item;
        this.traits = traits;
    }

    public ItemStack getItem() { return item; }

    public TraitHolder getTraits() { return traits; }

    public void setTraits(TraitHolder traits) { this.traits = traits; }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
