package xyz.atsumeru.manager.controller.dialogs;

import com.atsumeru.api.AtsumeruAPI;
import com.atsumeru.api.model.BoundService;
import com.atsumeru.api.model.Link;
import com.atsumeru.api.model.Serie;
import com.atsumeru.api.model.metadata.MetadataUpdateStatus;
import com.atsumeru.api.utils.ServiceType;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXRadioButton;
import com.jfoenix.controls.JFXTextArea;
import com.jpro.webapi.WebAPI;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIconView;
import io.github.palexdev.materialfx.controls.MFXComboBox;
import io.github.palexdev.materialfx.controls.MFXProgressSpinner;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.converter.FloatStringConverter;
import kotlin.Pair;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;
import xyz.atsumeru.manager.FXApplication;
import xyz.atsumeru.manager.adapter.AtsumeruAdapter;
import xyz.atsumeru.manager.archive.ReadableContent;
import xyz.atsumeru.manager.archive.ZipArchive;
import xyz.atsumeru.manager.callback.OnContentEditCallback;
import xyz.atsumeru.manager.controller.MainController;
import xyz.atsumeru.manager.controller.dialogs.metadata.MetadataParserDialogController;
import xyz.atsumeru.manager.controller.tabs.TabAtsumeruLibraryController;
import xyz.atsumeru.manager.controls.DragNDropFilesView;
import xyz.atsumeru.manager.enums.*;
import xyz.atsumeru.manager.helpers.*;
import xyz.atsumeru.manager.listeners.OnDialogInputListener;
import xyz.atsumeru.manager.managers.Settings;
import xyz.atsumeru.manager.managers.TabPaneManager;
import xyz.atsumeru.manager.managers.TabsManager;
import xyz.atsumeru.manager.managers.WorkspaceManager;
import xyz.atsumeru.manager.models.ExtendedSerie;
import xyz.atsumeru.manager.models.content.Content;
import xyz.atsumeru.manager.source.AtsumeruSource;
import xyz.atsumeru.manager.utils.FXUtils;
import xyz.atsumeru.manager.utils.ReflectionUtils;
import xyz.atsumeru.manager.utils.ViewUtils;
import xyz.atsumeru.manager.utils.globalutils.*;
import xyz.atsumeru.manager.views.Snackbar;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static xyz.atsumeru.manager.controller.tabs.TabAtsumeruLibraryController.*;

public class ContentEditDialogController extends BaseDialogController<Serie> implements DragNDropFilesView.OnFilesSelected, ImageCache.ImageLoadCallback {
    private static final String DEFAULT_TEXT_FIELD_ID_PREFIX = "tf";

    @FXML
    public Pane contentRoot;
    @FXML
    public BorderPane contentEdit;
    @FXML
    public VBox contentSelect;
    @FXML
    public DragNDropFilesView dndFiles;
    @FXML
    public MFXProgressSpinner spinnerLoading;

    @FXML
    public Label lblHint;

    @FXML
    public AnchorPane coverPane;
    @FXML
    public ImageView shContentBackground;
    @FXML
    public ImageView shContentCover;
    @FXML
    public JFXButton btnChangeImage;
    @FXML
    public JFXButton btnRestoreImage;

    @FXML
    public MFXTextField tfTitle;
    @FXML
    public MFXTextField tfAltTitle;
    @FXML
    public MFXTextField tfVolume;
    @FXML
    public MFXTextField tfJapTitle;
    @FXML
    public MFXTextField tfKorTitle;
    @FXML
    public MFXTextField tfFolder;
    @FXML
    public MFXTextField tfAuthors;
    @FXML
    public MFXTextField tfArtists;
    @FXML
    public MFXTextField tfPublisher;
    @FXML
    public MFXComboBox<String> cbContentType;
    @FXML
    public MFXComboBox<String> cbStatus;
    @FXML
    public MFXComboBox<String> cbTransStatus;
    @FXML
    public MFXComboBox<String> cbCensorship;
    @FXML
    public MFXComboBox<String> cbColor;
    @FXML
    public JFXCheckBox cbMature;
    @FXML
    public JFXCheckBox cbAdult;
    @FXML
    public MFXTextField tfTranslators;
    @FXML
    public MFXTextField tfGenres;
    @FXML
    public MFXTextField tfTags;
    @FXML
    public MFXTextField tfYear;
    @FXML
    public MFXTextField tfCountry;
    @FXML
    public MFXTextField tfLanguages;
    @FXML
    public MFXTextField tfEvent;
    @FXML
    public MFXTextField tfMagazines;
    @FXML
    public MFXTextField tfCharacters;
    @FXML
    public MFXTextField tfParodies;
    @FXML
    public MFXTextField tfCircles;
    @FXML
    public MFXTextField tfRating;
    @FXML
    public MFXTextField tfScore;
    @FXML
    public JFXTextArea taDescription;

    @FXML
    public JFXTextArea taLinks;

    @FXML
    public JFXButton btnCancel;
    @FXML
    public JFXButton btnSave;
    @FXML
    public JFXButton btnFetchMetadata;

    @FXML
    public JFXRadioButton rbSaveToLinkedArchives;
    @FXML
    public JFXRadioButton rbSaveExternal;
    @FXML
    public JFXRadioButton rbSaveIntoDBOnly;
    @FXML
    public JFXRadioButton rbSaveSerieOnly;

    @FXML
    public ScrollPane spRoot;
    @FXML
    public VBox vbRoot;

    // Bound Services
    @FXML
    public MFXTextField tfMyAnimeList;
    @FXML
    public MFXTextField tfShikimori;
    @FXML
    public MFXTextField tfKitsu;
    @FXML
    public MFXTextField tfAniList;
    @FXML
    public MFXTextField tfMangaUpdates;
    @FXML
    public MFXTextField tfAnimePlanet;
    @FXML
    public MFXTextField tfComicVine;
    @FXML
    public MFXTextField tfComicsDB;
    @FXML
    public MFXTextField tfHentag;

    @FXML
    public MFXTextField tfMyAnimeListID;
    @FXML
    public MFXTextField tfShikimoriID;
    @FXML
    public MFXTextField tfKitsuID;
    @FXML
    public MFXTextField tfAniListID;
    @FXML
    public MFXTextField tfMangaUpdatesID;
    @FXML
    public MFXTextField tfAnimePlanetID;
    @FXML
    public MFXTextField tfComicVineID;
    @FXML
    public MFXTextField tfComicsDBID;
    @FXML
    public MFXTextField tfHentagID;

