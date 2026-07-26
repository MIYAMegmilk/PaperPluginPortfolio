package dev.itemtraits.core;

/**
 * Immutable definition of a single trait type, loaded from config.yml.
 *
 * @param key         unique identifier (e.g. "attack")
 * @param displayName human-readable name shown in lore and commands
 * @param min         minimum rollable value (inclusive)
 * @param max         maximum rollable value (inclusive)
 * @param precision   decimal places to keep (0 = integer)
 * @param format      display template — {@code {value}} is replaced with the formatted number
 */
public record TraitDefinition(
        String key,
        String displayName,
        double min,
        double max,
        int precision,
        String format
) {
    public TraitDefinition {
        if (min > max) throw new IllegalArgumentException("min > max for trait: " + key);
        if (precision < 0) throw new IllegalArgumentException("negative precision for trait: " + key);
    }

    /** Format a numeric value using this definition's display template. */
    public String formatValue(double value) {
        String number = precision == 0
                ? String.valueOf((int) value)
                : String.format("%." + precision + "f", value);
        return format.replace("{value}", number);
    }
}
