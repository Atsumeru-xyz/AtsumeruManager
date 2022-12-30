package xyz.atsumeru.manager.controller.cards;

import com.atsumeru.api.AtsumeruAPI;
import com.atsumeru.api.model.importer.FolderProperty;
import com.jfoenix.controls.JFXButton;
import io.reactivex.schedulers.Schedulers;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import kotlin.Triple;
import xyz.atsumeru.manager.FXApplication;
import xyz.atsumeru.manager.controller.MainController;
import xyz.atsumeru.manager.controller.dialogs.importer.FoldersManagerDialogController;
import xyz.atsumeru.manager.controller.tabs.TabAtsumeruLibraryController;
import xyz.atsumeru.manager.controls.jfoenix.JFXCustomDialog;
import xyz.atsumeru.manager.helpers.DialogsHelper;
import xyz.atsumeru.manager.helpers.LocaleManager;
import xyz.atsumeru.manager.utils.FXUtils;
import xyz.atsumeru.manager.utils.ViewUtils;
import xyz.atsumeru.manager.views.Snackbar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FolderCardController {
    @FXML
    HBox hbRoot;
    @FXML
    VBox vbRoot;
    @FXML
    Label lblPath;
    @FXML
    Label lblSeriesSinglesAndArchives;
    @FXML
    JFXButton btnEdit;
    @FXML
    JFXButton btnSearchNew;
    @FXML
    JFXButton btnRescan;
    @FXML
    JFXButton btnDelete;

    private FoldersManagerDialogController controller;
    private FolderProperty folderProperty;
    private String folderId;

    public static Node createNode(FoldersManagerDialogController foldersManagerDialogController, FolderProperty folderProperty, int folderId) {
        FXMLLoader fxmlLoader = new FXMLLoader();
        Node node = null;
        try {
            node = fxmlLoader.load(FXApplication.getResource("/fxml/atsumeru/cards/FolderCard.fxml").openStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        FolderCardController controller = fxmlLoader.getController();
        controller.init(foldersManagerDialogController, "folderId" + folderId, folderProperty);
        controller.createFolderNode();
        return node;
    }

    public void init(FoldersManagerDialogController foldersManagerDialogController, String folderId, FolderProperty folderProperty) {
        this.controller = foldersManagerDialogController;
        this.folderId = folderId;
        this.folderProperty = folderProperty;
    }

    public void createFolderNode() {
        ViewUtils.createOnMouseEnterBorderEffect(btnEdit, btnSearchNew, btnRescan, btnDelete);
        ViewUtils.createOnMouseExitedBorderEffect(btnEdit, btnSearchNew, btnRescan, btnDelete);

        btnSearchNew.setTooltip(new Tooltip(LocaleManager.getString("atsumeru.import.scan_for_new_archives")));
        btnRescan.setTooltip(new Tooltip(LocaleManager.getString("atsumeru.import.full_rescan")));

        hbRoot.setId(folderId);
        lblPath.setText(folderProperty.getPath());
        lblSeriesSinglesAndArchives.setText(LocaleManager.getString(
                "gui.series_singles_archives",
                getItemsCountStr(folderProperty.getSeriesCount()),
                getItemsCountStr(folderProperty.getSinglesCount()),
                getItemsCountStr(folderProperty.getArchivesCount()),
                getItemsCountStr(folderProperty.getChaptersCount())
        ));

        vbRoot.setOnMouseClicked(event -> {
            FXUtils.copyToClipboard(lblPath.getText());
            Snackbar.showSnackBar(
                    MainController.getSnackbarRoot(),
                    LocaleManager.getString("gui.copied_to_clipboard"),
                    Snackbar.Type.SUCCESS
            );
        });

        // FIXME: 10.07.2021 поддержка редактирования
        btnEdit.setVisible(false);
        btnEdit.setOnMouseClicked(event -> {
        });

        btnSearchNew.setOnMouseClicked(event -> importerRescan(false, false));
        btnRescan.setOnMouseClicked(event -> {
            List<Triple<ButtonType, String, Runnable>> actionTriples = new ArrayList<>();
            actionTriples.add(new Triple<>(
                    ButtonType.NO,
                    LocaleManager.getString("gui.import.rescan_folder_with_update_covers"),
                    () -> importerRescan(true, true)
            ));
            actionTriples.add(new Triple<>(
                    ButtonType.YES,
                    LocaleManager.getString("gui.import.rescan_folder_normal"),
                    () -> importerRescan(true, false)
            ));

            DialogsHelper.showConfirmationDialog(
                    controller.getStackPane(),
                    JFXCustomDialog.DialogTransition.CENTER,
                    actionTriples,
                    LocaleManager.getString("gui.import.do_full_rescan"),
                    LocaleManager.getStringFormatted("gui.import.do_full_rescan.header", folderProperty.getPath()),
                    "",
                    LocaleManager.getString("gui.import.do_full_rescan.content")
            );
        });

        btnDelete.setOnMouseClicked(event -> {
            List<Triple<ButtonType, String, Runnable>> actionPairs = new ArrayList<>();
            actionPairs.add(new Triple<>(ButtonType.NO, null, null));
            actionPairs.add(new Triple<>(
                    ButtonType.YES,
                    null,
                    () -> AtsumeruAPI.removeImporterFolder(folderProperty.getHash())
                            .cache()
                            .subscribeOn(Schedulers.io())
                            .observeOn(Schedulers.io())
                            .subscribe(message -> Platform.runLater(() -> {
                                        controller.removeFolderNode(folderId);
                                        TabAtsumeruLibraryController.reloadItems();
                                    }),
                                    throwable -> {
                                        throwable.printStackTrace();
                                        Snackbar.showSnackBar(
                                                MainController.getSnackbarRoot(),
                                                LocaleManager.getString("gui.error.unable_delete_folder"),
                                                Snackbar.Type.ERROR
                                        );
                                    })
            ));

            DialogsHelper.showConfirmationDialog(
                    controller.getStackPane(),
                    JFXCustomDialog.DialogTransition.CENTER,
                    actionPairs,
                    LocaleManager.getString("gui.delete_folder"),
                    LocaleManager.getStringFormatted("gui.delete_folder.header", folderProperty.getPath()),
                    "",
                    LocaleManager.getString(
                            "gui.delete_folder.content",
                            getItemsCountStr(folderProperty.getSeriesCount()),
                            getItemsCountStr(folderProperty.getSinglesCount()),
                            getItemsCountStr(folderProperty.getArchivesCount()),
                            getItemsCountStr(folderProperty.getChaptersCount())
                    )
            );
        });
    }

    private void importerRescan(boolean fully, boolean updateCovers) {
        AtsumeruAPI.importerRescan(folderProperty.getHash(), fully, updateCovers).cache()
                .subscribeOn(Schedulers.newThread())
                .observeOn(Schedulers.io())
                .subscribe(message ->
                        Snackbar.showSnackBar(
                                MainController.getSnackbarRoot(),
                                LocaleManager.getString(fully ? "atsumeru.import.reimporting_started" : "atsumeru.import.importing_started"),
                                Snackbar.Type.SUCCESS
                        ), throwable ->
                        Snackbar.showSnackBar(
                                MainController.getSnackbarRoot(),
                                LocaleManager.getString(fully ? "atsumeru.import.unable_to_start_reimporting" : "atsumeru.import.unable_to_start_importing"),
                                Snackbar.Type.ERROR
                        ));
    }

    private String getItemsCountStr(long count) {
        return count >= 0 ? String.valueOf(count) : "?";
    }
}
