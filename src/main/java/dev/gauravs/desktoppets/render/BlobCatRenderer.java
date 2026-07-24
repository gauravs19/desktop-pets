package dev.gauravs.desktoppets.render;

import dev.gauravs.desktoppets.Palette;
import dev.gauravs.desktoppets.Particles;
import dev.gauravs.desktoppets.Pet;
import dev.gauravs.desktoppets.PetState;
import dev.gauravs.desktoppets.Screens;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;

/**
 * A round, procedurally-drawn cat.
 *
 * <p>Everything is vector shapes rather than sprite images, for three reasons: there are no art
 * assets to license or ship, the pet stays crisp at any DPI or scale factor, and poses can be
 * derived from continuous state (squash factor, walk phase, blink amount) instead of being quantised
 * into a handful of frames.
 *
 * <p>All drawing happens in "pet-local" coordinates: the origin sits between the pet's feet, x runs
 * forward (the direction it faces), and <em>y runs negative upward</em>. The caller-facing
 * {@link #draw} method installs the transform that maps those coordinates onto the canvas, applying
 * the horizontal mirror for facing and the volume-preserving squash-and-stretch.
 */
public final class BlobCatRenderer implements PetRenderer {

    private static final double FOOT_MARGIN = 14;

    // Body proportions in pet-local px at scale 1.0. BASE_HEIGHT (62) is head-top to feet.
    private static final double LEG_LEN = 11;
    private static final double LEG_W = 7;
    private static final double BODY_W = 46;
    private static final double BODY_H = 30;
    private static final double BODY_CY = -26;
    private static final double HEAD_R = 15.5;
    private static final double HEAD_CX = 12;
    private static final double HEAD_CY = -45;

    @Override
    public double footMargin() {
        return FOOT_MARGIN;
    }

    @Override
    public void draw(GraphicsContext g, Pet pet, Particles fx, double canvasW, double canvasH) {
        double ax = canvasW / 2.0;
        double ay = canvasH - FOOT_MARGIN;

        g.save();
        g.translate(ax, ay);

        drawShadow(g, pet);

        double sq = Math.max(0.4, pet.squash);
        g.save();
        // Mirror for facing, then squash vertically while widening horizontally so the pet reads as
        // squishy rather than simply smaller.
        g.scale(pet.facing * pet.scale / Math.sqrt(sq), pet.scale * sq);
        drawBody(g, pet);
        g.restore();

        // Particles are drawn outside the mirror so glyphs never come out backwards.
        fx.draw(g, pet.palette);

        g.restore();
    }

    /** A soft contact shadow that shrinks and fades as the pet gets further off the ground. */
    private void drawShadow(GraphicsContext g, Pet pet) {
        double air = Math.max(0, Screens.groundAt(pet.x, pet.y) - pet.y);
        double t = Math.min(1, air / 240.0);
        double k = 1 - 0.65 * t;
        double w = BODY_W * 0.92 * pet.scale * k;
        double h = 9 * pet.scale * k;
        g.setFill(Color.color(0, 0, 0, 0.22 * (1 - 0.7 * t)));
        g.fillOval(-w / 2, -h / 2, w, h);
    }

