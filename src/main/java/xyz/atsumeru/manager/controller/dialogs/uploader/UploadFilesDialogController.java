package xyz.atsumeru.manager.controller.dialogs.uploader;

import com.atsumeru.api.AtsumeruAPI;
import com.atsumeru.api.model.AtsumeruMessage;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import io.github.palexdev.materialfx.controls.MFXProgressSpinner;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import kotlin.Pair;
import lombok.Setter;
import xyz.atsumeru.manager.callback.OnImporterCallback;
import xyz.atsumeru.manager.controller.dialogs.BaseDialogController;
import xyz.atsumeru.manager.controls.DragNDropFilesView;
import xyz.atsumeru.manager.helpers.LocaleManager;
import xyz.atsumeru.manager.source.AtsumeruSource;
import xyz.atsumeru.manager.utils.FXUtils;
import xyz.atsumeru.manager.utils.ViewUtils;
import xyz.atsumeru.manager.utils.globalutils.GUArray;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class UploadFilesDialogController extends BaseDialogController<Void> {
    @FXML
    private DragNDropFilesView uploadFiles;
    @FXML
    private MFXProgressSpinner spinnerProgress;

    @FXML
    private JFXCheckBox cbOverrideFiles;
    @FXML
    private JFXCheckBox cbRepackFiles;
    @FXML
    private JFXButton btnCancel;
    @FXML
    private JFXButton btnUpload;

    private final AtomicReference<Disposable> uploadDisposable = new AtomicReference<>();
    @Setter
    private String serieId;
    @Setter
    private OnImporterCallback callback;

    private boolean uploadInProgress;

    public static void createAndShow(String serieTitle, String serieId, OnImporterCallback callback) {
        Pair<Node, UploadFilesDialogController> pair = FXUtils.loadFXML("/fxml/atsumeru/dialogs/uploader/UploadFilesDialog.fxml");
        UploadFilesDialogController controller = pair.getSecond();
        controller.setSerieId(serieId);
        controller.setCallback(callback);

        controller.show(LocaleManager.getStringFormatted("gui.upload_archives_into", serieTitle), pair.getFirst());
    }

    @FXML
    protected void initialize() {
        uploadFiles.setUploadMode(true);
        uploadFiles.setOnlyArchives(true);
        hideProgress();
    }

    private void showProgress() {
        ViewUtils.setNodeGone(cbOverrideFiles, cbRepackFiles, btnCancel, btnUpload);
        ViewUtils.setNodeVisible(spinnerProgress);
        uploadFiles.showUploadProgress();
    }

    private void hideProgress() {
        ViewUtils.setNodeVisible(cbOverrideFiles, cbRepackFiles, btnCancel, btnUpload);
        ViewUtils.setNodeGone(spinnerProgress);
        uploadFiles.hideUploadProgress();
    }

    private void checkUploadProgress(int filesUploaded, double currentFileUploadingProgress, int total, List<String> errorMessages) {
        uploadFiles.updateProgress(filesUploaded, currentFileUploadingProgress, total, errorMessages);
        if (filesUploaded >= total) {
            FXUtils.runLaterDelayed(() -> AtsumeruSource.requestSerieRescan(serieId, this::onImportDone), 1000);
        }
    }

    private void onImportDone() {
        FXUtils.runLaterDelayed(() -> {
            callback.onImportDone();

            uploadFiles.clearSelectedFiles();
            hideProgress();
            uploadDisposable.get().dispose();
            uploadInProgress = false;
        }, 1000);
    }

    @FXML
    void upload() {
        if (GUArray.isNotEmpty(uploadFiles.getSelectedFiles())) {
            uploadInProgress = true;
            AtomicInteger filesUploaded = new AtomicInteger(0);
            Map<Integer, Float> uploadingProgress = new HashMap<>();

            List<String> errorMessages = new ArrayList<>();
            List<Single<AtsumeruMessage>> singles = new ArrayList<>();
            uploadFiles.getSelectedFiles().forEach(file -> singles.add(
                    AtsumeruAPI.uploadFile(
                            serieId,
                            file.getAbsolutePath(),
                            (progress) -> {
                                float realProgress = Optional.ofNullable(uploadingProgress.get(filesUploaded.get()))
                                        .map(value -> value >= 0.5
                                                ? 0.5f + progress / 2
                                                : progress / 2)
                                        .orElse(0.0f);
                                uploadingProgress.put(filesUploaded.get(), realProgress);
                                checkUploadProgress(filesUploaded.get(), realProgress, singles.size(), errorMessages);
                            },
                            cbOverrideFiles.isSelected(),
                            cbRepackFiles.isSelected()
                    )
            ));

            showProgress();
            uploadDisposable.set(
                    Single.concat(singles)
                            .cache()
                            .subscribeOn(Schedulers.newThread())
                            .observeOn(Schedulers.io())
                            .subscribe(message -> {
                                if (!message.isOk()) {
                                    errorMessages.add(message.getMessage());
                                }

                                checkUploadProgress(filesUploaded.incrementAndGet(), 0.0, singles.size(), errorMessages);
                            }, throwable -> {
                                throwable.printStackTrace();
                                checkUploadProgress(filesUploaded.incrementAndGet(), 0.0, singles.size(), errorMessages);
                            })
            );
        }
    }

    @FXML
    void closeDialog() {
        if (!uploadInProgress) {
            Platform.runLater(() -> {
                close();
                callback = null;
            });
        }
    }

    @Override
    protected int minDialogWidth() {
        return 600;
    }

    @Override
    protected boolean isClosable() {
        return false;
    }
}
