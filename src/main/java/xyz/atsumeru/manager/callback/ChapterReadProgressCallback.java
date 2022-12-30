package xyz.atsumeru.manager.callback;

public interface ChapterReadProgressCallback {
    void onProgressChanged(String chash, int readedPages, int countPages);
}
