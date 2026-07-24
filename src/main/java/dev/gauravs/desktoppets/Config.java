package dev.gauravs.desktoppets;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Tiny properties-backed settings store at {@code ~/.desktop-pets/config.properties}.
 *
 * <p>Plain {@link Properties} rather than JSON keeps the app dependency-free (JavaFX is the only
 * third-party jar) and leaves the file hand-editable, which matters because the config is also how
 * you pre-seed a particular set of pets before launch.
 */
public final class Config {

    private static final Path DIR = Path.of(System.getProperty("user.home"), ".desktop-pets");
    private static final Path FILE = DIR.resolve("config.properties");

    /** How many pets to spawn on launch. */
    public int petCount = 1;
    /** Size multiplier applied to every pet. */
    public double scale = 1.0;
    /** Palette name per pet, index-aligned with the spawn order; may be shorter than petCount. */
    public final List<String> palettes = new ArrayList<>();
    /** Species per pet, index-aligned with the spawn order; missing entries default to PUPPY. */
    public final List<String> species = new ArrayList<>();

    public static Config load() {
        Config c = new Config();
        if (!Files.isReadable(FILE)) return c;

        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(FILE)) {
            p.load(in);
        } catch (IOException e) {
            System.err.println("desktop-pets: could not read config, using defaults: " + e.getMessage());
            return c;
        }

        c.petCount = clampInt(p.getProperty("pets"), 1, 1, 12);
        c.scale = clampDouble(p.getProperty("scale"), 1.0, 0.5, 2.0);
        readList(p.getProperty("palettes", ""), c.palettes);
        readList(p.getProperty("species", ""), c.species);
        return c;
    }

    private static void readList(String raw, List<String> into) {
        for (String s : raw.split(",")) {
            String t = s.trim();
            if (!t.isEmpty()) into.add(t);
        }
    }

    public void save() {
        Properties p = new Properties();
        p.setProperty("pets", Integer.toString(petCount));
        p.setProperty("scale", Double.toString(scale));
        p.setProperty("palettes", String.join(",", palettes));
        p.setProperty("species", String.join(",", species));
        try {
            Files.createDirectories(DIR);
            try (OutputStream out = Files.newOutputStream(FILE)) {
                p.store(out, "Desktop Pets settings"
                    + System.lineSeparator() + "# palettes: " + String.join(", ", paletteNames())
                    + System.lineSeparator() + "# species: PUPPY, CAT");
            }
        } catch (IOException e) {
            System.err.println("desktop-pets: could not save config: " + e.getMessage());
        }
    }

    private static List<String> paletteNames() {
        List<String> names = new ArrayList<>();
        for (Palette pal : Palette.ALL) names.add(pal.name());
        return names;
    }

    private static int clampInt(String raw, int def, int lo, int hi) {
        try {
            return Math.max(lo, Math.min(hi, Integer.parseInt(raw.trim())));
        } catch (Exception e) {
            return def;
        }
    }

    private static double clampDouble(String raw, double def, double lo, double hi) {
        try {
            return Math.max(lo, Math.min(hi, Double.parseDouble(raw.trim())));
        } catch (Exception e) {
            return def;
        }
    }
}
