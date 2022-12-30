package xyz.atsumeru.manager.utils.runnable;

import lombok.Getter;
import xyz.atsumeru.manager.controller.stages.StageReaderController;
import xyz.atsumeru.manager.models.manga.Chapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ContentLoader {
    public static final String PREFIX_LOAD_PAGE = "loadPage_";
    private static ContentLoader INSTANCE;
    @Getter
    private final List<BaseLoader> loaders = new ArrayList<>();

    private ContentLoader() {
    }

    public static ContentLoader getInstance() {
        return Optional.ofNullable(INSTANCE).orElseGet(() -> INSTANCE = new ContentLoader());
    }

    /**
     * Create a LoaderRunnable for loading a chapter page.
     *
     * @param server           the Server to load from
     * @param chapter          the Chapter the page is from
     * @param page             the 0-indexed page number
     * @param readerController the ReaderController to update before/after the page is loaded
     * @param preloading       whether the page is being preloaded or not (loaded before the user
     *                         gets to the page)
     * @param preloadingAmount the number of subsequent pages to preload, or -1 for infinite
     * @see PageLoader
     */
    public void loadPage(Chapter chapter, int page, StageReaderController readerController, boolean preloading, int preloadingAmount) {
        startThreadSafely(new PageLoader(
                PREFIX_LOAD_PAGE + chapter.getCHash() + "_" + page,
                this,
                chapter,
                page,
                readerController,
                preloading,
                preloadingAmount
        ));
    }

    /**
     * Start a Runnable in a new Thread while ensuring that the name does not overlap with
     * pre-existing threads.
     *
     * @param runnable the Runnable to run in the Thread
     */
    private void startThreadSafely(BaseLoader runnable) {
        Thread.getAllStackTraces().keySet()
                .stream()
                .filter(thread -> thread.getName().equals(runnable.getName()))
                .findFirst()
                .ifPresentOrElse(
                        _ignored -> {},
                        () -> {
                            Thread thread = new Thread(runnable);
                            thread.setName(runnable.getName());
                            thread.start();
                            loaders.add(runnable);
                        });
    }

    /**
     * Request threads with names starting with the given prefix to stop.
     * <p>
     * This method does not guarantee the threads to immediately stop -- it simply calls
     * requestStop() on all matching running LoaderRunnable's.
     *
     * @param prefix the prefix of the thread names to stop
     */
    public void stopThreads(String prefix) {
        loaders.stream()
                .filter(loader -> loader.getName().startsWith(prefix))
                .forEach(BaseLoader::requestStop);
    }

    /**
     * Request all threads to stop.
     * <p>
     * This method does not guarantee the threads to immediately stop -- it simply calls
     * requestStop() on all matching running LoaderRunnable's.
     */
    public void stopAllThreads() {
        loaders.forEach(BaseLoader::requestStop);
    }
}
