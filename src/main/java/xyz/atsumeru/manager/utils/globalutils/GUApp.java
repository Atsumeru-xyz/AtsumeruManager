package xyz.atsumeru.manager.utils.globalutils;

public class GUApp {

    public static void safeRun(Runnable runnable) {
        if (runnable != null) {
            runnable.run();
        }
    }
}
