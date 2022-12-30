package xyz.atsumeru.manager.helpers;

import xyz.atsumeru.manager.models.manga.Chapter;
import xyz.atsumeru.manager.utils.globalutils.GUType;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChapterRecognition {
    public static final Float UNKNOWN = -1f;
    public static final Float SINGLE_OR_EXTRA = -2f;

    private static final Pattern basicVolume = Pattern.compile("(?<![a-z])(v|ver|vol|vo|version|volume|season|s|episode|ep|том|выпуск)(.?|.\\s?)([0-9]+)");
    private static final Pattern dashedVolume = Pattern.compile("(\\d+)( - |-)(\\d+)");
    private static final Pattern shortVolWithNumber = Pattern.compile("v(\\d+)");
    private static final Pattern skipIfSingleOccur = Pattern.compile("(\\bsingle\\b|\\bсингл\\b)");
    private static final Pattern skipIfExtraOrSpecialOccur = Pattern.compile("(\\bextra\\b|\\bspecial\\b|\\bomake\\b|\\bэкстра\\b|\\bекстра\\b|\\bспешл\\b|\\bспэшл\\b)");
    private static final Pattern unwantedChapter = Pattern.compile("(?<![a-z])(ch|chapter).?[0-9]+");
    private static final Pattern unwantedWhiteSpace = Pattern.compile("(\\s)(extra|special|omake|экстра|екстра|спешл|спэшл)");

    public static void parseVolumeNumber(Chapter chapterItem, boolean isSkipSingles, boolean isSkipExtras) {
        // If volume number is known return
        if (chapterItem.getVolumeNumber().equals(SINGLE_OR_EXTRA) || chapterItem.getVolumeNumber() > -1f) {
            return;
        }

        // Get volume title with lower case
        String name = chapterItem.getTitle().toLowerCase();

        // Remove comma's from volume
        name = name.replace(',', '.');

        Matcher shortVolWithNumberMatcher = shortVolWithNumber.matcher(name);
        if (shortVolWithNumberMatcher.find()) {
            name = name.replace(shortVolWithNumberMatcher.group(), "vol." + shortVolWithNumberMatcher.group(1));
        }

        // If name contains defined values - skip parsing
        if (isSkipSingles && skipIfSingleOccur.matcher(name).find() || isSkipExtras && skipIfExtraOrSpecialOccur.matcher(name).find()) {
            chapterItem.setVolumeNumber(SINGLE_OR_EXTRA);
            return;
        }

        // Remove unwanted white spaces
        Matcher unwantedWhiteSpaceMatcher = unwantedWhiteSpace.matcher(name);
        while (unwantedWhiteSpaceMatcher.find()) {
            String occurence = unwantedWhiteSpaceMatcher.group();
            name = name.replace(occurence, occurence.trim());
        }

        // Remove unwanted tags
        Matcher unwantedMatcher = unwantedChapter.matcher(name);
        while (unwantedMatcher.find()) {
            name = name.replace(unwantedMatcher.group(), "");
        }

        // Check volume
        Matcher basicMatcher = basicVolume.matcher(name);
        if (updateVolume(basicMatcher, 3, chapterItem)) {
            return;
        }

        // Check volume dashed
        Matcher dashedMatcher = dashedVolume.matcher(name);
        if (updateVolume(dashedMatcher, 1, chapterItem)) {
            return;
        }

        // If name contains defined values - skip parsing
        if (skipIfSingleOccur.matcher(name).find() || skipIfExtraOrSpecialOccur.matcher(name).find()) {
            chapterItem.setVolumeNumber(SINGLE_OR_EXTRA);
        }
    }

    private static boolean updateVolume(Matcher match, int group, Chapter chapterItem) {
        if (match != null && match.find()) {
            String initialGroup = match.group(group);
            float initial = GUType.getFloatDef(initialGroup, -1f);
            chapterItem.setVolumeNumber(initial);
            return true;
        }
        return false;
    }
}
