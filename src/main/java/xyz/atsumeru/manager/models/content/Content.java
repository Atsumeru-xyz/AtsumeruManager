package xyz.atsumeru.manager.models.content;

import lombok.Data;
import xyz.atsumeru.manager.models.manga.Chapter;
import xyz.atsumeru.manager.utils.globalutils.GUString;
import xyz.atsumeru.manager.utils.globalutils.GUType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
public class Content implements Serializable {
    public static final Pattern DIGITS_PATTERN = Pattern.compile("(\\d+)");
    private static final String SCORE_STAR = "★";

    private Long id;
    private Long parserId;
    private String contentId;
    private String contentLink;
    private String folder;
    private String title;
    private String altTitle;
    private String japTitle;
    private String koreanTitle;
    private String description;
    @Deprecated
    private String related;

    private Info info;

    // Additional info
    private String firstInfoField;

    private String score;
    private String rating;

    private int epOrChReleased;
    private int epOrChTotal;

    private List<Chapter> chapters;

    public Content() {
        epOrChReleased = -1;
        epOrChTotal = -1;
    }

    public Info createInfo() {
        return info = new Info();
    }

    public String getCanonicalFolder() {
        return folder;
    }

    public void setId(int id) {
        this.id = (long) id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return GUString.isNotEmpty(title) ? title : "";
    }

    public String getAltTitle() {
        return GUString.isNotEmpty(altTitle) ? altTitle : "";
    }

    // Info fields
    public void setFirstInfoField(String value) {
        setFirstInfoField(value, true);
    }

    public void setFirstInfoField(String value, boolean parseReleasedValuesAndScore) {
        if (!parseReleasedValuesAndScore || !parseEpOrChReleasedAndTotalValues(value) && !parseScore(value)) {
            this.firstInfoField = value;
        }
    }

    private boolean parseScore(String str) {
        if (!hasScore() && GUString.isNotEmpty(str) && GUString.isNotEmpty(str) && str.startsWith(SCORE_STAR)) {
            score = str;
            return true;
        }
        return false;
    }

    private boolean parseEpOrChReleasedAndTotalValues(String str) {
        if (hasReleaseProgressValues()) {
            return false;
        }

        List<Integer> matches = new ArrayList<>();
        Matcher matcher = DIGITS_PATTERN.matcher(str.replace(".", "").replace(",", ""));
        while (matcher.find()) {
            int match = GUType.getIntDef(matcher.group(), -1);
            if (match >= 0) {
                matches.add(match);
            }
        }

        if (matches.size() >= 2) {
            epOrChReleased = matches.get(matches.size() - 2);
            epOrChTotal = matches.get(matches.size() - 1);
        }

        return epOrChReleased >= 0 && epOrChTotal >= 0;
    }

    public boolean hasScore() {
        return GUString.isNotEmpty(score);
    }

    public boolean hasReleaseProgressValues() {
        return epOrChReleased >= 0 && epOrChTotal >= 0;
    }
}
