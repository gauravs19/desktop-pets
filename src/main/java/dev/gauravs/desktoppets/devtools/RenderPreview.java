package dev.gauravs.desktoppets.devtools;

import dev.gauravs.desktoppets.Palette;
import dev.gauravs.desktoppets.Particles;
import dev.gauravs.desktoppets.Pet;
import dev.gauravs.desktoppets.PetState;
import dev.gauravs.desktoppets.Species;
import dev.gauravs.desktoppets.render.PetRenderer;
import java.awt.image.BufferedImage;
import java.io.File;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javax.imageio.ImageIO;

/**
 * Development-only harness: renders every pet species in every {@link PetState} into a single
 * contact-sheet PNG and exits.
 *
 * <p>This exists because the pets are otherwise only visible as transparent always-on-top windows,
 * which are awkward to inspect and impossible to diff. Rendering the poses offscreen makes the art
 * reviewable the same way any other output is — look at the sheet, adjust a proportion, regenerate.
 *
 * <pre>
 * java -cp target\desktop-pets.jar dev.gauravs.desktoppets.devtools.RenderPreview preview.png
 * </pre>
 */
public final class RenderPreview extends Application {

    private static final double CELL = 170;
    private static final double LABEL_H = 20;
    private static final PetState[] POSES = {
        PetState.IDLE, PetState.WALK, PetState.RUN, PetState.SIT, PetState.GROOM,
        PetState.SLEEP, PetState.FALL, PetState.JUMP, PetState.DRAG
    };

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        String out = getParameters().getRaw().isEmpty()
            ? "preview.png"
            : getParameters().getRaw().get(0);

        Species[] kinds = Species.values();
        double w = CELL * POSES.length;
        double h = (CELL + LABEL_H) * kinds.length;

        Canvas canvas = new Canvas(w, h);
        GraphicsContext g = canvas.getGraphicsContext2D();

        // Light backdrop with a per-cell tint, so silhouettes and cell edges are both obvious.
        g.setFill(Color.web("#eceff4"));
        g.fillRect(0, 0, w, h);

        for (int row = 0; row < kinds.length; row++) {
            Species kind = kinds[row];
            PetRenderer renderer = kind.newRenderer();
            double rowY = row * (CELL + LABEL_H);

            g.setFill(Color.web("#3a4152"));
            g.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
            g.fillText(kind.label(), 8, rowY + 14);

            for (int col = 0; col < POSES.length; col++) {
                PetState pose = POSES[col];
                double cellX = col * CELL;
                double cellY = rowY + LABEL_H;

                if ((row + col) % 2 == 0) {
                    g.setFill(Color.web("#e2e6ee"));
                    g.fillRect(cellX, cellY, CELL, CELL);
                }
                g.setFill(Color.web("#7b8494"));
                g.setFont(Font.font("Segoe UI", 11));
                g.fillText(pose.name(), cellX + 6, cellY + 14);

                Pet pet = poseFor(kind, pose, col);
                Particles fx = particlesFor(pose, pet);

                g.save();
                g.translate(cellX, cellY);
                renderer.draw(g, pet, fx, CELL, CELL);
                g.restore();
            }
        }

        // A Scene is required for snapshot() even though nothing is ever shown.
        new Scene(new Group(canvas));
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        WritableImage img = canvas.snapshot(params, null);

        writePng(img, new File(out));
        System.out.println("wrote " + new File(out).getAbsolutePath()
            + " (" + (int) img.getWidth() + "x" + (int) img.getHeight() + ")");
        Platform.exit();
    }

    /** Build a pet frozen at a representative moment of the given pose. */
    private static Pet poseFor(Species kind, PetState pose, int col) {
        Pet pet = new Pet("preview", Palette.byIndex(col), CELL / 2, 0);
        pet.species = kind;
        pet.facing = 1;
        pet.state = pose;
        // Mid-stride for gaited poses; a slow drift elsewhere so idle bobs are visible.
        pet.age = switch (pose) {
            case WALK -> Math.PI / 2 / 8.0;
            case RUN -> Math.PI / 2 / 15.0;
            default -> 0.4;
        };
        pet.squash = pose == PetState.JUMP ? 1.12 : 1.0;
        // y == ground so the contact shadow renders at full strength.
        pet.y = dev.gauravs.desktoppets.Screens.groundAt(pet.x, 0);
        return pet;
    }

    /** Pre-warm a particle system so sleeping pets show Zzz's and landings show dust. */
    private static Particles particlesFor(PetState pose, Pet pet) {
        Particles fx = new Particles();
        if (pose == PetState.SLEEP) {
            for (int i = 0; i < 3; i++) {
                fx.sleepTick(99, pet.height());     // large dt forces an immediate emit
                fx.update(0.55);
            }
        } else if (pose == PetState.JUMP) {
            fx.hearts(pet.height());
            fx.update(0.35);
        }
        return fx;
    }

    private static void writePng(WritableImage img, File file) throws Exception {
        int w = (int) img.getWidth();
        int h = (int) img.getHeight();
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        PixelReader pr = img.getPixelReader();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                bi.setRGB(x, y, pr.getArgb(x, y));
            }
        }
        ImageIO.write(bi, "png", file);
    }
}
