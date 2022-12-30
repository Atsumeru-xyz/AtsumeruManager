package xyz.atsumeru.manager.models.content;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContentList {
    private String status;
    private Integer page;
    private List<Content> contentList;
}
