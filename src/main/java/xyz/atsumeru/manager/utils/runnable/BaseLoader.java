package xyz.atsumeru.manager.utils.runnable;

import lombok.Getter;

public class BaseLoader implements Runnable {
    final ContentLoader contentLoader;

    @Getter
    private final String name;

    boolean running;

    /**
     * A generic Runnable that is run by ContentLoader.
     *
     * @param name          the name of the thread
     * @param contentLoader the ContentLoader which created this instance
     */
    BaseLoader(String name, ContentLoader contentLoader) {
        this.name = name;
        this.contentLoader = contentLoader;
        this.running = true;
    }

    @Override
    public void run() {
        finish();
    }

    /**
     * Safely stop the runnable.
     * <p>
     * The {@link #run()} method must make use of the {@link #running} parameter for this method to
     * be useful. If it doesn't, the runnable might not be meaningful enough to stop, anyway.
     */
    public void requestStop() {
        this.running = false;
    }

    /**
     * Called when the {@link #run()} method has finished execution.
     */
    void finish() {
        contentLoader.getLoaders().remove(this);
    }
}
