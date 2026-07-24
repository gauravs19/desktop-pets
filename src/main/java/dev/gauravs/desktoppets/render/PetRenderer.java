package dev.gauravs.desktoppets.render;

import dev.gauravs.desktoppets.Particles;
import dev.gauravs.desktoppets.Pet;
import javafx.scene.canvas.GraphicsContext;

/**
 * Draws a pet into a canvas.
 *
 * <p>The renderer is the only part of the app that knows what a pet <em>looks like</em>; the model,
 * physics, and behaviour engine deal purely in position and state. Keeping that seam means a
 * sprite-sheet-backed implementation can be dropped in later without touching the simulation.
 */
public interface PetRenderer {

    /**
     * @param g        canvas context, already cleared
     * @param pet      the pet to draw
     * @param fx       particle system belonging to this pet
     * @param canvasW  canvas width in px
     * @param canvasH  canvas height in px
     */
    void draw(GraphicsContext g, Pet pet, Particles fx, double canvasW, double canvasH);

    /** Distance from the bottom of the canvas to the pet's foot anchor, in px. */
    double footMargin();
}
