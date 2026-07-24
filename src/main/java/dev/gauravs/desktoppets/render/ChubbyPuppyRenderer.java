package dev.gauravs.desktoppets.render;

import dev.gauravs.desktoppets.Palette;
import dev.gauravs.desktoppets.Particles;
import dev.gauravs.desktoppets.Pet;
import dev.gauravs.desktoppets.PetState;
import dev.gauravs.desktoppets.Screens;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;

/**
 * A round, chubby puppy — the default pet.
 *
 * <p>Everything is vector shapes drawn in pet-local coordinates: the origin sits between the feet,
 * x runs forward (the direction the pet faces), and <em>y runs negative upward</em>.
 *
 * <h2>Layout</h2>
 * The body is drawn in profile but the head is drawn <b>frontal three-quarter</b> — a deliberate
 * mixed perspective, and the single most important decision in this class. A profile head puts the
 * muzzle on the same side as one ear, so that ear has nowhere to hang except across the face. Facing
 * the head forward lets both ears hang symmetrically off the sides of the skull where they are fully
 * visible, which is what makes the silhouette read as a floppy-eared dog at 60 px tall.
 *
 * <pre>
 *   legs    y   0 .. -12     stubby and thick
 *   torso   y -10 .. -50     58 wide x 40 tall — wider than tall, which is the "chubby" part
 *   head    x -11 .. +27     r = 19, centred at (8, -52) — oversized, sat on the front of the chest
 *   ears    pinned at x = -8 and +24, tips out at x = -25 and +41, hanging to the chin line
 *   muzzle  centred low on the face, with the nose and mouth stacked below the eyes
 * </pre>
 *
 * <p>Two further rules, learned the hard way:
 * <ol>
 *   <li><b>The ears must break the head's outline.</b> An ear that fits inside the head circle reads
 *       as shading — or worse, as a dark cap. Both are pinned at the skull's left and right extremes
 *       and are long enough for the tips to hang outside the circle and down to the chin.</li>
 *   <li><b>Draw order is skull, then ears, then face.</b> The ear tops overlap the skull so they look
 *       attached, and painting the face last keeps the eyes and nose clear of them.</li>
 * </ol>
 *
 * <p>Poses come from continuous state rather than discrete frames: squash, walk phase, ear sway, and
 * tail wag are all real numbers, so a hard landing squashes the puppy more than a gentle one.
 */
public final class ChubbyPuppyRenderer implements PetRenderer {

    private static final double FOOT_MARGIN = 16;

    private static final double LEG_LEN = 12;
    private static final double LEG_W = 11;

    private static final double BODY_W = 58;
    private static final double BODY_H = 40;
    private static final double BODY_CY = -30;

    private static final double HEAD_R = 19;
    private static final double HEAD_CX = 8;
    private static final double HEAD_CY = -52;

    /** Horizontal offset of each ear pin from the head centre — at the skull's extremes. */
    private static final double EAR_PIN_X = 16;
    private static final double EAR_W = 14;
    private static final double EAR_H = 35;
    /** Resting ear angle from straight-down. Wide enough that the ears clear the head's outline. */
    private static final double EAR_ANGLE = 38;

    @Override
    public double footMargin() {
        return FOOT_MARGIN;
    }

    @Override
    public void draw(GraphicsContext g, Pet pet, Particles fx, double canvasW, double canvasH) {
        g.save();
        g.translate(canvasW / 2.0, canvasH - FOOT_MARGIN);

        drawShadow(g, pet);

        double sq = Math.max(0.4, pet.squash);
        g.save();
        // Mirror for facing, then squash vertically while widening horizontally, so the puppy reads
        // as squishy rather than merely smaller.
        g.scale(pet.facing * pet.scale / Math.sqrt(sq), pet.scale * sq);
        drawPuppy(g, pet);
        g.restore();

        // Outside the facing mirror, so Zzz glyphs are never drawn backwards.
        fx.draw(g, pet.palette);
        g.restore();
    }

