package xyz.atsumeru.manager.archive;

import com.atsumeru.api.model.Chapter;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.kursx.parser.fb2.FictionBook;
import com.kursx.parser.fb2.Image;
import kotlin.Pair;
import lombok.Data;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import xyz.atsumeru.manager.archive.helper.ContentDetector;
import xyz.atsumeru.manager.archive.iterator.IArchiveIterator;
import xyz.atsumeru.manager.enums.BookType;
import xyz.atsumeru.manager.enums.ContentType;
import xyz.atsumeru.manager.helpers.ChapterRecognition;
import xyz.atsumeru.manager.helpers.ImageCache;
import xyz.atsumeru.manager.managers.WorkspaceManager;
import xyz.atsumeru.manager.metadata.bookinfo.BookInfo;
import xyz.atsumeru.manager.metadata.comicinfo.ComicInfo;
import xyz.atsumeru.manager.metadata.epub.EpubOPF;
import xyz.atsumeru.manager.metadata.fb2.FictionBookInfo;
import xyz.atsumeru.manager.models.ExtendedSerie;
import xyz.atsumeru.manager.models.content.Content;
import xyz.atsumeru.manager.utils.globalutils.GUArray;
import xyz.atsumeru.manager.utils.globalutils.GUFile;
import xyz.atsumeru.manager.utils.globalutils.GUString;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

@Data
public class ReadableContent implements Closeable {
    public static final String EXTERNAL_INFO_DIRECTORY_NAME = ".atsumeru";
    public static final String XML_INFO_FILENAME = "ComicInfo.xml";
    public static final String OPF_INFO_EXTENSION = ".opf";
    public static final String BOOK_JSON_INFO_FILENAME = "book_info.json";
    public static final String CHAPTER_JSON_INFO_FILENAME = "chapter_info.json";
    public static final String[] SUPPORTED_IMAGE_EXTENSIONS = {".jpg", ".jpeg", ".png", ".bmp", ".webp", ".heic"};
    public static final String ZERO_COVER_FILENAME_START = "00000.";
    public static final String COVER_FILENAME_START = "cover";
    private static final Logger logger = LoggerFactory.getLogger(ReadableContent.class.getSimpleName());
    private static final Gson gson = new Gson()
            .newBuilder()
            .setExclusionStrategies(new ExclusionStrategy() {
                @Override
                public boolean shouldSkipField(FieldAttributes fieldAttributes) {
                    String fieldName = fieldAttributes.getName();
                    return fieldName.equals("folder")
                            || fieldName.equals("pagesCount")
                            || fieldName.equals("createdAt")
                            || fieldName.equals("updatedAt")
                            || fieldName.equals("history");
                }

                @Override
                public boolean shouldSkipClass(Class<?> clazz) {
                    return false;
                }
            })
            .excludeFieldsWithoutExposeAnnotation()
            .setPrettyPrinting()
            .create();
    private IArchiveIterator archiveIterator = null;

    private boolean hasMetadata;
    private InputStream xmlInfoStream; // ComicInfo.xml
    private InputStream opfInfoStream; // .*.opf
    private InputStream bookJsonInfoStream; // book_info.json

    private Pair<String, FictionBook> fictionBookPair; // <Path, FictionBook>

    private Map<String, InputStream> chapterJsonInfoStream = new TreeMap<>(IArchiveIterator.natSortComparator); // map of <chapter folder, chapter_info.json>
    private InputStream coverStream; // first image from archive
    private String coverFilePath;
    private ImageCache.Images images;
    private List<String> pageEntryNames;
    private List<Chapter> chapters = new ArrayList<>();

    private String serieHash;
    private ExtendedSerie serie;
    private Map<String, List<String>> chapterPages;
    private boolean isArchiveFile;
    private boolean asSingle;
    private boolean isBookFile;

    public static boolean saveMetadata(ExtendedSerie serie, boolean insertIntoArchive) {
        boolean isSaved = insertIntoArchive(serie, insertIntoArchive);
        if (isSaved) {
            deleteExternalMetadata(serie);
        }
        if (!isSaved && (!insertIntoArchive || serie.isBook())) {
            isSaved = saveIntoDirectory(serie);
        }
        return isSaved;
    }

    public static boolean saveMetadata(Content item) {
        return insertIntoArchive(item);
    }

