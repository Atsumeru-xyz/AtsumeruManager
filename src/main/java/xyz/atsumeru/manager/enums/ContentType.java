package xyz.atsumeru.manager.enums;

public enum ContentType {
    UNKNOWN(0),

    // Text (images)
    MANGA(10),
    MANHUA(11),
    MANHWA(12),
    DOUJINSHI(13),
    HENTAI_MANGA(14),
    YAOI(15),
    YAOI_MANGA(16),
    WEBCOMICS(17),
    RUMANGA(18),
    OEL_MANGA(19),
    STRIP(20),
    COMICS(21),
    YURI(26),
    YURI_MANGA(27),
    HENTAI_MANHWA(28),

    // Text (books)
    LIGHT_NOVEL(22),
    NOVEL(23),
    BOOK(24),
    TEXT_PORN(25),
    FANFICTION(29);

    public final int id;

    ContentType(int id) {
        this.id = id;
    }

    public static boolean isBookContent(ContentType contentType) {
        return contentType == LIGHT_NOVEL
                || contentType == NOVEL
                || contentType == BOOK
                || contentType == TEXT_PORN
                || contentType == FANFICTION;
    }

    public static boolean isMatureContent(ContentType contentType) {
        return contentType == HENTAI_MANGA
                || contentType == HENTAI_MANHWA
                || contentType == YAOI
                || contentType == YAOI_MANGA
                || contentType == YURI
                || contentType == YURI_MANGA
                || contentType == TEXT_PORN;
    }
}

