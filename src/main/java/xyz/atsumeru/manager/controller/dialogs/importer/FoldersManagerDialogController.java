package xyz.atsumeru.manager.controller.dialogs.importer;

import com.atsumeru.api.AtsumeruAPI;
import com.atsumeru.api.model.importer.FolderProperty;
import com.jfoenix.controls.JFXButton;
import io.github.palexdev.materialfx.controls.MFXProgressSpinner;
import io.reactivex.schedulers.Schedulers;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import kotlin.Pair;
import kotlin.Triple;
import xyz.atsumeru.manager.controller.cards.FolderCardController;
import xyz.atsumeru.manager.controller.dialogs.BaseDialogController;
import xyz.atsumeru.manager.controls.EmptyView;
import xyz.atsumeru.manager.controls.ErrorView;
import xyz.atsumeru.manager.controls.jfoenix.JFXCustomDialog;
import xyz.atsumeru.manager.enums.ErrorType;
import xyz.atsumeru.manager.helpers.DialogsHelper;
import xyz.atsumeru.manager.helpers.LocaleManager;
import xyz.atsumeru.manager.utils.FXUtils;
import xyz.atsumeru.manager.utils.ViewUtils;
import xyz.atsumeru.manager.utils.globalutils.GUArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FoldersManagerDialogController extends BaseDialogController<Void> {
    private final List<FolderProperty> folderProperties = new ArrayList<>();
    @FXML
    StackPane container;
    @FXML
    VBox vbFoldersList;
    @FXML
    JFXButton btnAddFolder;
    @FXML
    MFXProgressSpinner spinnerLoading;
    @FXML
    EmptyView evEmptyView;
    @FXML
    ErrorView evErrorView;
    private int folderId = 0;

    public static void createAndShow() {
        Pair<Node, FoldersManagerDialogController> pair = FXUtils.loadFXML("/fxml/atsumeru/dialogs/importer/FoldersManagerDialog.fxml");
        FoldersManagerDialogController controller = pair.getSecond();
        controller.show(LocaleManager.getString("gui.folders_management"), pair.getFirst());
    }

    @FXML
    protected void initialize() {
        fetchItems();
        configureButtons();
        FXUtils.runLaterDelayed(() -> container.requestFocus(), 100);
    }

    private void fetchItems() {
        ViewUtils.setNodeInvisible(vbFoldersList);

        showLoading();
        evEmptyView.hideEmptyView();
        evErrorView.hideErrorView();

        AtsumeruAPI.getImporterFoldersList()
                .cache().subscribeOn(Schedulers.newThread())
                .observeOn(Schedulers.io())
                .subscribe(properties -> {
                    folderProperties.addAll(properties);
                    evEmptyView.checkEmptyItems(properties, vbFoldersList, spinnerLoading);
                    properties.forEach(property -> Platform.runLater(() -> addFolderNode(FolderCardController.createNode(this, property, ++folderId))));
                }, throwable -> {
                    throwable.printStackTrace();
                    evErrorView.showErrorView(throwable, ErrorType.LOAD_CONTENT, spinnerLoading,
                            true, this::fetchItems, false, null);
                });
    }

    private void configureButtons() {
        btnAddFolder.setOnMouseClicked(event -> {
            String initialPath = GUArray.isNotEmpty(folderProperties)
                    ? folderProperties.get(folderProperties.size() - 1).getPath()
                    : null;

            AddContentFolderDialogController.createAndShow(
                    initialPath,
                    (observable, oldProperty, property) -> {
                        if (property != null) {
                            AtsumeruAPI.addImporterFolder(property)
                                    .cache().subscribeOn(Schedulers.newThread())
                                    .observeOn(Schedulers.io())
                                    .subscribe(message -> Platform.runLater(() -> {
                                                folderProperties.add(property);
                                                addFolderNode(FolderCardController.createNode(this, property, ++folderId));
                                                evEmptyView.checkEmptyItems(folderProperties, vbFoldersList, spinnerLoading);
                                            }),
                                            throwable -> {
                                                throwable.printStackTrace();
                                                Platform.runLater(() -> DialogsHelper.showConfirmationDialog(
                                                        getStackPane(),
                                                        JFXCustomDialog.DialogTransition.CENTER,
                                                        Collections.singletonList(new Triple<>(ButtonType.OK, null, null)),
                                                        LocaleManager.getString("gui.import.add_folder"),
                                                        LocaleManager.getString("gui.import.folder_already_added_or_not_found"),
                                                        "",
                                                        LocaleManager.getString("gui.import.choose_another_folder")
                                                ));
                                            });
                        }
                    }
            );
        });
    }

    public void addFolderNode(Node node) {
        vbFoldersList.getChildren().add(node);
    }

    public void removeFolderNode(String folderId) {
        vbFoldersList.getChildren().remove(vbFoldersList.lookup("#" + folderId));
    }

    public void showLoading() {
        ViewUtils.setNodeVisible(spinnerLoading);
    }

    @FXML
    private void handleCancelButton() {
        close();
    }

    public StackPane getStackPane() {
        return container;
    }

    @Override
    protected int minDialogWidth() {
        return 800;
    }
}
