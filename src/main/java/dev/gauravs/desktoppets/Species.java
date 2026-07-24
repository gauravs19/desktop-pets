package dev.gauravs.desktoppets;

import dev.gauravs.desktoppets.render.BlobCatRenderer;
import dev.gauravs.desktoppets.render.ChubbyPuppyRenderer;
import dev.gauravs.desktoppets.render.PetRenderer;

/**
 * What kind of animal a pet is. Species affects appearance only — physics and behaviour are shared,
 * which is why a new species is one renderer class and one enum constant rather than a new pet type.
 */
public enum Species {

    PUPPY("Puppy") {
        @Override
        public PetRenderer newRenderer() {
            return new ChubbyPuppyRenderer();
        }
    },
    CAT("Cat") {
        @Override
        public PetRenderer newRenderer() {
            return new BlobCatRenderer();
        }
    };

    private final String label;

    Species(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public abstract PetRenderer newRenderer();

    /** Parse a config value, defaulting to {@link #PUPPY} for anything unrecognised. */
    public static Species parse(String raw) {
        if (raw != null) {
            for (Species s : values()) {
                if (s.name().equalsIgnoreCase(raw.trim())) return s;
            }
        }
        return PUPPY;
    }
}