    public static ReadableContent create(@Nullable String serieHash, String archivePath, boolean asSingle) throws IOException, ParserConfigurationException, SAXException {
        ExtendedSerie extendedSerie = new ExtendedSerie();
        extendedSerie.setFolder(archivePath);

        // Создание модели ReadableContent для заполнения
        ReadableContent archive = readContent(extendedSerie, archivePath, asSingle);

        logger.info("Reading and filling metadata...");

        // Заполнение модели ExtendedSerie
        archive.fillSerie(extendedSerie, archivePath);

        // Установка хеша архива модели BookArchive, если хеш не был прочитан из метаданных
        if (GUString.isEmpty(extendedSerie.getId())) {
            extendedSerie.setId(GUString.getMHash2(BookInfo.ATSUMERU_ARCHIVE_HASH_TAG, UUID.randomUUID().toString()));
        }

        // Создание/чтение хеша Серии
        archive.setSerieHash(Optional.ofNullable(extendedSerie.getSerieId())
                .orElse(GUString.isNotEmpty(serieHash) ? serieHash : createSerieHash()));
        extendedSerie.setSerieId(archive.getSerieHash());

        // Установка ссылки на локальную обложку
        extendedSerie.setCover(extendedSerie.getId());

        // Парсинг номеров томов
        if (GUString.isNotEmpty(extendedSerie.getFolder())) {
            xyz.atsumeru.manager.models.manga.Chapter chapter = new xyz.atsumeru.manager.models.manga.Chapter();
            chapter.setTitle(extendedSerie.getTitle());
            chapter.setVolumeNumber(extendedSerie.getVolume());

            ChapterRecognition.parseVolumeNumber(chapter, true, true);

            extendedSerie.setVolume(Math.max(extendedSerie.getVolume(), chapter.getVolumeNumber()));
        }

        if (archive.isArchiveFile() && GUArray.isNotEmpty(archive.getChapterPages())) {
            // Импорт глав из архива. На основе директорий или из файлов chapter_info.json
            importChapters(archive.getChapterPages(), archive.getArchiveIterator(), archive, extendedSerie.getId());
            extendedSerie.getChapters().addAll(archive.getChapters());
        }

        GUFile.closeQuietly(archive);
        return archive;
    }

    private static ReadableContent readContent(@Nullable ExtendedSerie extendedSerie, String archivePath, boolean asSingle) throws IOException, ParserConfigurationException, SAXException {
        // Опредение является ли файл книгой (EPUB, FB2, PDF)
        BookType bookType = ContentDetector.detectBookType(Paths.get(archivePath));
        boolean isArchiveFile = bookType == BookType.ARCHIVE || bookType == BookType.EPUB;

        ReadableContent archive = new ReadableContent();
        archive.setArchiveFile(isArchiveFile);
        archive.setAsSingle(asSingle);
        archive.setSerie(extendedSerie);
        archive.findExternalBookInfo(archivePath);

        if (isArchiveFile) {
            // Открытие архива для чтения
            archive.setArchiveIterator(ArchiveReader.getArchiveIterator(archivePath));
            create(archive, bookType != BookType.ARCHIVE, archive.getBookJsonInfoStream() != null);
        } else if (bookType == BookType.FB2) {
            createForFictionBook(archive, archivePath);
        }

        return archive;
    }

