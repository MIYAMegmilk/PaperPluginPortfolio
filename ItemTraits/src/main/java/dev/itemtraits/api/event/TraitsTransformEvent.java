package dev.itemtraits.api.event;

import dev.itemtraits.api.TraitHolder;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

/**
 * Fired when traits are about to be copied from a source item to a target item.
 * Cancelling prevents the transfer. Listeners may replace the holder to modify
 * which values are carried over.
 */
public class TraitsTransformEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final ItemStack source;
    private final ItemStack target;
    private TraitHolder traits;
    private boolean cancelled;

    public TraitsTransformEvent(ItemStack source, ItemStack target, TraitHolder traits) {
        this.source = source;
        this.target = target;
        this.traits = traits;
    }

    public ItemStack getSource() { return source; }

    public ItemStack getTarget() { return target; }

    public TraitHolder getTraits() { return traits; }

    public void setTraits(TraitHolder traits) { this.traits = traits; }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
