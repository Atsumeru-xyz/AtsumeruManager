package xyz.atsumeru.manager.controller.stages;

import com.atsumeru.api.model.server.Server;
import com.jpro.webapi.WebAPI;
import io.github.palexdev.materialfx.controls.MFXProgressSpinner;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.Setter;
import xyz.atsumeru.manager.FXApplication;
import xyz.atsumeru.manager.callback.ChapterReadProgressCallback;
import xyz.atsumeru.manager.controller.BaseController;
import xyz.atsumeru.manager.helpers.LocaleManager;
import xyz.atsumeru.manager.managers.MangaReaderCommunicationManager;
import xyz.atsumeru.manager.managers.Settings;
import xyz.atsumeru.manager.models.manga.Chapter;
import xyz.atsumeru.manager.source.AtsumeruSource;
import xyz.atsumeru.manager.utils.FXUtils;
import xyz.atsumeru.manager.utils.ViewUtils;
import xyz.atsumeru.manager.utils.runnable.ContentLoader;
import xyz.atsumeru.manager.views.Snackbar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StageReaderController extends BaseController {
    public static final int VSCROLLBAR_WIDTH = 20;
    private static final MangaReaderCommunicationManager COMMUNICATION_MANAGER = new MangaReaderCommunicationManager();
    private static StageReaderController INSTANCE;
    @FXML
    private MFXProgressSpinner imageProgressIndicator;
    @FXML
    private Label errorText;
    @FXML
    public StackPane stackPane;
    @FXML
    private ImageView imageViewSingle;
    @FXML
    private VBox container;
    @FXML
    private MenuBar menuBar;
    @FXML
    private ScrollPane imageScrollPane;
    @FXML
    private HBox navContainer;
    @FXML
    private HBox navCenterContainer;
    @FXML
    private TextField pageNumField;
    @FXML
    private TextField totalPagesField;
    @FXML
    private Button firstPageButton;
    @FXML
    private Button prevPageButton;
    @FXML
    private Button nextPageButton;
    @FXML
    private Button lastPageButton;
    @FXML
    private Button prevChapterButton;
    @FXML
    private Button nextChapterButton;
    @FXML
    private CheckMenuItem showNavBarItem;
    // TODO: 13.01.2021
//    @FXML
//    private CheckMenuItem nightModeItem;
    @FXML
    private CheckMenuItem fullscreenItem;
    @FXML
    private RadioMenuItem fitAutoRadio;
    @FXML
    private RadioMenuItem fitHeightRadio;
    @FXML
    private RadioMenuItem fitWidthRadio;
    @FXML
    private RadioMenuItem actualSizeRadio;
    private Scene scene;
    private Server server;
    @Setter
    private String contentName;
    private List<List<Chapter>> chapters;
    private Chapter chapter;
    @Setter
    private ChapterReadProgressCallback callback;
    private EventHandler<KeyEvent> keyEventHandler;
    private ChangeListener imageViewListener;

    public static void showReader(String contentName, List<List<Chapter>> chapters, Chapter chapter, ChapterReadProgressCallback callback) {
        if (INSTANCE == null) {
            try {
                FXMLLoader fxmlLoader = new FXMLLoader(FXApplication.getResource("/fxml/stages/StageReader.fxml"), LocaleManager.getResourceBundle());
                Parent layout = fxmlLoader.load();

                Stage stage = new Stage();
                ViewUtils.addAppIconToStage(stage);

                Scene scene = WebAPI.isBrowser()
                        ? ViewUtils.createScene(stage, StageStyle.UNDECORATED, layout, 1053, 650)
                        : ViewUtils.createDecoratedScene(stage, layout, 1053, 650);

                INSTANCE = fxmlLoader.getController();
                INSTANCE.setScene(scene);
                INSTANCE.setContentName(contentName);
                INSTANCE.setChapters(chapters);
                INSTANCE.setChapter(chapter);
                INSTANCE.setCallback(callback);

                ViewUtils.addStylesheetToSceneWithAccent(scene, layout, Settings.getAppAccentColor());

                stage.setScene(scene);
                FXUtils.showStage(stage);
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Failed to create new Window");
            }
        } else {
            INSTANCE.setContentName(contentName);
            INSTANCE.setChapters(chapters);
            INSTANCE.setChapter(chapter);
            INSTANCE.setCallback(callback);
            INSTANCE.onMadeActive();
        }
    }

    private void setScene(Scene scene) {
        this.scene = scene;
    }

    /**
     * Exit the stage
     */
    @FXML
    private void exit() {
        ((Stage) scene.getWindow()).close();
    }

    @FXML
    protected void initialize() throws IOException {
        super.initialize();

        // fit the imageScrollPane width to the width of the stage
        stackPane.minHeightProperty().bind(container.heightProperty().subtract(navContainer.heightProperty()).subtract(menuBar.heightProperty()));

        imageScrollPane.boundsInParentProperty().addListener((o, oldVal, newVal) -> centerImageView());
        imageScrollPane.minHeightProperty().bind(container.heightProperty().subtract(navContainer.heightProperty()).subtract(menuBar.heightProperty()));

        // increase scroll distance on image
        imageViewSingle.setOnScroll(e -> {
            // adapted from https://stackoverflow.com/a/40993755
            double deltaY = e.getDeltaY();
            double width = imageScrollPane.getContent().getBoundsInLocal().getWidth();
            double vvalue = imageScrollPane.getVvalue();
            imageScrollPane.setVvalue(vvalue + -deltaY / width);
        });
        // make pageNumField and totalPagesField grow if the number of pages grows
        totalPagesField.textProperty().addListener((ob, o, n) -> totalPagesField.setPrefColumnCount(totalPagesField.getText().length()));
        pageNumField.textProperty().addListener((ob, o, n) -> pageNumField.setPrefColumnCount(pageNumField.getText().length()));

        // properly size the ImageView based on default fit setting
        updateImageViewFit();

        Platform.runLater(() -> {
            FXUtils.setOnHiding(scene, event -> onMadeInactive());
            FXUtils.requestFocus(stackPane);
            onMadeActive();
        });
    }

    /**
     * This method enables the keyEventHandler and begins loading the first page of the set chapter.
     */
    public void onMadeActive() {
        keyEventHandler = newKeyEventHandler();
        scene.addEventHandler(KeyEvent.ANY, keyEventHandler);
        applyImageFilter();
        imageViewSingle.requestFocus();
        parseChapter();
    }

    /**
     * This method disables the keyEventHandler and resets components.
     */
    public void onMadeInactive() {
        saveHistory();

        // stop active page loads
        ContentLoader.getInstance().stopThreads(ContentLoader.PREFIX_LOAD_PAGE);

        scene.removeEventHandler(KeyEvent.ANY, keyEventHandler);
        hideLoading();
        errorText.getParent().setVisible(false);
        errorText.getParent().setManaged(false);

        totalPagesField.setText("??");
        chapter.clearImages();

        INSTANCE = null;
    }

    private void parseChapter() {
        Single.just(AtsumeruSource.getSource(server))
                .cache()
                .subscribeOn(Schedulers.newThread())
                .observeOn(Schedulers.io())
                .map(server -> server.fetchChapterItem(chapter))
                .subscribe(chapterItem -> {
                    chapter.setImages(chapterItem.getImages(), null);
                    loadCurrentPage();
                }, throwable -> {
                    throwable.printStackTrace();

                    String errorMessage = LocaleManager.getStringFormatted("gui.reader.error_exception", throwable);
                    showError(errorMessage);
                    ViewUtils.setNodeInvisible(navCenterContainer);

                    Snackbar.showSnackBar(
                            stackPane,
                            errorMessage,
                            Snackbar.Type.ERROR
                    );
                });
    }

    /**
     * Create a new KeyEvent EventHandler for controlling the page.
     * <p>
     * Normally it would be sufficient to simply create the handler in initialize(), but the config
     * with key bindings may change before the client is restarted, so we instead make a new event
     * handler at every onMadeActive() using the current config.
     * <p>
     * We also could have put Config.getValue's in the event itself, but that would be very
     * inefficient. We also take account for the invert reading style checkbox user config, if it
     * has been ticked we invert the keys for keyprevious and keynext.
     *
     * @return a complete KeyEvent EventHandler for the reader page
     */
    private EventHandler<KeyEvent> newKeyEventHandler() {
        // TODO: 13.01.2021 !!!
//        Config config = sceneManager.getConfig();
//
//        // We account for whether the invert reading style checkbox has been checked by the user
//        boolean invertReadingStyle = (boolean) config.getValue(Config.Field.INVERT_READING_STYLE);
        boolean invertReadingStyle = true;

//        KeyCode keyPrevPage = KeyCode.valueOf((String) config.getValue(Config.Field.READER_KEY_PREV_PAGE));
//        KeyCode keyNextPage = KeyCode.valueOf((String) config.getValue(Config.Field.READER_KEY_NEXT_PAGE));
//        KeyCode keyFirstPage = KeyCode.valueOf((String) config.getValue(Config.Field.READER_KEY_FIRST_PAGE));
//        KeyCode keyLastPage = KeyCode.valueOf((String) config.getValue(Config.Field.READER_KEY_LAST_PAGE));

        KeyCode keyPrevPage = KeyCode.LEFT;
        KeyCode keyNextPage = KeyCode.RIGHT;
        KeyCode keyFirstPage = KeyCode.UP;
        KeyCode keyLastPage = KeyCode.DOWN;

        return event -> {
            // only handle KeyEvent.KEY_RELEASE -- not ideal, since this may
            // make the client appear slower to respond, but most non-letter
            // keys are not picked up by KEY_PRESSED
            if (event.getEventType() == KeyEvent.KEY_RELEASED) {
                // only perform actions if the user is not in the page num textfield

                if (!pageNumField.isFocused()) {

                    KeyCode keyPrev = keyPrevPage;
                    KeyCode keyNext = keyNextPage;
                    // Check if invert reading style setting is active.
                    if (invertReadingStyle) {
                        keyPrev = keyNextPage;
                        keyNext = keyPrevPage;
                    }

                    if (event.getCode() == keyPrev) {
                        if (chapter.getCurrentPage() == 1) {
                            previousChapter();
                        } else {
                            previousPage();
                        }
                    } else if (event.getCode() == keyNext) {
                        if (chapter.getCurrentPage() > chapter.getPagesCount() - 1) {
                            nextChapter();
                        } else {
                            nextPage();
                        }
                    } else if (event.getCode() == keyFirstPage) {
                        firstPage();
                    } else if (event.getCode() == keyLastPage) {
                        lastPage();
                    }
//                    else if (event.getCode() == keyToSeries) {
//                        goToSeries();
//                    }
                    else if (event.getCode() == KeyCode.ESCAPE) {
                        setFullscreen(false);
                    }
                }
                event.consume();
            }
        };
    }

    // TODO: 13.01.2021
//    @Override
//    public void toggleNightMode() {
//        super.toggleNightMode();
//        applyImageFilter();
//    }

    /**
     * Begin loading the image for the current page.
     */
    private void loadCurrentPage() {
        Platform.runLater(() -> {
            // clear current page and show progress indicator so the user
            // clearly understands that the new page is being loaded
            setImage(null);
            showLoading();

            // set text field to current page number
            int currentPageNum = Math.max(chapter.getCurrentPage(), 1);
            pageNumField.setText(Integer.toString(currentPageNum));

            // TODO: 13.01.2021 реализовать!!!
            // determine how many pages to preload, if any
//        Config config = sceneManager.getConfig();
//        boolean restrict_preload_pages =
//                (boolean) config.getValue(Config.Field.RESTRICT_PRELOAD_PAGES);
//        int preloadingAmount =
//                restrict_preload_pages ? (int) config.getValue(Config.Field.PRELOAD_PAGES_AMOUNT)
//                        : -1;

            int preloadingAmount = 3;
            // start the thread to load the page, which will subsequently begin
            // preloading pages if necessary
            ContentLoader.getInstance().loadPage(chapter, currentPageNum, this, false, preloadingAmount);
        });
    }

    /**
     * Set the image of the reader's ImageView.
     * <p>
     * This method ensures that the image is set when the FX thread is available.
     *
     * @param image the Image to display in the ImageView
     */
    public void setImage(Image image) {
        Platform.runLater(() -> imageViewSingle.setImage(image));
    }

    /**
     * Update components using fields from the set chapter. Taking account of whether invert reading
     * style setting is active.
     */
    public void refreshPage() {
        int pageNumber = chapter.getCurrentPage();
        // enable/disable appropriate navigation buttons

        // TODO: 13.01.2021 реализовать!!!
//        Config config = sceneManager.getConfig();

        // We account for whether the invert reading style checkbox has been checked by the user
//        boolean invertReadingStyle = (boolean) config.getValue(Config.Field.INVERT_READING_STYLE);
        boolean invertReadingStyle = true;

        // When invert reading style setting is active, user reads from left to right.
        // Meaning left/prev would go to the next page
        // And right/next would go to the previous page
        Platform.runLater(() -> {
            if (invertReadingStyle) {
                prevPageButton.setDisable(pageNumber + 1 > chapter.getPagesCount());
                nextPageButton.setDisable(pageNumber <= 1);
            } else {
                prevPageButton.setDisable(pageNumber <= 1);
                nextPageButton.setDisable(pageNumber + 1 > chapter.getPagesCount());
            }
            firstPageButton.setDisable(prevPageButton.isDisable());
            lastPageButton.setDisable(nextPageButton.isDisable());

            // update the number of total pages
            try {
                totalPagesField.setText(Integer.toString(chapter.getPagesCount()));
            } catch (Exception ignored) {
            }

            centerImageView();
        });
    }

    /**
     * Center the imageView on the stage.
     */
    private void centerImageView() {
        imageViewSingle.setTranslateX((imageScrollPane.getWidth() - imageViewSingle.getBoundsInParent().getWidth()) / 2);
        if (imageViewSingle.getTranslateX() < 0 || fitWidthRadio.isSelected()) {
            imageViewSingle.setTranslateX(0);
        }
    }

    private void saveHistory() {
        // TODO: 16.07.2021 files deletion
        int progress = Math.max(chapter.getCurrentPage(), 1);
        int total = chapter.getPagesCount();
        COMMUNICATION_MANAGER.onSaveHistory(chapter, progress, total);
        if (callback != null) {
            callback.onProgressChanged(chapter.getCHash(), progress, total);
        }
    }

    /**
     * Go to, and load, the page represented by the contents of pageNumField.
     *
     * @see #pageNumField
     */
    @FXML
    private void specificPage() {
        chapter.setCurrentPage(Integer.parseInt(pageNumField.getText()));
        loadCurrentPage();
    }

    /**
     * Go to, and load, the first page.
     */
    @FXML
    private void firstPage() {
        chapter.setCurrentPage(1);
        parseChapter();
    }

    /**
     * Go to, and load, the next page.
     */
    @FXML
    private void nextPage() {
        chapter.setCurrentPage(Math.max(chapter.getCurrentPage(), 1) + 1);
        loadCurrentPage();
        saveHistory();
    }

    /**
     * Go to, and load, the previous page.
     */
    @FXML
    private void previousPage() {
        chapter.setCurrentPage(chapter.getCurrentPage() - 1);
        loadCurrentPage();
        saveHistory();
    }

    /**
     * The left page function is called by the previous page button on the UI. Default action is to
     * go to the previous page however if the invert reading style is active, then it will go to the
     * next page.
     */
    @FXML
    private void leftPage() {
        // TODO: 13.01.2021 реализовать!!!
        // Get the current configuration settings
//        Config config = sceneManager.getConfig();

        // We account for whether the invert reading style checkbox has been checked by the user
//        boolean invertReadingStyle = (boolean) config.getValue(Config.Field.INVERT_READING_STYLE);
        boolean invertReadingStyle = true;

        // If invert reading style setting is active instead of previous page we go to the next page
        if (invertReadingStyle) {
            nextPage();
        } else {
            previousPage();
        }
    }

    /**
     * The right page function is called by the next page button on the UI. Default action is to go
     * to the next page however if the invert reading style is active, then it will go to the
     * previous page.
     */
    @FXML
    private void rightPage() {
        // TODO: 13.01.2021 реализовать!!!
        // Get the current configuration settings
//        Config config = sceneManager.getConfig();

        // We account for whether the invert reading style checkbox has been checked by the user
//        boolean invertReadingStyle = (boolean) config.getValue(Config.Field.INVERT_READING_STYLE);
        boolean invertReadingStyle = true;

        // If invert reading style setting is active instead of next page we go to previous page
        if (invertReadingStyle) {
            previousPage();
        } else {
            nextPage();
        }
    }

    /**
     * Go to, and load, the last page.
     */
    @FXML
    private void lastPage() {
        chapter.setCurrentPage(chapter.getPagesCount());
        loadCurrentPage();
    }

    /**
     * Go to the previous chapter and load the first page.
     * <p>
     * This function does not validate whether a previous chapter is actually available - that
     * should be enforced by disabling the prev chapter button.
     */
    @FXML
    private void previousChapter() {
        saveHistory();
        int prevChapterIndex = getPrevChapterIndex();

        if (prevChapterIndex >= 0) {
            setChapter(chapters.get(prevChapterIndex).get(0));

            // reset the number of total pages
            totalPagesField.setText("??");

            firstPage();
        }
    }

    /**
     * Go to the next chapter and load the first page.
     */
    @FXML
    private void nextChapter() {
        saveHistory();
        int nextChapterIndex = getNextChapterIndex();
        if (nextChapterIndex >= 0 && nextChapterIndex < chapters.size()) {
            setChapter(chapters.get(nextChapterIndex).get(0));

            // reset the number of total pages
            totalPagesField.setText("??");

            firstPage();
        }
    }

    private int getPrevChapterIndex() {
        int prevChapterIndex = -1;
        for (int i = 0; i < chapters.size(); i++) {
            List<Chapter> chapterHolder = chapters.get(i);
            if (chapterHolder.get(0).getCHash().equals(chapter.getCHash())) {
                prevChapterIndex = --i;
                break;
            }
        }

        return prevChapterIndex;
    }

    private int getNextChapterIndex() {
        int nextChapterIndex = -2;
        for (int i = 0; i < chapters.size(); i++) {
            List<Chapter> chapterHolder = chapters.get(i);
            if (chapterHolder.get(0).getCHash().equals(chapter.getCHash()) && i != chapters.size() - 1) {
                nextChapterIndex = ++i;
                break;
            }
        }

        return nextChapterIndex;
    }

    /**
     * Toggle whether the navigation bar is visible.
     * <p>
     * The navigation bar is the top bar which contains the page number display and forward/back
     * buttons. Users who hide the bar can still navigate the display using the key shortcuts
     * defined in keyEventHandler.
     *
     * @see #showNavBarItem
     * @see #navContainer
     * @see #keyEventHandler
     */
    @FXML
    private void toggleNavBar() {
        navContainer.setVisible(showNavBarItem.isSelected());
        if (showNavBarItem.isSelected()) {
            navContainer.setMinHeight(navContainer.getPrefHeight());
            navContainer.setMaxHeight(navContainer.getPrefHeight());
        } else {
            navContainer.setMinHeight(0);
            navContainer.setMaxHeight(0);
        }

        // ensure the page image is sized appropriately
        updateImageViewFit();
    }

    /**
     * Toggle whether the stage is set as fullscreen.
     */
    @FXML
    private void toggleFullscreen() {
        boolean wasFullscreen = ((Stage) scene.getWindow()).isFullScreen();
        setFullscreen(!wasFullscreen);
    }

    /**
     * Set the fullscreen state.
     *
     * @param fullscreen whether fullscreen should be enabled
     */
    private void setFullscreen(boolean fullscreen) {
        fullscreenItem.setSelected(fullscreen);
        ((Stage) scene.getWindow()).setFullScreen(fullscreen);
        menuBar.setVisible(!fullscreen);
        menuBar.setManaged(!fullscreen);
    }

    /**
     * Apply the appropriate filter to the page ImageView, if necessary.
     */
    private void applyImageFilter() {
        ColorAdjust filterAdjust = null;

        // TODO: 13.01.2021 !!!
//        Config config = sceneManager.getConfig();
//        boolean nightModeEnabled = (boolean) config.getValue(Config.Field.NIGHT_MODE_ENABLED);
//        boolean nightModeOnly = (boolean) config.getValue(Config.Field.PAGE_FILTER_NIGHT_MODE_ONLY);
        boolean nightModeEnabled = false;
        boolean nightModeOnly = false;

        if (nightModeEnabled || !nightModeOnly) {
            filterAdjust = new ColorAdjust();
            // apply color filter
            // TODO: 13.01.2021 !!!
//            if ((boolean) config.getValue(Config.Field.PAGE_FILTER_COLOR_ENABLED)) {
            if (false) {
//                filterAdjust.setHue((double) config.getValue(Config.Field.PAGE_FILTER_COLOR_HUE));
//                filterAdjust.setSaturation((double) config.getValue(Config.Field.PAGE_FILTER_COLOR_SATURATION));
            }
            // apply brightness filter
//            if ((boolean) config.getValue(Config.Field.PAGE_FILTER_BRIGHTNESS_ENABLED)) {
            if (false) {
//                filterAdjust = new ColorAdjust();
//                filterAdjust.setBrightness((double) config.getValue(Config.Field.PAGE_FILTER_BRIGHTNESS));
            }
        }
        imageViewSingle.setEffect(filterAdjust);
    }

    /**
     * Update the imageViewSingle fit properties corresponding to the selected style.
     *
     * @see #fitAutoRadio
     * @see #fitHeightRadio
     * @see #fitWidthRadio
     * @see #actualSizeRadio
     */
    @FXML
    private void updateImageViewFit() {
        if (fitAutoRadio.isSelected()) {
            imageViewListener = (o, oldValue, newValue) -> {
                imageViewSingle.fitWidthProperty().unbind();
                imageViewSingle.fitHeightProperty().unbind();
                imageViewSingle.fitHeightProperty()
                        .bind(container.heightProperty()
                                .subtract(menuBar.heightProperty())
                                .subtract(navContainer.heightProperty()));
                if (imageViewSingle.getBoundsInParent().getWidth() > container.getWidth()) {
                    imageViewSingle.fitHeightProperty().unbind();
                    imageViewSingle.fitWidthProperty().bind(container.widthProperty());
                }

                centerImageView();
            };
        } else if (fitHeightRadio.isSelected()) {
            imageViewListener = (o, oldValue, newValue) -> {
                imageViewSingle.fitWidthProperty().unbind();
                imageViewSingle.setFitWidth(-1);
                imageViewSingle.fitHeightProperty()
                        .bind(container.heightProperty()
                                .subtract(menuBar.heightProperty())
                                .subtract(navContainer.heightProperty()));

                centerImageView();
            };
        } else if (fitWidthRadio.isSelected()) {
            imageViewListener = (o, oldValue, newValue) -> {
                imageViewSingle.fitHeightProperty().unbind();
                imageViewSingle.setPreserveRatio(false);
                imageViewSingle.setFitHeight(-1);
                imageViewSingle.setPreserveRatio(true);
                imageViewSingle.fitWidthProperty().bind(container.widthProperty().subtract(VSCROLLBAR_WIDTH));

                centerImageView();
            };
        } else if (actualSizeRadio.isSelected()) {
            imageViewListener = (o, oldValue, newValue) -> {
                imageViewSingle.fitHeightProperty().unbind();
                imageViewSingle.fitWidthProperty().unbind();
                imageViewSingle.setFitHeight(-1);
                imageViewSingle.fitWidthProperty().bind(imageViewSingle.getImage() == null ? new SimpleDoubleProperty(0) : imageViewSingle.getImage().widthProperty());

                centerImageView();
            };
        }

        // add the listener to all meaningful properties -- the value of the
        // passed arguments don't matter
        container.heightProperty().addListener(imageViewListener);
        container.widthProperty().addListener(imageViewListener);
        imageViewSingle.imageProperty().addListener(imageViewListener);
        // hack to force listener operation to run
        // we wouldn't be able to do this if the listener function depended
        // on the given arguments
        imageViewListener.changed(new SimpleDoubleProperty(0), 0, 0);
    }

    public void setChapters(List<List<Chapter>> chapters) {
        this.chapters = new ArrayList<>(chapters);
    }

    public Chapter getChapter() {
        return chapter;
    }

    public void setChapter(Chapter chapter) {
        // clear images from the previous chapter
        if (this.chapter != null) {
            this.chapter.clearImages();
        }

        this.chapter = chapter;
        this.server = AtsumeruSource.getServerById((int) chapter.getServiceId());

        // enable/disable next and previous chapter buttons
//        Series series = chapter.getSeries();
        int prevChapterIndex = getPrevChapterIndex();
        int nextChapterIndex = getNextChapterIndex();
        nextChapterButton.setDisable(nextChapterIndex < 0);
        prevChapterButton.setDisable(prevChapterIndex < 0);

        // update read status of new chapter
        // TODO: 13.01.2021 сохранение чтения в историю
//        chapter.setRead(true);

        // upload read count on tracker if enabled
        // TODO: 13.01.2021
//        Config config = sceneManager.getConfig();
//        String series_id = series.getTrackerId(AniList.ID);
//        int chapter_num = (int) Math.round(chapter.chapterNum);
//        if ((boolean) config.getValue(Config.Field.TRACKER_ANILIST_UPDATE_AUTO)) {
//            Tracker tracker = sceneManager.getPluginManager().getTracker(AniList.ID);
//
//            Track track = new Track(series_id, null, null, chapter_num, null, null);
//            sceneManager.getContentLoader().updateSeriesTracker(tracker, series_id, track, true,
//                    false);
//        }

        Platform.runLater(() -> ((Stage) scene.getWindow()).setTitle(String.format("%s - %s", contentName, chapter.getTitle())));
    }

    @Override
    public Pane getContentRoot() {
        return container;
    }

    public void showConfigStage(ActionEvent actionEvent) {
    }

    public void toggleNightMode(ActionEvent actionEvent) {
    }

    public void showLoading() {
        imageProgressIndicator.setVisible(true);
    }

    public void showError(String error) {
        hideLoading();
        ViewUtils.setNodeVisible(errorText.getParent());
        errorText.setText(error);
    }

    public void hideLoading() {
        imageProgressIndicator.setVisible(false);
    }

    public void hideError() {
        ViewUtils.setNodeGone(errorText.getParent());
    }
}
