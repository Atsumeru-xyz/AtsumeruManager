package xyz.atsumeru.manager.controls;

import com.jpro.webapi.WebAPI;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIconView;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import lombok.Getter;
import xyz.atsumeru.manager.archive.helper.ContentDetector;
import xyz.atsumeru.manager.helpers.FileDirChooserHelper;
import xyz.atsumeru.manager.helpers.LocaleManager;
import xyz.atsumeru.manager.utils.FXUtils;
import xyz.atsumeru.manager.utils.ViewUtils;
import xyz.atsumeru.manager.utils.comparator.AlphanumComparator;
import xyz.atsumeru.manager.utils.globalutils.GUArray;
import xyz.atsumeru.manager.utils.globalutils.GUFile;
import xyz.atsumeru.manager.utils.globalutils.GUString;
import xyz.atsumeru.manager.utils.globalutils.GUType;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class DragNDropFilesView extends VBox {
    private static final String STYLE_ACTIVE_UPLOAD_BOX = "active";
    private static final String STYLE_INACTIVE_UPLOAD_BOX = "inactive";
    private final BooleanProperty uploadMode = new SimpleBooleanProperty(false);
    private final BooleanProperty onlyArchives = new SimpleBooleanProperty(false);
    private final BooleanProperty folderSelectMode = new SimpleBooleanProperty(false);
    private final StringProperty chooserExtensionsFilterDescription = new SimpleStringProperty(null);
    private final StringProperty extensionsFilter = new SimpleStringProperty("");
    @FXML
    private VBox root;
    @FXML
    private Label lblFilesToUpload;
    @FXML
    private VBox containerNodes;
    @FXML
    private MaterialDesignIconView mdivIcon;
    @FXML
    private ProgressBar pbUploading;
    @FXML
    private Label lblLog;
    @Getter
    private List<File> selectedFiles;
    @Getter
    private List<File> declinedFiles;
    private OnFilesSelected callback;

    public DragNDropFilesView() {
        FXUtils.loadComponent(this, "/fxml/controls/DragNDropFilesView.fxml");
    }

    public void setCallback(OnFilesSelected callback) {
        this.callback = callback;
    }

    public void setIcon(MaterialDesignIcon icon) {
        mdivIcon.setGlyphName(icon.name());
    }

    @FXML
    private void initialize() {
        if (!WebAPI.isBrowser()) {
            containerNodes.setOnMouseClicked(event -> selectFiles());
            containerNodes.setOnMouseEntered(event -> setUploadBoxState(true));
            containerNodes.setOnMouseExited(event -> setUploadBoxState(false));
        }

        ViewUtils.setNodeVisibleAndManaged(uploadMode.get(), uploadMode.get(), pbUploading);
        configureDragAndDropNode();
        configureIcon();

        uploadMode.addListener((observable, oldValue, newValue) -> ViewUtils.setNodeVisibleAndManaged(newValue, newValue, pbUploading));
        folderSelectMode.addListener((observable, oldValue, newValue) -> {
            configureIcon();
            clearSelectedFiles();
        });

        if (WebAPI.isBrowser()) {
            lblFilesToUpload.setText(LocaleManager.getString("gui.error.unsupported_in_web"));
        }
    }

    @FXML
    private void selectFiles() {
        if (folderSelectMode.get()) {
            Optional.ofNullable(FileDirChooserHelper.chooseDirectory()).ifPresent(dir -> selectedFiles = Collections.singletonList(dir));
        } else {
            selectedFiles = FileDirChooserHelper.chooseFiles(chooserExtensionsFilterDescription.get(), GUArray.splitString(extensionsFilter.get(), ","));
        }

        onFilesSelected();
        FXUtils.requestFocus(root);
    }

    public void updateProgress(int progress, double currentFileUploadingProgress, int total, List<String> errorMessages) {
        Platform.runLater(() -> {
            double currentProgress = GUType.roundDouble((progress + currentFileUploadingProgress) / total, 2);
            if (pbUploading.getProgress() != currentProgress) {
                pbUploading.setProgress(currentProgress);
                lblFilesToUpload.setText(
                        LocaleManager.getStringFormatted("gui.uploading", progress < selectedFiles.size() ? selectedFiles.get(progress).getName() : "")
                );
                lblLog.setText(GUString.join("\n", errorMessages));
            }
        });
    }

    public void selectFiles(List<File> files) {
        clearSelectedFiles();
        selectedFiles = files;
        onFilesSelected();
    }

    public boolean hasSelectedFiles() {
        return GUArray.isNotEmpty(selectedFiles);
    }

    public boolean hasDeclinedFiles() {
        return GUArray.isNotEmpty(declinedFiles);
    }

    public void clearSelectedFiles() {
        if (GUArray.isNotEmpty(selectedFiles)) {
            selectedFiles.clear();
        }
        if (GUArray.isNotEmpty(declinedFiles)) {
            declinedFiles.clear();
        }
        onFilesSelected();
    }

    public void showUploadProgress() {
        ViewUtils.setNodeVisible(lblLog, pbUploading);
        FXUtils.requestFocus(root);
    }

    public void hideUploadProgress() {
        ViewUtils.setNodeGone(lblLog, pbUploading);
        FXUtils.requestFocus(root);
    }

    private void onFilesSelected() {
        filterSelectedFiles();

        if (GUArray.isNotEmpty(selectedFiles)) {
            lblFilesToUpload.setText(
                    folderSelectMode.get()
                            ? LocaleManager.getStringFormatted("gui.selected_folders", selectedFiles.size())
                            : LocaleManager.getStringFormatted("gui.selected_files", selectedFiles.size())
            );
            if (callback != null) {
                callback.onFilesSelected(selectedFiles, declinedFiles);
            }

            ViewUtils.addTooltipToNode(this, GUString.join("\n", selectedFiles.stream().map(File::getAbsolutePath).collect(Collectors.toList())), 12);
        } else {
            if (WebAPI.isBrowser()) {
                lblFilesToUpload.setText(LocaleManager.getString("gui.error.unsupported_in_web"));
            } else {
                lblFilesToUpload.setText(LocaleManager.getString(
                        folderSelectMode.get()
                                ? "gui.drag_n_drop_select_folders"
                                : "gui.drag_n_drop_select_files"
                ));
            }

            if (callback != null) {
                callback.onFilesCleared();
            }
        }
    }

    private void filterSelectedFiles() {
        if (GUArray.isNotEmpty(selectedFiles)) {
            Map<Boolean, List<File>> groups = selectedFiles.stream().collect(Collectors.partitioningBy(file -> {
                if (folderSelectMode.get()) {
                    return file.isDirectory();
                } else if (onlyArchives.get()) {
                    return file.isFile() && ContentDetector.isArchiveFile(file);
                } else {
                    return file.isFile();
                }
            }));

            selectedFiles = groups.get(true);
            declinedFiles = groups.get(false);
        }
    }

    private void configureDragAndDropNode() {
        setUploadBoxState(false);

        containerNodes.setOnDragOver(event -> {
            if (event.getGestureSource() != containerNodes && event.getDragboard().hasFiles()) {
                setUploadBoxState(true);
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });

        containerNodes.setOnDragExited(event -> setUploadBoxState(false));

        containerNodes.setOnDragDropped(event -> {
            Dragboard dragboard = event.getDragboard();
            boolean success = false;
            if (dragboard.hasFiles()) {
                selectedFiles = new ArrayList<>();
                List<File> files = dragboard.getFiles();
                files.forEach(file -> {
                    if (!getFolderSelectMode() && file.isDirectory()) {
                        selectedFiles.addAll(GUFile.getAllFilesFromDirectory(file.getAbsolutePath(), null, true));
                    } else {
                        selectedFiles.add(file);
                    }
                });
                selectedFiles.sort((file1, file2) -> AlphanumComparator.compareStrings(file1.getAbsolutePath(), file2.getAbsolutePath()));
                onFilesSelected();
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private void configureIcon() {
        if (folderSelectMode.get()) {
            setIcon(MaterialDesignIcon.FOLDER);
        }
    }

    private void setUploadBoxState(boolean isActive) {
        ObservableList<String> styleClass = containerNodes.getStyleClass();
        if (isActive) {
            styleClass.removeAll(STYLE_INACTIVE_UPLOAD_BOX);
            styleClass.add(STYLE_ACTIVE_UPLOAD_BOX);
        } else {
            styleClass.removeAll(STYLE_ACTIVE_UPLOAD_BOX);
            styleClass.add(STYLE_INACTIVE_UPLOAD_BOX);
        }
    }

    public final boolean getUploadMode() {
        return uploadMode.get();
    }

    public final void setUploadMode(boolean value) {
        uploadMode.set(value);
    }

    public final boolean getOnlyArchives() {
        return onlyArchives.get();
    }

    public final void setOnlyArchives(boolean value) {
        onlyArchives.set(value);
    }

    public final boolean getFolderSelectMode() {
        return folderSelectMode.get();
    }

    public final void setFolderSelectMode(boolean value) {
        folderSelectMode.set(value);
    }

    public final String getChooserExtensionsFilterDescription() {
        return chooserExtensionsFilterDescription.get();
    }

    public final void setChooserExtensionsFilterDescription(String value) {
        chooserExtensionsFilterDescription.set(value);
    }

    public final String getExtensionsFilter() {
        return extensionsFilter.get();
    }

    public final void setExtensionsFilter(String value) {
        extensionsFilter.set(value);
    }

    public interface OnFilesSelected {
        void onFilesSelected(List<File> selectedFiles, List<File> declinedFiles);

        void onFilesCleared();
    }
}
