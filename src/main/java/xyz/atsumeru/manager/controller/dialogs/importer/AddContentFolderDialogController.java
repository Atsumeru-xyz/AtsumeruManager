package xyz.atsumeru.manager.controller.dialogs.importer;

import com.atsumeru.api.model.importer.FolderProperty;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXTextField;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Node;
import kotlin.Pair;
import lombok.Setter;
import xyz.atsumeru.manager.controller.dialogs.BaseDialogController;
import xyz.atsumeru.manager.controller.dialogs.FolderExplorerDialogController;
import xyz.atsumeru.manager.filesystem.AtsumeruRemoteFileManager;
import xyz.atsumeru.manager.helpers.LocaleManager;
import xyz.atsumeru.manager.utils.FXUtils;
import xyz.atsumeru.manager.utils.ViewUtils;
import xyz.atsumeru.manager.utils.globalutils.GUString;

public class AddContentFolderDialogController extends BaseDialogController<FolderProperty> {
    @FXML
    JFXTextField tfFolderToImport;
    @FXML
    JFXCheckBox cbImportAsSingles;
    @FXML
    JFXCheckBox cbImportAsSinglesOnlyFromRoot;
    @FXML
    JFXCheckBox cbImportAsSinglesIfInRootWithFolders;
    @FXML
    JFXButton btnSelectFolder;

    @Setter
    private String initialPath;
    private FolderProperty folderProperty;

    public static void createAndShow(String initialPath, ChangeListener<FolderProperty> changeListener) {
        Pair<Node, AddContentFolderDialogController> pair = FXUtils.loadFXML("/fxml/atsumeru/dialogs/importer/AddContentFolderDialog.fxml");
        AddContentFolderDialogController controller = pair.getSecond();
        controller.setInitialPath(initialPath);

        controller.show(null, LocaleManager.getString("gui.import_content_from_folder"), pair.getFirst(), changeListener);
    }

    @FXML
    protected void initialize() {
        ViewUtils.createOnMouseEnterBorderEffect(btnSelectFolder);
        ViewUtils.createOnMouseExitedBorderEffect(btnSelectFolder);
        Platform.runLater(() -> tfFolderToImport.requestFocus());
    }

    @FXML
    void selectFolder() {
        FolderExplorerDialogController.createAndShow(
                initialPath,
                (observable, oldPath, path) -> {
                    if (GUString.isNotEmpty(path)) {
                        tfFolderToImport.setText(path);
                    }
                },
                AtsumeruRemoteFileManager.create());
    }

    @FXML
    void closeDialog() {
        property.setValue(folderProperty);
        close();
    }

    @FXML
    void save() {
        if (GUString.isNotEmpty(tfFolderToImport.getText())) {
            folderProperty = new FolderProperty();
            folderProperty.setPath(tfFolderToImport.getText());
            folderProperty.createHash();
            folderProperty.setSingles(cbImportAsSingles.isSelected());
            folderProperty.setSinglesInRoot(cbImportAsSinglesOnlyFromRoot.isSelected());
            folderProperty.setSinglesIfInRootWithFolders(cbImportAsSinglesIfInRootWithFolders.isSelected());
            folderProperty.setSeriesCount(Long.MIN_VALUE);
            folderProperty.setSinglesCount(Long.MIN_VALUE);
            folderProperty.setArchivesCount(Long.MIN_VALUE);
            closeDialog();
        }
    }

    @Override
    protected int minDialogWidth() {
        return 600;
    }
}
