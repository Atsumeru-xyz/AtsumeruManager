package xyz.atsumeru.manager.controls;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.Nullable;
import xyz.atsumeru.manager.helpers.Kaomoji;
import xyz.atsumeru.manager.utils.FXUtils;
import xyz.atsumeru.manager.utils.ViewUtils;
import xyz.atsumeru.manager.utils.globalutils.GUArray;

import java.util.List;

@SuppressWarnings("unused")
public class EmptyView extends VBox {
    @FXML
    private VBox vbEmptyView;
    @FXML
    private Label lblKaomoji;
    @FXML
    private Label lblNothingFound;

    private Kaomoji kaomoji;

    public EmptyView() {
        FXUtils.loadComponent(this, "/fxml/controls/EmptyView.fxml");
    }

    public boolean checkEmptyItems(List<?> list, @Nullable Node showNodeIfNotEmpty, @Nullable ProgressIndicator loadingPane) {
        ViewUtils.setNodeGone(loadingPane);
        if (GUArray.isNotEmpty(list)) {
            hideEmptyView();
            if (showNodeIfNotEmpty != null) {
                ViewUtils.setNodeVisible(showNodeIfNotEmpty);
            }
            return false;
        } else {
            showEmptyView(Kaomoji.SADNESS);
            return true;
        }
    }

    public void hideEmptyView() {
        ViewUtils.setNodeGone(vbEmptyView);
    }

    public void showEmptyView() {
        Platform.runLater(() -> ViewUtils.setNodeVisible(vbEmptyView));
    }

    public void showEmptyView(Kaomoji kaomoji) {
        setKaomoji(kaomoji);
        showEmptyView();
    }

    public void setKaomoji(Kaomoji kaomoji) {
        if (kaomoji != this.kaomoji) {
            Platform.runLater(() -> lblKaomoji.setText(kaomoji.getRandomEmotion()));
        }
        this.kaomoji = kaomoji;
    }

    public void setEmptyText(String text) {
        Platform.runLater(() -> lblNothingFound.setText(text));
    }
}
