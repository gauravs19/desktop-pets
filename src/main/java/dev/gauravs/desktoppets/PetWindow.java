package dev.gauravs.desktoppets;

import dev.gauravs.desktoppets.render.PetRenderer;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * One borderless, transparent, always-on-top window carrying one pet.
 *
 * <p>A window per pet (rather than one full-screen overlay) is what keeps the app well-behaved: the
 * window is only ~170 px square, so it intercepts mouse clicks over a tiny patch of desktop instead
 * of swallowing every click on the screen. It also means each pet can independently sit above the
 * taskbar and roam across monitors without any manual clipping.
 */
public final class PetWindow {

    /** Canvas/window size. Generous enough for a large pet plus the particles above its head. */
    private static final double SIZE = 170;
    /** Pixels of drag before a press is treated as a drag rather than a pet. */
    private static final double DRAG_THRESHOLD = 4;

    private final Stage stage;
    private final Canvas canvas;
    private final GraphicsContext gc;
    /** Swapped live when the species is changed from the context menu. */
    private PetRenderer renderer;
    private final Particles fx = new Particles();

    private final Pet pet;
    private final Behavior brain;
    private final App app;

    // Drag bookkeeping
    private boolean dragging;
    private boolean pressed;
    private double pressScreenX, pressScreenY;
    private double grabDx, grabDy;
    private double lastDragX, lastDragY, lastDragT;
    private double throwVx, throwVy;

    private PetState stateBeforeDrag = PetState.IDLE;

    public PetWindow(App app, Pet pet, long seed) {
        this.app = app;
        this.pet = pet;
        this.brain = new Behavior(seed);
        this.renderer = pet.species.newRenderer();

        canvas = new Canvas(SIZE, SIZE);
        gc = canvas.getGraphicsContext2D();

        Pane root = new Pane(canvas);
        root.setStyle("-fx-background-color: transparent;");

        Scene scene = new Scene(root, SIZE, SIZE, Color.TRANSPARENT);

        stage = new Stage(StageStyle.TRANSPARENT);
        stage.setScene(scene);
        stage.setAlwaysOnTop(true);
        stage.setResizable(false);
        stage.setTitle("Desktop Pet — " + pet.species.label() + " (" + pet.palette.name() + ")");
        // Keep the pet out of the taskbar and Alt-Tab; it is an ornament, not an app window.
        stage.setOpacity(1.0);

        installMouseHandlers(scene);
        syncWindowToPet();
        stage.show();
    }

    // ------------------------------------------------------------------ frame update

    /** Advance the simulation and repaint. Called once per frame by {@link App}. */
    public void tick(double dt) {
        brain.update(pet, dt);

        double squashBefore = pet.squash;
        Physics.update(pet, dt);
        // A sudden squash means Physics just registered a landing; kick up dust to match.
        if (pet.squash < 0.9 && squashBefore >= 0.9) {
            fx.dust((1 - pet.squash) * 1200);
        }

        if (pet.state == PetState.SLEEP) {
            fx.sleepTick(dt, pet.height());
        }
        fx.update(dt);

        syncWindowToPet();
        render();
    }

    private void syncWindowToPet() {
        stage.setX(pet.x - SIZE / 2);
        stage.setY(pet.y - (SIZE - renderer.footMargin()));
    }

    private void render() {
        gc.clearRect(0, 0, SIZE, SIZE);
        renderer.draw(gc, pet, fx, SIZE, SIZE);
    }

    // ------------------------------------------------------------------ interaction