    private static void create(ReadableContent archive, boolean isBookFile, boolean hasExternalMetadata) throws IOException {
        IArchiveIterator archiveIterator = archive.getArchiveIterator();

        // <Chapter name, List<File path>>
        Map<String, List<String>> chapterPages = new TreeMap<>(IArchiveIterator.natSortComparator);

        boolean zeroCoverFound = false;
        List<String> pageEntryNames = new ArrayList<>();
        while (archiveIterator.next()) {
            String entryName = archiveIterator.getEntryName();
            String fileName = entryName.toLowerCase();

            if (!hasExternalMetadata) {
                // Поиск XML файла с метаданными и получение InputStream, если файл найден
                if (GUString.equalsIgnoreCase(fileName, ReadableContent.XML_INFO_FILENAME)) {
                    archive.setXmlInfoStream(archiveIterator.getEntryInputStream());
                    log("Found " + ReadableContent.XML_INFO_FILENAME + " file");
                    continue;
                }

                // Поиск EPUB OPF файла с метаданными книги и получение InputStream, если файл найден
                if (GUString.endsWithIgnoreCase(fileName, ReadableContent.OPF_INFO_EXTENSION)) {
                    archive.setOpfInfoStream(archiveIterator.getEntryInputStream());
                    log("Found EPUB .*" + ReadableContent.OPF_INFO_EXTENSION + " file");
                    continue;
                }

                // Поиск JSON файла с метаданными книги и получение InputStream, если файл найден
                if (GUString.equalsIgnoreCase(fileName, ReadableContent.BOOK_JSON_INFO_FILENAME)) {
                    archive.setBookJsonInfoStream(archiveIterator.getEntryInputStream());
                    log("Found " + ReadableContent.BOOK_JSON_INFO_FILENAME + " file");
                    continue;
                }

                // Поиск JSON файла с метаданными главы и получение InputStream, если файл найден
                if (GUString.endsWithIgnoreCase(fileName, ReadableContent.CHAPTER_JSON_INFO_FILENAME)) {
                    archive.putChapterJsonInfoStream(entryName, archiveIterator.getEntryInputStream());
                    log("Found " + ReadableContent.CHAPTER_JSON_INFO_FILENAME + " file in path " + entryName);
                    continue;
                }
            }

            for (String extension : ReadableContent.SUPPORTED_IMAGE_EXTENSIONS) {
                if (fileName.endsWith(extension)) {
                    // Поиск первого изображения и получение InputStream оного. В последствии данное изображение будет
                    // использовано как превью и полноформатная обложка
                    if (!zeroCoverFound && (archive.getCoverStream() == null || isCoverFile(fileName))) {
                        if (archive.getCoverStream() == null || !isCoverFile(archive.getCoverFilePath())) {
                            archive.setCoverStream(archiveIterator.getEntryInputStream());
                            archive.setCoverFilePath(fileName);
                            log("Found cover image in path: " + fileName);
                            zeroCoverFound = GUFile.getFileNameWithExt(fileName).toLowerCase().startsWith(ZERO_COVER_FILENAME_START);
                        }
                    }

                    if (!isBookFile) {
                        // Сохранение названия Entry в список для подальшего использования
                        pageEntryNames.add(archiveIterator.getEntryName());

                        // Добавление пути к файлу к "главам"
                        separateIntoChapters(chapterPages, entryName);
                    }
                    break;
                }
            }
        }

        if (!isBookFile) {
            // Установка путей к страницам в архиве
            archive.setPageEntryNames(pageEntryNames);
        }

        archive.setChapterPages(chapterPages);
        archive.setBookFile(isBookFile);
    }

    private static void createForFictionBook(ReadableContent archive, String filePath) throws IOException, ParserConfigurationException, SAXException {
        // Чтение FictionBook
        FictionBook fictionBook = new FictionBook(new File(filePath));

        // Поиск и чтение обложки
        InputStream stream = fictionBook.getDescription().getTitleInfo().getCoverPage()
                .stream()
                .filter(image -> GUString.isNotEmpty(image.getValue()))
                .limit(1)
                .map(Image::getValue)
                .map(imageId -> imageId.replace("#", ""))
                .map(imageId -> fictionBook.getBinaries().get(imageId))
                .filter(Objects::nonNull)
                .map(binary -> Base64.getDecoder().decode(binary.getBinary().replace("\n", "").getBytes(StandardCharsets.UTF_8)))
                .map(ByteArrayInputStream::new)
                .findAny()
                .orElse(null);

        archive.setCoverStream(stream);

        archive.setFictionBookPair(new Pair<>(filePath, fictionBook));
        archive.setBookFile(true);
    }

    private static void importChapters(Map<String, List<String>> chapterPages, IArchiveIterator archiveIterator, ReadableContent archive, String archiveHash) {
        chapterPages.forEach((chapterPath, pagePaths) -> {
            pagePaths.sort((path1, path2) -> IArchiveIterator.natSortComparator.compare(path1.toLowerCase(), path2.toLowerCase()));

            Chapter chapter = new Chapter();
            String archivePath = archiveIterator.getArchivePath();
            InputStream stream = archive.getChapterJsonInfoStream().get(chapterPath.replace("/", "|").replace("\\", "|"));
            if (stream != null) {
                try {
                    BookInfo.fromJSON(chapter, IOUtils.toString(stream, StandardCharsets.UTF_8), chapterPath, archivePath, archiveHash);
                    logger.info("Found chapter in archive metadata with title = '" + chapter.getTitle() + "' and id = " + chapter.getId());
                } catch (IOException ex) {
                    logger.error("Unable to read chapter_info.json stream...", ex);
                    return;
                } finally {
                    GUFile.closeQuietly(stream);
                }
            } else {
                chapter = new Chapter();
                chapter.setId(archiveHash);
                chapter.setTitle(getChapterTitle(chapterPath, archivePath));
                chapter.setFolder(chapterPath);
                logger.info("Found chapter in archive with title = '" + chapter.getTitle() + "' and id = " + chapter.getId());
            }

            archive.getChapters().add(chapter);
        });
    }