    private void drawShadow(GraphicsContext g, Pet pet) {
        double air = Math.max(0, Screens.groundAt(pet.x, pet.y) - pet.y);
        double t = Math.min(1, air / 240.0);
        double k = 1 - 0.65 * t;
        double w = BODY_W * 1.02 * pet.scale * k;
        double h = 7 * pet.scale * k;
        // Flat, faint, and sat low: a taller shadow shows through the gap between the front legs and
        // reads as a grey slab rather than as contact shading.
        g.setFill(Color.color(0, 0, 0, 0.16 * (1 - 0.7 * t)));
        g.fillOval(-w / 2, -h / 2 + 3, w, h);
    }

    // ------------------------------------------------------------------ pose

    /** Per-state pose offsets. Mutable holder rather than a record; it is filled once per frame. */
    private static final class Pose {
        double swing;            // fore/aft leg travel, px
        double bodyDy;
        double bodyW = BODY_W;
        double bodyH = BODY_H;
        double headDx;
        double headDy;
        /** Extra downward offset for where the legs attach into the torso. */
        double legTopDy;
        boolean showLegs = true;
        boolean sitting;
        double earSway;          // degrees away from the ears' resting angle
        double tailSpeed = 9;    // dogs wag fast, faster when excited
        double tailAmp = 22;
        boolean tongueOut = true;
    }

    private Pose poseFor(Pet pet, double walkPhase) {
        Pose q = new Pose();
        switch (pet.state) {
            case WALK -> {
                q.swing = 4.0;
                q.bodyDy = -Math.abs(Math.sin(walkPhase)) * 1.8;
                q.earSway = Math.sin(walkPhase) * 10;
                q.tailSpeed = 13;
                q.tailAmp = 30;
            }
            case RUN -> {
                q.swing = 7.0;
                q.bodyDy = -Math.abs(Math.sin(walkPhase)) * 3.0;
                q.bodyW = BODY_W + 3;
                q.earSway = Math.sin(walkPhase) * 20 - 8;   // ears stream backwards
                q.tailSpeed = 18;
                q.tailAmp = 34;
            }
            case SIT -> {
                q.sitting = true;
                q.legTopDy = 4;
                q.bodyDy = 6;
                q.bodyH = BODY_H + 4;
                q.headDy = 2;
                q.earSway = Math.sin(pet.age * 1.2) * 3;
                q.tailSpeed = 10;
                q.tailAmp = 26;
            }
            case GROOM -> {
                q.sitting = true;
                q.legTopDy = 4;
                q.bodyDy = 6;
                q.bodyH = BODY_H + 4;
                q.headDy = 3;
                q.earSway = Math.sin(pet.age * 9) * 13;     // ear-scratching jiggle
                q.tailSpeed = 7;
                q.tailAmp = 16;
            }
            case SLEEP -> {
                q.showLegs = false;
                q.bodyDy = 12;
                q.bodyH = BODY_H - 12;
                q.bodyW = BODY_W + 6;
                q.headDx = 2;
                q.headDy = 10 + Math.sin(pet.age * 1.4) * 0.8;
                // Only a little splay: swung out much further the ears go horizontal and mask the face.
                q.earSway = 12;
                q.tailSpeed = 1.2;
                q.tailAmp = 5;
                q.tongueOut = false;
            }
            case DRAG -> {
                q.legTopDy = -5;                            // legs stretched out beneath
                q.swing = 2.4 * Math.sin(pet.age * 9);      // dangling paws
                q.bodyDy = -2;
                q.earSway = -8;                             // hanging straight down
                q.tailSpeed = 6;
                q.tailAmp = 14;
            }
            case FALL, JUMP -> {
                q.legTopDy = -3;
                q.swing = 6.0;
                q.headDy = -2;
                q.earSway = Math.sin(pet.age * 16) * 24;    // flapping
                q.tailSpeed = 14;
                q.tailAmp = 30;
            }
            default -> {
                // IDLE: a slow panting bob.
                q.bodyDy = Math.sin(pet.age * 2.2) * 1.0;
                q.bodyH = BODY_H + Math.sin(pet.age * 2.2) * 0.8;
                q.earSway = Math.sin(pet.age * 1.6) * 3;
            }
        }
        return q;
    }

    // ------------------------------------------------------------------ drawing

