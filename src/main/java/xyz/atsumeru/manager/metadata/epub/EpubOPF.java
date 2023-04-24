package xyz.atsumeru.manager.metadata.epub;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Safelist;
import org.jsoup.select.Elements;
import xyz.atsumeru.manager.models.ExtendedSerie;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Optional;

public class EpubOPF {

    public static boolean readInfo(ExtendedSerie extendedSerie, InputStream stream) {
        try {
            Document opf = Jsoup.parse(stream, "UTF-8", "", Parser.xmlParser());

            String title = Optional.ofNullable(opf.selectFirst("metadata > dc|title"))
                    .map(Element::text)
                    .orElse(null);

            String author = Optional.ofNullable(opf.selectFirst("metadata > dc|creator"))
                    .map(Element::text)
                    .orElse(null);

            String description = Optional.ofNullable(opf.selectFirst("metadata > dc|description"))
                    .map(Element::text)
                    .map(value -> Jsoup.clean(value, Safelist.none()))
                    .orElse(null);

            LocalDate date = Optional.ofNullable(opf.selectFirst("metadata > dc|date"))
                    .map(Element::text)
                    .map(EpubOPF::parseDate)
                    .orElse(null);

            // TODO: 23.12.2021 !!!
            String isbn = Optional.ofNullable(opf.select("metadata > dc|identifier"))
                    .map(Elements::text)
                    .map(value -> value.toLowerCase().replace("isbn:", ""))
                    .orElse(null);

            extendedSerie.setTitle(title);
            extendedSerie.setAuthors(Collections.singletonList(author));
            extendedSerie.setDescription(description);
            extendedSerie.setYear(Optional.ofNullable(date).map(LocalDate::toString).orElse(null));

            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private static LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date, DateTimeFormatter.ISO_DATE);
        } catch (Exception e) {
            try {
                return LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (Exception e1) {
                try {
                    return LocalDate.parse(date, DateTimeFormatter.ISO_DATE_TIME);
                } catch (Exception e2) {
                    return null;
                }
            }
        }
    }
}