    private static String getChapterTitle(String chapterPath, String archivePath) {
        if (GUString.isNotEmpty(chapterPath)) {
            String chapterTitle = GUFile.getDirName(chapterPath);
            if (GUString.isEmpty(chapterTitle)) {
                return chapterPath;
            }
            return chapterTitle;
        } else {
            return GUFile.getFileName(archivePath);
        }
    }

    private static boolean isCoverFile(String fileName) {
        fileName = GUFile.getFileNameWithExt(fileName);
        return fileName.startsWith(ZERO_COVER_FILENAME_START) || fileName.contains(COVER_FILENAME_START);
    }

    private static void separateIntoChapters(Map<String, List<String>> chapters, String entryName) {
        String chapterPath = GUFile.getPath(entryName);
        List<String> filePaths = chapters.get(chapterPath);
        if (GUArray.isEmpty(filePaths)) {
            filePaths = new ArrayList<>();
            chapters.put(chapterPath, filePaths);
        }

        filePaths.add(entryName);
    }

    private static String createSerieHash() {
        return GUString.getMHash2(BookInfo.ATSUMERU_SERIE_HASH_TAG, UUID.randomUUID().toString());
    }

    private static void changeCover(String imagePath, String inputHash, String destHash, boolean forceChange) {
        File destFile = getSerieCoverFile(imagePath, inputHash, destHash);
        if (!destFile.exists() || forceChange) {
            logger.info("Changing cover image in path " + WorkspaceManager.CACHE_DIR + "original|thumbnail" + File.separator + destHash + ".png file");
            GUFile.copyFile(new File(imagePath), destFile);
        }
    }

    private static File getSerieCoverFile(String imagePath, String inputHash, String destHash) {
        return new File(imagePath.replace(inputHash, destHash));
    }

    private static boolean insertIntoArchive(ExtendedSerie serie, boolean insertIntoArchive) {
        BookType bookType = ContentDetector.detectBookType(Paths.get(serie.getFolder()));
        boolean isArchiveFile = bookType == BookType.ARCHIVE || bookType == BookType.EPUB;

        if (isArchiveFile && insertIntoArchive) {
            IArchiveIterator archiveIterator = createArchiveIterator(serie.getFolder());
            if (archiveIterator != null) {
                Map<String, String> contentToSave = new HashMap<>();
                contentToSave.put(ReadableContent.BOOK_JSON_INFO_FILENAME, BookInfo.toJSON(serie).toString(4));
                serie.getChapters().forEach(chapter -> contentToSave.put(GUFile.addPathSlash(chapter.getFolder()) + ReadableContent.CHAPTER_JSON_INFO_FILENAME, gson.toJson(chapter)));
                return archiveIterator.saveIntoArchive(serie.getFolder(), contentToSave);
            }
        }
        return false;
    }

    private static boolean insertIntoArchive(Content item) {
        BookType bookType = ContentDetector.detectBookType(Paths.get(item.getFolder()));
        boolean isArchiveFile = bookType == BookType.ARCHIVE || bookType == BookType.EPUB;

        if (isArchiveFile) {
            IArchiveIterator archiveIterator = createArchiveIterator(item.getFolder());
            if (archiveIterator != null) {
                Map<String, String> contentToSave = new HashMap<>();
                contentToSave.put(
                        ReadableContent.BOOK_JSON_INFO_FILENAME,
                        BookInfo.toJSON(
                                item,
                                BookInfo.generateSerieHash(),
                                BookInfo.generateArchiveHash()
                        ).toString(4)
                );
                item.getChapters().forEach(chapter -> contentToSave.put(GUFile.addPathSlash(chapter.getFolder()) + ReadableContent.CHAPTER_JSON_INFO_FILENAME, gson.toJson(chapter)));
                return archiveIterator.saveIntoArchive(item.getFolder(), contentToSave);
            }
        }
        return false;
    }

    private static boolean saveIntoDirectory(ExtendedSerie serie) {
        File contentFolder = getContentMetadataFolder(serie);
        contentFolder.mkdirs();

        boolean isSaved = GUFile.writeStringToFile(
                new File(contentFolder, ReadableContent.BOOK_JSON_INFO_FILENAME),
                BookInfo.toJSON(serie).toString(4)
        );

        for (Chapter chapter : serie.getChapters()) {
            File chapterFolder = new File(contentFolder, chapter.getFolder());
            isSaved = isSaved && GUFile.writeStringToFile(
                    new File(chapterFolder, ReadableContent.CHAPTER_JSON_INFO_FILENAME),
                    gson.toJson(chapter)
            );
        }

        return isSaved;
    }

