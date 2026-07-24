package dev.gauravs.desktoppets;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/**
 * The little cosmetic flourishes: sleep Zzz's, affection hearts, and puffs of dust on landing.
 *
 * <p>Particles live in coordinates relative to the pet's foot anchor (x right, y up) so they travel
 * with the pet's window and never need to know about screen geometry. They are purely decorative —
 * nothing here feeds back into physics or behaviour.
 */
public final class Particles {

    public enum Kind { ZZZ, HEART, DUST }

    private static final Font ZZZ_FONT = Font.font("Segoe UI", FontWeight.BOLD, 15);

    private static final class P {
        Kind kind;
        double x, y, vx, vy, life, maxLife, size, rot;
    }

    private final List<P> live = new ArrayList<>();
    private final Random rng = new Random();
    private double zzzCooldown;

    /** Emit a slow-drifting Zzz, rate-limited so a sleeping pet does not fog up the screen. */
    public void sleepTick(double dt, double petHeight) {
        zzzCooldown -= dt;
        if (zzzCooldown > 0) return;
        zzzCooldown = 1.5 + rng.nextDouble() * 0.8;

        P p = new P();
        p.kind = Kind.ZZZ;
        p.x = 10 + rng.nextDouble() * 6;
        p.y = -petHeight * 0.55;
        p.vx = 9 + rng.nextDouble() * 5;
        p.vy = -20 - rng.nextDouble() * 8;
        p.maxLife = p.life = 2.6;
        p.size = 0.75 + rng.nextDouble() * 0.4;
        live.add(p);
    }

    /** A small fountain of hearts, used when the pet is petted. */
    public void hearts(double petHeight) {
        for (int i = 0; i < 6; i++) {
            P p = new P();
            p.kind = Kind.HEART;
            p.x = (rng.nextDouble() - 0.5) * 26;
            p.y = -petHeight * (0.55 + rng.nextDouble() * 0.3);
            p.vx = (rng.nextDouble() - 0.5) * 40;
            p.vy = -50 - rng.nextDouble() * 45;
            p.maxLife = p.life = 1.0 + rng.nextDouble() * 0.6;
            p.size = 5 + rng.nextDouble() * 4;
            live.add(p);
        }
    }

    /** Dust kicked sideways on impact; {@code strength} scales with landing speed. */
    public void dust(double strength) {
        int n = (int) Math.min(10, 3 + strength / 160);
        for (int i = 0; i < n; i++) {
            P p = new P();
            p.kind = Kind.DUST;
            p.x = (rng.nextDouble() - 0.5) * 30;
            p.y = -2;
            p.vx = (rng.nextDouble() - 0.5) * 150;
            p.vy = -20 - rng.nextDouble() * 45;
            p.maxLife = p.life = 0.35 + rng.nextDouble() * 0.3;
            p.size = 2 + rng.nextDouble() * 3;
            live.add(p);
        }
    }

    public void update(double dt) {
        for (Iterator<P> it = live.iterator(); it.hasNext(); ) {
            P p = it.next();
            p.life -= dt;
            if (p.life <= 0) {
                it.remove();
                continue;
            }
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.rot += dt * 2;
            if (p.kind == Kind.DUST) {
                p.vy += 260 * dt;                  // dust settles back down
                p.vx *= 1 - Math.min(1, dt * 4);
            } else {
                p.vy *= 1 - Math.min(1, dt * 0.6); // hearts and Zzz's ease upward
            }
        }
    }

    /**
     * Draw every live particle. Expects the graphics context to already be translated to the pet's
     * foot anchor and <em>not</em> mirrored — mirroring would reverse the Zzz glyphs.
     */
    public void draw(GraphicsContext g, Palette palette) {
        for (P p : live) {
            double a = Math.min(1, p.life / (p.maxLife * 0.6));
            switch (p.kind) {
                case ZZZ -> {
                    g.setGlobalAlpha(a * 0.9);
                    g.setFill(palette.eye().deriveColor(0, 1, 2.4, 1));
                    g.setFont(ZZZ_FONT);
                    g.setTextAlign(TextAlignment.CENTER);
                    g.save();
                    g.translate(p.x, p.y);
                    g.scale(p.size, p.size);
                    g.fillText("z", 0, 0);
                    g.restore();
                }
                case HEART -> {
                    g.setGlobalAlpha(a);
                    g.setFill(Color.web("#ff6b8a"));
                    drawHeart(g, p.x, p.y, p.size);
                }
                case DUST -> {
                    g.setGlobalAlpha(a * 0.5);
                    g.setFill(Color.web("#c9c4bd"));
                    g.fillOval(p.x - p.size / 2, p.y - p.size / 2, p.size, p.size);
                }
            }
        }
        g.setGlobalAlpha(1);
    }

    private static void drawHeart(GraphicsContext g, double x, double y, double s) {
        // Two lobes plus a triangular point — cheaper and rounder-looking than a bezier path.
        g.fillOval(x - s * 0.55, y - s * 0.5, s * 0.7, s * 0.7);
        g.fillOval(x - s * 0.15, y - s * 0.5, s * 0.7, s * 0.7);
        g.fillPolygon(
            new double[]{x - s * 0.52, x + s * 0.52, x},
            new double[]{y - s * 0.1, y - s * 0.1, y + s * 0.6}, 3);
    }

    public boolean isEmpty() {
        return live.isEmpty();
    }
}
