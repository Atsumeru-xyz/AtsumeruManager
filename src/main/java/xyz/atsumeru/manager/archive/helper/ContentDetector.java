package xyz.atsumeru.manager.archive.helper;

import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import xyz.atsumeru.manager.enums.BookType;
import xyz.atsumeru.manager.utils.globalutils.GUFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ContentDetector {
    private static TikaConfig tika;
    private static final List<String> ARCHIVE_MEDIA_TYPES = new ArrayList<>();

    public static String detectMediaType(Path path) {
        Metadata metadata = new Metadata();
        metadata.add(Metadata.RESOURCE_NAME_KEY, path.getFileName().toString());

        TikaInputStream tikaInputStream = null;
        try {
            return tika.getDetector().detect(tikaInputStream = TikaInputStream.get(path), metadata).toString();
        } catch (Exception ex) {
            return "unknown";
        } finally {
            GUFile.closeQuietly(tikaInputStream);
        }
    }

    public static boolean isArchiveFile(File file) {
        String mediaType = detectMediaType(file.toPath());
        return ARCHIVE_MEDIA_TYPES.contains(mediaType);
    }

    public static BookType detectBookType(Path path) {
        String mediaType = detectMediaType(path);
        switch (mediaType) {
            case "application/zip":
            case "application/x-rar-compressed":
            case "application/x-rar-compressed; version=4":
            case "application/x-7z-compressed":
                return BookType.ARCHIVE;
            case "application/epub":
            case "application/epub+zip":
                return BookType.EPUB;
            case "application/x-fictionbook":
            case "application/x-fictionbook+xml":
                return BookType.FB2;
            case "application/pdf":
                return BookType.PDF;
        }
        return null;
    }

    static {
        ARCHIVE_MEDIA_TYPES.add("application/zip");
        ARCHIVE_MEDIA_TYPES.add("application/x-rar-compressed");
        ARCHIVE_MEDIA_TYPES.add("application/x-rar-compressed; version=4");
        ARCHIVE_MEDIA_TYPES.add("application/x-7z-compressed");

        try {
            tika = new TikaConfig();
        } catch (TikaException | IOException e) {
            e.printStackTrace();
        }
    }
}