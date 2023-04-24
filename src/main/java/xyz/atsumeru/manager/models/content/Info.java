package xyz.atsumeru.manager.models.content;

import lombok.Data;
import xyz.atsumeru.manager.enums.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class Info implements Serializable {
    // Anime & Manga info
    private Images images;
    private String year;
    private String country;
    private String publisher;
    private String language;
    private String event;
    private List<String> authors = new ArrayList<>();
    private List<String> genres = new ArrayList<>();
    private List<String> tags = new ArrayList<>();
    private List<String> translators = new ArrayList<>();
    private List<String> artists = new ArrayList<>();
    private List<String> magazines = new ArrayList<>();
    private List<String> circles = new ArrayList<>();
    private List<String> series = new ArrayList<>();
    private List<String> parodies = new ArrayList<>();
    private List<String> characters = new ArrayList<>();
    private int chaptersCount;

    private boolean mature;
    private boolean adult;

    private ContentType contentType = ContentType.UNKNOWN;
    private Status status = Status.UNKNOWN;
    private Censorship censorship = Censorship.UNKNOWN;
    private TranslationStatus mangaTranslationStatus = TranslationStatus.UNKNOWN;
    private PlotType plotType = PlotType.UNKNOWN;
    private Color color = Color.UNKNOWN;
    private String volumesCount;

    // Scores
    private String score;
}
