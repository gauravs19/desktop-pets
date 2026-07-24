package dev.gauravs.desktoppets;

/**
 * Mutable state for one pet.
 *
 * <p>Position is anchored at the pet's <em>feet</em>: {@code x} is the horizontal centre and
 * {@code y} is the ground contact point, both in screen (virtual desktop) pixels. Anchoring at the
 * feet rather than the top-left corner keeps the physics readable — landing is simply
 * {@code y == groundY}, and squash-and-stretch scales around the anchor without the pet appearing
 * to sink into the floor.
 */
public final class Pet {

    /** Nominal pet height in pixels at scale 1.0; the body is drawn to fit this. */
    public static final double BASE_HEIGHT = 62;

    public final String id;
    public final Palette palette;

    /** Which animal this pet is drawn as. Purely cosmetic; physics and behaviour are shared. */
    public Species species = Species.PUPPY;

    /** How big this pet is relative to {@link #BASE_HEIGHT}. */
    public double scale = 1.0;

    public double x;
    public double y;
    public double vx;
    public double vy;

    /** 1 = facing right, -1 = facing left. */
    public int facing = 1;

    public PetState state = PetState.IDLE;
    /** Seconds spent in the current state. */
    public double stateTime;
    /** Seconds until the behaviour engine is allowed to pick a new state. */
    public double stateTimeout = 2.0;
    /** Monotonic seconds since birth; drives all the cosmetic wobbles. */
    public double age;

    /** Vertical squash factor — 1.0 is neutral, &lt;1 squashed, &gt;1 stretched. */
    public double squash = 1.0;
    /** Seconds until the next blink, and how much of the current blink is left. */
    public double blinkIn = 3.0;
    public double blinking;

    /** Set while the pet is airborne so the landing impact can be scaled by fall speed. */
    public double lastAirSpeed;

    public Pet(String id, Palette palette, double x, double y) {
        this.id = id;
        this.palette = palette;
        this.x = x;
        this.y = y;
    }

    public double height() {
        return BASE_HEIGHT * scale;
    }

    /** Switch state, resetting the per-state clock and drawing a fresh duration for it. */
    public void enter(PetState next, double durationSeconds) {
        this.state = next;
        this.stateTime = 0;
        this.stateTimeout = durationSeconds;
    }

    public boolean airborne() {
        return state == PetState.FALL || state == PetState.JUMP;
    }
}
