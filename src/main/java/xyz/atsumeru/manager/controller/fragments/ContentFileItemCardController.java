package xyz.atsumeru.manager.controller.fragments;

import com.jfoenix.controls.JFXButton;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIconView;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import kotlin.Pair;
import lombok.Getter;
import lombok.Setter;
import xyz.atsumeru.manager.controller.stages.StageReaderController;
import xyz.atsumeru.manager.listeners.DetailsListener;
import xyz.atsumeru.manager.models.manga.Chapter;
import xyz.atsumeru.manager.utils.FXUtils;
import xyz.atsumeru.manager.utils.ViewUtils;

import java.util.List;

public class ContentFileItemCardController {
    @Getter @FXML Pane root;
    @FXML Pane rippler;
    @FXML HBox hbRoot;
    @FXML VBox vbRoot;

    @FXML Label lblTitle;
    @FXML Label lblService;
    @FXML Label lblProgress;
    @FXML JFXButton btnDownloadOrDelete;

    @FXML MaterialDesignIconView mdivFileIcon;

    // Text
    @Setter private List<Chapter> chapter;

    // Listeners
    @Setter private DetailsListener listener;

    @Setter private ReadOnlyDoubleProperty rootWidthProperty;

    public static ContentFileItemCardController createNode(ReadOnlyDoubleProperty rootWidthProperty, DetailsListener listener, List<Chapter> chapter) {
        Pair<Node, ContentFileItemCardController> pair = FXUtils.loadFXML("/fxml/fragments/ContentFileItemCard.fxml");

        ContentFileItemCardController controller = pair.getSecond();
        controller.setRootWidthProperty(rootWidthProperty);
        controller.setListener(listener);
        controller.setChapter(chapter);
        return controller;
    }

    @FXML
    public void initialize() {
        ViewUtils.setNodeGone(lblService);
        ViewUtils.setNodeGone(btnDownloadOrDelete);
        ViewUtils.createRoundedRippler(rippler, root, 12, 12);
        Platform.runLater(() -> {
            root.prefWidthProperty().bind(rootWidthProperty);
            bindChapterViews();
        });
    }

    private void bindChapterViews() {
        Chapter chapter = this.chapter.get(0);
        lblTitle.setText(chapter.getTitle());
        ViewUtils.setNodeInvisible(lblProgress);
        root.setOnMouseClicked(event -> StageReaderController.showReader(listener.getContentTitle(), listener.getChapters(), chapter, null));
    }
}
