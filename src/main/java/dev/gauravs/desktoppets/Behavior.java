package dev.gauravs.desktoppets;

import java.util.Random;

/**
 * The pet's little brain: a weighted random state machine.
 *
 * <p>Each state carries its own duration, and when that runs out the engine rolls for the next one
 * using weights that depend on the current state. The weighting is what stops the pet from looking
 * like a random-number generator — a sleeping pet is far more likely to sit up than to immediately
 * sprint, and a walking pet mostly keeps walking. Physics states (FALL, DRAG) are never chosen here;
 * they are imposed from outside and the brain simply waits them out.
 */
public final class Behavior {

    private static final double WALK_SPEED = 42;
    private static final double RUN_SPEED = 132;

    private final Random rng;

    public Behavior(long seed) {
        this.rng = new Random(seed);
    }

    public void update(Pet pet, double dt) {
        updateBlink(pet, dt);

        // Airborne or held: no decisions to make, gravity and the mouse are in charge.
        if (pet.state == PetState.DRAG || pet.state == PetState.FALL) return;

        if (pet.state == PetState.JUMP) {
            // JUMP hands off to FALL as soon as the pet starts descending.
            if (pet.vy >= 0) pet.enter(PetState.FALL, 6);
            return;
        }

        if (pet.stateTime < pet.stateTimeout) {
            steerWhileMoving(pet);
            return;
        }

        choose(pet);
    }

    /** Keep walking pets pointed sensibly and nudge their speed to match their state. */
    private void steerWhileMoving(Pet pet) {
        if (pet.state == PetState.WALK || pet.state == PetState.RUN) {
            double target = (pet.state == PetState.RUN ? RUN_SPEED : WALK_SPEED) * pet.facing;
            pet.vx += (target - pet.vx) * 0.08;
            // Occasionally change its mind mid-stroll.
            if (rng.nextDouble() < 0.004) {
                pet.facing = -pet.facing;
            }
        }
    }

    private void choose(Pet pet) {
        PetState previous = pet.state;
        double r = rng.nextDouble();

        // Waking up is a two-step affair: sleep -> sit -> whatever next.
        if (previous == PetState.SLEEP) {
            pet.enter(PetState.SIT, 1.5 + rng.nextDouble() * 2);
            return;
        }

        if (previous == PetState.WALK || previous == PetState.RUN) {
            if (r < 0.45) {
                pet.facing = rng.nextBoolean() ? 1 : -1;
                pet.enter(PetState.WALK, 2 + rng.nextDouble() * 4);
            } else if (r < 0.55) {
                pet.enter(PetState.RUN, 0.8 + rng.nextDouble() * 1.4);
            } else if (r < 0.85) {
                pet.vx = 0;
                pet.enter(PetState.IDLE, 1.5 + rng.nextDouble() * 3);
            } else {
                pet.vx = 0;
                pet.enter(PetState.SIT, 2 + rng.nextDouble() * 4);
            }
            return;
        }

        // From a resting state: mostly get up and move, sometimes settle in deeper.
        if (r < 0.42) {
            pet.facing = rng.nextBoolean() ? 1 : -1;
            pet.enter(PetState.WALK, 2 + rng.nextDouble() * 4);
        } else if (r < 0.50) {
            pet.facing = rng.nextBoolean() ? 1 : -1;
            pet.enter(PetState.RUN, 0.8 + rng.nextDouble() * 1.2);
        } else if (r < 0.66) {
            pet.vx = 0;
            pet.enter(PetState.IDLE, 1.5 + rng.nextDouble() * 3);
        } else if (r < 0.80) {
            pet.vx = 0;
            pet.enter(PetState.GROOM, 2 + rng.nextDouble() * 2);
        } else if (r < 0.92) {
            pet.vx = 0;
            pet.enter(PetState.SIT, 2 + rng.nextDouble() * 5);
        } else {
            pet.vx = 0;
            pet.enter(PetState.SLEEP, 10 + rng.nextDouble() * 20);
        }
    }

    private void updateBlink(Pet pet, double dt) {
        if (pet.state == PetState.SLEEP) {
            pet.blinking = 1;
            return;
        }
        if (pet.blinking > 0) {
            pet.blinking -= dt / 0.11;              // ~110 ms per blink
            if (pet.blinking < 0) pet.blinking = 0;
            return;
        }
        pet.blinkIn -= dt;
        if (pet.blinkIn <= 0) {
            pet.blinking = 1;
            pet.blinkIn = 2.0 + rng.nextDouble() * 4.5;
        }
    }

    /** Reaction to being petted: a hop plus a burst of enthusiasm. */
    public void jump(Pet pet) {
        pet.vy = -640;
        pet.squash = 1.22;
        pet.enter(PetState.JUMP, 3);
    }

    /** Called when the user drops the pet, converting drag motion into a throw. */
    public void release(Pet pet, double throwVx, double throwVy) {
        pet.vx = throwVx;
        pet.vy = throwVy;
        pet.lastAirSpeed = Math.abs(throwVy);
        pet.enter(PetState.FALL, 6);
    }

    public void toggleSleep(Pet pet) {
        if (pet.state == PetState.SLEEP) {
            pet.enter(PetState.SIT, 1.5);
        } else {
            pet.vx = 0;
            pet.enter(PetState.SLEEP, 20 + rng.nextDouble() * 30);
        }
    }
}
