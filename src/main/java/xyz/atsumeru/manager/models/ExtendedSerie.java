package xyz.atsumeru.manager.models;

import com.atsumeru.api.model.Chapter;
import com.atsumeru.api.model.Serie;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ExtendedSerie extends Serie {
    private transient String serieId;
    private transient boolean isBook = false;
    private transient List<Chapter> chapters = new ArrayList<>();
}