    private void installMouseHandlers(Scene scene) {
        ContextMenu menu = buildContextMenu();

        scene.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                menu.show(stage, e.getScreenX(), e.getScreenY());
                return;
            }
            if (e.getButton() != MouseButton.PRIMARY) return;
            menu.hide();
            pressed = true;
            dragging = false;
            pressScreenX = e.getScreenX();
            pressScreenY = e.getScreenY();
            grabDx = pet.x - e.getScreenX();
            grabDy = pet.y - e.getScreenY();
            lastDragX = e.getScreenX();
            lastDragY = e.getScreenY();
            lastDragT = nowSeconds();
        });

        scene.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            if (!pressed) return;

            if (!dragging) {
                double moved = Math.hypot(e.getScreenX() - pressScreenX, e.getScreenY() - pressScreenY);
                if (moved < DRAG_THRESHOLD) return;
                dragging = true;
                stateBeforeDrag = pet.state;
                pet.enter(PetState.DRAG, Double.MAX_VALUE);
                pet.vx = 0;
                pet.vy = 0;
            }

            pet.x = e.getScreenX() + grabDx;
            pet.y = e.getScreenY() + grabDy;

            // Estimate throw velocity from recent cursor motion, smoothed so a single jittery
            // sample cannot launch the pet across the desktop.
            double t = nowSeconds();
            double dt = t - lastDragT;
            if (dt > 0.008) {
                double vx = (e.getScreenX() - lastDragX) / dt;
                double vy = (e.getScreenY() - lastDragY) / dt;
                throwVx = throwVx * 0.55 + vx * 0.45;
                throwVy = throwVy * 0.55 + vy * 0.45;
                lastDragX = e.getScreenX();
                lastDragY = e.getScreenY();
                lastDragT = t;
            }

            // Face the direction of travel while being carried.
            if (Math.abs(throwVx) > 30) pet.facing = throwVx > 0 ? 1 : -1;
        });

        scene.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            if (!pressed || e.getButton() != MouseButton.PRIMARY) return;
            pressed = false;

            if (dragging) {
                dragging = false;
                double cap = 1400;
                brain.release(pet,
                    clamp(throwVx, -cap, cap),
                    clamp(throwVy, -cap, cap));
                throwVx = throwVy = 0;
            } else {
                giveAffection(); // a click that never moved is a pat, not a drag
            }
        });
    }

    /** Petting: hearts, a happy hop, and a wake-up if the pet was asleep. */
    private void giveAffection() {
        fx.hearts(pet.height());
        if (pet.state == PetState.SLEEP) {
            pet.enter(PetState.SIT, 2.0);
        } else {
            brain.jump(pet);
        }
    }

    private ContextMenu buildContextMenu() {
        ContextMenu menu = new ContextMenu();

        MenuItem add = new MenuItem("Add another pet");
        add.setOnAction(e -> app.addPet());

        MenuItem sleep = new MenuItem("Nap / wake up");
        sleep.setOnAction(e -> brain.toggleSleep(pet));

        MenuItem come = new MenuItem("Come here (centre screen)");
        come.setOnAction(e -> {
            javafx.geometry.Rectangle2D b = Screens.screenAt(pet.x, pet.y);
            pet.x = b.getMinX() + b.getWidth() / 2;
            pet.y = b.getMinY() + b.getHeight() / 3;
            brain.release(pet, 0, 0);
        });

        Menu species = new Menu("Species");
        for (Species sp : Species.values()) {
            MenuItem mi = new MenuItem(sp.label());
            mi.setOnAction(e -> {
                pet.species = sp;
                renderer = sp.newRenderer();
                stage.setTitle("Desktop Pet — " + sp.label() + " (" + pet.palette.name() + ")");
                app.saveConfig();
            });
            species.getItems().add(mi);
        }

        Menu size = new Menu("Size");
        for (double[] opt : new double[][]{{0.7, 0}, {1.0, 0}, {1.3, 0}}) {
            double v = opt[0];
            MenuItem mi = new MenuItem(v < 1 ? "Small" : v > 1 ? "Large" : "Normal");
            mi.setOnAction(e -> {
                pet.scale = v;
                app.saveConfig();
            });
            size.getItems().add(mi);
        }

        CheckMenuItem onTop = new CheckMenuItem("Always on top");
        onTop.setSelected(true);
        onTop.setOnAction(e -> stage.setAlwaysOnTop(onTop.isSelected()));

        MenuItem remove = new MenuItem("Send this pet home");
        remove.setOnAction(e -> app.removePet(this));

        MenuItem quit = new MenuItem("Quit Desktop Pets");
        quit.setOnAction(e -> app.quit());

        menu.getItems().addAll(add, sleep, come, species, size, onTop,
            new SeparatorMenuItem(), remove, quit);

        // The remove item is disabled for the last pet, since there would then be no window left
        // to right-click and the app would be unreachable.
        menu.setOnShowing(e -> remove.setDisable(app.petCount() <= 1));
        return menu;
    }

    // ------------------------------------------------------------------ misc

    public Pet pet() {
        return pet;
    }

    public void close() {
        stage.close();
    }

    private static double nowSeconds() {
        return System.nanoTime() / 1_000_000_000.0;
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : v > hi ? hi : v;
    }
}
