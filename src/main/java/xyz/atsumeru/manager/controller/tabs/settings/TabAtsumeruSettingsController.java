package xyz.atsumeru.manager.controller.tabs.settings;

import com.atsumeru.api.model.server.Server;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.controlsfx.control.SegmentedButton;
import org.jetbrains.annotations.NotNull;
import xyz.atsumeru.manager.FXApplication;
import xyz.atsumeru.manager.controller.BaseController;
import xyz.atsumeru.manager.controller.MainController;
import xyz.atsumeru.manager.controller.cards.ServerCardController;
import xyz.atsumeru.manager.controller.dialogs.EditServerDialogController;
import xyz.atsumeru.manager.controller.tabs.TabAtsumeruLibraryController;
import xyz.atsumeru.manager.controls.jfoenix.JFXCustomColorPickerDialog;
import xyz.atsumeru.manager.controls.jfoenix.JFXCustomDialog;
import xyz.atsumeru.manager.enums.AppCloseBehaviorInMetadataEditor;
import xyz.atsumeru.manager.enums.GridScaleType;
import xyz.atsumeru.manager.helpers.DialogBuilder;
import xyz.atsumeru.manager.helpers.LocaleManager;
import xyz.atsumeru.manager.listeners.OnItemClickListener;
import xyz.atsumeru.manager.managers.Settings;
import xyz.atsumeru.manager.managers.StatusBarManager;
import xyz.atsumeru.manager.managers.TabsManager;
import xyz.atsumeru.manager.source.AtsumeruSource;
import xyz.atsumeru.manager.utils.FXUtils;
import xyz.atsumeru.manager.utils.ViewUtils;
import xyz.atsumeru.manager.views.Snackbar;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class TabAtsumeruSettingsController extends BaseController {
    @FXML
    private Pane container;
    @FXML
    private JFXButton btnAddServer;
    @FXML
    private Label lblNoServers;
    @FXML
    private VBox vbServersList;
    @FXML
    private HBox hbCardCount;
    @FXML
    private SegmentedButton sgAppLanguage;
    @FXML
    private SegmentedButton sgAppCloseBehaviorInMetadataEditor;
    @FXML
    private SegmentedButton sgGridScaleType;
    @FXML
    private JFXCheckBox cbDisableProgressSync;
    @FXML
    private MFXTextField tfComicVineAPIKey;
    @FXML
    private Hyperlink hpRequestComicVineAPIKey;

    @FXML
    private Label lblApi;
    @FXML
    private HBox hbApi;

    public static ObservableList<Node> getServersChildrenNodes() {
        return TabsManager.getTabController(TabAtsumeruSettingsController.class).vbServersList.getChildren();
    }

    public static void pickAccentColor() {
        JFXCustomColorPickerDialog dialog = new JFXCustomColorPickerDialog(FXApplication.getInstance().getCurrentStage());
        dialog.setTabTextColor(Color.WHITE);
        dialog.currentColorProperty().addListener(new ChangeListener<>() {
            final long delayTime = 100;
            TimerTask task = null;

            @Override
            public void changed(ObservableValue<? extends Color> observable, Color oldValue, Color newValue) {
                if (task != null) {
                    task.cancel();
                }

                task = new TimerTask() {
                    @Override
                    public void run() {
                        Platform.runLater(() -> {
                            String hexColor = FXUtils.toHexString(newValue);
                            FXUtils.setAppAccentColor(FXApplication.getInstance().getRootNode(), hexColor);
                            Settings.putAppAccentColor(hexColor);
                        });
                    }
                };

                new Timer().schedule(task, delayTime);
            }
        });

        DialogBuilder.create(TabsManager.getTabController(MainController.class).getContentRoot())
                .withDialogType(DialogBuilder.DialogType.NODE)
                .withTransition(JFXCustomDialog.DialogTransition.CENTER)
                .withMinWidth(440)
                .withBody(dialog.getContainer())
                .withNoButtons()
                .withOnOpenRunnable(() -> {
                    FXApplication.dimContent(true);
                    dialog.initRun();
                })
                .withOnCloseRunnable(() -> FXApplication.dimContent(false))
                .show();
    }

    @FXML
    protected void initialize() throws IOException {
        super.initialize();

        configureServersSection();
        configureMainSection();
        configureAppearanceSection();
        configureAdministrationSection();
        configureAPIAndMetadataSourcesSection();
    }

    private void configureServersSection() {
        btnAddServer.setOnMouseClicked(event -> {
            EditServerDialogController.createAndShow(
                    null,
                    (observable, oldServer, server) -> {
                        if (server != null) {
                            FXUtils.runLaterDelayed(() -> {
                                createServerCard(server);
                                if (AtsumeruSource.listServers().size() == 1) {
                                    TabsManager.getTabController(TabAtsumeruLibraryController.class)
                                            .changeServer(AtsumeruSource.listServers().get(0).getId());
                                }
                                TabsManager.getTabController(TabAtsumeruLibraryController.class).recalculateGridView();
                            }, 300);
                        }
                    }
            );
        });

        ViewUtils.createOnMouseEnterBorderEffect(btnAddServer);
        ViewUtils.createOnMouseExitedBorderEffect(btnAddServer);

        lblNoServers.managedProperty().bind(Bindings.createBooleanBinding(
                () -> vbServersList.getChildren().isEmpty(), vbServersList.getChildren())
        );

        AtsumeruSource.listServers().forEach(this::createServerCard);
    }

    private void configureMainSection() {
        String defaultAppLanguageCode = LocaleManager.getSystemLanguageCode();
        sgAppLanguage.getButtons().addAll(
                createAppLanguageToggleButton("en", defaultAppLanguageCode.equals("en")),
                createAppLanguageToggleButton("ru", defaultAppLanguageCode.equals("ru")));

        AppCloseBehaviorInMetadataEditor appCloseBehaviorInMetadataEditor = Settings.getAppCloseBehaviorInMetadataEditor();
        sgAppCloseBehaviorInMetadataEditor.getButtons().addAll(
                createAppCloseBehaviorInMetadataEditorToggleButton(AppCloseBehaviorInMetadataEditor.CLOSE_APP, appCloseBehaviorInMetadataEditor == AppCloseBehaviorInMetadataEditor.CLOSE_APP),
                createAppCloseBehaviorInMetadataEditorToggleButton(AppCloseBehaviorInMetadataEditor.CLOSE_EDITOR, appCloseBehaviorInMetadataEditor == AppCloseBehaviorInMetadataEditor.CLOSE_EDITOR)
        );
    }

    private void configureAppearanceSection() {
        hbCardCount.getChildren().add(StatusBarManager.getInstance().getGridSlider());

        GridScaleType gridScaleType = Settings.getGridScaleType();
        sgGridScaleType.getButtons().addAll(
                createGridScaleTypeToggleButton(GridScaleType.FIXED_SCALE, gridScaleType == GridScaleType.FIXED_SCALE),
                createGridScaleTypeToggleButton(GridScaleType.PROPORTIONAL_SCALE, gridScaleType == GridScaleType.PROPORTIONAL_SCALE)
        );
    }

    private void configureAdministrationSection() {
        cbDisableProgressSync.setSelected(Settings.Atsumeru.isDisableProgressSync());
        cbDisableProgressSync.selectedProperty().addListener((observable, oldValue, newValue) -> Settings.Atsumeru.putDisableProgressSync(newValue));
    }

    private void createServerCard(@NotNull Server server) {
        ServerCardController controller = ServerCardController.createCard(server);

        OnItemClickListener clickListener = (action, position, payload) -> {
            if (action.equalsIgnoreCase(ServerCardController.ACTION_DELETE)) {
                AtsumeruSource.removeSource(server.getId());
                AtsumeruSource.saveServers();
                vbServersList.getChildren().remove(controller.getRootNode());
            }
        };

        controller.setOnItemClickListener(clickListener);
        vbServersList.getChildren().add(controller.getRootNode());
    }

    private ToggleButton createAppLanguageToggleButton(String languageCode, boolean selected) {
        ToggleButton tg = new ToggleButton(LocaleManager.getString("gui.settings.language." + languageCode));
        tg.setId(languageCode);
        tg.setSelected(selected);
        setAppLanguageToggleButtonSelectedListener(tg);
        return tg;
    }

    private ToggleButton createAppCloseBehaviorInMetadataEditorToggleButton(AppCloseBehaviorInMetadataEditor appCloseBehaviorInMetadataEditor, boolean selected) {
        ToggleButton tg = new ToggleButton(LocaleManager.getString("gui.settings.app_close_behavior_in_metadata_editor." + appCloseBehaviorInMetadataEditor.toString().toLowerCase()));
        tg.setId(appCloseBehaviorInMetadataEditor.toString());
        tg.setSelected(selected);
        tg.setUserData(appCloseBehaviorInMetadataEditor);
        setAppCloseBehaviorInMetadataEditorToggleButtonSelectedListener(tg);
        return tg;
    }

    private ToggleButton createGridScaleTypeToggleButton(GridScaleType gridScaleType, boolean selected) {
        ToggleButton tg = new ToggleButton(LocaleManager.getString("gui.settings.grid_scale_type." + gridScaleType.toString().toLowerCase()));
        tg.setId(gridScaleType.toString());
        tg.setSelected(selected);
        tg.setUserData(gridScaleType);
        setGridScaleTypeToggleButtonSelectedListener(tg);
        return tg;
    }

    private void setAppLanguageToggleButtonSelectedListener(ToggleButton tg) {
        tg.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                String languageCode = tg.getId();
                Settings.putDefaultAppLanguageCode(languageCode);
                Snackbar.showSnackBar(
                        MainController.getSnackbarRoot(),
                        LocaleManager.getString("gui.language_change_need_restart"),
                        Snackbar.Type.WARNING
                );
            }
        });
    }

    private void setAppCloseBehaviorInMetadataEditorToggleButtonSelectedListener(ToggleButton tg) {
        tg.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                AppCloseBehaviorInMetadataEditor behaviorInMetadataEditor = (AppCloseBehaviorInMetadataEditor) tg.getUserData();
                Settings.putAppCloseBehaviorInMetadataEditor(behaviorInMetadataEditor);
            }
        });
    }

    private void setGridScaleTypeToggleButtonSelectedListener(ToggleButton tg) {
        tg.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                GridScaleType gridScaleType = (GridScaleType) tg.getUserData();
                Settings.putGridScaleType(gridScaleType);
                TabsManager.getTabController(TabAtsumeruLibraryController.class).recalculateGridView();
            }
        });
    }

    private void configureAPIAndMetadataSourcesSection() {
        ViewUtils.setNodeVisibleAndManaged(FXApplication.getParsersManager().isSupportsMetadataFetching(), lblApi, hbApi);
        tfComicVineAPIKey.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.isEmpty() && !oldValue.equals(newValue)) {
                Settings.Metadata.putComicVineApiKey(newValue);
                FXApplication.getParsersManager().createComicVineParser();
            }
        });
        tfComicVineAPIKey.setText(Settings.Metadata.getComicVineApiKey());
        hpRequestComicVineAPIKey.setStyle("-fx-text-fill: -fx-accent-color");
    }

    @FXML
    void handleRequestComicVineApiKey() {
        FXApplication.getParsersManager().openComicVineAPIRequestUrl();
    }

    @FXML
    private void handleAccentColorPick() {
        pickAccentColor();
    }

    @Override
    public Pane getContentRoot() {
        return container;
    }
}