    private void drawPuppy(GraphicsContext g, Pet pet) {
        Palette p = pet.palette;
        PetState st = pet.state;
        double walkPhase = pet.age * (st == PetState.RUN ? 15 : 8);
        Pose q = poseFor(pet, walkPhase);

        double bodyCy = BODY_CY + q.bodyDy;
        double bodyBottom = bodyCy + q.bodyH / 2;
        double headCx = HEAD_CX + q.headDx;
        double headCy = HEAD_CY + q.bodyDy + q.headDy;
        double s = Math.sin(walkPhase) * q.swing;

        // Legs attach *inside* the torso and always run down to the foot anchor at y = 0. Starting
        // them at the torso's lowest point instead leaves them hanging in mid-air, because an ellipse
        // has no width there — which is what made the paws look detached.
        double legTop = bodyCy + q.bodyH * 0.28 + q.legTopDy;
        double legH = -legTop;

        drawTail(g, pet, q, bodyCy);

        // Far-side legs, overlapped by the torso.
        if (q.showLegs) {
            if (q.sitting) {
                drawHaunch(g, p.furDark(), -18, bodyBottom - 6, 1.0);
            } else {
                drawLeg(g, p.furDark(), -19, legTop, legH, +s, lift(walkPhase, q.swing));
                drawLeg(g, p.furDark(), 12, legTop, legH, -s, lift(walkPhase + Math.PI, q.swing));
            }
        }

        drawTorso(g, p, q, bodyCy);

        // Near-side legs, in front of the torso.
        if (q.showLegs) {
            if (q.sitting) {
                drawHaunch(g, p.fur(), -15, bodyBottom - 5, 1.18);
                drawLeg(g, p.fur(), 14, legTop, legH, 0, 0);
                drawLeg(g, p.fur(), 5, legTop, legH, 0, 0);
            } else {
                drawLeg(g, p.fur(), -12, legTop, legH, -s, lift(walkPhase + Math.PI, q.swing));
                drawLeg(g, p.fur(), 21, legTop, legH, +s, lift(walkPhase, q.swing));
            }
        }

        // Skull, then both ears hanging off its sides, then the face on top of all of it.
        drawSkull(g, p, headCx, headCy);
        drawEar(g, p, headCx - EAR_PIN_X, headCy - 9, -EAR_ANGLE - q.earSway);
        drawEar(g, p, headCx + EAR_PIN_X, headCy - 9, EAR_ANGLE + q.earSway);
        drawFace(g, pet, q, headCx, headCy);
        // A curled-up dog's collar is tucked under its chin and out of sight.
        if (st != PetState.SLEEP) {
            drawCollar(g, p, headCx, headCy, bodyCy, q.bodyH);
        }

        if (st == PetState.GROOM) {
            drawScratchingPaw(g, pet, headCx, headCy);
        }
    }

    private void drawTorso(GraphicsContext g, Palette p, Pose q, double bodyCy) {
        double w = q.bodyW, h = q.bodyH;

        g.setFill(p.fur());
        g.fillOval(-w / 2, bodyCy - h / 2, w, h);

        // Two soft patches on the back — puppy markings, not tabby stripes.
        g.setFill(p.furDark());
        g.setGlobalAlpha(0.5);
        g.fillOval(-w * 0.36, bodyCy - h * 0.40, w * 0.30, h * 0.34);
        g.fillOval(-w * 0.06, bodyCy - h * 0.46, w * 0.22, h * 0.26);
        g.setGlobalAlpha(1);

        // Belly, plus a chest blaze running up under the chin. The blaze is what separates the head
        // from the torso, since both are the same fur colour.
        g.setFill(p.belly());
        g.fillOval(-w * 0.28, bodyCy - h * 0.02, w * 0.62, h * 0.56);
        g.fillOval(w * 0.04, bodyCy - h * 0.34, w * 0.36, h * 0.72);
    }

    private static double lift(double phase, double swing) {
        if (swing <= 0) return 0;
        return Math.max(0, Math.sin(phase)) * (swing * 0.4);
    }

    private void drawLeg(GraphicsContext g, Color c, double x, double top, double len,
                         double dx, double liftBy) {
        double h = Math.max(3, len - liftBy);
        g.setFill(c);
        g.fillRoundRect(x + dx - LEG_W / 2, top, LEG_W, h, LEG_W, LEG_W);
        // A paler pad at the bottom sells the stubbiness.
        g.setFill(Color.color(1, 1, 1, 0.20));
        g.fillOval(x + dx - LEG_W / 2, top + h - 4.5, LEG_W, 4.5);
    }