    @FXML
    MaterialDesignIconView btnLockTitle;
    @FXML
    MaterialDesignIconView btnLockAltTitle;
    @FXML
    MaterialDesignIconView btnLockJapTitle;
    @FXML
    MaterialDesignIconView btnLockKorTitle;
    @FXML
    MaterialDesignIconView btnLockAuthors;
    @FXML
    MaterialDesignIconView btnLockArtists;
    @FXML
    MaterialDesignIconView btnLockPublisher;
    @FXML
    MaterialDesignIconView btnLockCountry;
    @FXML
    MaterialDesignIconView btnLockYear;
    @FXML
    MaterialDesignIconView btnLockGenres;
    @FXML
    MaterialDesignIconView btnLockTags;
    @FXML
    MaterialDesignIconView btnLockLanguages;
    @FXML
    MaterialDesignIconView btnLockTranslators;
    @FXML
    MaterialDesignIconView btnLockCharacters;
    @FXML
    MaterialDesignIconView btnLockParodies;
    @FXML
    MaterialDesignIconView btnLockEvent;
    @FXML
    MaterialDesignIconView btnLockMagazines;
    @FXML
    MaterialDesignIconView btnLockCircles;
    @FXML
    MaterialDesignIconView btnLockDescription;

    @Setter
    private boolean isDialogMode;
    private boolean isContextMenuInitialized;
    private boolean isSerie;
    @Getter
    private Serie serie;

    @Nullable
    @Setter
    private OnContentEditCallback callback;

    private Tooltip currentCoverTooltip;

    private Disposable disposable;

    ChangeListener<String> tfFolderChangeListener;

    AtomicBoolean linkFieldOneTimeChangeLocker = new AtomicBoolean();
    AtomicBoolean idFieldOneTimeChangeLocker = new AtomicBoolean();

    BooleanProperty isShowSaveTypeCheckboxes = new SimpleBooleanProperty(true);

    public static void createAndShow(@Nullable Serie serie, ChangeListener<Serie> changeListener, boolean isSerie, @Nullable OnContentEditCallback callback) {
        Pair<Node, ContentEditDialogController> pair = FXUtils.loadFXML("/fxml/atsumeru/dialogs/ContentEditDialog.fxml");
        ContentEditDialogController controller = pair.getSecond();
        controller.setDialogMode(true);
        controller.setSerie(isSerie);
        controller.setContent(serie);
        controller.setCallback(callback);

        controller.show(serie, LocaleManager.getString("gui.metadata_editor"), pair.getFirst(), changeListener);
    }

    public void setData(Serie serie, boolean isSerie) {
        deleteTempMetadataZipFile();
        Platform.runLater(() -> dndFiles.clearSelectedFiles());
        setLocalData(serie, isSerie);
    }

    public void setLocalData(Serie serie, boolean isSerie) {
        this.serie = serie;
        this.isSerie = isSerie;
        Platform.runLater(this::resetViews);
        Platform.runLater(this::fillData);
    }

    private void resetViews() {
        spRoot.setVvalue(0.0);
        rbSaveExternal.setDisable(isTempMetadataZipFileExists());
        rbSaveIntoDBOnly.setDisable(isTempMetadataZipFileExists());
        rbSaveToLinkedArchives.setDisable(isTempMetadataZipFileExists());
    }

