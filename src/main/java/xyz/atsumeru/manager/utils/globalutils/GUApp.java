package xyz.atsumeru.manager.utils.globalutils;

public class GUApp {

    public static void sleepThread(int millis) {
        if (millis > 0) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void safeRun(Runnable runnable) {
        if (runnable != null) {
            runnable.run();
        }
    }
}
