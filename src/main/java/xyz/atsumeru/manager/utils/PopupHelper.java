package xyz.atsumeru.manager.utils;

import com.atsumeru.api.utils.ImportType;
import com.jfoenix.controls.JFXListView;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.util.Duration;
import xyz.atsumeru.manager.FXApplication;
import xyz.atsumeru.manager.controls.jfoenix.JFXCustomPopup;
import xyz.atsumeru.manager.helpers.LocaleManager;
import xyz.atsumeru.manager.source.AtsumeruSource;

import java.util.stream.Collectors;

public class PopupHelper {

    public static JFXCustomPopup createListViewPopup(ListView<Node> listViewNodes, ChangeListener<Node> changeListener) {
        JFXCustomPopup popup = new JFXCustomPopup(listViewNodes);
        popup.setOnShowRunnable(() -> FXApplication.animateDimContent(Duration.seconds(.25), true));
        popup.setOnHideRunnable(() -> FXApplication.dimContent(false));

        listViewNodes.getItems().forEach(node -> {
            ViewUtils.createOnMouseEnterBackgroundEffect(node);
            ViewUtils.createOnMouseExitedBackgroundEffect(node);
        });

        listViewNodes.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            changeListener.changed(observable, oldValue, newValue);
            FXUtils.runLaterDelayed(popup::hide, 50);
        });

        return popup;
    }

    public static void showPopupForButton(JFXCustomPopup popup, Node node) {
        popup.show(node, JFXCustomPopup.PopupVPosition.BOTTOM, JFXCustomPopup.PopupHPosition.LEFT, 0, -50);
    }

    public static JFXListView<Node> createEmptyPopupList() {
        JFXListView<Node> list = new JFXListView<>();
        list.getStyleClass().add("paddingless-list-cell");
        list.getStyleClass().add("light-list-cell");
        return list;
    }

    public static JFXListView<Node> createPopupListForAtsumeruServers() {
        JFXListView<Node> list = new JFXListView<>();
        list.getStyleClass().add("paddingless-list-cell");
        list.getStyleClass().add("light-list-cell");

        AtsumeruSource.listServers().forEach(server -> list.getItems().add(
                ViewUtils.createTwoLabelWithMaterialDesignIconNode(
                        server.getName(),
                        server.getHost(),
                        MaterialDesignIcon.SERVER_NETWORK,
                        26,
                        "white",
                        server.getId()
                )));

        return list;
    }

    public static JFXListView<Node> createPopupListForAtsumeruImport() {
        JFXListView<Node> list = new JFXListView<>();
        list.getStyleClass().add("paddingless-list-cell");
        list.getStyleClass().add("light-list-cell");

        list.getItems().addAll(
                ViewUtils.createLabelWithMaterialDesignIconNode(
                        LocaleManager.getString("gui.folders_management"),
                        MaterialDesignIcon.FOLDER_STAR,
                        350,
                        26,
                        "white",
                        "folders_management"
                ),
                ViewUtils.createLabelWithMaterialDesignIconNode(
                        LocaleManager.getString("atsumeru.import.scan_for_new_archives"),
                        MaterialDesignIcon.RELOAD,
                        350,
                        26,
                        "white",
                        ImportType.NEW
                ),
                ViewUtils.createLabelWithMaterialDesignIconNode(
                        LocaleManager.getString("atsumeru.import.full_rescan"),
                        MaterialDesignIcon.REFRESH,
                        350,
                        26,
                        "white",
                        ImportType.FULL
                ),
                ViewUtils.createLabelWithMaterialDesignIconNode(
                        LocaleManager.getString("atsumeru.import.full_rescan_with_covers_replace"),
                        MaterialDesignIcon.REFRESH,
                        350,
                        26,
                        "white",
                        ImportType.FULL_WITH_COVERS
                )
        );
        return list;
    }

    public static JFXListView<Node> createPopupListForAtsumeruAdminFunctions() {
        JFXListView<Node> list = new JFXListView<>();
        list.getStyleClass().add("paddingless-list-cell");
        list.getStyleClass().add("light-list-cell");

        list.getItems().addAll(
                ViewUtils.createLabelWithMaterialDesignIconNode(
                        LocaleManager.getString("gui.check_downloaded_from_links"),
                        MaterialDesignIcon.LINK,
                        350,
                        26,
                        "white",
                        "check_downloaded_from_links"
                ),
                ViewUtils.createLabelWithMaterialDesignIconNode(
                        LocaleManager.getString("gui.clear_server_cache"),
                        "#e74c3c",
                        MaterialDesignIcon.DELETE_SWEEP,
                        350,
                        26,
                        "#e74c3c",
                        "clear_cache"
                ),
                ViewUtils.createLabelWithMaterialDesignIconNode(
                        LocaleManager.getString("gui.generate_unique_archive_hashes"),
                        "#e74c3c",
                        MaterialDesignIcon.KEY_PLUS,
                        350,
                        26,
                        "#e74c3c",
                        "generate_hashes"
                ),
                ViewUtils.createLabelWithMaterialDesignIconNode(
                        LocaleManager.getString("gui.insert_all_metadata_into_archives"),
                        "#e74c3c",
                        MaterialDesignIcon.ZIP_BOX,
                        350,
                        26,
                        "#e74c3c",
                        "insert_metadata"
                )
        );
        return list;
    }

    public static JFXListView<Node> createPopupListForCategories(ObservableList<Tab> tabs) {
        JFXListView<Node> list = new JFXListView<>();
        list.getStyleClass().add("paddingless-list-cell");
        list.getStyleClass().add("light-list-cell");

        list.getItems().addAll(
                tabs.stream()
                .map(tab -> ViewUtils.createLabelWithInsets(
                        tab.getText(),
                        "#ffffff",
                        290,
                        tab.getUserData()
                ))
                .collect(Collectors.toList())
        );

        return list;
    }
}
