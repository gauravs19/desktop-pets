package dev.gauravs.desktoppets;

import javafx.geometry.Rectangle2D;

/**
 * Integrates position, gravity, and ground contact for one pet.
 *
 * <p>Deliberately a fixed-behaviour, stateless helper: every frame it is handed the pet and a delta
 * time and it mutates the pet in place. Keeping it free of its own state means the simulation can be
 * paused, or a pet can be teleported by the drag handler, without anything getting out of sync.
 */
public final class Physics {

    /** Downward acceleration in px/s². Tuned to feel snappy rather than realistic. */
    private static final double GRAVITY = 1900;
    /** Fraction of vertical speed retained on a bounce. */
    private static final double BOUNCE = 0.32;
    /** Ground friction applied to horizontal speed after landing, per second. */
    private static final double FRICTION = 6.0;
    /** Below this vertical speed a bounce is not worth doing; the pet just settles. */
    private static final double SETTLE_SPEED = 90;

    private Physics() {
    }

    public static void update(Pet pet, double dt) {
        pet.age += dt;
        pet.stateTime += dt;

        // Squash-and-stretch always relaxes back toward neutral, whatever caused it.
        pet.squash += (1.0 - pet.squash) * Math.min(1, dt * 9);

        if (pet.state == PetState.DRAG) {
            // While held, the cursor is the authority on position — see PetWindow.
            return;
        }

        double ground = Screens.groundAt(pet.x, pet.y);

        if (pet.airborne()) {
            pet.vy += GRAVITY * dt;
            pet.lastAirSpeed = Math.max(pet.lastAirSpeed, Math.abs(pet.vy));
        }

        pet.x += pet.vx * dt;
        pet.y += pet.vy * dt;

        clampHorizontally(pet);

        // Ground level can change mid-step when crossing onto a monitor with a different height.
        ground = Screens.groundAt(pet.x, pet.y);

        if (pet.airborne()) {
            if (pet.y >= ground) {
                land(pet, ground);
            }
        } else {
            // Walked onto a taller monitor's floor? Step down rather than hovering.
            if (pet.y < ground - 1) {
                pet.vy = 0;
                pet.enter(PetState.FALL, 6);
            } else {
                pet.y = ground;
                if (pet.state != PetState.WALK && pet.state != PetState.RUN) {
                    pet.vx -= pet.vx * Math.min(1, dt * FRICTION);
                    if (Math.abs(pet.vx) < 3) pet.vx = 0;
                }
            }
        }
    }

    /** Turn the pet around when it reaches the outer edge of the whole virtual desktop. */
    private static void clampHorizontally(Pet pet) {
        Rectangle2D desktop = Screens.desktopBounds();
        double margin = 8;
        if (pet.x < desktop.getMinX() + margin) {
            pet.x = desktop.getMinX() + margin;
            if (pet.vx < 0) pet.vx = -pet.vx * (pet.airborne() ? 0.5 : 1);
            pet.facing = 1;
        } else if (pet.x > desktop.getMaxX() - margin) {
            pet.x = desktop.getMaxX() - margin;
            if (pet.vx > 0) pet.vx = -pet.vx * (pet.airborne() ? 0.5 : 1);
            pet.facing = -1;
        }
    }

    private static void land(Pet pet, double ground) {
        pet.y = ground;
        double impact = pet.lastAirSpeed;
        pet.lastAirSpeed = 0;

        if (Math.abs(pet.vy) > SETTLE_SPEED) {
            pet.vy = -Math.abs(pet.vy) * BOUNCE;
            pet.vx *= 0.7;
            pet.squash = 0.72;                       // splat on contact
            pet.enter(PetState.FALL, 6);
        } else {
            pet.vy = 0;
            pet.squash = Math.max(0.6, 1.0 - impact / 2600.0);
            // A hard landing leaves the pet sitting there dazed; a gentle one just resumes.
            pet.enter(impact > 700 ? PetState.SIT : PetState.IDLE, 1.2 + Math.random() * 1.5);
        }
    }
}
