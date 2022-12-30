package xyz.atsumeru.manager.controls;

import com.atsumeru.api.model.category.Category;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.layout.HBox;
import lombok.Getter;
import xyz.atsumeru.manager.utils.FXUtils;
import xyz.atsumeru.manager.utils.ViewUtils;

@SuppressWarnings("unused")
public class CategoryEntry extends HBox {
    @FXML
    private JFXCheckBox chbCategory;
    @FXML
    private JFXButton btnEdit;
    @FXML
    private JFXButton btnDelete;

    @Getter
    private final Category category;

    public CategoryEntry(Category category) {
        this.category = category;
        FXUtils.loadComponent(this, "/fxml/atsumeru/controls/CategoryEntry.fxml");
    }

    @FXML
    private void initialize() {
        chbCategory.setText(category.getName());

        ViewUtils.createOnMouseEnterBorderEffect(btnEdit);
        ViewUtils.createOnMouseExitedBorderEffect(btnEdit);
    }

    public void setOnEditAction(EventHandler<ActionEvent> eventHandler) {
        btnEdit.setOnAction(eventHandler);
    }

    public void setOnDeleteAction(EventHandler<ActionEvent> eventHandler) {
        btnDelete.setOnAction(eventHandler);
    }

    public void setSelected(boolean selected) {
        chbCategory.setSelected(selected);
    }

    public boolean isSelected() {
        return chbCategory.isSelected();
    }
}
