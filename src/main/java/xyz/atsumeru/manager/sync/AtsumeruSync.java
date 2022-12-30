package xyz.atsumeru.manager.sync;

import io.reactivex.schedulers.Schedulers;
import xyz.atsumeru.manager.controller.MainController;
import xyz.atsumeru.manager.managers.Settings;
import xyz.atsumeru.manager.models.manga.Chapter;
import xyz.atsumeru.manager.source.AtsumeruSource;
import xyz.atsumeru.manager.views.Snackbar;

public class AtsumeruSync {

    protected static void syncReadProgress(Chapter chapter, int readedPages) {
        if (!Settings.Atsumeru.isDisableProgressSync() && !AtsumeruSource.isChapterHash(chapter.getCHash())) {
            AtsumeruSource.getSource(AtsumeruSource.getCurrentServer()).syncReaded(chapter.getCHash(), null, readedPages)
                    .cache().subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe(
                            message -> System.out.println("syncReadProgress: got message from server - " + message.getMessage()),
                            throwable -> Snackbar.showExceptionSnackbar(MainController.getSnackbarRoot(true), throwable, true)
                    );
        }
    }
}