    @FXML
    protected void initialize() {
        initializeLazy();

        configureNodesAccentColor();

        lblHint.setText(LocaleManager.getString(WebAPI.isBrowser() ? "gui.metadata_editor.select_files_web" : "gui.metadata_editor.select_files"));
        dndFiles.setIcon(MaterialDesignIcon.ZIP_BOX);
        dndFiles.setCallback(this);

        rbSaveIntoDBOnly.setSelected(Settings.Atsumeru.isSaveMetadataIntoDBOnly());
        rbSaveExternal.setSelected(!Settings.Atsumeru.isSaveMetadataIntoLinkedArchives() && !Settings.Atsumeru.isSaveMetadataIntoDBOnly());

        rbSaveIntoDBOnly.selectedProperty().addListener((observable, oldValue, newValue) -> Settings.Atsumeru.putSaveMetadataIntoDBOnly(newValue));
        rbSaveToLinkedArchives.selectedProperty().addListener((observable, oldValue, newValue) -> Settings.Atsumeru.putSaveMetadataIntoLinkedArchives(newValue));

        rbSaveExternal.visibleProperty().bind(isShowSaveTypeCheckboxes);
        rbSaveExternal.managedProperty().bind(isShowSaveTypeCheckboxes);
        rbSaveIntoDBOnly.visibleProperty().bind(isShowSaveTypeCheckboxes);
        rbSaveIntoDBOnly.managedProperty().bind(isShowSaveTypeCheckboxes);
        rbSaveToLinkedArchives.visibleProperty().bind(isShowSaveTypeCheckboxes);
        rbSaveToLinkedArchives.managedProperty().bind(isShowSaveTypeCheckboxes);

        tfFolderChangeListener = (observable, oldValue, newValue) -> {
            tfFolder.textProperty().removeListener(tfFolderChangeListener);
            tfFolder.setText(oldValue);
            tfFolder.textProperty().addListener(tfFolderChangeListener);
        };

        if (!isDialogMode) {
            contentEdit.setOnDragEntered(event -> {
                contentEdit.getScene().getWindow().requestFocus();
                ViewUtils.setNodeGone(contentEdit);
                ViewUtils.setNodeVisible(contentSelect);
            });
            Platform.runLater(() -> contentEdit.getScene().getWindow().focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue && dndFiles.hasSelectedFiles()) {
                    ViewUtils.setNodeGone(contentSelect);
                    ViewUtils.setNodeVisible(contentEdit);
                }
            }));
        }

        ViewUtils.setNodeOnScroll(vbRoot, spRoot, 1);
        Platform.runLater(this::initializeBoundServicesFields);
        Platform.runLater(this::lockFields);
        Platform.runLater(this::fillData);
    }

    private void configureNodesAccentColor() {
        javafx.scene.paint.Color accentColor = javafx.scene.paint.Color.web(Settings.getAppAccentColor());
        taDescription.setFocusColor(accentColor);
        taLinks.setFocusColor(accentColor);

        cbMature.setCheckedColor(accentColor);
        cbAdult.setCheckedColor(accentColor);
    }

    private void initializeBoundServicesFields() {
        for (ServiceType serviceType : ServiceType.values()) {
            getServiceFieldsStream(serviceType).forEach(field -> {
                if (GUString.endsWithIgnoreCase(field.getId(), "ID")) {
                    field.textProperty().addListener((observable, oldValue, newValue) -> {
                        createBoundServiceFieldChangeListener(
                                serviceType,
                                "",
                                idFieldOneTimeChangeLocker,
                                linkFieldOneTimeChangeLocker,
                                () -> serviceType.createUrl(newValue)
                        );
                    });
                } else {
                    field.textProperty().addListener((observable, oldValue, newValue) -> {
                        createBoundServiceFieldChangeListener(
                                serviceType,
                                "ID",
                                linkFieldOneTimeChangeLocker,
                                idFieldOneTimeChangeLocker,
                                () -> serviceType.extractId(newValue)
                        );
                    });
                }
            });
        }
    }

    private Stream<MFXTextField> getServiceFieldsStream(@Nullable ServiceType serviceType) {
        return Arrays.stream(this.getClass().getDeclaredFields())
                .filter(field -> GUString.startsWithIgnoreCase(field.getName(), DEFAULT_TEXT_FIELD_ID_PREFIX + Optional.ofNullable(serviceType).map(Enum::name).orElse("")))
                .map(field -> ReflectionUtils.getAccessibleField(field, this))
                .filter(Objects::nonNull)
                .filter(obj -> obj instanceof MFXTextField)
                .map(MFXTextField.class::cast);
    }

    private void createBoundServiceFieldChangeListener(ServiceType serviceType, String fieldNameAppendix, AtomicBoolean currentFieldOneTimeLocker,
                                                       AtomicBoolean alternateFieldOneTimeChangeLocker, Supplier<String> fieldValueSupplier) {
        if (!currentFieldOneTimeLocker.get()) {
            findAndSetBoundServiceAlternateFieldValue(
                    serviceType,
                    fieldNameAppendix,
                    alternateFieldOneTimeChangeLocker,
                    fieldValueSupplier
            );
        }
        currentFieldOneTimeLocker.set(false);
    }

    private void findAndSetBoundServiceAlternateFieldValue(ServiceType serviceType, String fieldNameAppendix, AtomicBoolean alternateFieldOneTimeChangeLocker,
                                                           Supplier<String> fieldValueSupplier) {
        Arrays.stream(this.getClass().getDeclaredFields())
                .filter(field -> GUString.equalsIgnoreCase(field.getName(), DEFAULT_TEXT_FIELD_ID_PREFIX + serviceType + fieldNameAppendix))
                .map(field1 -> ReflectionUtils.getAccessibleField(field1, this))
                .filter(Objects::nonNull)
                .map(MFXTextField.class::cast)
                .findFirst()
                .ifPresent(field1 -> {
                    alternateFieldOneTimeChangeLocker.set(true);
                    String value = fieldValueSupplier.get();
                    if (GUString.isNotEmpty(value)) {
                        field1.setText(value);
                    } else {
                        field1.clear();
                    }
                });
    }

    private void lockFields() {
        Arrays.stream(this.getClass().getDeclaredFields())
                .filter(field -> GUString.startsWithIgnoreCase(field.getName(), "btnLock"))
                .map(field -> ReflectionUtils.getAccessibleField(field, this))
                .filter(Objects::nonNull)
                .map(MaterialDesignIconView.class::cast)
                .forEach(this::setButtonLocked);
    }

    private void setButtonLocked(MaterialDesignIconView mdiv) {
        setLockButtonTooltip(mdiv);
        setLockButtonOnMouseHover(mdiv);
        if (Settings.Atsumeru.isButtonLocked(getLockButtonName(mdiv))) {
            mdiv.setGlyphName("LOCK");
            mdiv.setOpacity(1.0);
        } else {
            mdiv.setGlyphName("LOCK_OPEN");
            mdiv.setOpacity(0.1);
        }
    }

    private void setLockButtonTooltip(MaterialDesignIconView mdiv) {
        ViewUtils.addTooltipToNode(mdiv, LocaleManager.getString("gui.metadata_lock_field"), 12);
    }

    private void setLockButtonOnMouseHover(MaterialDesignIconView mdiv) {
        mdiv.setOnMouseEntered(event -> mdiv.setOpacity(1.0));
        mdiv.setOnMouseExited(event -> {
            if (!Settings.Atsumeru.isButtonLocked(getLockButtonName(mdiv))) {
                mdiv.setOpacity(0.1);
            }
        });
    }

    @SuppressWarnings("ConstantConditions")
    private void fillData() {
        FXUtils.requestFocus(contentRoot);
        if (serie == null) {
            ViewUtils.setNodeGone(contentEdit, spinnerLoading);
            ViewUtils.setNodeVisible(contentSelect);
        } else {
            ViewUtils.setNodeGone(contentSelect, spinnerLoading);
            ViewUtils.setNodeVisible(contentEdit);
        }

        preFillTasks();
        if (serie != null) {
            loadCover(serie);

            setComboBoxItems(
                    ContentType.class,
                    Arrays.asList(ContentType.values()),
                    cbContentType,
                    Optional.ofNullable(serie.getContentType())
                            .filter(GUString::isNotEmpty)
                            .orElse(ContentType.UNKNOWN.name())
            );
            setComboBoxItems(
                    Status.class,
                    Status.getReadableStatuses(),
                    cbStatus,
                    Optional.ofNullable(serie.getStatus())
                            .filter(GUString::isNotEmpty)
                            .orElse(Status.UNKNOWN.name())
            );
            setComboBoxItems(
                    TranslationStatus.class,
                    Arrays.asList(TranslationStatus.values()),
                    cbTransStatus,
                    Optional.ofNullable(serie.getTranslationStatus())
                            .filter(GUString::isNotEmpty)
                            .orElse(TranslationStatus.UNKNOWN.name())
            );
            setComboBoxItems(
                    Censorship.class,
                    Arrays.asList(Censorship.values()),
                    cbCensorship,
                    Optional.ofNullable(serie.getCensorship())
                            .filter(GUString::isNotEmpty)
                            .orElse(Censorship.UNKNOWN.name())
            );
            setComboBoxItems(
                    Color.class,
                    Arrays.asList(Color.values()),
                    cbColor,
                    Optional.ofNullable(serie.getColor())
                            .filter(GUString::isNotEmpty)
                            .orElse(Color.UNKNOWN.name())
            );

            configureMatureAdultCheckBoxes(serie.isMature(), serie.isAdult());
            configureVolumeAndRatingFields(!isSerie ? serie.getVolume() : serie.getVolumesCount(), serie.getRating());

            ViewUtils.setNotEmptyTextToTextInput(tfTitle, serie.getTitle());
            ViewUtils.setNotEmptyTextToTextInput(tfAltTitle, serie.getAltTitle());
            ViewUtils.setNotEmptyTextToTextInput(tfJapTitle, serie.getJapTitle());
            ViewUtils.setNotEmptyTextToTextInput(tfKorTitle, serie.getKorTitle());
            ViewUtils.setNotEmptyTextToTextInput(tfFolder, serie.getFolder());
            ViewUtils.setNotEmptyTextToTextInput(tfAuthors, GUString.join(",", serie.getAuthors()));
            ViewUtils.setNotEmptyTextToTextInput(tfArtists, GUString.join(",", serie.getArtists()));
            ViewUtils.setNotEmptyTextToTextInput(tfPublisher, serie.getPublisher());
            ViewUtils.setNotEmptyTextToTextInput(tfTranslators, GUString.join(",", serie.getTranslators()));
            ViewUtils.setNotEmptyTextToTextInput(tfGenres, GUString.join(",", AtsumeruAdapter.toLocalizedGenre(serie.getGenres())));
            ViewUtils.setNotEmptyTextToTextInput(tfTags, GUString.join(",", serie.getTags()));
            ViewUtils.setNotEmptyTextToTextInput(tfYear, serie.getYear());
            ViewUtils.setNotEmptyTextToTextInput(tfCountry, serie.getCountry());
            ViewUtils.setNotEmptyTextToTextInput(tfLanguages, GUString.join(",", serie.getLanguages()));
            ViewUtils.setNotEmptyTextToTextInput(tfEvent, serie.getEvent());
            ViewUtils.setNotEmptyTextToTextInput(tfMagazines, GUString.join(",", serie.getMagazines()));
            ViewUtils.setNotEmptyTextToTextInput(tfCharacters, GUString.join(",", serie.getCharacters()));
            ViewUtils.setNotEmptyTextToTextInput(tfParodies, GUString.join(",", serie.getParodies()));
            ViewUtils.setNotEmptyTextToTextInput(tfCircles, GUString.join(",", serie.getCircles()));
            ViewUtils.setNotEmptyTextToTextInput(tfScore, serie.getScore());
            ViewUtils.setNotEmptyTextToTextInput(taDescription, serie.getDescription());
            ViewUtils.setNotEmptyTextToTextInput(taLinks, GUString.join("\n", serie.getLinks()
                    .stream()
                    .map(Link::getLink)
                    .collect(Collectors.toList())));

            if (GUString.isEmpty(serie.getFolder()) || isTempMetadataZipFileExists()) {
                ViewUtils.setNodeGone(tfFolder);
            }

            fillBoundServicesFields(serie.getBoundServices());

            postFillTasks();
        } else {
            cbContentType.getSelectionModel().selectFirst();
            cbStatus.getSelectionModel().selectFirst();
            cbTransStatus.getSelectionModel().selectFirst();
            cbCensorship.getSelectionModel().selectFirst();
            cbColor.getSelectionModel().selectFirst();

            cbMature.setSelected(false);
            cbAdult.setSelected(false);

            taDescription.clear();
            taLinks.clear();

            getServiceFieldsStream(null).forEach(MFXTextField::clear);
        }
    }

    private void fillData(Content item, boolean overrideFields) {
        preFillTasks();

        setComboBoxItems(
                ContentType.class,
                Arrays.asList(ContentType.values()),
                cbContentType,
                Optional.ofNullable(item.getInfo().getContentType())
                        .map(Enum::toString)
                        .orElse(ContentType.UNKNOWN.name())
        );
        setComboBoxItems(
                Status.class,
                Status.getReadableStatuses(),
                cbStatus,
                Optional.ofNullable(item.getInfo().getStatus())
                        .map(Enum::toString)
                        .orElse(Status.UNKNOWN.name())
        );
        setComboBoxItems(
                TranslationStatus.class,
                Arrays.asList(TranslationStatus.values()),
                cbTransStatus,
                Optional.ofNullable(item.getInfo().getMangaTranslationStatus())
                        .map(Enum::toString)
                        .orElse(TranslationStatus.UNKNOWN.name())
        );
        setComboBoxItems(
                Censorship.class,
                Arrays.asList(Censorship.values()),
                cbCensorship,
                Optional.ofNullable(item.getInfo().getCensorship())
                        .map(Enum::toString)
                        .orElse(Censorship.UNKNOWN.name())
        );
        setComboBoxItems(
                Color.class,
                Arrays.asList(Color.values()),
                cbColor,
                Optional.ofNullable(item.getInfo().getColor())
                        .map(Enum::toString)
                        .orElse(Color.UNKNOWN.name())
        );

        configureMatureAdultCheckBoxes(item.getInfo().isMature(), item.getInfo().isAdult());
        configureVolumeAndRatingFields(GUType.getFloatDef(tfVolume.getText(), -1), GUType.getIntDef(item.getRating(), 0));

        fillLockableTextInput(tfTitle, item.getTitle(), overrideFields);
        fillLockableTextInput(tfAltTitle, item.getAltTitle(), overrideFields);
        fillLockableTextInput(tfJapTitle, item.getJapTitle(), overrideFields);
        fillLockableTextInput(tfKorTitle, item.getKoreanTitle(), overrideFields);
        fillLockableTextInput(tfAuthors, item.getInfo().getAuthors(), overrideFields);
        fillLockableTextInput(tfArtists, item.getInfo().getArtists(), overrideFields);
        fillLockableTextInput(tfPublisher, item.getInfo().getPublisher(), overrideFields);
        fillLockableTextInput(tfTranslators, item.getInfo().getTranslators(), overrideFields);
        fillLockableTextInput(tfGenres, item.getInfo().getGenres(), overrideFields);
        fillLockableTextInput(tfTags, item.getInfo().getTags(), overrideFields);
        fillLockableTextInput(tfYear, item.getInfo().getYear(), overrideFields);
        fillLockableTextInput(tfCountry, item.getInfo().getCountry(), overrideFields);
        fillLockableTextInput(tfLanguages, GUArray.splitString(item.getInfo().getLanguage()), overrideFields);
        fillLockableTextInput(tfEvent, item.getInfo().getEvent(), overrideFields);
        fillLockableTextInput(tfMagazines, item.getInfo().getMagazines(), overrideFields);
        fillLockableTextInput(tfCharacters, item.getInfo().getCharacters(), overrideFields);
        fillLockableTextInput(tfParodies, item.getInfo().getParodies(), overrideFields);
        fillLockableTextInput(tfCircles, item.getInfo().getCircles(), overrideFields);
        fillLockableTextInput(tfScore, item.getScore(), overrideFields);
        fillLockableTextInput(taDescription, item.getDescription(), overrideFields);

        postFillTasks();
    }

    private void fillLockableTextInput(TextInputControl textInputControl, List<String> values, boolean overrideFields) {
        if (!Settings.Atsumeru.isButtonLocked(getTextInputLockButtonName(textInputControl)) && (textInputControl.textProperty().isEmpty().get() || overrideFields)) {
            ViewUtils.setNotEmptyTextToTextInput(textInputControl, GUString.join(",", values));
        }
    }

    private void fillLockableTextInput(TextInputControl textInputControl, String value, boolean overrideFields) {
        if (!Settings.Atsumeru.isButtonLocked(getTextInputLockButtonName(textInputControl)) && (textInputControl.textProperty().isEmpty().get() || overrideFields)) {
            ViewUtils.setNotEmptyTextToTextInput(textInputControl, value);
        }
    }

    private void preFillTasks() {
        removeAutocompletionFields();
        makeFolderTextFieldModifiable();
        if (isSerie) {
            tfVolume.setDisable(true);
            tfVolume.setFloatingText(LocaleManager.getString("details.manga.volumes").replace(":", ""));
        }

    }

    private void postFillTasks() {
        bindAutocompletionFields();
        addTextFieldContextMenuItems();
        configureBottomButtons();
        makeFolderTextFieldNotModifiable();
    }

    private void fillBoundServicesFields(List<BoundService> boundServices) {
        for (BoundService boundService : boundServices) {
            getServiceFieldsStream(boundService.getServiceType()).forEach(field -> field.setText(GUString.endsWithIgnoreCase(field.getId(), "ID") ? boundService.getId() : boundService.getLink()));
        }
    }

    private <E extends Enum<E>> void setComboBoxItems(Class<E> classE, List<E> enums, Control comboBox, String value) {
        setComboBoxItems(
                comboBox,
                enums.stream()
                        .map(GUEnum::getEnumLocalizedString)
                        .toArray(String[]::new),
                GUEnum.getEnumLocalizedString(GUEnum.valueOf(classE, value))
        );
    }

    private void removeAutocompletionFields() {
        AutocompletionHelper.removeListeners(tfAuthors);
        AutocompletionHelper.removeListeners(tfArtists);
        AutocompletionHelper.removeListeners(tfPublisher);
        AutocompletionHelper.removeListeners(tfTranslators);
        AutocompletionHelper.removeListeners(tfGenres);
        AutocompletionHelper.removeListeners(tfTags);
        AutocompletionHelper.removeListeners(tfYear);
        AutocompletionHelper.removeListeners(tfCountry);
        AutocompletionHelper.removeListeners(tfLanguages);
        AutocompletionHelper.removeListeners(tfEvent);
        AutocompletionHelper.removeListeners(tfMagazines);
        AutocompletionHelper.removeListeners(tfCharacters);
        AutocompletionHelper.removeListeners(tfParodies);
        AutocompletionHelper.removeListeners(tfCircles);
    }

    private void bindAutocompletionFields() {
        TabAtsumeruLibraryController controller = TabsManager.getTabController(TabAtsumeruLibraryController.class);
        if (controller != null && GUArray.isNotEmpty(controller.getAllFiltersMap())) {
            AutocompletionHelper.bindMultiAutocompletion(tfAuthors, controller.getAllFiltersMap().get(AUTHORS_TAG).keySet());
            AutocompletionHelper.bindMultiAutocompletion(tfArtists, controller.getAllFiltersMap().get(ARTISTS_TAG).keySet());
            AutocompletionHelper.bindSingleAutocompletion(tfPublisher, controller.getAllFiltersMap().get(PUBLISHERS_TAG).keySet());
            AutocompletionHelper.bindMultiAutocompletion(tfTranslators, controller.getAllFiltersMap().get(TRANSLATORS_TAG).keySet());
            AutocompletionHelper.bindMultiAutocompletion(tfGenres, new HashSet<>(AtsumeruAdapter.toLocalizedGenre(Genre.values())));
            AutocompletionHelper.bindMultiAutocompletion(tfTags, controller.getAllFiltersMap().get(TAGS_TAG).keySet());
            AutocompletionHelper.bindSingleAutocompletion(tfYear, controller.getAllFiltersMap().get(YEARS_TAG).keySet());
            AutocompletionHelper.bindSingleAutocompletion(tfCountry, controller.getAllFiltersMap().get(COUNTRIES_TAG).keySet());
            AutocompletionHelper.bindMultiAutocompletion(tfLanguages, controller.getAllFiltersMap().get(LANGUAGES_TAG).keySet());
            AutocompletionHelper.bindSingleAutocompletion(tfEvent, controller.getAllFiltersMap().get(EVENTS_TAG).keySet());
            AutocompletionHelper.bindMultiAutocompletion(tfMagazines, controller.getAllFiltersMap().get(MAGAZINES_TAG).keySet());
            AutocompletionHelper.bindMultiAutocompletion(tfCharacters, controller.getAllFiltersMap().get(CHARACTERS_TAG).keySet());
            AutocompletionHelper.bindMultiAutocompletion(tfParodies, controller.getAllFiltersMap().get(PARODIES_TAG).keySet());
            AutocompletionHelper.bindMultiAutocompletion(tfCircles, controller.getAllFiltersMap().get(CIRCLES_TAG).keySet());
        }
    }

    private void addTextFieldContextMenuItems() {
        if (isDialogMode || !isContextMenuInitialized) {
            TextFieldContextMenuHelper.addMenuItemsIntoDefaultMenu(tfTitle, tfAltTitle, tfJapTitle, tfKorTitle, tfAuthors,
                    tfArtists, tfPublisher, tfTranslators, tfGenres, tfTags, tfYear, tfCountry, tfLanguages, tfEvent,
                    tfMagazines, tfCharacters, tfParodies, tfCircles, tfRating, tfScore);
            TextFieldContextMenuHelper.localizeDefaultMenu(taLinks, taDescription, tfFolder,
                    // Combo Boxes
                    cbContentType, cbStatus, cbTransStatus, cbCensorship, cbColor,
                    // Bound Services
                    tfMyAnimeList, tfShikimori, tfKitsu, tfAniList, tfMangaUpdates, tfAnimePlanet, tfComicVine, tfComicsDB, tfHentag,
                    tfMyAnimeListID, tfShikimoriID, tfKitsuID, tfAniListID, tfMangaUpdatesID, tfAnimePlanetID, tfComicVineID, tfComicsDBID, tfHentagID);
            isContextMenuInitialized = true;
        }
    }

    private void configureMatureAdultCheckBoxes(boolean isMature, boolean isAdult) {
        cbMature.setSelected(isMature);
        cbAdult.setSelected(isAdult);

        setCheckBoxExcludeListeners(cbMature, cbAdult);
        setCheckBoxExcludeListeners(cbAdult, cbMature);
    }

    private void configureBottomButtons() {
        btnCancel.setOnMouseClicked(event -> notifyChangeAndCloseDialog(false));
        btnSave.setOnMouseClicked(event -> saveMetadata(dndFiles.hasSelectedFiles()));
        btnFetchMetadata.setOnMouseClicked(event -> {
            if (FXApplication.getParsersManager().isSupportsMetadataFetching()) {
                MetadataParserDialogController.createAndShow(
                        new OnDialogInputListener() {
                            @Override
                            public void onConfirmInput(String input, boolean overrideFields) {
                                fetchMetadata(input, null, overrideFields);
                            }

                            @Override
                            public void onConfirmInput(String input, String secondInput, boolean overrideFields) {
                                fetchMetadata(input, secondInput, overrideFields);
                            }
                        });
            } else {
                Snackbar.showSnackBar(
                        contentRoot,
                        LocaleManager.getString(WebAPI.isBrowser() ? "gui.error.unsupported_in_web" : "gui.error.unsupported_in_open_source"),
                        Snackbar.Type.ERROR
                );
            }
        });
    }

    private void resetEditMode() {
        if (dndFiles.hasSelectedFiles()) {
            finishLocalEditing();
        } else {
            TabPaneManager.selectHomeTab();
        }
    }

    private void makeFolderTextFieldModifiable() {
        tfFolder.textProperty().removeListener(tfFolderChangeListener);
    }

    private void makeFolderTextFieldNotModifiable() {
        tfFolder.textProperty().removeListener(tfFolderChangeListener);
        tfFolder.textProperty().addListener(tfFolderChangeListener);
        tfFolder.addEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, Event::consume);
    }

    private void configureVolumeAndRatingFields(float volume, int rating) {
        setTextFieldNumericOnly(tfVolume, Math.max(volume, -1), true);
        if (volume < 0) {
            tfVolume.clear();
        }

        setTextFieldNumericOnly(tfRating, Math.max(rating, 0), false);
        if (rating < 0) {
            tfRating.clear();
        }
    }

    private void fetchMetadata(String input, String secondInput, boolean overrideFields) {
        if (GUString.isNotEmpty(input)) {
            FXApplication.getParsersManager().fetchMetadata(input, secondInput, tfComicsDB, tfComicVine, this::addLinkIntoLinksTextField, (success, newItem) -> {
                if (success) {
                    addLinkIntoLinksTextField(input);
                    fillData(newItem, overrideFields);

                    Snackbar.showSnackBar(contentRoot,
                            LocaleManager.getStringFormatted("gui.snackbar.fetch_metadata.success", newItem.getTitle()),
                            Snackbar.Type.SUCCESS);
                } else {
                    Snackbar.showSnackBar(contentRoot,
                            LocaleManager.getString("gui.snackbar.fetch_metadata.error"),
                            Snackbar.Type.ERROR);
                }
            });
        }
    }

    private void setTextFieldNumericOnly(TextField textField, float defaultValue, boolean allowDot) {
        UnaryOperator<TextFormatter.Change> integerFilter = change -> {
            String newText = change.getText();
            if (newText.matches(allowDot ? "-?([0-9]*)?|(\\d+\\.\\d+)?" : "-?([0-9]*)?")) {
                return change;
            }
            return null;
        };

        textField.setTextFormatter(new TextFormatter<>(new FloatStringConverter(), defaultValue, integerFilter));
    }

    private void setCheckBoxExcludeListeners(JFXCheckBox firstCB, JFXCheckBox secondCB) {
        firstCB.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                secondCB.setSelected(false);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void setComboBoxItems(Control control, String[] items, String selectedItem) {
        ObservableList<String> types = FXCollections.observableArrayList(items);
        if (control instanceof ComboBox) {
            ComboBox<String> comboBox = (ComboBox<String>) control;
            comboBox.setItems(types);
            comboBox.getSelectionModel().clearAndSelect(types.indexOf(selectedItem));
        } else if (control instanceof MFXComboBox) {
            MFXComboBox<String> comboBox = (MFXComboBox<String>) control;
            comboBox.getItems().clear();
            comboBox.getItems().addAll(types);
            comboBox.getSelectionModel().selectItem(selectedItem);
        }
    }

    private void loadCover(Serie serie) {
        //В новом потоке получаем изображение из кеша (или сначала грузим его туда и уже потом берем из кеша) и отображаем
        //его в Rectangle Shape. Добавляем тень с заокругливанием
        if (GUString.isNotEmpty(serie.getCover()) && !isTempMetadataZipFileExists()) {
            ImageCache.create(AtsumeruAdapter.getCoverHash(serie.getCover()), AtsumeruAdapter.getCoverLink(serie.getCover()))
                    .withContentId(serie.getId())
                    .withCacheType(ImageCache.ImageCacheType.THUMBNAIL)
                    .withPreserveRatio()
                    .withSmooth()
                    .withHeaders(AtsumeruSource.createAuthorizationHeaders())
                    .withBackgroundLoadingFromFS()
                    .withCallback(this)
                    .getAsync();
        } else {
            setImage(ImageCache.PLACEHOLDER_IMAGE);
        }
    }

    private void addLinkIntoLinksTextField(String link) {
        if (!taLinks.getText().contains(link)) {
            taLinks.appendText("\n" + link);
        }
    }

    private void saveMetadata(boolean isLocalEditing) {
        serie.setLinks(
                Arrays.stream(taLinks.getText().split("\\n"))
                        .map(link -> {
                            Link links = new Link();
                            links.setSource(GULinks.getHostName(link));
                            links.setLink(link);
                            return links;
                        })
                        .collect(Collectors.toList())
        );

        serie.setTitle(tfTitle.getText().trim());
        serie.setAltTitle(tfAltTitle.getText().trim());
        if (!tfVolume.isDisable()) {
            serie.setVolume(GUType.getFloatDef(tfVolume.getText().trim(), -1f));
        }
        serie.setJapTitle(tfJapTitle.getText().trim());
        serie.setKorTitle(tfKorTitle.getText().trim());

        serie.setPublisher(tfPublisher.getText().trim());
        serie.setYear(tfYear.getText().trim());
        serie.setCountry(tfCountry.getText().trim());
        serie.setEvent(tfEvent.getText().trim());
        serie.setEvent(tfEvent.getText().trim());
        serie.setRating(GUType.getIntDef(tfRating.getText().trim(), 0));
        serie.setScore(tfScore.getText().trim());
        serie.setDescription(taDescription.getText().trim());

        List<String> genreIds = new ArrayList<>();
        GUArray.splitString(tfGenres.getText().trim(), ",").forEach(genreStr -> {
            Genre genre = Genre.getGenreFromString(genreStr.trim());
            if (genre != null) {
                genreIds.add(String.valueOf(genre.ordinal()));
            }
        });
        serie.setGenres(genreIds);

        serie.setAuthors(arrayValuesToList(getArrayValues(tfAuthors)));
        serie.setArtists(arrayValuesToList(getArrayValues(tfArtists)));
        serie.setTranslators(arrayValuesToList(getArrayValues(tfTranslators)));
        serie.setTags(arrayValuesToList(getArrayValues(tfTags)));
        serie.setLanguages(arrayValuesToList(getArrayValues(tfLanguages)));
        serie.setMagazines(arrayValuesToList(getArrayValues(tfMagazines)));
        serie.setCharacters(arrayValuesToList(getArrayValues(tfCharacters)));
        serie.setParodies(arrayValuesToList(getArrayValues(tfParodies)));
        serie.setCircles(arrayValuesToList(getArrayValues(tfCircles)));

        serie.setContentType(Arrays.asList(ContentType.values()).get(cbContentType.getSelectionModel().getSelectedIndex()).name());
        serie.setStatus(Status.getReadableStatuses().get(cbStatus.getSelectionModel().getSelectedIndex()).name());
        serie.setTranslationStatus(Arrays.asList(TranslationStatus.values()).get(cbTransStatus.getSelectionModel().getSelectedIndex()).name());
        serie.setCensorship(Arrays.asList(Censorship.values()).get(cbCensorship.getSelectionModel().getSelectedIndex()).name());
        serie.setColor(Arrays.asList(Color.values()).get(cbColor.getSelectionModel().getSelectedIndex()).name());

        serie.setMature(cbMature.isSelected());
        serie.setAdult(cbAdult.isSelected());

        fillSerieBoundServices();

        if (isLocalEditing) {
            boolean success = DialogsHelper.showProgressDialog(
                    LocaleManager.getString("gui.dialog.saving_metadata"),
                    LocaleManager.getString("gui.dialog.saving_metadata.header"), createSaveMetadataWorker());
            if (success) {
                unpackTempMetadataZipIfPresent();
                finishLocalEditing();
            }

            Snackbar.showSnackBar(MainController.getSnackbarRoot(),
                    success
                            ? LocaleManager.getString("gui.dialog.saving_metadata.success")
                            : LocaleManager.getString("gui.dialog.saving_metadata.error"),
                    success
                            ? Snackbar.Type.SUCCESS
                            : Snackbar.Type.ERROR);
        } else {
            patchSerieOnRemoteServer(rbSaveSerieOnly.isSelected(), rbSaveToLinkedArchives.isSelected(), rbSaveIntoDBOnly.isSelected());
        }
    }

    private void fillSerieBoundServices() {
        List<BoundService> boundServices = new ArrayList<>();
        for (ServiceType serviceType : ServiceType.values()) {
            BoundService boundService = new BoundService(serviceType, null, null);
            getServiceFieldsStream(serviceType).forEach(field -> {
                if (GUString.endsWithIgnoreCase(field.getId(), "ID")) {
                    boundService.setId(field.getText());
                } else {
                    boundService.setLink(field.getText());
                }
            });

            if (GUString.isNotEmpty(boundService.getId()) && GUString.isNotEmpty(boundService.getLink())) {
                boundServices.add(boundService);
            }
        }

        serie.setBoundServices(boundServices);
    }

    public Task<Boolean> createSaveMetadataWorker() {
        return new Task<>() {
            @Override
            protected Boolean call() {
                boolean success = ReadableContent.saveMetadata((ExtendedSerie) serie, true);

                List<File> files = dndFiles.getSelectedFiles();
                File tempFile = createTempMetadataZipFile();
                if (tempFile.exists()) {
                    files = Collections.singletonList(tempFile);
                }

                for (int i = 1; i < files.size(); i++) {
                    File archive = files.get(i);
                    try {
                        ExtendedSerie newSerie = ReadableContent.create(((ExtendedSerie) serie).getSerieId(), archive.toString(), false).getSerie();
                        serie.setId(newSerie.getId());
                        serie.setFolder(newSerie.getFolder());
                        serie.setVolume(newSerie.getVolume());
                        success = success && ReadableContent.saveMetadata((ExtendedSerie) serie, true);
                    } catch (Exception ex) {
                        return false;
                    }
                }
                return success;
            }
        };
    }

    private void patchSerieOnRemoteServer(boolean serieOnly, boolean saveIntoArchives, boolean saveIntoDBOnly) {
        disposable = AtsumeruAPI.updateMetadata(serie, serieOnly, saveIntoArchives, saveIntoDBOnly)
                .cache()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(message -> {
                    if (message.isOk()) {
                        Platform.runLater(() -> DialogsHelper.showProgressDialog(
                                LocaleManager.getString("gui.dialog.saving_metadata"),
                                LocaleManager.getString("gui.dialog.saving_metadata.header"), createUpdateMetadataUpdateWorker()));
                    } else {
                        onError(message.getMessage());
                    }
                }, throwable -> onError(throwable.getMessage()));
    }

    private void onError(String message) {
        Snackbar.showSnackBar(
                MainController.getSnackbarRoot(),
                LocaleManager.getStringFormatted("atsumeru.unable_to_start_metadata_update", message),
                Snackbar.Type.ERROR
        );

        if (disposable != null) {
            disposable.dispose();
        }
    }

    public Task<Boolean> createUpdateMetadataUpdateWorker() {
        return new Task<>() {
            @Override
            protected Boolean call() {
                MetadataUpdateStatus status;
                do {
                    status = AtsumeruAPI.getMetadataUpdateStatus().blockingGet();
                    updateMessage(String.format("%d / %d", status.getUpdated(), status.getTotal()));
                    updateProgress(status.getUpdated(), status.getTotal());
                } while (status.getPercent() < 100 && status.getTotal() > 0);
                notifyChangeAndCloseDialog(true);
                return true;
            }
        };
    }

    private String[] getArrayValues(TextField textField) {
        String text = textField.getText().trim();
        return GUString.isNotEmpty(text) ? text.split(",", -1) : new String[0];
    }

    private List<String> arrayValuesToList(String[] values) {
        return new ArrayList<>(Arrays.asList(values));
    }

    private void setContentCover(Image contentCover) {
        boolean isLandscapeImage = contentCover.getHeight() < contentCover.getWidth();
        shContentCover.setImage(contentCover);
        shContentCover.setEffect(LayoutHelpers.COVER_ADJUST_DEFAULT);
        shContentCover.setPreserveRatio(isLandscapeImage);

        if (isLandscapeImage) {
            shContentBackground.setImage(contentCover);
            shContentBackground.setImage(contentCover);
            shContentBackground.setEffect(LayoutHelpers.BACKGROUND_ADJUST_BOX_BLUR);
            ViewUtils.centerImage(shContentCover);
            ViewUtils.setNodeVisibleAndManaged(true, true, shContentBackground);
        } else {
            ViewUtils.resetImagePosition(shContentCover);
        }
    }

    protected void setSerie(boolean isSerie) {
        this.isSerie = isSerie;
    }

    protected void setContent(Serie serie) {
        this.serie = serie;
    }

    private void finishLocalEditing() {
        serie = null;
        deleteTempMetadataZipFile();
        dndFiles.clearSelectedFiles();
        fillData();
    }

    private void unpackTempMetadataZipIfPresent() {
        File tempMetadataZipFile = createTempMetadataZipFile();
        if (tempMetadataZipFile.exists()) {
            List<File> files = dndFiles.getSelectedFiles();
            files.sort(Comparator.naturalOrder());
            File firstFile = files.get(0);

            ZipArchive.unpack(tempMetadataZipFile, firstFile.getParentFile());
        }
    }

    private void deleteTempMetadataZipFile() {
        createTempMetadataZipFile().delete();
    }

    private boolean isJsonMetadataFile(File file) {
        String extension = GUFile.getFileExt(file.getAbsolutePath());
        return GUString.equalsIgnoreCase(extension, "json");
    }

    private File createTempMetadataZipFile() {
        return new File(WorkspaceManager.TMP_DIR, "info_tmp.zip");
    }

    private boolean isTempMetadataZipFileExists() {
        return createTempMetadataZipFile().exists();
    }

    private void notifyChangeAndCloseDialog(boolean success) {
        Platform.runLater(() -> {
            if (!dndFiles.hasSelectedFiles()) {
                TabAtsumeruLibraryController.notifyItemUpdate(Collections.singletonList(serie));
            }
            if (success && callback != null) {
                callback.onEdit();
            }
            if (isDialogMode) {
                property.setValue(null);
                property.setValue(serie);
                close();
            } else {
                resetEditMode();
            }
        });
    }

    private Rectangle createRectangle() {
        Rectangle rectangle = new Rectangle();
        rectangle.setArcHeight(16);
        rectangle.setArcWidth(16);
        rectangle.setWidth(200.0);
        rectangle.setHeight(280);

        return rectangle;
    }

    private void initializeLazy() {
        Platform.runLater(() -> {
            if (!isDialogMode) {
                try {
                    super.initialize();
                } catch (IOException ignored) {
                }
            }
        });
    }

    private void setImage(Image image) {
        setContentCover(image);
        coverPane.setClip(createRectangle());
        FXUtils.runLaterDelayed(() -> {
            ViewUtils.removeTooltipFromNode(coverPane, currentCoverTooltip);
            currentCoverTooltip = ViewUtils.addCoverTooltip(coverPane, image, 400);
        }, 300);

        // TODO: 23.07.2021 blur on 18+ !!!
        //        shContentCover.setEffect(LayoutHelpers.COVER_ADJUST_BOX_BLUR);
    }

    @Override
    public void onLoad(Image image, String contentId, boolean fromCache, boolean loadNow) {
        Platform.runLater(() -> setImage(image));
    }

    @Override
    public void onFilesSelected(List<File> selectedFiles, List<File> declinedFiles) {
        ViewUtils.setNodeGone(contentSelect);
        ViewUtils.setNodeVisible(spinnerLoading);
        if (GUArray.isNotEmpty(selectedFiles)) {
            Single.just(selectedFiles)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(Schedulers.io())
                    .subscribe(files -> {
                        deleteTempMetadataZipFile();

                        files.sort(Comparator.naturalOrder());
                        File firstFile = files.get(0);

                        if (isJsonMetadataFile(firstFile)) {
                            File tempFile = createTempMetadataZipFile();
                            ZipArchive.packSingleFile(firstFile, tempFile.getAbsolutePath());
                            firstFile = tempFile;
                        }

                        try {
                            ReadableContent content = ReadableContent.create(null, firstFile.toString(), false);
                            setLocalData(content.getSerie(), selectedFiles.size() > 1);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Snackbar.showSnackBar(MainController.getSnackbarRoot(), "Unable to read archive: " + e.getMessage(), Snackbar.Type.ERROR);
                        }
                    }, throwable -> {
                        throwable.printStackTrace();
                        Snackbar.showSnackBar(MainController.getSnackbarRoot(), "Unable to read archive: " + throwable.getMessage(), Snackbar.Type.ERROR);
                    });
        }
    }

    @Override
    public void onFilesCleared() {
    }

    private String getTextInputLockButtonName(TextInputControl tic) {
        return tic.getId()
                .replaceAll("^tf", "")
                .replaceAll("^ta", "");
    }

    private String getLockButtonName(MaterialDesignIconView mdiv) {
        return mdiv.getId().replaceAll("^btnLock", "");
    }

    @FXML
    public void onLockClick(MouseEvent event) {
        MaterialDesignIconView mdiv = (MaterialDesignIconView) event.getPickResult().getIntersectedNode();
        String buttonName = getLockButtonName(mdiv);
        Settings.Atsumeru.putButtonLocked(buttonName, !Settings.Atsumeru.isButtonLocked(buttonName));
        setButtonLocked(mdiv);
    }

    @Override
    protected int minDialogWidth() {
        return 1400;
    }
}
