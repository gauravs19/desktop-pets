package dev.gauravs.desktoppets;

/** The mutually exclusive things a pet can be doing at any instant. */
public enum PetState {
    /** Standing still, breathing, occasionally blinking. */
    IDLE,
    /** Ambling along the ground at a leisurely pace. */
    WALK,
    /** Sprinting — same gait as WALK but faster and leaning forward. */
    RUN,
    /** Parked on its haunches. */
    SIT,
    /** Licking a paw; a short, fussy little loop. */
    GROOM,
    /** Curled up with Zzz particles drifting off. */
    SLEEP,
    /** Airborne and subject to gravity: thrown, dropped, or walked off a ledge. */
    FALL,
    /** Held by the mouse cursor; physics is suspended while dragging. */
    DRAG,
    /** A deliberate upward hop, usually a reaction to being petted. */
    JUMP
}