    private static void deleteExternalMetadata(ExtendedSerie serie) {
        File contentFolder = getContentMetadataFolder(serie);
        new File(contentFolder, ReadableContent.BOOK_JSON_INFO_FILENAME).delete();
        for (Chapter chapter : serie.getChapters()) {
            File chapterFolder = new File(contentFolder, chapter.getFolder());
            new File(chapterFolder, ReadableContent.CHAPTER_JSON_INFO_FILENAME).delete();
        }
    }

    private static File getContentMetadataFolder(ExtendedSerie serie) {
        File atsumeruFolder = new File(new File(serie.getFolder()).getParent(), ReadableContent.EXTERNAL_INFO_DIRECTORY_NAME);
        return new File(atsumeruFolder, GUFile.getFileName(serie.getFolder(), true));
    }

    private static IArchiveIterator createArchiveIterator(String folder) {
        try {
            return ArchiveReader.getArchiveIterator(folder);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void log(String message) {
        if (logger != null) {
            logger.info(message);
        }
    }

    private void setAttributeHidden(File file) {
        try {
            Files.setAttribute(file.toPath(), "dos:hidden", Boolean.TRUE, LinkOption.NOFOLLOW_LINKS);
        } catch (IOException ignored) {
        }
    }

    private void findExternalBookInfo(String filePath) {
        File bookInfo = Stream.of(new File(new File(filePath).getParentFile(), EXTERNAL_INFO_DIRECTORY_NAME))
                .filter(GUFile::isDirectory)
                .peek(this::setAttributeHidden)
                .map(atsumeruFolder -> new File(atsumeruFolder, GUFile.getFileName(filePath, true)))
                .filter(GUFile::isDirectory)
                .peek(contentFolder -> GUFile.getAllFilesFromDirectory(contentFolder.toString(), new String[]{"json"}, true)
                        .stream()
                        .filter(file -> !file.toString().contains(BOOK_JSON_INFO_FILENAME))
                        .forEach(file -> {
                            try {
                                String chapterPath = file.toString()
                                        .replace(contentFolder + File.separator, "")
                                        .replace(CHAPTER_JSON_INFO_FILENAME, "");
                                putChapterJsonInfoStream(chapterPath, new FileInputStream(file));
                            } catch (IOException ignored) {
                            }
                        }))
                .map(contentFolder -> new File(contentFolder, BOOK_JSON_INFO_FILENAME))
                .filter(GUFile::isFile)
                .findAny()
                .orElse(null);

        if (bookInfo != null) {
            try {
                setBookJsonInfoStream(new FileInputStream(bookInfo));
            } catch (FileNotFoundException ignored) {
            }
        }
    }

    private void fillSerie(ExtendedSerie extendedSerie, String archivePath) throws IOException {
        if (bookJsonInfoStream != null) {
            log("Reading info from " + BOOK_JSON_INFO_FILENAME);
            hasMetadata = BookInfo.fromJSON(extendedSerie, IOUtils.toString(bookJsonInfoStream, StandardCharsets.UTF_8));
        } else if (xmlInfoStream != null) {
            log("Reading info from " + XML_INFO_FILENAME);
            hasMetadata = ComicInfo.readComicInfo(extendedSerie, xmlInfoStream);
        } else if (opfInfoStream != null) {
            log("Reading info from .*" + OPF_INFO_EXTENSION);
            hasMetadata = EpubOPF.readInfo(extendedSerie, opfInfoStream);
        } else if (fictionBookPair != null) {
            log("Reading info from FictionBook");
            hasMetadata = FictionBookInfo.readInfo(extendedSerie, fictionBookPair.getFirst(), fictionBookPair.getSecond());
        }

        if (!hasMetadata) {
            logger.warn("No supported info file found! Filling base info");
        }

        if (GUString.isEmpty(extendedSerie.getTitle())) {
            extendedSerie.setTitle(GUFile.getFileName(archivePath, true));
        }

        if (isBookFile) {
            extendedSerie.setBook(true);
            if (GUString.equalsIgnoreCase(extendedSerie.getContentType(), ContentType.UNKNOWN.toString())) {
                extendedSerie.setContentType(ContentType.LIGHT_NOVEL.toString());
            }
        }
    }

    private void putChapterJsonInfoStream(String entryName, InputStream stream) {
        chapterJsonInfoStream.put(GUFile.getPath(entryName).replace("/", "|").replace("\\", "|"), stream);
    }

    @Override
    public void close() {
        GUFile.closeQuietly(archiveIterator);
        GUFile.closeQuietly(xmlInfoStream);
        GUFile.closeQuietly(opfInfoStream);
        GUFile.closeQuietly(bookJsonInfoStream);
        GUFile.closeQuietly(coverStream);
        chapterJsonInfoStream.values().forEach(GUFile::closeQuietly);
    }
}

