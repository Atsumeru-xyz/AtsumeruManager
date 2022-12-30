package xyz.atsumeru.manager.helpers;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import lombok.Getter;
import lombok.Setter;
import net.coobird.thumbnailator.Thumbnails;
import okhttp3.*;
import okio.BufferedSink;
import okio.Okio;
import org.checkerframework.checker.nullness.qual.Nullable;
import xyz.atsumeru.manager.FXApplication;
import xyz.atsumeru.manager.managers.WorkspaceManager;
import xyz.atsumeru.manager.utils.globalutils.GUArray;
import xyz.atsumeru.manager.utils.globalutils.GUFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ImageCache {
    private static final String PNG_EXTENSION = "png";
    private static final String PLACEHOLDER_NO_IMAGE_PATH = "/images/no_image.png";
    public static final Image PLACEHOLDER_IMAGE = new Image(ImageCache.PLACEHOLDER_NO_IMAGE_PATH);
    private static final Cache<String, Image> IMAGE_CACHE = Caffeine.newBuilder()
            .maximumSize(300)
            .expireAfterAccess(3, TimeUnit.MINUTES)
            .build();

    private final String imageHash;
    private final String imageUrl;
    private final String extension;
    private String contentId;
    private ImageCacheType imageCacheType;
    private Map<String, String> headers;
    private double requestedWidth, requestedHeight;
    private boolean preserveRatio, smooth;
    private boolean backgroundLoadingFromFS;
    private boolean asLocalFile;
    private boolean cacheAll;
    private ImageLoadCallback callback;

    private static final ExecutorService downloadingExecutorService = Executors.newFixedThreadPool(6);
    private static final ExecutorService readingExecutorService = Executors.newFixedThreadPool(6);

    private ImageCache(String imageHash, String imageUrl) {
        this.imageHash = imageHash;
        this.imageUrl = imageUrl;
        this.imageCacheType = ImageCacheType.THUMBNAIL;

        this.extension = Optional.ofNullable(imageUrl)
                .map(GUFile::getFileExtFromUrl)
                .orElse(PNG_EXTENSION);
    }

    public static boolean isInCache(String imageHash, ImageCacheType cacheType) {
        return checkFile(getFile(imageHash, cacheType));
    }

    public static File getFile(String imageHash, ImageCacheType cacheType) {
        return ImageCache.create(imageHash, null).getImageFile(cacheType);
    }

    public static Image getFromInMemoryCache(String imageHash, ImageCacheType cacheType) {
        return IMAGE_CACHE.get(
                ImageCache.create(imageHash, null).getImageFile(cacheType).getAbsolutePath(),
                _unused -> null
        );
    }

    public static void removeFromCache(String imageHash) {
        IMAGE_CACHE.invalidate(imageHash);
    }

    public static ImageCache create(String imageHash, @Nullable String imageUrl) {
        return new ImageCache(imageHash, imageUrl);
    }

    public static ImageCache create(File file) {
        return new ImageCache(file.toString(), file.toString()).asLocalFile();
    }

    private static boolean checkFile(File file) {
        if (file.length() == 0) {
            file.delete();
        }
        return GUFile.isFile(file);
    }

    private static Image getPlaceholderNoImage(String contentId, ImageLoadCallback callback) {
        if (callback != null) {
            callback.onLoad(PLACEHOLDER_IMAGE, contentId, true, true);
        }
        return PLACEHOLDER_IMAGE;
    }

    public ImageCache asLocalFile() {
        this.asLocalFile = true;
        return this;
    }

    public ImageCache withContentId(String contentId) {
        this.contentId = contentId;
        return this;
    }

    public ImageCache withCacheType(ImageCacheType type) {
        this.imageCacheType = type;
        return this;
    }

    public ImageCache withHeaders(Map<String, String> headers) {
        this.headers = headers;
        return this;
    }

    public ImageCache withRequestedWidth(double requestedWidth) {
        this.requestedWidth = requestedWidth;
        return this;
    }

    public ImageCache withRequestedHeight(double requestedHeight) {
        this.requestedHeight = requestedHeight;
        return this;
    }

    public ImageCache withPreserveRatio() {
        this.preserveRatio = true;
        return this;
    }

    public ImageCache withSmooth() {
        this.smooth = true;
        return this;
    }

    public ImageCache withBackgroundLoadingFromFS() {
        this.backgroundLoadingFromFS = true;
        return this;
    }

    public ImageCache withCacheAll() {
        this.cacheAll = true;
        return this;
    }

    public ImageCache withCallback(ImageLoadCallback callback) {
        this.callback = callback;
        return this;
    }

    public Image get() {
        return Optional.of(asLocalFile ? new File(imageUrl) : getImageFile(imageCacheType))
                .filter(File::exists)
                .map(this::loadImageFromFile)
                .orElseGet(this::loadImageFromNetwork);
    }

    public void getAsync() {
        Optional.of(asLocalFile ? new File(imageUrl) : getImageFile(imageCacheType))
                .filter(File::exists)
                .map(file -> readingExecutorService)
                .orElse(downloadingExecutorService)
                .submit(() -> {
                    Optional.of(asLocalFile ? new File(imageUrl) : getImageFile(imageCacheType))
                            .filter(File::exists)
                            .map(this::loadImageFromFile)
                            .orElseGet(this::loadImageFromNetwork);
                });
    }

    public File getImageFile(ImageCacheType cacheType) {
        return getImageFile(cacheType, cacheType == ImageCacheType.THUMBNAIL ? PNG_EXTENSION : extension);
    }

    public File getImageFile(ImageCacheType cacheType, String extension) {
        File folder = new File(WorkspaceManager.CACHE_DIR, cacheType.getFolder());
        folder.mkdirs();
        return new File(folder, String.format("%s.%s", imageHash, extension));
    }

    private Image loadImageFromFile(File imageFile) {
        Image image = IMAGE_CACHE.get(imageFile.getAbsolutePath(), _unused -> {
            if (imageFile.exists()) {
                Image img = new Image(imageFile.toURI().toString(), backgroundLoadingFromFS);
                try {
                    return img.isError() ? SwingFXUtils.toFXImage(ImageIO.read(imageFile), null) : img;
                } catch (Exception exception) {
                    return img;
                }
            }
            return null;
        });
        if (image == null || image.isError()) {
            IMAGE_CACHE.invalidate(imageFile.getAbsolutePath());
        }
        onLoad(image, true);
        return image;
    }

    private Image loadImageFromNetwork() {
        System.out.println("Loading image from network: " + imageUrl);
        try {
            return Optional.ofNullable(downloadImage(true))
                    .map(source -> {
                        saveIntoCache(source);
                        return source;
                    }).orElseGet(() -> getPlaceholderNoImage(contentId, callback));
        } catch (Exception ex) {
            ex.printStackTrace();
            return getPlaceholderNoImage(contentId, callback);
        }
    }

    private Image downloadImage(boolean firstRequest) {
        OkHttpClient okhttp = FXApplication.getOkHttpClient().newBuilder().build();

        Response response = null;
        ResponseBody responseBody = null;
        try {
            Request.Builder builder = new Request.Builder()
                    .url(new URL(imageUrl));

            if (GUArray.isNotEmpty(headers)) {
                headers.forEach(builder::addHeader);
            }

            Call call = okhttp.newCall(builder.build());
            response = call.execute();
            if (response.isSuccessful()) {
                responseBody = response.body();

                File originalFile = getImageFile(ImageCacheType.ORIGINAL);
                BufferedSink sink = Okio.buffer(Okio.sink(originalFile));
                sink.writeAll(response.body().source());
                sink.close();

                Image image = new Image(originalFile.toURI().toString(), requestedWidth, requestedHeight, preserveRatio, smooth);
                return image.isError() ? SwingFXUtils.toFXImage(ImageIO.read(originalFile), null) : image;
            }
        } catch (Exception ex) {
            if (firstRequest) {
                downloadImage(false);
            } else {
                ex.printStackTrace();
            }
        } finally {
            GUFile.closeQuietly(responseBody);
            GUFile.closeQuietly(response);
        }
        return null;
    }

    private void saveIntoCache(Image image) {
        saveIntoFile(SwingFXUtils.fromFXImage(image, null));
        onLoad(image, false);
    }

    private void saveIntoFile(BufferedImage bufferedImage) {
        try {
            if (imageCacheType == ImageCacheType.ORIGINAL || cacheAll) {
                ImageIO.write(bufferedImage, extension, getImageFile(ImageCacheType.ORIGINAL));
            }
            if (imageCacheType == ImageCacheType.THUMBNAIL || cacheAll) {
                Thumbnails.of(bufferedImage)
                        .size(230, 320)
                        .toFile(getImageFile(ImageCacheType.THUMBNAIL));
            }
        } catch (IOException | IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    private void onLoad(@Nullable Image image, boolean fromCache) {
        if (callback != null && image != null) {
            callback.onLoad(image, contentId, fromCache, false);
        }
    }

    public enum ImageCacheType {
        ORIGINAL("original"),
        THUMBNAIL("thumbnail");

        @Getter
        private final String folder;

        ImageCacheType(String folder) {
            this.folder = folder;
        }
    }

    public interface ImageLoadCallback {
        void onLoad(Image image, String contentId, boolean fromCache, boolean loadNow);
    }

    public static class Images {
        @Getter
        @Setter
        private String original;
        @Getter
        @Setter
        private String thumbnail;

        public Images(String original, String thumbnail) {
            this.original = original;
            this.thumbnail = thumbnail;
        }
    }
}