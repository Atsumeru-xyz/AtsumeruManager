package xyz.atsumeru.manager.sync;

import xyz.atsumeru.manager.models.manga.Chapter;

public class SyncManager {

    public static void syncReadProgress(Chapter chapter, int readedPages) {
        AtsumeruSync.syncReadProgress(chapter, readedPages);
    }
}