package xyz.atsumeru.manager.controller.dialogs;

import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import io.github.palexdev.materialfx.controls.MFXProgressSpinner;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import kotlin.Pair;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;
import xyz.atsumeru.manager.controls.ErrorView;
import xyz.atsumeru.manager.enums.ErrorType;
import xyz.atsumeru.manager.helpers.LocaleManager;
import xyz.atsumeru.manager.managers.filesystem.IFileSystemManager;
import xyz.atsumeru.manager.models.filesystem.DirectoryListing;
import xyz.atsumeru.manager.utils.FXUtils;
import xyz.atsumeru.manager.utils.ViewUtils;
import xyz.atsumeru.manager.utils.globalutils.GUString;

import java.util.ArrayList;
import java.util.List;

public class FolderExplorerDialogController extends BaseDialogController<String> {
    @FXML
    StackPane container;
    @FXML
    ScrollPane spFilesList;
    @FXML
    VBox vbFilesList;

    @FXML
    MFXProgressSpinner spinnerLoading;

    @FXML
    ErrorView vbErrorView;

    @Getter
    private String selectedFilePath;
    @Setter
    private String initialPath;
    @Setter
    private IFileSystemManager fileSystemManager;

    private Disposable mDisposable;

    public static void createAndShow(@Nullable String initialPath, ChangeListener<String> changeListener, IFileSystemManager fsManager) {
        Pair<Node, FolderExplorerDialogController> pair = FXUtils.loadFXML("/fxml/dialogs/FolderExplorerDialog.fxml");
        FolderExplorerDialogController controller = pair.getSecond();
        controller.setInitialPath(initialPath);
        controller.setFileSystemManager(fsManager);

        controller.show(initialPath, LocaleManager.getString("gui.folder_management.title"), pair.getFirst(), changeListener);
    }

    @FXML
    protected void initialize() {
        hideLoading();
        vbErrorView.hideErrorView();

        vbFilesList.minWidthProperty().bind(spFilesList.widthProperty().subtract(10));
        vbErrorView.maxHeightProperty().bind(spFilesList.heightProperty());

        FXUtils.runLaterDelayed(() -> {
            FXUtils.requestFocus(container);
            FXUtils.setOnHiding(container, event -> handleCancelButton());
            listDirectory(getInitialDirectoryPath());
        }, 100);
    }

    private void listDirectory(String requestPath) {
        selectedFilePath = requestPath;
        vbFilesList.getChildren().clear();

        showLoading();
        vbErrorView.hideErrorView();

        mDisposable = fileSystemManager.getDirectoryListing(requestPath)
                .cache()
                .subscribeOn(Schedulers.newThread())
                .observeOn(Schedulers.io())
                .subscribe(directoryListing -> Platform.runLater(() -> {
                    createDirectoryNode(directoryListing);
                    hideLoading();
                    mDisposable.dispose();
                }), throwable -> {
                    mDisposable.dispose();
                    vbErrorView.showErrorView(
                            throwable,
                            ErrorType.LOAD_CONTENT,
                            spinnerLoading,
                            true,
                            () -> listDirectory(selectedFilePath),
                            false,
                            null
                    );
                });
    }

    private void createDirectoryNode(DirectoryListing directoryListing) {
        List<Node> nodes = new ArrayList<>();
        if (GUString.isNotNull(directoryListing.parent)) {
            createDirectoryLabel(nodes, LocaleManager.getString("gui.back_ellipsize"), directoryListing.parent, MaterialDesignIcon.ARROW_LEFT);
        }

        directoryListing.directories.forEach(directoryPath
                -> createDirectoryLabel(nodes, directoryPath.name, directoryPath.path, MaterialDesignIcon.FOLDER));

        vbFilesList.getChildren().addAll(nodes);
        ViewUtils.setVerticalScrollPaneOnScroll(spFilesList, Math.max(vbFilesList.getChildren().size() - 10, 1));
    }

    private void createDirectoryLabel(List<Node> nodes, String title, String path, MaterialDesignIcon icon) {
        Label node = ViewUtils.createLabelWithMaterialDesignIconNode(title, icon, 28, "white", null);
        node.setOnMouseClicked(event -> listDirectory(path));

        ViewUtils.createOnMouseEnterBackgroundEffect(node);
        ViewUtils.createOnMouseExitedBackgroundEffect(node);
        node.minWidthProperty().bind(vbFilesList.widthProperty());

        nodes.add(node);
    }

    private String getInitialDirectoryPath() {
        return GUString.isNotEmpty(initialPath) ? initialPath : "";
    }

    public void showLoading() {
        ViewUtils.setNodeVisibleAndManaged(true, true, spinnerLoading);
    }

    public void hideLoading() {
        ViewUtils.setNodeVisibleAndManaged(false, false, spinnerLoading);
    }

    @FXML
    private void handleCancelButton() {
        selectedFilePath = null;
        closeDialog();
    }

    @FXML
    private void closeDialog() {
        FXUtils.setOnHiding(container, event -> {});
        property.setValue(selectedFilePath);
        close();
    }

    @Override
    protected int minDialogWidth() {
        return 450;
    }
}
