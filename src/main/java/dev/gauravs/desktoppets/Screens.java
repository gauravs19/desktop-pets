package dev.gauravs.desktoppets;

import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;

/**
 * Multi-monitor geometry helpers.
 *
 * <p>Everything works in "visual bounds", which is the monitor rectangle minus the taskbar and any
 * other docked shell surface. That is what makes a pet sit <em>on top of</em> the Windows taskbar
 * edge instead of behind it, and it is why a pet on a secondary monitor lands at that monitor's own
 * floor rather than the primary one's.
 */
public final class Screens {

    private Screens() {
    }

    /** The monitor whose visual bounds contain the point, or the closest one if the point is in a gap. */
    public static Rectangle2D screenAt(double x, double y) {
        Rectangle2D best = null;
        double bestDist = Double.MAX_VALUE;
        for (Screen s : Screen.getScreens()) {
            Rectangle2D b = s.getVisualBounds();
            if (b.contains(x, y)) return b;
            double dx = Math.max(0, Math.max(b.getMinX() - x, x - b.getMaxX()));
            double dy = Math.max(0, Math.max(b.getMinY() - y, y - b.getMaxY()));
            double d = dx * dx + dy * dy;
            if (d < bestDist) {
                bestDist = d;
                best = b;
            }
        }
        return best != null ? best : Screen.getPrimary().getVisualBounds();
    }

    /** Floor height (screen Y) for a pet standing at the given point. */
    public static double groundAt(double x, double y) {
        return screenAt(x, y).getMaxY();
    }

    /** Union of every monitor's visual bounds — the region a pet is allowed to roam. */
    public static Rectangle2D desktopBounds() {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (Screen s : Screen.getScreens()) {
            Rectangle2D b = s.getVisualBounds();
            minX = Math.min(minX, b.getMinX());
            minY = Math.min(minY, b.getMinY());
            maxX = Math.max(maxX, b.getMaxX());
            maxY = Math.max(maxY, b.getMaxY());
        }
        return new Rectangle2D(minX, minY, maxX - minX, maxY - minY);
    }

    /**
     * True when the horizontal position is still over some monitor at this height. Used to decide
     * whether a walking pet has reached a real edge of the desktop or merely the seam between two
     * monitors, which it should be free to cross.
     */
    public static boolean standable(double x, double y) {
        for (Screen s : Screen.getScreens()) {
            Rectangle2D b = s.getVisualBounds();
            if (x >= b.getMinX() && x <= b.getMaxX() && y >= b.getMinY() - 2 && y <= b.getMaxY() + 2) {
                return true;
            }
        }
        return false;
    }
}