    private void drawBody(GraphicsContext g, Pet pet) {
        Palette p = pet.palette;
        PetState st = pet.state;

        // --- pose parameters derived from state -----------------------------------------
        double walkPhase = pet.age * (st == PetState.RUN ? 15 : 7.5);
        double swing = 0;
        double bodyDy = 0;
        double bodyH = BODY_H;
        double bodyW = BODY_W;
        double headDx = 0;
        double headDy = 0;
        double lean = 0;
        double legLen = LEG_LEN;
        boolean showLegs = true;
        boolean sitting = false;
        double tailAmp;

        switch (st) {
            case WALK -> {
                swing = 4.2;
                bodyDy = -Math.abs(Math.sin(walkPhase)) * 2.0;
                tailAmp = 9;
                lean = 1.5;
            }
            case RUN -> {
                swing = 7.5;
                bodyDy = -Math.abs(Math.sin(walkPhase)) * 3.2;
                bodyW = BODY_W + 3;
                tailAmp = 14;
                lean = 4;
            }
            case SIT, GROOM -> {
                sitting = true;
                legLen = 5;
                bodyDy = 5;
                bodyH = BODY_H + 3;
                headDy = 1;
                tailAmp = 6;
            }
            case SLEEP -> {
                showLegs = false;
                bodyDy = 12;
                bodyH = BODY_H - 9;
                bodyW = BODY_W + 9;
                headDx = 3;
                headDy = 16 + Math.sin(pet.age * 1.4) * 0.8;   // slow sleeping breath
                tailAmp = 1.5;
            }
            case DRAG -> {
                legLen = LEG_LEN + 6;
                swing = 2.5 * Math.sin(pet.age * 9);           // dangling paws
                bodyDy = -2;
                headDy = -1;
                tailAmp = 15;
            }
            case FALL, JUMP -> {
                legLen = LEG_LEN + 3;
                swing = 6;
                headDy = -2;
                tailAmp = 16;
            }
            default -> {
                // IDLE: a gentle breathing bob.
                bodyDy = Math.sin(pet.age * 2.1) * 0.9;
                bodyH = BODY_H + Math.sin(pet.age * 2.1) * 0.7;
                tailAmp = 5;
            }
        }

        double bodyCy = BODY_CY + bodyDy;
        double bodyBottom = bodyCy + bodyH / 2;
        double headCx = HEAD_CX + headDx + lean;
        double headCy = HEAD_CY + bodyDy + headDy;

        drawTail(g, pet, tailAmp, bodyCy);

        if (showLegs) {
            // Far-side legs first, in the darker fur tone, so the body overlaps them.
            double s = Math.sin(walkPhase) * swing;
            if (sitting) {
                drawHaunch(g, p.furDark(), -13, bodyBottom - 2, 1.0);
            } else {
                drawLeg(g, p.furDark(), -13, bodyBottom, legLen, +s, liftFor(walkPhase, swing));
                drawLeg(g, p.furDark(), 11, bodyBottom, legLen, -s, liftFor(walkPhase + Math.PI, swing));
            }
        }

        // --- torso ---------------------------------------------------------------------
        g.setFill(p.fur());
        g.fillOval(-bodyW / 2, bodyCy - bodyH / 2, bodyW, bodyH);

        // Belly highlight, clipped visually by simply insetting it.
        g.setFill(p.belly());
        g.fillOval(-bodyW * 0.30, bodyCy - bodyH * 0.05, bodyW * 0.62, bodyH * 0.52);

        drawStripes(g, p, bodyW, bodyCy, bodyH);

        if (showLegs) {
            double s = Math.sin(walkPhase) * swing;
            if (sitting) {
                drawHaunch(g, p.fur(), -11, bodyBottom - 1, 1.15);
                drawLeg(g, p.fur(), 12, bodyBottom, legLen, 0, 0);
                drawLeg(g, p.fur(), 5, bodyBottom, legLen, 0, 0);
            } else {
                drawLeg(g, p.fur(), -8, bodyBottom, legLen, -s, liftFor(walkPhase + Math.PI, swing));
                drawLeg(g, p.fur(), 16, bodyBottom, legLen, +s, liftFor(walkPhase, swing));
            }
        }

        drawHead(g, pet, headCx, headCy, st);

        if (st == PetState.GROOM) {
            drawGroomingPaw(g, pet, headCx, headCy);
        }
    }

    /** Vertical lift for a leg at the given phase — only the forward half of the stride lifts. */
    private static double liftFor(double phase, double swing) {
        if (swing <= 0) return 0;
        return Math.max(0, Math.sin(phase)) * (swing * 0.45);
    }

    private void drawLeg(GraphicsContext g, Color c, double x, double top, double len,
                         double dx, double lift) {
        g.setFill(c);
        double h = Math.max(2, len - lift);
        g.fillRoundRect(x + dx - LEG_W / 2, top, LEG_W, h + 2, LEG_W, LEG_W);
    }

    /** The folded rear leg of a sitting pet. */
    private void drawHaunch(GraphicsContext g, Color c, double x, double top, double k) {
        g.setFill(c);
        g.fillOval(x - 9 * k, top - 9 * k, 18 * k, 14 * k);
    }

    private void drawStripes(GraphicsContext g, Palette p, double bodyW, double bodyCy, double bodyH) {
        g.setStroke(p.furDark());
        g.setLineWidth(2.6);
        g.setLineCap(StrokeLineCap.ROUND);
        for (int i = 0; i < 3; i++) {
            double sx = -bodyW * 0.20 + i * bodyW * 0.16;
            double top = bodyCy - bodyH * 0.42;
            g.strokeLine(sx, top, sx - 3, top + bodyH * 0.26);
        }
    }

    private void drawTail(GraphicsContext g, Pet pet, double amp, double bodyCy) {
        Palette p = pet.palette;
        double wag = Math.sin(pet.age * (pet.state == PetState.SLEEP ? 0.9 : 3.4)) * amp;
        double rootX = -20, rootY = bodyCy - 2;
        double ctrlX = -33, ctrlY = rootY - 12 + wag * 0.6;
        double endX = -34 - amp * 0.2, endY = rootY - 26 + wag;

        if (pet.state == PetState.SLEEP) {
            // Curled forward around the body instead of raised.
            ctrlY = rootY + 14;
            endX = -4;
            endY = rootY + 16 + wag * 0.4;
        }

        g.setStroke(p.fur());
        g.setLineWidth(6.5);
        g.setLineCap(StrokeLineCap.ROUND);
        g.beginPath();
        g.moveTo(rootX, rootY);
        g.quadraticCurveTo(ctrlX, ctrlY, endX, endY);
        g.stroke();

        g.setStroke(p.belly());
        g.setLineWidth(3.4);
        g.strokeLine(endX, endY, endX + (ctrlX - endX) * 0.28, endY + (ctrlY - endY) * 0.28);
    }

