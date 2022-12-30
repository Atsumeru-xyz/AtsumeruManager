package xyz.atsumeru.manager.models.manga;

import lombok.Data;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class Chapter implements Serializable {
    private long serviceId;
    private long timestamp;
    private String date;
    private String cHash;
    private String title;
    private String link;
    private String contentId;
    private String mangaLink;
    private String cover;
    private String folder;
    private Float chapterNumber;
    private Float volumeNumber;
    private boolean isReaded;

    private int currentPage = Integer.MIN_VALUE;
    private int pagesCount = Integer.MIN_VALUE;

    private List<String> images = new ArrayList<>();
    private transient Object[] downloadedImages;

    public void setImages(List<String> pages, Integer currentPage) {
        this.images = pages;
        if (currentPage != null) {
            this.currentPage = currentPage;
        }
        pagesCount = pages.size();
        downloadedImages = new Object[pagesCount + 1];
    }

    public boolean isDir() {
        return getLink().startsWith("/") && new File(getLink()).isDirectory();
    }

    public void clearImages() {
        downloadedImages = new Object[pagesCount > 0 ? pagesCount + 1 : 1];
    }

    @SuppressWarnings("unchecked")
    public <T> T[] getDownloadedImages() {
        return (T[]) downloadedImages;
    }
}
