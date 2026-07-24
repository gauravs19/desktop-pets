package dev.gauravs.desktoppets;

import javafx.scene.paint.Color;

/**
 * A pet's colour scheme. Each pet gets one at birth so that a family of pets on the same
 * desktop stays visually distinguishable, and so the renderer never has to hard-code a colour.
 */
public record Palette(String name, Color fur, Color furDark, Color belly, Color ear, Color eye, Color accent) {

    /** Shipped colour schemes, cycled through as pets are added. */
    public static final Palette[] ALL = {
        new Palette("Marmalade",
            Color.web("#f5a25d"), Color.web("#d97e33"), Color.web("#fde3c4"),
            Color.web("#f2809c"), Color.web("#2b2118"), Color.web("#7ec8e3")),
        new Palette("Slate",
            Color.web("#8f9bb3"), Color.web("#6a7590"), Color.web("#e3e8f2"),
            Color.web("#e79bb0"), Color.web("#1d2330"), Color.web("#ffd166")),
        new Palette("Matcha",
            Color.web("#a8c686"), Color.web("#7fa25e"), Color.web("#f0f5e1"),
            Color.web("#eda7b4"), Color.web("#26301c"), Color.web("#ff8fab")),
        new Palette("Cocoa",
            Color.web("#8d6248"), Color.web("#6b482f"), Color.web("#e8d3c0"),
            Color.web("#d98da3"), Color.web("#22150e"), Color.web("#9ad7c2")),
        new Palette("Blossom",
            Color.web("#f3c0d0"), Color.web("#dd97ad"), Color.web("#fff1f5"),
            Color.web("#e08ba3"), Color.web("#40222c"), Color.web("#a5d8ff")),
        new Palette("Midnight",
            Color.web("#4a4a63"), Color.web("#333349"), Color.web("#cfcfe0"),
            Color.web("#c07b93"), Color.web("#101018"), Color.web("#ffe066"))
    };

    public static Palette byIndex(int i) {
        return ALL[Math.floorMod(i, ALL.length)];
    }

    /** Look up by {@link #name()}, falling back to the first palette when the name is unknown. */
    public static Palette byName(String wanted) {
        if (wanted != null) {
            for (Palette p : ALL) {
                if (p.name().equalsIgnoreCase(wanted)) return p;
            }
        }
        return ALL[0];
    }
}
