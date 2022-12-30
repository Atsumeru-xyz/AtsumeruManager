package xyz.atsumeru.manager.metadata.comicinfo;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import xyz.atsumeru.manager.models.ExtendedSerie;
import xyz.atsumeru.manager.utils.globalutils.GUArray;
import xyz.atsumeru.manager.utils.globalutils.GUString;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.Collections;

public class ComicInfo {

    public static boolean readComicInfo(ExtendedSerie serie, InputStream comicInfoStream) {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(comicInfoStream);
            NodeList root = document.getElementsByTagName("ComicInfo");

            String number = null;
            String count = null;
            String year = null;
            String month = null;

            String blackAndWhite = null;
            String characters = null;
            String pageCount = null;
            NodeList pages; // Pages

            for (int i = 0; i < root.getLength(); i++) {
                NodeList nodes = root.item(i).getChildNodes();
                for (int j = 0; j < nodes.getLength(); j++) {
                    Node node = nodes.item(j);
                    switch (node.getNodeName()) {
                        case "Title":
                            serie.setTitle(node.getTextContent());
                            break;
                        case "Series":
//                            serie.setSeries(node.getTextContent());
                            break;
                        case "Circles":
                            try {
                                serie.setCircles(GUArray.splitString(node.getTextContent(), ", "));
                            } catch (Exception ex) {
                                System.err.println("Unable to parse Circles from ComicInfo.xml");
                            }
                            break;
                        // TODO: 28.09.2020 !!!
//                        case "Conventions":
//                            try {
//                                serie.getCircles().addAll(GUArray.splitString(node.getTextContent(), ", "));
//                            } catch (Exception ex) {
//                                System.err.println("Unable to parse Conventions from ComicInfo.xml");
//                            }
//                            break;
                        case "Summary":
                            serie.setDescription(node.getTextContent());
                            break;
                        case "Volume":
                            try {
                                serie.setVolume(Float.parseFloat(node.getTextContent()));
                            } catch (NumberFormatException ex) {
                                System.err.println("Unable to parse volume number from ComicInfo.xml");
                            }
                            break;
                        case "Number":
                            number = node.getTextContent();
                            break;
                        case "Count":
                            count = node.getTextContent();
                            break;
                        case "Year":
                            year = node.getTextContent();
                            break;
                        case "Month":
                            month = node.getTextContent();
                            break;
                        case "Writer":
                            serie.setAuthors(Collections.singletonList(node.getTextContent()));
                            break;
                        case "Publisher":
                            serie.setPublisher(node.getTextContent());
                            break;
                        case "Genre":
                            serie.setGenres(GUArray.splitString(node.getTextContent(), ", "));
                            break;
                        case "BlackAndWhite":
                            blackAndWhite = node.getTextContent();
                            break;
                        case "Manga":
                            // TODO: 20.02.2022 reading direction support
//                            String manga = node.getTextContent();
//                            if (GUString.isNotEmpty(manga) && manga.equalsIgnoreCase("YES")) {
//                                serie.setReadingDirection(ReadingDirection.RIGHT_TO_LEFT);
//                            } else {
//                                serie.setReadingDirection(ReadingDirection.LEFT_TO_RIGHT);
//                            }
                            break;
                        case "Characters":
                            serie.setCharacters(GUArray.splitString(node.getTextContent(), ", "));
                            break;
                        case "PageCount":
                            pageCount = node.getTextContent();
                            break;
                    }
                }
            }

            serie.setYear(GUString.isNotEmpty(month) ? year + "-" + month + "-01" : year);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
