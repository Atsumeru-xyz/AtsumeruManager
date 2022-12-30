package xyz.atsumeru.manager.models.content;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContentFull {
    private String status;
    private Content content = new Content();
}
