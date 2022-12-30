package xyz.atsumeru.manager.managers;

import xyz.atsumeru.manager.models.manga.Chapter;
import xyz.atsumeru.manager.sync.SyncManager;

public class MangaReaderCommunicationManager {

    public void onSaveHistory(Chapter chapter, int readedPages, int countPages) {
        if (readedPages > countPages) {
            readedPages = countPages;
        }

        // Синхронизация прогресса чтения с сервисами отслеживания
        SyncManager.syncReadProgress(chapter, readedPages);
    }
}