    /** The folded rear leg of a sitting puppy. */
    private void drawHaunch(GraphicsContext g, Color c, double x, double top, double k) {
        g.setFill(c);
        g.fillOval(x - 12 * k, top - 12 * k, 24 * k, 18 * k);
    }

    /**
     * A short stubby tail. The wag rotates the whole stub rather than whipping a curve, which is what
     * a stumpy tail actually does.
     */
    private void drawTail(GraphicsContext g, Pet pet, Pose q, double bodyCy) {
        Palette p = pet.palette;
        double wag = Math.sin(pet.age * q.tailSpeed) * q.tailAmp;

        g.save();
        g.translate(-BODY_W * 0.44, bodyCy - 9);
        // Angled up rather than out. Swept sideways with a pale tip it reads as a flipper.
        g.rotate(pet.state == PetState.SLEEP ? 84 + wag * 0.4 : -58 + wag);
        g.setStroke(p.fur());
        g.setLineWidth(10);
        g.setLineCap(StrokeLineCap.ROUND);
        g.strokeLine(0, 0, -11, 0);
        g.setStroke(p.furDark());
        g.setGlobalAlpha(0.35);
        g.setLineWidth(6);
        g.strokeLine(-7, 0, -12, 0);
        g.setGlobalAlpha(1);
        g.restore();
    }

    /**
     * One floppy ear: an ellipse pinned at the skull's edge and rotated about that pin, so the angle
     * swings it like a hinge. The ellipse starts just above the pin and runs downward, so almost all
     * of it lands outside the head circle.
     *
     * <p>Ears use the darker fur tone. Fur-coloured ears disappear against a fur-coloured head, and
     * the darker pair doubles as the classic two-tone puppy marking.
     *
     * @param angleDeg rotation from straight-down; negative swings left, positive swings right
     */
    private void drawEar(GraphicsContext g, Palette p, double px, double py, double angleDeg) {
        g.save();
        g.translate(px, py);
        g.rotate(angleDeg);
        // Deepened a touch beyond furDark: in the warmer palettes furDark is close enough to fur that
        // the ears would otherwise blend into the skull.
        g.setFill(p.furDark().deriveColor(0, 1.05, 0.88, 1));
        g.fillOval(-EAR_W / 2, -4, EAR_W, EAR_H);
        g.setFill(p.ear());
        g.setGlobalAlpha(0.40);
        g.fillOval(-EAR_W * 0.24, 3, EAR_W * 0.48, EAR_H * 0.52);
        g.setGlobalAlpha(1);
        g.restore();
    }

    /** The bare skull: fur circle plus a faint rim to lift it off the torso. */
    private void drawSkull(GraphicsContext g, Palette p, double cx, double cy) {
        g.setFill(p.fur());
        g.fillOval(cx - HEAD_R, cy - HEAD_R, HEAD_R * 2, HEAD_R * 2);
        g.setStroke(p.furDark());
        g.setGlobalAlpha(0.40);
        g.setLineWidth(1.5);
        g.strokeOval(cx - HEAD_R, cy - HEAD_R, HEAD_R * 2, HEAD_R * 2);
        g.setGlobalAlpha(1);
    }

