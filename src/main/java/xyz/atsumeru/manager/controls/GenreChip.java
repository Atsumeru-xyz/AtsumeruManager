package xyz.atsumeru.manager.controls;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import xyz.atsumeru.manager.listeners.OnClickListener;
import xyz.atsumeru.manager.utils.FXUtils;
import xyz.atsumeru.manager.utils.ViewUtils;

public class GenreChip extends StackPane {
    public static final String DEFAULT_BORDER_CSS = "genre-chip-border";
    public static final String HOVER_BORDER_CSS = "genre-chip-border-colored";

    @FXML private StackPane root;
    @FXML private Pane rippler;
    @FXML private Label lblChipText;

    private boolean isSelected = false;

    public GenreChip(String text, OnClickListener clickListener) {
        FXUtils.loadComponent(this, "/fxml/controls/GenreChip.fxml");
        ViewUtils.createRoundedRippler(rippler, root, 16, 16);

        setChipText(text);
        setOnMouseClicked(event -> clickListener.onClick(text, -1));

        ViewUtils.createOnMouseEnterBorderEffect(DEFAULT_BORDER_CSS, HOVER_BORDER_CSS, root);
        ViewUtils.createOnMouseExitedBorderEffect(DEFAULT_BORDER_CSS, HOVER_BORDER_CSS, root);
    }

    public void setChipText(String text) {
        lblChipText.setText(text);
    }

    public String getChipText() {
        return lblChipText.getText();
    }

    public void setSelected(boolean isSelected) {
        this.isSelected = isSelected;
        root.setStyle(isSelected ? "-fx-background-color: -fx-accent-color" : "-fx-background-color: transparent");
    }

    public boolean isSelected() {
        return isSelected;
    }
}
