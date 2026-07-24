package dev.gauravs.desktoppets;

/**
 * Plain (non-Application) entry point.
 *
 * When the main class extends {@code javafx.application.Application} and JavaFX lives on the
 * classpath rather than the module path, the launcher refuses to start with "JavaFX runtime
 * components are missing". Bouncing through a class that does <em>not</em> extend Application
 * sidesteps that check, which is what makes the shaded fat jar runnable with a bare
 * {@code java -jar}.
 */
public final class Launcher {
    public static void main(String[] args) {
        App.main(args);
    }
}
