package xyz.atsumeru.manager.controller.dialogs.settings;

import com.atsumeru.api.AtsumeruAPI;
import com.atsumeru.api.model.settings.ServerSettings;
import com.jfoenix.controls.JFXCheckBox;
import io.github.palexdev.materialfx.controls.MFXProgressSpinner;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import kotlin.Pair;
import xyz.atsumeru.manager.controller.MainController;
import xyz.atsumeru.manager.controller.dialogs.BaseDialogController;
import xyz.atsumeru.manager.controls.EmptyView;
import xyz.atsumeru.manager.controls.ErrorView;
import xyz.atsumeru.manager.enums.ErrorType;
import xyz.atsumeru.manager.helpers.LocaleManager;
import xyz.atsumeru.manager.helpers.RXUtils;
import xyz.atsumeru.manager.managers.Settings;
import xyz.atsumeru.manager.utils.FXUtils;
import xyz.atsumeru.manager.utils.ViewUtils;
import xyz.atsumeru.manager.views.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class ServerSettingsDialogController extends BaseDialogController<Void> {
    @FXML
    private ScrollPane spRoot;
    @FXML
    private VBox vbRoot;

    @FXML
    MFXProgressSpinner spinnerLoading;

    @FXML
    EmptyView evEmptyView;
    @FXML
    ErrorView evErrorView;

    public static void createAndShow() {
        Pair<Node, ServerSettingsDialogController> pair = FXUtils.loadFXML("/fxml/atsumeru/dialogs/settings/ServerSettingsDialog.fxml");
        ServerSettingsDialogController controller = pair.getSecond();
        controller.show(LocaleManager.getString("atsumeru.server_settings"), pair.getFirst());
    }

    @FXML
    protected void initialize() {
        loadSettings();
        ViewUtils.setVBoxOnScroll(vbRoot, spRoot, 1);
        FXUtils.requestFocus(spRoot);
    }

    private void loadSettings() {
        ViewUtils.setNodeGone(spRoot);

        showLoading();
        evEmptyView.hideEmptyView();
        evErrorView.hideErrorView();

        AtomicReference<Disposable> disposable = new AtomicReference<>();
        disposable.set(AtsumeruAPI.getServerSettings()
                .subscribeOn(Schedulers.newThread())
                .observeOn(Schedulers.io())
                .subscribe(settings -> {
                    Platform.runLater(() -> onSettingsLoad(settings));
                    FXUtils.requestFocus(spRoot);
                    RXUtils.safeDispose(disposable);
                }, throwable -> {
                    throwable.printStackTrace();
                    evErrorView.showErrorView(throwable, ErrorType.LOAD_CONTENT, spinnerLoading,
                            true, this::loadSettings, false, null);
                }));
    }

    private void onSettingsLoad(ServerSettings settings) {
        List<Node> nodes = new ArrayList<>();

        // Lists
        nodes.add(ViewUtils.createLabel(LocaleManager.getString("gui.lists"), "white"));
        nodes.add(createCheckBox(
                LocaleManager.getString("atsumeru.settings.allow_loading_list_with_volumes"),
                settings.getAllowLoadingListWithVolumes(),
                () -> {
                    settings.setAllowLoadingListWithVolumes(!settings.getAllowLoadingListWithVolumes());
                    updateServerSettings(settings);
                }
        ));
        nodes.add(createCheckBox(
                LocaleManager.getString("atsumeru.settings.allow_loading_list_with_chapters"),
                settings.getAllowLoadingListWithChapters(),
                () -> {
                    settings.setAllowLoadingListWithChapters(!settings.getAllowLoadingListWithChapters());
                    updateServerSettings(settings);
                }
        ));

        // Import
        Label importing = ViewUtils.createLabel(LocaleManager.getString("atsumeru.import"), "white");
        VBox.setMargin(importing, new Insets(10, 0, 0, 0));
        nodes.add(importing);
        nodes.add(createCheckBox(
                LocaleManager.getString("atsumeru.settings.disable_chapters"),
                settings.getDisableChapters(),
                () -> {
                    settings.setDisableChapters(!settings.getDisableChapters());
                    updateServerSettings(settings);
                }
        ));

        // Logs
        Label logs = ViewUtils.createLabel(LocaleManager.getString("gui.logs"), "white");
        VBox.setMargin(logs, new Insets(10, 0, 0, 0));
        nodes.add(logs);
        nodes.add(createCheckBox(
                LocaleManager.getString("atsumeru.settings.disable_request_logging_into_console"),
                settings.getDisableRequestLoggingIntoConsole(),
                () -> {
                    settings.setDisableRequestLoggingIntoConsole(!settings.getDisableRequestLoggingIntoConsole());
                    updateServerSettings(settings);
                }
        ));

        // Bonjour
        Label bonjour = ViewUtils.createLabel("Bonjour", "white");
        VBox.setMargin(bonjour, new Insets(10, 0, 0, 0));
        nodes.add(bonjour);
        nodes.add(createCheckBox(
                LocaleManager.getString("atsumeru.settings.disable_bonjour_service"),
                settings.getDisableBonjourService(),
                () -> {
                    settings.setDisableBonjourService(!settings.getDisableBonjourService());
                    updateServerSettings(settings);
                }
        ));

        // FSWatcher
        Label fsWatcher = ViewUtils.createLabel("FileSystem Watcher", "white");
        VBox.setMargin(fsWatcher, new Insets(10, 0, 0, 0));
        nodes.add(fsWatcher);
        nodes.add(createCheckBox(
                LocaleManager.getString("atsumeru.settings.disable_watch_for_modified_files"),
                settings.getDisableWatchForModifiedFiles(),
                () -> {
                    settings.setDisableWatchForModifiedFiles(!settings.getDisableWatchForModifiedFiles());
                    updateServerSettings(settings);
                }
        ));
        nodes.add(createCheckBox(
                LocaleManager.getString("atsumeru.settings.disable_file_watcher"),
                settings.getDisableFileWatcher(),
                () -> {
                    settings.setDisableFileWatcher(!settings.getDisableFileWatcher());
                    updateServerSettings(settings);
                }
        ));

        vbRoot.getChildren().addAll(nodes);
        evEmptyView.checkEmptyItems(nodes, spRoot, spinnerLoading);
    }

    private JFXCheckBox createCheckBox(String title, boolean isSelected, Runnable onChangeRunnable) {
        JFXCheckBox checkBox = new JFXCheckBox(title);
        checkBox.setSelected(isSelected);
        checkBox.setCheckedColor(Color.web(Settings.getAppAccentColor()));
        checkBox.setFont(Font.font(16));
        checkBox.selectedProperty().addListener((observable, oldValue, newValue) -> onChangeRunnable.run());

        return checkBox;
    }

    private void updateServerSettings(ServerSettings settings) {
        AtomicReference<Disposable> disposable = new AtomicReference<>();
        disposable.set(AtsumeruAPI.updateServerSettings(settings)
                .subscribeOn(Schedulers.newThread())
                .observeOn(Schedulers.io())
                .subscribe(message -> {
                    Snackbar.showSnackBar(MainController.getSnackbarRoot(), LocaleManager.getString("gui.settings_updated"), Snackbar.Type.SUCCESS);
                    RXUtils.safeDispose(disposable);
                }, throwable -> Snackbar.showExceptionSnackbar(MainController.getSnackbarRoot(true), throwable, true)));
    }

    private void showLoading() {
        ViewUtils.setNodeVisible(spinnerLoading);
    }

    @FXML
    void closeDialog() {
        close();
    }

    @Override
    protected int minDialogWidth() {
        return 700;
    }
}
