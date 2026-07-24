package dev.gauravs.desktoppets;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.stage.Stage;

/**
 * Application entry point and owner of the single simulation loop.
 *
 * <p>One {@link AnimationTimer} drives every pet, rather than one timer per pet. That keeps the
 * frame delta consistent across pets — so two pets thrown identically behave identically — and means
 * adding a tenth pet costs a little drawing rather than a whole extra scheduler.
 */
public final class App extends Application {

    /** Delta cap in seconds. Without it, a laptop resume would teleport pets through the floor. */
    private static final double MAX_DT = 1.0 / 20;

    private final List<PetWindow> pets = new ArrayList<>();
    private final Random rng = new Random();
    private Config config;
    private long lastNanos;
    private int spawnCounter;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage unusedPrimaryStage) {
        // Every pet gets its own Stage; the primary stage handed to us is never shown, and without
        // this the app would exit the moment a pet is sent home.
        Platform.setImplicitExit(false);

        config = Config.load();
        for (int i = 0; i < config.petCount; i++) {
            spawn(i < config.palettes.size() ? Palette.byName(config.palettes.get(i)) : null,
                  i < config.species.size() ? Species.parse(config.species.get(i)) : Species.PUPPY);
        }

        AnimationTimer loop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastNanos == 0) {
                    lastNanos = now;
                    return;
                }
                double dt = Math.min(MAX_DT, (now - lastNanos) / 1_000_000_000.0);
                lastNanos = now;
                // Copy guarded against a context-menu action removing a pet mid-frame.
                for (PetWindow w : List.copyOf(pets)) {
                    w.tick(dt);
                }
            }
        };
        loop.start();
    }

    // ------------------------------------------------------------------ pet lifecycle

    public void addPet() {
        if (pets.size() >= 12) return;          // past a dozen it stops being charming
        // A new pet matches whatever the existing litter is, so adding a friend for a puppy
        // does not produce a surprise cat.
        Species kind = pets.isEmpty() ? Species.PUPPY : pets.get(pets.size() - 1).pet().species;
        spawn(null, kind);
        saveConfig();
    }

    private void spawn(Palette forced, Species kind) {
        Rectangle2D screen = Screens.desktopBounds();
        Palette palette = forced != null ? forced : Palette.byIndex(spawnCounter);
        double x = screen.getMinX() + 80 + rng.nextDouble() * Math.max(1, screen.getWidth() - 160);
        double y = Screens.groundAt(x, screen.getMaxY() - 1);

        Pet pet = new Pet("pet-" + (++spawnCounter), palette, x, y);
        pet.species = kind;
        pet.scale = config.scale;
        pet.facing = rng.nextBoolean() ? 1 : -1;
        // Stagger the brains so a freshly-spawned litter does not move in lockstep.
        pet.age = rng.nextDouble() * 5;
        pet.stateTimeout = 0.5 + rng.nextDouble() * 2;

        pets.add(new PetWindow(this, pet, rng.nextLong()));
    }

    public void removePet(PetWindow window) {
        if (pets.size() <= 1) return;
        pets.remove(window);
        window.close();
        saveConfig();
    }

    public int petCount() {
        return pets.size();
    }

    /** Persist the current litter so the same pets come back next launch. */
    public void saveConfig() {
        config.petCount = pets.size();
        config.palettes.clear();
        config.species.clear();
        double scale = config.scale;
        for (PetWindow w : pets) {
            config.palettes.add(w.pet().palette.name());
            config.species.add(w.pet().species.name());
            scale = w.pet().scale;
        }
        config.scale = scale;
        config.save();
    }

    public void quit() {
        saveConfig();
        for (PetWindow w : List.copyOf(pets)) {
            w.close();
        }
        pets.clear();
        Platform.exit();
    }
}
