package xyz.atsumeru.manager.utils.runnable;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.Nullable;
import xyz.atsumeru.manager.FXApplication;
import xyz.atsumeru.manager.controller.stages.StageReaderController;
import xyz.atsumeru.manager.exceptions.ContentUnavailableException;
import xyz.atsumeru.manager.helpers.LocaleManager;
import xyz.atsumeru.manager.models.manga.Chapter;
import xyz.atsumeru.manager.source.AtsumeruSource;
import xyz.atsumeru.manager.utils.globalutils.GUArray;
import xyz.atsumeru.manager.utils.globalutils.GUFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class PageLoader extends BaseLoader {
    private final Chapter chapter;
    private final int page;
    private final StageReaderController readerController;
    private final boolean preloading;
    private final int preloadingAmount;
    private final OkHttpClient okhttp;

    public PageLoader(String name, ContentLoader contentLoader, Chapter chapter, int page, StageReaderController readerController,
                      boolean preloading, int preloadingAmount) {
        super(name, contentLoader);
        this.chapter = chapter;
        this.page = page;
        this.readerController = readerController;
        this.preloading = preloading;
        this.preloadingAmount = preloadingAmount;
        this.okhttp = FXApplication.getOkHttpClient();
    }

    private Image imageFromURL(String url, Map<String, String> headers) throws Exception {
        Response response = GET(okhttp, url, headers);
        Image image = null;
        if (response.isSuccessful() && response.body() != null) {
            image = new Image(response.peekBody(Long.MAX_VALUE).byteStream());
            if (image.isError()) {
                BufferedImage bufferedImage = ImageIO.read(response.peekBody(Long.MAX_VALUE).byteStream());
                image = SwingFXUtils.toFXImage(bufferedImage, null);
                System.out.println();
            }
            GUFile.closeQuietly(response.body());
        }
        GUFile.closeQuietly(response);
        return image;
    }

    @Override
    public void run() {
        if (page <= chapter.getPagesCount()) {
            int currentPage = Math.max(chapter.getCurrentPage(), 1);
            if (chapter.getDownloadedImages()[page] == null) {
                AtomicReference<Image> image = new AtomicReference<>(null);
                try {
                    image.set(getImage(chapter, page - 1));
                } catch (ContentUnavailableException e) {
                    Platform.runLater(() -> readerController.showError(e.getMessage() + "\n\n(" + e.getClass().getSimpleName() + ")"));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // ensure that our chapter is still the active one in the reader
                if (chapter == readerController.getChapter() && running) {
                    Platform.runLater(() -> {
                        chapter.getDownloadedImages()[page] = image.get();
                        if (image.get() != null && currentPage == page) {
                            setImage(image.get());
                        }
                    });
                }
            } else if (currentPage == page) {
                Platform.runLater(() -> setImage((Image) chapter.getDownloadedImages()[page]));
            }

            preloadMore();
        }

        finish();
    }

    private void preloadMore() {
        if (running && preloading && (preloadingAmount > 0 || preloadingAmount == -1)) {
            int preloadAmount = preloadingAmount == -1 ? -1 : preloadingAmount - 1;
            contentLoader.loadPage(chapter, page + 1, readerController, true, preloadAmount);
        }
    }

    private void setImage(Image image) {
        readerController.setImage(image);
        readerController.hideLoading();
        readerController.hideError();
        readerController.refreshPage();
    }

    private Image getImage(Chapter chapter, int page) throws ContentUnavailableException {
        try {
            return imageFromURL(chapter.getImages().get(page), AtsumeruSource.createAuthorizationHeaders());
        } catch (Exception ex) {
            throw new ContentUnavailableException(LocaleManager.getString("gui.reader.get_image_link_error"));
        }
    }

    @SuppressWarnings("ConstantConditions")
    private static Response GET(OkHttpClient client, String url, @Nullable Map<String, String> headers) throws IOException {
        Request.Builder builder = new Request.Builder()
                .url(url);

        if (GUArray.isNotEmpty(headers)) {
            headers.forEach(builder::addHeader);
        }

        return client.newCall(builder.build()).execute();
    }
}
