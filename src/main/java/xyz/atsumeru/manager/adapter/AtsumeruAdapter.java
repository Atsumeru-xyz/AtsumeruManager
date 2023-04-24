package xyz.atsumeru.manager.adapter;

import com.atsumeru.api.model.Readable;
import com.atsumeru.api.model.Serie;
import com.atsumeru.api.model.server.Server;
import com.atsumeru.api.utils.AtsumeruApiConstants;
import com.atsumeru.api.utils.LibraryPresentation;
import xyz.atsumeru.manager.enums.*;
import xyz.atsumeru.manager.helpers.LocaleManager;
import xyz.atsumeru.manager.managers.WorkspaceManager;
import xyz.atsumeru.manager.models.content.Content;
import xyz.atsumeru.manager.models.content.ContentFull;
import xyz.atsumeru.manager.models.content.Images;
import xyz.atsumeru.manager.models.content.Info;
import xyz.atsumeru.manager.models.manga.Chapter;
import xyz.atsumeru.manager.utils.globalutils.*;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("ConstantConditions")
public class AtsumeruAdapter {
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private final LibraryPresentation libraryPresentation;

    public AtsumeruAdapter(LibraryPresentation libraryPresentation) {
        this.libraryPresentation = libraryPresentation;
    }

    public ContentFull toContent(Serie serie, Server server, Long id) {
        Content content = new Content();
        Info info = content.createInfo();

        content.setId(id);
        content.setContentId(serie.getId());
        content.setContentLink(serie.getId());
        content.setParserId((long) server.getId());

        content.setTitle(serie.getTitle());
        content.setAltTitle(serie.getAltTitle());
        content.setJapTitle(serie.getJapTitle());
        content.setKoreanTitle(serie.getKorTitle());

        content.setFirstInfoField(String.format("%s %s", LocaleManager.getString("details.manga.volumes"), serie.getVolumesCount()));

        content.setDescription(serie.getDescription());
        content.setRelated(serie.getRelated());

        content.setFolder(getContentDirectory(serie.getId(), GUFile.createValidFileName(content.getTitle(), true)));

        info.setImages(new Images(getCoverLink(serie.getCover(), true), getCoverLink(serie.getCover(), false)));

        info.setMature(serie.isMature());
        info.setAdult(serie.isAdult());

        info.getGenres().addAll(toLocalizedGenre(serie.getGenres()));
        info.getTags().addAll(serie.getTags());

        info.setPublisher(serie.getPublisher());
        info.setAuthors(serie.getAuthors());
        info.setTranslators(serie.getTranslators());
        info.setYear(serie.getYear());
        info.setCountry(serie.getCountry());
        info.setLanguage(GUString.join(", ", serie.getLanguages()));
        info.setContentType(GUEnum.valueOf(ContentType.class, serie.getContentType()));

        info.setVolumesCount(String.valueOf(serie.getVolumesCount()));
        info.setChaptersCount((int) serie.getChaptersCount());
        info.setStatus(libraryPresentation != LibraryPresentation.SINGLES && libraryPresentation != LibraryPresentation.ARCHIVES
                ? GUEnum.valueOf(Status.class, serie.getStatus())
                : Status.SINGLE);
        info.setMangaTranslationStatus(GUEnum.valueOf(TranslationStatus.class, serie.getTranslationStatus()));
        info.setPlotType(GUEnum.valueOf(PlotType.class, serie.getPlotType()));
        info.setCensorship(GUEnum.valueOf(Censorship.class, serie.getCensorship()));

        info.setArtists(serie.getArtists());
        info.setEvent(serie.getEvent());
        info.setMagazines(serie.getMagazines());
        info.setCircles(serie.getCircles());
        info.setSeries(serie.getSeries());
        info.setParodies(serie.getParodies());
        info.setCharacters(serie.getCharacters());
        info.setColor(GUEnum.valueOf(Color.class, serie.getColor()));
        info.setScore(serie.getScore());

        return new ContentFull("success", content);
    }

    public List<List<Chapter>> toContentChapters(List<? extends Readable> readables, Content content, Server server) {
        return readables.stream()
                .map(readable -> mapToChapter(readable, content, server))
                .collect(Collectors.toList());
    }

    public static List<Chapter> mapToChapter(Readable readable, Content content, Server server) {
        Chapter chapter = new Chapter();
        chapter.setServiceId(server.getId());
        chapter.setDate(DATE_FORMAT.format(new Date(readable.getCreatedAt())));
        chapter.setTimestamp(System.currentTimeMillis());

        chapter.setTitle(readable.getTitle());
        chapter.setLink(readable.getId());
        chapter.setContentId(content.getContentId());
        chapter.setMangaLink(content.getContentLink());
        chapter.setCover(getCoverLink(readable.getId(), true));
        chapter.setFolder(content.getCanonicalFolder() + GUFile.createValidFileName(chapter.getTitle(), true));
        chapter.setCHash(readable.getId());

        chapter.setCurrentPage(Math.max(readable.getHistory().getCurrentPage(), 0));
        chapter.setPagesCount(readable.getPagesCount());
        chapter.setReaded(chapter.getPagesCount() > 0 && chapter.getCurrentPage() == chapter.getPagesCount());

        return Collections.singletonList(chapter);
    }

    public static List<String> toLocalizedGenre(Genre... genres) {
        return Arrays.stream(genres)
                .map(Enum::toString)
                .collect(Collectors.toList());
    }

    public static List<String> toLocalizedGenre(List<String> genres) {
        return genres.stream()
                .map(genreStr -> Optional.ofNullable(getMangaGenreFromString(genreStr))
                        .map(Enum::toString)
                        .orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public static Genre getMangaGenreFromString(String genreStr) {
        genreStr = genreStr.toLowerCase().trim();
        Genre result = null;

        int n = GUType.getIntDef(genreStr, -1);
        if (n >= 0 && n < Genre.values().length) {
            result = Genre.values()[n];
        }

        return result;
    }

    public static String getCoverHash(String imageHash) {
        return imageHash.replaceAll("\\?t=\\d+", "");
    }

    public static String getCoverLink(String imageHash) {
        return getCoverLink(getCoverHash(imageHash), true);
    }

    public static String getCoverLink(String coverHash, boolean isThumbnail) {
        return GULinks.addGetQueryParam(
                new StringBuilder(AtsumeruApiConstants.getCoversUrl().replace("{hash}", coverHash).replace("{is_convert}", "false")),
                "type",
                isThumbnail ? "thumbnail" : "original"
        );
    }

    private static String getContentDirectory(String uniq, String title) {
        String dir;
        if (GUString.isEmpty(uniq)) {
            uniq = GUFile.createValidFileName(title, false);
        }
        dir = GULinks.addPathSlashToEnd(GULinks.addPathSlashToEnd(WorkspaceManager.DOWNLOADS_DIR + File.separator + "atsumeru") + uniq);
        return dir;
    }
}
