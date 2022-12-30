package xyz.atsumeru.manager.models.content;

import lombok.Data;

import java.io.Serializable;

@Data
public class Images implements Serializable {
    private String thumbnail;
    private String original;

    private String local;

    public Images() {
    }

    public Images(String url) {
        thumbnail = url;
        original = url;
    }

    public Images(String thumbnailUrl, String originalUrl) {
        thumbnail = thumbnailUrl;
        original = originalUrl;
    }
}
