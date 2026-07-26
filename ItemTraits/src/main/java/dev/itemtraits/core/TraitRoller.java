package dev.itemtraits.core;

import dev.itemtraits.api.TraitHolder;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Random;

/** Generates random trait values according to definitions. */
public final class TraitRoller {

    private final Random random;

    public TraitRoller(Random random) {
        this.random = random;
    }

    /** Roll values for all given definitions and return them as a holder. */
    public TraitHolder roll(Collection<TraitDefinition> definitions) {
        var values = new LinkedHashMap<String, Double>();
        for (var def : definitions) {
            values.put(def.key(), rollSingle(def));
        }
        return new TraitHolder(values);
    }

    /** Roll a single value within [min, max], respecting precision. */
    public double rollSingle(TraitDefinition def) {
        double raw = def.min() + random.nextDouble() * (def.max() - def.min());
        return round(raw, def.precision());
    }

    static double round(double value, int precision) {
        double factor = Math.pow(10, precision);
        return Math.round(value * factor) / factor;
    }
}
