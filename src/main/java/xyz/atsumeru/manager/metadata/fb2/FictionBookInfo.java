package xyz.atsumeru.manager.metadata.fb2;

import com.kursx.parser.fb2.FictionBook;
import com.kursx.parser.fb2.Person;
import com.kursx.parser.fb2.PublishInfo;
import com.kursx.parser.fb2.TitleInfo;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import xyz.atsumeru.manager.helpers.LocaleManager;
import xyz.atsumeru.manager.models.ExtendedSerie;
import xyz.atsumeru.manager.utils.globalutils.GUType;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

public class FictionBookInfo {

    public static boolean readInfo(ExtendedSerie serie, String filePath, FictionBook fictionBook) {
        try (FileInputStream fis = new FileInputStream(filePath)) {
            Document xml = Jsoup.parse(fis, "UTF-8", "", Parser.xmlParser());

            TitleInfo titleInfo = fictionBook.getDescription().getTitleInfo();
            PublishInfo publishInfo = fictionBook.getDescription().getPublishInfo();

            serie.setTitle(titleInfo.getSequence().getName());
            serie.setAltTitle(fictionBook.getTitle());
            serie.setAuthors(titleInfo.getAuthors()
                    .stream()
                    .map(Person::getFullName)
                    .collect(Collectors.toList()));
            serie.setTranslators(titleInfo.getTranslators()
                    .stream()
                    .map(Person::getFullName)
                    .collect(Collectors.toList()));
            serie.setPublisher(publishInfo.getPublisher());
            serie.setYear(publishInfo.getYear());
            // TODO: 23.12.2021 localization
            serie.setLanguages(Collections.singletonList(fictionBook.getLang()));
            serie.setVolume(GUType.getFloatDef(titleInfo.getSequence().getNumber(), -1f));
            serie.setTags(titleInfo.getGenres()
                    .stream()
                    .map(genre -> "fb2_" + genre)
                    .map(LocaleManager::getString)
                    .collect(Collectors.toList()));

            serie.setDescription(Optional.ofNullable(xml.select("description > title-info > annotation"))
                    .map(Elements::text)
                    .orElse(null));

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
