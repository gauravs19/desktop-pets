package dev.gauravs.desktoppets.devtools;

/**
 * Non-{@code Application} entry point for {@link RenderPreview}, for the same reason
 * {@code Launcher} exists for the app itself: with JavaFX on the classpath rather than the module
 * path, launching a class that extends {@code Application} directly fails with "JavaFX runtime
 * components are missing".
 *
 * <pre>
 * java -cp target\desktop-pets.jar dev.gauravs.desktoppets.devtools.PreviewMain preview.png
 * </pre>
 */
public final class PreviewMain {
    public static void main(String[] args) {
        RenderPreview.main(args);
    }
}