    private void drawHead(GraphicsContext g, Pet pet, double cx, double cy, PetState st) {
        Palette p = pet.palette;

        drawEar(g, p, cx - 7, cy - 10, cx - 0.5, cy - 13, cx - 6.5, cy - 25);
        drawEar(g, p, cx + 3.5, cy - 13, cx + 10.5, cy - 8.5, cx + 10, cy - 24);

        g.setFill(p.fur());
        g.fillOval(cx - HEAD_R, cy - HEAD_R, HEAD_R * 2, HEAD_R * 2);

        // Muzzle
        g.setFill(p.belly());
        g.fillOval(cx + 1, cy + 1, 14, 10);

        boolean eyesShut = st == PetState.SLEEP || pet.blinking > 0;
        boolean wide = st == PetState.FALL || st == PetState.DRAG || st == PetState.JUMP;

        drawEye(g, p, cx + 8.5, cy - 2, eyesShut, wide);
        drawEye(g, p, cx - 2.5, cy - 2, eyesShut, wide);

        // Nose and mouth
        g.setFill(p.ear());
        g.fillPolygon(
            new double[]{cx + 8, cx + 13, cx + 10.5},
            new double[]{cy + 3.5, cy + 3.5, cy + 6.5}, 3);
        g.setStroke(p.eye());
        g.setLineWidth(1.1);
        g.strokeLine(cx + 10.5, cy + 6.5, cx + 10.5, cy + 8);
        g.strokeArc(cx + 6.5, cy + 5.5, 4, 4, 200, 130, javafx.scene.shape.ArcType.OPEN);
        g.strokeArc(cx + 10.5, cy + 5.5, 4, 4, 210, 130, javafx.scene.shape.ArcType.OPEN);

        // Whiskers
        g.setStroke(Color.color(1, 1, 1, 0.65));
        g.setLineWidth(1.0);
        for (int i = -1; i <= 1; i++) {
            g.strokeLine(cx + 12, cy + 4 + i * 0.6, cx + 22, cy + 1 + i * 3.4);
        }

        if (st == PetState.SLEEP || st == PetState.SIT || st == PetState.GROOM) {
            // Blush marks — only when settled, so the pet looks content rather than startled.
            g.setFill(Color.color(1, 0.55, 0.62, 0.28));
            g.fillOval(cx + 3, cy + 1.5, 7, 4);
            g.fillOval(cx - 8, cy + 1.5, 7, 4);
        }
    }

    private void drawEar(GraphicsContext g, Palette p,
                         double x1, double y1, double x2, double y2, double tx, double ty) {
        g.setFill(p.furDark());
        g.fillPolygon(new double[]{x1, x2, tx}, new double[]{y1, y2, ty}, 3);
        // Inner ear: the same triangle pulled 40% toward its own centroid.
        double gx = (x1 + x2 + tx) / 3, gy = (y1 + y2 + ty) / 3;
        g.setFill(p.ear());
        g.fillPolygon(
            new double[]{lerp(x1, gx, 0.42), lerp(x2, gx, 0.42), lerp(tx, gx, 0.34)},
            new double[]{lerp(y1, gy, 0.42), lerp(y2, gy, 0.42), lerp(ty, gy, 0.34)}, 3);
    }

    private void drawEye(GraphicsContext g, Palette p, double x, double y,
                         boolean shut, boolean wide) {
        if (shut) {
            g.setStroke(p.eye());
            g.setLineWidth(1.6);
            g.setLineCap(StrokeLineCap.ROUND);
            g.strokeArc(x - 3.2, y - 2.2, 6.4, 4.4, 200, 140, javafx.scene.shape.ArcType.OPEN);
            return;
        }
        double r = wide ? 4.4 : 3.6;
        g.setFill(p.eye());
        g.fillOval(x - r * 0.82, y - r, r * 1.64, r * 2);
        g.setFill(Color.color(1, 1, 1, 0.9));
        g.fillOval(x - r * 0.1, y - r * 0.75, r * 0.7, r * 0.7);
    }

    /** A raised front paw held up near the muzzle, with a small licking wobble. */
    private void drawGroomingPaw(GraphicsContext g, Pet pet, double headCx, double headCy) {
        double wob = Math.sin(pet.age * 8) * 1.6;
        g.setFill(pet.palette.fur());
        g.fillRoundRect(headCx + 6, headCy + 6 + wob, LEG_W, 16, LEG_W, LEG_W);
        g.setFill(pet.palette.belly());
        g.fillOval(headCx + 6.5, headCy + 4 + wob, LEG_W - 1, LEG_W - 1);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
