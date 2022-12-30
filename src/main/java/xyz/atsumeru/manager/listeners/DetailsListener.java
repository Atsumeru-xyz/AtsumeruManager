package xyz.atsumeru.manager.listeners;

import xyz.atsumeru.manager.models.content.Content;
import xyz.atsumeru.manager.models.manga.Chapter;

import java.util.List;

public interface DetailsListener {
    String getContentTitle();
    Content getContent();
    List<List<Chapter>> getChapters();
}