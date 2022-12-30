package xyz.atsumeru.manager.controller;

import com.jfoenix.controls.JFXTabPane;
import com.jpro.webapi.WebAPI;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import org.controlsfx.control.StatusBar;
import xyz.atsumeru.manager.FXApplication;
import xyz.atsumeru.manager.controls.jfoenix.JFXCustomDialog;
import xyz.atsumeru.manager.helpers.DialogBuilder;
import xyz.atsumeru.manager.helpers.LocaleManager;
import xyz.atsumeru.manager.managers.*;
import xyz.atsumeru.manager.utils.FXUtils;
import xyz.atsumeru.manager.utils.ViewUtils;
import xyz.atsumeru.manager.utils.globalutils.GUArray;
import xyz.atsumeru.manager.views.Snackbar;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class MainController extends BaseController {
    @FXML
    public Pane opaqueLayer;
    @FXML
    private StackPane container;
    @FXML
    private JFXTabPane tabPaneMain;
    @FXML
    private StatusBar statusBar;

    public static Pane getSnackbarRoot() {
        return getSnackbarRoot(false);
    }

    public static Pane getSnackbarRoot(boolean getRoot) {
        MainController controller = TabsManager.getTabController(MainController.class);
        return controller.opaqueLayer.isVisible() && !getRoot
                ? controller.opaqueLayer
                : controller.getContentRoot();
    }

    @FXML
    protected void initialize() throws IOException {
        super.initialize();
    }

    public void init() {
        ViewUtils.setNodeVisibleAndManaged(WebAPI.isBrowser(), statusBar);

        StatusBarManager.init(Optional.ofNullable(FXApplication.getInstance().getStatusBar()).orElse(statusBar));
        TabPaneManager.init(tabPaneMain);

        FXUtils.runDelayed(() -> {
            UpdatesChecker.check(getContentRoot(), Settings.getUpdateBranch(), false);
            FXApplication.getParsersManager().checkParserUpdates(this::onResult, this::onError);
        }, 1500);
    }

    @Override
    public StackPane getContentRoot() {
        return container;
    }

    private void onResult(List<Long> parserIds) {
        if (GUArray.isNotEmpty(parserIds)) {
            Platform.runLater(() -> {
                Label titleLabel = new Label(LocaleManager.getString("gui.please_wait"));

                DialogBuilder builder = DialogBuilder.create(getContentRoot())
                        .withDialogType(DialogBuilder.DialogType.WAIT)
                        .withTransition(JFXCustomDialog.DialogTransition.CENTER)
                        .withHeading(LocaleManager.getString("gui.updating_parsers"))
                        .withBody(titleLabel)
                        .withOnProgressDone(() -> {
                            FXApplication.getParsersManager().reload();
                            Platform.runLater(() -> Snackbar.showSnackBar(getSnackbarRoot(true), LocaleManager.getString("gui.parsers_updated"), Snackbar.Type.SUCCESS));
                        })
                        .withOnOpenRunnable(() -> FXApplication.dimContent(true))
                        .withOnCloseRunnable(() -> FXApplication.dimContent(false))
                        .build();

                JFXCustomDialog dialog = builder.show();

                FXUtils.runLaterDelayed(() -> FXApplication.getParsersManager().updateParsers(
                        parserIds,
                        title -> onParserTitleUpdate(titleLabel, title),
                        builder::updateProgress,
                        throwable -> onParserUpdateError(dialog, throwable)
                ), 1000);
            });
        }
    }

    private void onError(Throwable throwable) {
        FXApplication.LOGGER.error("checkParserUpdates: " + throwable.getMessage());
    }

    private void onParserTitleUpdate(Label titleLabel, String title) {
        Platform.runLater(() -> titleLabel.setText(title));
    }

    private void onParserUpdateError(JFXCustomDialog dialog, Throwable throwable) {
        Platform.runLater(() -> {
            dialog.close();

            FXApplication.LOGGER.error("Unable to update parsers", throwable);
            Snackbar.showSnackBar(getSnackbarRoot(true), LocaleManager.getString("gui.parsers_update_error"), Snackbar.Type.ERROR);
        });
    }
}
