package xyz.atsumeru.manager.metadata.bookinfo;

import com.atsumeru.api.model.BoundService;
import com.atsumeru.api.model.Chapter;
import com.atsumeru.api.model.Link;
import com.atsumeru.api.model.Serie;
import com.atsumeru.api.utils.ServiceType;
import org.jetbrains.annotations.Nullable;
import org.json2.JSONArray;
import org.json2.JSONException;
import org.json2.JSONObject;
import xyz.atsumeru.manager.enums.*;
import xyz.atsumeru.manager.helpers.JSONHelper;
import xyz.atsumeru.manager.models.ExtendedSerie;
import xyz.atsumeru.manager.models.content.Content;
import xyz.atsumeru.manager.utils.globalutils.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public class BookInfo {
    public static final String ATSUMERU_ARCHIVE_HASH_TAG = "atsumeru";
    public static final String ATSUMERU_SERIE_HASH_TAG = "atsumeru-serie";

    private static final String MERGED_BOOK_INFO_FILE_NAME = "merged_book_info.json";

    public static String generateSerieHash() {
        return GUString.getMHash2(ATSUMERU_SERIE_HASH_TAG, UUID.randomUUID().toString());
    }

    public static String generateArchiveHash() {
        return GUString.getMHash2(ATSUMERU_ARCHIVE_HASH_TAG, UUID.randomUUID().toString());
    }

    public static String mergeMetadata(List<File> files) {
        try {
            Serie mainSerie = new Serie();
            BookInfo.fromJSON(mainSerie, GUString.join("\n", Files.readAllLines(files.get(0).toPath(), StandardCharsets.UTF_8)));

            for (int i = 1; i < files.size(); i++) {
                Serie tempSerie = new Serie();
                BookInfo.fromJSON(tempSerie, GUString.join("\n", Files.readAllLines(files.get(i).toPath(), StandardCharsets.UTF_8)));

                GUArray.mergeArraysWithoutDuplicates(mainSerie.getAuthors(), tempSerie.getAuthors());
                GUArray.mergeArraysWithoutDuplicates(mainSerie.getArtists(), tempSerie.getArtists());
                GUArray.mergeArraysWithoutDuplicates(mainSerie.getTranslators(), tempSerie.getTranslators());
                GUArray.mergeArraysWithoutDuplicates(mainSerie.getGenres(), tempSerie.getGenres());
                GUArray.mergeArraysWithoutDuplicates(mainSerie.getTags(), tempSerie.getTags());
                GUArray.mergeArraysWithoutDuplicates(mainSerie.getLanguages(), tempSerie.getLanguages());
                GUArray.mergeArraysWithoutDuplicates(mainSerie.getSeries(), tempSerie.getSeries());
                GUArray.mergeArraysWithoutDuplicates(mainSerie.getParodies(), tempSerie.getParodies());
                GUArray.mergeArraysWithoutDuplicates(mainSerie.getCircles(), tempSerie.getCircles());
                GUArray.mergeArraysWithoutDuplicates(mainSerie.getMagazines(), tempSerie.getMagazines());
                GUArray.mergeArraysWithoutDuplicates(mainSerie.getCharacters(), tempSerie.getCharacters());
            }

            String mergedFilePath = new File(files.get(0).getParentFile(), MERGED_BOOK_INFO_FILE_NAME).toString();
            saveToFile(mergedFilePath, BookInfo.toJSON(mainSerie));
            return mergedFilePath;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private static void saveToFile(String path, JSONObject obj) {
        try {
            File infoFile = new File(path);
            if (!GUFile.isFileExist(infoFile)) {
                infoFile.getParentFile().mkdirs();
                Writer writerBookInfo = new OutputStreamWriter(new FileOutputStream(infoFile), StandardCharsets.UTF_8);
                writerBookInfo.write(obj.toString(4));
                writerBookInfo.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static JSONObject toJSON(Serie serie) throws JSONException {
        JSONObject obj = new JSONObject();

        // Basic Metadata
        if (GUString.isNotEmpty(serie.getId())) {
            String serieHash = Optional.of(serie)
                    .filter(ExtendedSerie.class::isInstance)
                    .map(ExtendedSerie.class::cast)
                    .map(ExtendedSerie::getSerieId)
                    .orElse(null);

            putHashes(obj, serieHash, serie.getId());
        }
        putJSON(obj, "link", serie.getLink());
        putLinks(obj, serie.getLinks());
        putJSON(obj, "cover", serie.getCover());  // TODO: 18.12.2021 указывать путь к обложке внутри архива

        // Titles
        putJSON(obj, "title", serie.getTitle());
        putJSON(obj, "alt_title", serie.getAltTitle());
        putJSON(obj, "jap_title", serie.getJapTitle());
        putJSON(obj, "kor_title", serie.getKorTitle());

        // Main info
        putJSON(obj, "country", serie.getCountry());
        putJSON(obj, "publisher", serie.getPublisher());
        putJSON(obj, "published", serie.getYear());
        putJSON(obj, "event", serie.getEvent());
        putJSON(obj, "description", serie.getDescription());

        // Info lists
        putJSON(obj, "authors", serie.getAuthors());
        putJSON(obj, "artists", serie.getArtists());
        putJSON(obj, "languages", serie.getLanguages());
        putJSON(obj, "translators", serie.getTranslators());
        putJSON(obj, "series", serie.getSeries());
        putJSON(obj, "parodies", serie.getParodies());
        putJSON(obj, "circles", serie.getCircles());
        putJSON(obj, "magazines", serie.getMagazines());
        putJSON(obj, "characters", serie.getCharacters());

        // Volumes/Chapters
        putJSON(obj, "volume", serie.getVolume());
        putJSON(obj, "volumes", serie.getVolumesCount());
        putJSON(obj, "chapters", serie.getChaptersCount());

        // Genres/Tags
        putJSON(obj, "genres", serie.getGenres());
        putJSON(obj, "tags", serie.getTags());

        // Age Rating
        putJSON(obj, "age_rating", serie.isAdult()
                ? AgeRating.ADULTS_ONLY.toString()
                : serie.isMature()
                ? AgeRating.MATURE.toString()
                : AgeRating.EVERYONE.toString());

        // Statuses
        putJSON(obj, "status", serie.getStatus());
        putJSON(obj, "translation_status", serie.getTranslationStatus());
        putJSON(obj, "plot_type", serie.getPlotType());
        putJSON(obj, "censorship", serie.getCensorship());
        putJSON(obj, "content_type", serie.getContentType());
        putJSON(obj, "color", serie.getColor());
//        putJSON(obj, "read_dir", serie.getReadingDirection()); // TODO: 18.12.2021

        // Score
        putJSON(obj, "score", serie.getScore());
        putJSON(obj, "rating", serie.getRating());

        putBoundServices(obj, serie.getBoundServices());

        return obj;
    }

    public static boolean fromJSON(Serie serie, String jsonStr) {
        JSONObject obj = JSONHelper.fromString(jsonStr);

        // Hashes
        JSONObject atsumeru = JSONHelper.getObjectSafe(obj, "atsumeru");
        if (atsumeru != null) {
            serie.setId(JSONHelper.getStringSafe(atsumeru, "hash", serie.getId()));
            if (serie instanceof ExtendedSerie) {
                ExtendedSerie extendedSerie = (ExtendedSerie) serie;
                ((ExtendedSerie) serie).setSerieId(JSONHelper.getStringSafe(atsumeru, "serie_hash", extendedSerie.getSerieId()));
            }
        }

        // Basic Metadata
        serie.setLink(JSONHelper.getStringSafe(obj, "link"));
        serie.setLinks(getLinks(obj));
        serie.setCover(JSONHelper.getStringSafe(obj, "cover"));

        // Titles
        serie.setTitle(JSONHelper.getStringSafe(obj, "title"));
        serie.setAltTitle(JSONHelper.getStringSafe(obj, "alt_title"));
        serie.setJapTitle(JSONHelper.getStringSafe(obj, "jap_title"));
        serie.setKorTitle(JSONHelper.getStringSafe(obj, "kor_title"));
        // TODO: 18.12.2021 add synonyms
//        serie.setSynonyms(JSONHelper.getStringSafe(obj, "synonyms"));

        // Main info
        List<String> authors = JSONHelper.getStringList(obj, "authors");
        String author = JSONHelper.getStringSafe(obj, "author");
        if (GUString.isNotEmpty(author)) {
            authors.add(author);
        }

        serie.setAuthors(authors);
        serie.setCountry(JSONHelper.getStringSafe(obj, "country"));
        serie.setPublisher(JSONHelper.getStringSafe(obj, "publisher"));
        serie.setYear(JSONHelper.getStringSafe(obj, "published"));

        serie.setVolume(JSONHelper.getFloatSafe(obj, "volume", -1));
        serie.setVolumesCount(JSONHelper.getLongSafe(obj, "volumes", -1));
        serie.setChaptersCount(getChaptersCount(obj));

        serie.setGenres(getGenres(obj));
        serie.setTags(JSONHelper.getStringList(obj, "tags"));
        serie.setArtists(JSONHelper.getStringList(obj, "artists"));
        serie.setTranslators(JSONHelper.getStringList(obj, "translators"));
        serie.setLanguages(JSONHelper.getStringList(obj, "languages"));
        serie.setSeries(JSONHelper.getStringList(obj, "series"));
        serie.setParodies(JSONHelper.getStringList(obj, "parodies"));
        serie.setCircles(JSONHelper.getStringList(obj, "circles"));
        serie.setMagazines(JSONHelper.getStringList(obj, "magazines"));
        serie.setCharacters(JSONHelper.getStringList(obj, "characters"));
        serie.setEvent(JSONHelper.getStringSafe(obj, "event"));
        serie.setDescription(JSONHelper.getStringSafe(obj, "description"));

        // Age Rating
        AgeRating ageRating = GUEnum.valueOf(AgeRating.class, JSONHelper.getStringSafe(obj, "age_rating"));
        serie.setMature(ageRating == AgeRating.MATURE);
        serie.setAdult(ageRating == AgeRating.ADULTS_ONLY);

        // Statuses
        serie.setStatus(GUEnum.valueOf(Status.class, JSONHelper.getStringSafe(obj, "status")).toString());
        serie.setTranslationStatus(GUEnum.valueOf(TranslationStatus.class, JSONHelper.getStringSafe(obj, "translation_status")).toString());
        serie.setPlotType(GUEnum.valueOf(PlotType.class, JSONHelper.getStringSafe(obj, "plot_type")).toString());
        serie.setCensorship(GUEnum.valueOf(Censorship.class, JSONHelper.getStringSafe(obj, "censorship")).toString());
        serie.setContentType(GUEnum.valueOf(ContentType.class, JSONHelper.getStringSafe(obj, "content_type")).toString());
        serie.setColor(GUEnum.valueOf(Color.class, JSONHelper.getStringSafe(obj, "color")).toString());

        // Score
        serie.setScore(JSONHelper.getStringSafe(obj, "score"));
        serie.setRating(JSONHelper.getIntSafe(obj, "rating", 0));

        // BoundServices
        serie.setBoundServices(getBoundServices(obj));

        return true;
    }

    public static JSONObject toJSON(Content content, @Nullable String serieHash, @Nullable String volumeHash) throws JSONException {
        JSONObject obj = new JSONObject();

        // Hashes
        putHashes(obj, serieHash, volumeHash);

        // Basic Metadata
        putJSON(obj, "link", content.getContentLink());
        putLinks(obj, Optional.ofNullable(content.getContentLink())
                .filter(GUString::isNotEmpty)
                .map(link -> {
                    Link linkObj = new Link();
                    linkObj.setSource(GULinks.getHostName(link));
                    linkObj.setLink(link);
                    return linkObj;
                })
                .map(Collections::singletonList)
                .orElse(null));

        // Titles
        putJSON(obj, "title", content.getTitle());
        putJSON(obj, "alt_title", content.getAltTitle());
        putJSON(obj, "jap_title", content.getJapTitle());
        putJSON(obj, "kor_title", content.getKoreanTitle());

        // Main info
        putJSON(obj, "country", content.getInfo().getCountry());
        putJSON(obj, "publisher", content.getInfo().getPublisher());
        putJSON(obj, "published", content.getInfo().getYear());
        putJSON(obj, "event", content.getInfo().getEvent());
        putJSON(obj, "description", content.getDescription());

        // Info lists
        putJSON(obj, "authors", content.getInfo().getAuthors());
        putJSON(obj, "artists", content.getInfo().getArtists());
        putJSON(obj, "languages", GUArray.splitString(content.getInfo().getLanguage(), ","));
        putJSON(obj, "translators", content.getInfo().getTranslators());
        putJSON(obj, "series", content.getInfo().getSeries());
        putJSON(obj, "parodies", content.getInfo().getParodies());
        putJSON(obj, "circles", content.getInfo().getCircles());
        putJSON(obj, "magazines", content.getInfo().getMagazines());
        putJSON(obj, "characters", content.getInfo().getCharacters());

        // Chapters
        Optional.of(String.valueOf(content.getChapters().size()))
                .map(count -> GUType.getFloatDef(count, -1))
                .filter(count -> count > 0)
                .ifPresent(count -> putJSON(obj, "chapters", count));

        // Genres/Tags
        putJSON(obj, "genres", content.getInfo().getGenres()
                .stream()
                .map(Genre::getGenreFromString)
                .map(Enum::ordinal)
                .collect(Collectors.toList()));
        putJSON(obj, "tags", content.getInfo().getTags());

        // Age Rating
        putJSON(obj, "age_rating", content.getInfo().isAdult()
                ? AgeRating.ADULTS_ONLY.toString()
                : content.getInfo().isMature()
                ? AgeRating.MATURE.toString()
                : AgeRating.EVERYONE.toString());

        // Statuses
        putJSON(obj, "status", content.getInfo().getStatus().toString());
        putJSON(obj, "translation_status", content.getInfo().getMangaTranslationStatus().toString());
        putJSON(obj, "plot_type", content.getInfo().getPlotType().toString());
        putJSON(obj, "censorship", content.getInfo().getCensorship().toString());
        putJSON(obj, "content_type", content.getInfo().getContentType().toString());
        putJSON(obj, "color", content.getInfo().getColor().toString());

        // Score
        putJSON(obj, "score", content.getScore());
        putJSON(obj, "rating", content.getRating());

        return obj;
    }

    public static void fromJSON(Chapter chapter, String json, String chapterFolder, String archivePath, String archiveHash) {
        JSONObject obj = JSONHelper.fromString(json);

        chapter.setTitle(
                Optional.ofNullable(JSONHelper.getStringSafe(obj, "title"))
                        .filter(GUString::isNotEmpty)
                        .orElseGet(() -> Optional.ofNullable(chapterFolder)
                                .filter(GUString::isNotEmpty)
                                .map(GUFile::getDirName)
                                .orElseGet(() -> GUFile.getFileName(archivePath)))
        );
        chapter.setAltTitle(JSONHelper.getStringSafe(obj, "alt_title"));

        chapter.setFolder(chapterFolder);
        chapter.setId(
                Optional.ofNullable(JSONHelper.getStringSafe(obj, "id"))
                        .filter(GUString::isNotEmpty)
                        .orElseGet(() -> GUString.md5Hex(archiveHash + chapter.getTitle()))
        );

        chapter.setArtists(JSONHelper.getStringList(obj, "artists"));
        chapter.setTranslators(JSONHelper.getStringList(obj, "translators"));
        chapter.setLanguages(JSONHelper.getStringList(obj, "languages"));
        chapter.setParodies(JSONHelper.getStringList(obj, "parodies"));
        chapter.setCharacters(JSONHelper.getStringList(obj, "characters"));

        chapter.setCensorship(GUEnum.valueOf(Censorship.class, JSONHelper.getStringSafe(obj, "censorship")).toString());
        chapter.setColor(GUEnum.valueOf(Color.class, JSONHelper.getStringSafe(obj, "color")).toString());

        chapter.setDescription(JSONHelper.getStringSafe(obj, "description"));

        chapter.setGenres(getGenres(obj));
        chapter.setTags(JSONHelper.getStringList(obj, "tags"));
    }

    private static List<String> getGenres(JSONObject obj) {
        try {
            return JSONHelper.getStringList(obj, "genres");
        } catch (Exception ignored) {
        }
        return null;
    }

    private static List<Link> getLinks(JSONObject obj) {
        List<Link> links = new ArrayList<>();
        JSONArray linksArray = JSONHelper.getArraySafe(obj, "links");
        if (linksArray != null) {
            for (Object object : linksArray) {
                if (object instanceof JSONObject) {
                    JSONObject linksObject = (JSONObject) object;
                    String source = JSONHelper.getStringSafe(linksObject, "source");
                    String link = JSONHelper.getStringSafe(linksObject, "link");
                    if (GUString.isNotEmpty(link)) {
                        Link linkObj = new Link();
                        linkObj.setSource(source);
                        linkObj.setLink(link);
                        links.add(linkObj);
                    }
                }
            }
        }

        return Optional.of(links)
                .filter(GUArray::isNotEmpty)
                .orElse(null);
    }

    private static long getChaptersCount(JSONObject obj) {
        try {
            return JSONHelper.getLongSafe(obj, "chapters", -1);
        } catch (Exception ex) {
            return -1;
        }
    }

    private static List<BoundService> getBoundServices(JSONObject obj) {
        List<BoundService> boundServices = new ArrayList<>();
        JSONArray servicesArray = JSONHelper.getArraySafe(obj, "bound_services");
        if (servicesArray != null) {
            for (Object object : servicesArray) {
                if (object instanceof JSONObject) {
                    JSONObject servicesObject = (JSONObject) object;

                    ServiceType serviceType = GUEnum.valueOfOrNull(ServiceType.class, JSONHelper.getStringSafe(servicesObject, "service_type"));
                    if (serviceType != null) {
                        String id = JSONHelper.getStringSafe(servicesObject, "id");
                        String link = JSONHelper.getStringSafe(servicesObject, "link");

                        if (GUString.isNotEmpty(id)) {
                            boundServices.add(new BoundService(serviceType, id, link));
                        }
                    }
                }
            }
        }

        return boundServices;
    }

    private static void putHashes(JSONObject obj, String serieHash, String archiveHash) {
        JSONObject atsumeru = new JSONObject();
        putJSON(atsumeru, "serie_hash", serieHash);
        putJSON(atsumeru, "hash", archiveHash);

        if (GUString.isNotEmpty(serieHash) || GUString.isNotEmpty(archiveHash)) {
            obj.put("atsumeru", atsumeru);
        }
    }

    private static void putLinks(JSONObject obj, List<Link> links) {
        if (GUArray.isNotEmpty(links)) {
            JSONArray linksArray = new JSONArray();
            links.forEach(link -> {
                JSONObject linksObj = new JSONObject();
                putJSON(linksObj, "source", link.getSource());
                putJSON(linksObj, "link", link.getLink());
                linksArray.put(linksObj);
            });

            obj.put("links", linksArray);
        }
    }

    private static void putBoundServices(JSONObject obj, List<BoundService> boundServices) {
        if (GUArray.isNotEmpty(boundServices)) {
            JSONArray servicesArray = new JSONArray();

            boundServices.forEach(boundService -> {
                JSONObject serviceObj = new JSONObject();
                putJSON(serviceObj, "service_type", boundService.getServiceType().toString());
                putJSON(serviceObj, "id", boundService.getId());
                putJSON(serviceObj, "link", boundService.getLink());
                servicesArray.put(serviceObj);
            });

            obj.put("bound_services", servicesArray);
        }
    }

    public static void putJSON(JSONObject obj, String name, Collection<?> collection) throws JSONException {
        if (GUArray.isNotEmpty(collection)) {
            obj.put(name, collection);
        }
    }

    public static void putJSON(JSONObject obj, String name, String value) throws JSONException {
        if (GUString.isNotEmpty(value)) {
            obj.put(name, value);
        }
    }

    public static void putJSON(JSONObject obj, String name, int value) throws JSONException {
        if (value > 0) {
            obj.put(name, value);
        }
    }

    public static void putJSON(JSONObject obj, String name, float value) throws JSONException {
        if (value > 0) {
            obj.put(name, value);
        }
    }

    public static void putJSON(JSONObject obj, String name, boolean value) throws JSONException {
        obj.put(name, value);
    }
}