    /**
     * The frontal face: a pale muzzle patch low on the head, eyes above it, then nose, mouth and
     * tongue stacked down the muzzle's centre line. Drawn last so the ears pass behind it.
     */
    private void drawFace(GraphicsContext g, Pet pet, Pose q, double cx, double cy) {
        Palette p = pet.palette;
        PetState st = pet.state;

        // Muzzle patch, centred slightly toward the facing direction.
        g.setFill(p.belly());
        g.fillOval(cx - 4, cy + 0.5, 21, 15);

        boolean shut = st == PetState.SLEEP || pet.blinking > 0;
        boolean wide = st == PetState.FALL || st == PetState.DRAG || st == PetState.JUMP;
        drawEye(g, p, cx + 8, cy - 5, shut, wide);
        drawEye(g, p, cx - 7, cy - 5, shut, wide);

        if (!shut) {
            // Eyebrow dots — cheap, and they give the face an expression.
            g.setFill(p.furDark());
            g.setGlobalAlpha(0.6);
            g.fillOval(cx + 5.5, cy - 13.5, 5, 2.6);
            g.fillOval(cx - 9.5, cy - 13.5, 5, 2.6);
            g.setGlobalAlpha(1);
        }

        // Big black button nose, centred on the muzzle.
        g.setFill(Color.web("#241c19"));
        g.fillOval(cx + 2, cy + 2, 9.5, 7.5);
        g.setFill(Color.color(1, 1, 1, 0.45));
        g.fillOval(cx + 3.6, cy + 3, 2.8, 2.1);

        // Philtrum plus a two-arc smile, hanging off the bottom of the nose.
        g.setStroke(Color.web("#3a2c26"));
        g.setLineWidth(1.3);
        g.setLineCap(StrokeLineCap.ROUND);
        g.strokeLine(cx + 6.7, cy + 9.5, cx + 6.7, cy + 11);
        g.strokeArc(cx + 2.2, cy + 10.5, 5, 4.5, 190, 140, ArcType.OPEN);
        g.strokeArc(cx + 6.2, cy + 10.5, 5, 4.5, 210, 140, ArcType.OPEN);

        if (q.tongueOut) {
            double bob = Math.sin(pet.age * 6) * 0.9;
            g.setFill(Color.web("#f4718c"));
            g.fillRoundRect(cx + 3.7, cy + 12 + bob, 6.5, 7.5, 5, 5);
            g.setStroke(Color.web("#d9526f"));
            g.setLineWidth(0.9);
            g.strokeLine(cx + 6.9, cy + 14 + bob, cx + 6.9, cy + 18 + bob);
        }

        if (st == PetState.SLEEP || st == PetState.SIT || st == PetState.GROOM) {
            g.setFill(Color.color(1, 0.55, 0.62, 0.30));
            g.fillOval(cx + 10, cy + 0.5, 7.5, 4.5);
            g.fillOval(cx - 17, cy + 0.5, 7.5, 4.5);
        }
    }

    private void drawEye(GraphicsContext g, Palette p, double x, double y,
                         boolean shut, boolean wide) {
        if (shut) {
            g.setStroke(p.eye());
            g.setLineWidth(1.7);
            g.setLineCap(StrokeLineCap.ROUND);
            g.strokeArc(x - 3.4, y - 2.4, 6.8, 4.8, 200, 140, ArcType.OPEN);
            return;
        }
        double r = wide ? 4.7 : 4.1;
        g.setFill(p.eye());
        g.fillOval(x - r, y - r, r * 2, r * 2);
        g.setFill(Color.color(1, 1, 1, 0.92));
        g.fillOval(x - r * 0.1, y - r * 0.8, r * 0.8, r * 0.8);
    }

    /**
     * Accent-coloured collar with a round tag. Sits well clear of the chin — placed any higher it
     * cuts across the muzzle and the hanging tongue.
     */
    private void drawCollar(GraphicsContext g, Palette p, double headCx, double headCy,
                            double bodyCy, double bodyH) {
        // Clamped to the torso so a low-slung head cannot drag the collar off the body entirely.
        double y = Math.min(headCy + HEAD_R + 7, bodyCy + bodyH * 0.15);
        // An arc rather than a straight line: a flat band across a round chest reads as a slab.
        g.setStroke(p.accent());
        g.setLineWidth(5);
        g.setLineCap(StrokeLineCap.ROUND);
        g.strokeArc(headCx - 14, y - 8, 27, 13, 200, 145, ArcType.OPEN);
        g.setFill(Color.web("#f2c14e"));
        g.fillOval(headCx - 3, y + 1, 6.5, 6.5);
    }

    /** A hind paw brought up beside the head to scratch behind the ear. */
    private void drawScratchingPaw(GraphicsContext g, Pet pet, double headCx, double headCy) {
        double wob = Math.sin(pet.age * 11) * 2.2;
        g.setFill(pet.palette.furDark());
        g.fillRoundRect(headCx - 25, headCy + 8 + wob, LEG_W - 2, 21, LEG_W, LEG_W);
        g.setFill(pet.palette.belly());
        g.fillOval(headCx - 25, headCy + 5 + wob, LEG_W - 2, LEG_W - 2);
    }
}
