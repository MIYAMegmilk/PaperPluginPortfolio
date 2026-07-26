package dev.itemtraits.api;

import dev.itemtraits.core.TraitDefinition;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Optional;

/**
 * Public API for reading, writing, and transforming per-item traits.
 *
 * <p>Obtain via {@code Bukkit.getServicesManager().getRegistration(ItemTraitsAPI.class)}.
 */
public interface ItemTraitsAPI {

    /** Read traits from an item. Returns empty if the item has no traits. */
    Optional<TraitHolder> getTraits(ItemStack item);

    /** Write the given traits to an item's PDC. */
    ItemStack applyTraits(ItemStack item, TraitHolder traits);

    /** Roll random traits according to config and apply them. Fires {@code TraitsGenerateEvent}. */
    ItemStack generateTraits(ItemStack item);

    /** Copy traits from source to target. Fires {@code TraitsTransformEvent}. */
    ItemStack transformTraits(ItemStack source, ItemStack target);

    /** All registered trait definitions from config.yml. */
    Collection<TraitDefinition> getDefinitions();
}
