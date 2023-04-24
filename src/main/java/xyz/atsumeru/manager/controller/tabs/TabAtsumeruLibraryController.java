package xyz.atsumeru.manager.controller.tabs;

import com.atsumeru.api.AtsumeruAPI;
import com.atsumeru.api.model.AtsumeruMessage;
import com.atsumeru.api.model.Serie;
import com.atsumeru.api.model.Volume;
import com.atsumeru.api.model.category.Category;
import com.atsumeru.api.model.importer.ImportStatus;
import com.atsumeru.api.model.server.Server;
import com.atsumeru.api.model.services.ServicesStatus;
import com.atsumeru.api.model.sync.History;
import com.atsumeru.api.utils.ImportType;
import com.atsumeru.api.utils.LibraryPresentation;
import com.atsumeru.api.utils.Sort;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXListView;
import com.jfoenix.controls.JFXTextField;
import com.jfoenix.controls.JFXTreeView;
import com.jpro.webapi.WebAPI;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIconView;
import impl.org.controlsfx.skin.GridViewSkin;
import io.github.palexdev.materialfx.controls.MFXProgressSpinner;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import kotlin.Pair;
import kotlin.Triple;
import lombok.Getter;
import org.controlsfx.control.GridView;
import xyz.atsumeru.manager.FXApplication;
import xyz.atsumeru.manager.adapter.AtsumeruAdapter;
import xyz.atsumeru.manager.cell.EmptyGridCell;
import xyz.atsumeru.manager.cell.SerieGridCell;
import xyz.atsumeru.manager.controller.BaseController;
import xyz.atsumeru.manager.controller.MainController;
import xyz.atsumeru.manager.controller.dialogs.ContentCategoryDialogController;
import xyz.atsumeru.manager.controller.dialogs.ContentEditDialogController;
import xyz.atsumeru.manager.controller.dialogs.DownloadedLinksCheckerDialogController;
import xyz.atsumeru.manager.controller.dialogs.importer.FoldersManagerDialogController;
import xyz.atsumeru.manager.controller.dialogs.settings.ServerSettingsDialogController;
import xyz.atsumeru.manager.controller.dialogs.users.UserManagerDialogController;
import xyz.atsumeru.manager.controller.fragments.DetailsFragmentController;
import xyz.atsumeru.manager.controller.tabs.settings.TabAtsumeruSettingsController;
import xyz.atsumeru.manager.controls.EmptyView;
import xyz.atsumeru.manager.controls.ErrorView;
import xyz.atsumeru.manager.controls.jfoenix.JFXCustomDialog;
import xyz.atsumeru.manager.controls.jfoenix.JFXCustomPopup;
import xyz.atsumeru.manager.enums.*;
import xyz.atsumeru.manager.exceptions.ApiParseException;
import xyz.atsumeru.manager.helpers.*;
import xyz.atsumeru.manager.listeners.OnImportListener;
import xyz.atsumeru.manager.managers.Settings;
import xyz.atsumeru.manager.managers.StatusBarManager;
import xyz.atsumeru.manager.managers.TabPaneManager;
import xyz.atsumeru.manager.managers.TabsManager;
import xyz.atsumeru.manager.source.AtsumeruSource;
import xyz.atsumeru.manager.utils.FXUtils;
import xyz.atsumeru.manager.utils.PopupHelper;
import xyz.atsumeru.manager.utils.ViewUtils;
import xyz.atsumeru.manager.utils.comparator.AlphanumComparator;
import xyz.atsumeru.manager.utils.comparator.NaturalStringComparator;
import xyz.atsumeru.manager.utils.globalutils.GUArray;
import xyz.atsumeru.manager.utils.globalutils.GUEnum;
import xyz.atsumeru.manager.utils.globalutils.GUString;
import xyz.atsumeru.manager.views.Snackbar;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TabAtsumeruLibraryController extends BaseController {
    public static final String CONTENT_TYPE_TAG = "content_type";
    public static final String STATUS_TAG = "status";
    public static final String TRANSLATION_STATUS_TAG = "translation_status";
    public static final String PLOT_TYPE_TAG = "plot_type";
    public static final String CENSORSHIP_TAG = "censorship";
    public static final String COLOR_TAG = "color";
    public static final String AGE_RATING_TAG = "age_rating";
    public static final String AUTHORS_TAG = "authors";
    public static final String ARTISTS_TAG = "artists";
    public static final String PUBLISHERS_TAG = "publishers";
    public static final String TRANSLATORS_TAG = "translators";
    public static final String GENRES_TAG = "genres";
    public static final String TAGS_TAG = "tags";
    public static final String YEARS_TAG = "years";
    public static final String COUNTRIES_TAG = "countries";
    public static final String LANGUAGES_TAG = "languages";
    public static final String EVENTS_TAG = "events";
    public static final String MAGAZINES_TAG = "magazines";
    public static final String CHARACTERS_TAG = "characters";
    public static final String SERIES_TAG = "series";
    public static final String PARODIES_TAG = "parodies";
    public static final String CIRCLES_TAG = "circles";

    private static final String GLYPH_FILTER = "FILTER";
    private static final String GLYPH_FILTER_REMOVE = "FILTER_REMOVE";
    private static final String[] ENUM_TAGS = {
            CONTENT_TYPE_TAG,
            STATUS_TAG,
            TRANSLATION_STATUS_TAG,
            PLOT_TYPE_TAG,
            CENSORSHIP_TAG,
            COLOR_TAG,
            AGE_RATING_TAG,
    };

    private static final String[] STRING_TAGS = {
            AUTHORS_TAG,
            ARTISTS_TAG,
            PUBLISHERS_TAG,
            TRANSLATORS_TAG,
            GENRES_TAG,
            TAGS_TAG,
            YEARS_TAG,
            COUNTRIES_TAG,
            LANGUAGES_TAG,
            EVENTS_TAG,
            MAGAZINES_TAG,
            CHARACTERS_TAG,
            SERIES_TAG,
            PARODIES_TAG,
            CIRCLES_TAG
    };

    private static final String TAGS_RES_BUNDLE_PREFIX = "tags.";
    private static final String ENUM_RES_BUNDLE_PREFIX = "enum.";
    private static final Map<String, List<Serie>> cache = new HashMap<>();
    @Deprecated
    @Getter
    private static LibraryPresentation libraryPresentation = LibraryPresentation.SERIES_AND_SINGLES;
    private static Category mCurrentCategory = null;
    private final List<OnImportListener> mImportDoneListeners = new ArrayList<>();
    private final AtomicBoolean mImporterActive = new AtomicBoolean();
    private final AtomicLong mLastImportTime = new AtomicLong();
    private final AtomicBoolean mMetadataUpdateActive = new AtomicBoolean();
    private final ObjectProperty<List<TreeItem<Object>>> mCurrentSelectedTreeItems = new SimpleObjectProperty<>(null);

    private final BooleanProperty isRecalculating = new SimpleBooleanProperty(false);
    private final BooleanProperty isLoadingData = new SimpleBooleanProperty(false);

    GridView<Serie> gridView;
    @FXML
    StackPane contentRoot;
    @FXML
    SplitPane contentContainer;
    @FXML
    StackPane spGridHolder;
    @FXML
    StackPane paneFilters;
    @FXML
    HBox hbTabs;
    @FXML
    TabPane tpLibraryTabs;
    @FXML
    JFXButton btnReload;
    @FXML
    JFXButton btnListCategories;
    @FXML
    JFXButton btnLockCategories;
    @FXML
    MaterialDesignIconView mdivLockCategories;
    @FXML
    JFXTextField tfFilterSearch;
    @FXML
    JFXTreeView<Object> tvFilters;
    @FXML
    JFXButton btnClearFilters;
    @FXML
    MFXProgressSpinner spinnerLoading;
    // Action bar
    @FXML
    HBox hbActionBar;
    @FXML
    JFXButton btnChangeCategory;
    @FXML
    JFXButton btnSelectAll;
    @FXML
    JFXButton btnClearSelection;
    // Search Box
    @FXML
    HBox hbSearch;
    @FXML
    JFXTextField tfSearch;
    @FXML
    JFXButton btnSortOrder;
    @FXML
    MaterialDesignIconView mdivSortOrder;
    @FXML
    JFXButton btnSort;
    @FXML
    MaterialDesignIconView mdivSort;
    @FXML
    JFXButton btnServers;
    // Buttons
    @FXML
    JFXButton btnImport;
    @FXML
    JFXButton btnCategories;
    @FXML
    JFXButton btnUsers;
    @FXML
    JFXButton btnAdmin;
    @FXML
    JFXButton btnFilters;
    @FXML
    Tooltip btnFiltersTooltip;
    @FXML
    MaterialDesignIconView ivFilters;
    @FXML
    JFXButton btnSettings;
    @FXML
    EmptyView vbEmptyView;
    @FXML
    ErrorView vbErrorView;

    boolean hasServersOnStart;
    private Disposable mDisposable;
    private Disposable mServicesStatusDisposable;
    private Disposable mOrderCategoriesDisposable;
    private FilteredList<Serie> filteredList;
    private SortedList<Serie> sortedList;
    private List<Serie> listCopy;
    private Sort currentSort = Settings.Atsumeru.getCurrentAtsumeruSort();
    private boolean reversedOrder = Settings.Atsumeru.getCurrentAtsumeruSortOrder();
    private TreeItem<Object> treeItemAll;
    @Getter
    private Map<String, Map<String, Integer>> allFiltersMap;
    private Map<String, Map<String, Integer>> filtersMap;
    private List<Category> categories;

    @Deprecated
    public static boolean isSeriesOrSinglesPresentation() {
        return libraryPresentation == LibraryPresentation.SERIES
                || libraryPresentation == LibraryPresentation.SINGLES
                || libraryPresentation == LibraryPresentation.SERIES_AND_SINGLES;
    }

    @Deprecated
    public static boolean isSinglesPresentation() {
        return libraryPresentation == LibraryPresentation.SINGLES;
    }

    public static void notifyItemUpdate(List<Serie> series) {
        TabsManager.getTabController(TabAtsumeruLibraryController.class).onItemUpdate(series);
    }

    public static void clearCache() {
        cache.clear();
    }

    public static void reloadItems() {
        clearCache();
        Platform.runLater(() -> TabsManager.getTabController(TabAtsumeruLibraryController.class).onServerChange());
    }

    public static long getLastReadForBook(Serie bookItem) {
        return Optional.ofNullable(bookItem.getVolumes())
                .filter(GUArray::isNotEmpty)
                .map(list -> list.stream()
                        .map(Volume::getHistory)
                        .filter(Objects::nonNull).max(Comparator.comparingLong(History::getLastReadAt))
                        .map(History::getLastReadAt)
                        .orElse(0L)
                )
                .orElse(0L);
    }

    @Deprecated
    public void setLibraryPresentation(LibraryPresentation libraryPresentation) {
        TabAtsumeruLibraryController.libraryPresentation = libraryPresentation;
        fetchItems();
    }

    public void onServerChange() {
        tpLibraryTabs.getTabs().clear();
        loadCustomCategories();
        fetchItems();
    }

    private void onItemUpdate(List<Serie> series) {
        checkIsModifiedSerieCategoriesChanged(series);
        fetchFullFiltersList();
        initTreeMaps();
        fillTreeMapWithFilters(filtersMap, sortedList);
        initTreeViewData();
        configureTreeView();
    }

    @FXML
    protected void initialize() throws IOException {
        super.initialize();

        hideLoading();
        vbEmptyView.hideEmptyView();
        vbErrorView.hideErrorView();

        createGridView();
        bindGridVisibilityAndManagedProperties();

        createSearchListener();
        createFilterSearchListener();

        setSortOrderIcon();
        configureButtons();
        configureActionBar();
        configureLibraryTabs();
        createGridSliderValueListener();

        setCategoriesLockMode();

        if (!Settings.Atsumeru.isShowFilters()) {
            handleFiltersButtonAction();
        } else {
            configureFiltersButton();
        }

        if (hasServersOnStart = GUArray.isNotEmpty(AtsumeruSource.listServers())) {
            loadCustomCategories();
            fetchFullFiltersList();
        }

        Platform.runLater(() -> {
            ObservableList<Node> observableList = TabAtsumeruSettingsController.getServersChildrenNodes();
            BooleanBinding hasServersBindings = Bindings.createBooleanBinding(() -> {
                boolean isEmpty = observableList.isEmpty();
                if (isEmpty) {
                    hasServersOnStart = false;
                    isLoadingData.set(true);
                    vbErrorView.showErrorView(
                            new ApiParseException(LocaleManager.getString("gui.error.no_atsumeru_servers")),
                            ErrorType.NO_SERVER,
                            spinnerLoading,
                            false,
                            null,
                            false,
                            null);
                } else if (!hasServersOnStart) {
                    hasServersOnStart = true;
                    fetchItems();
                    setSortButtonTitleAndIcon();
                    setServersButtonTitle();
                }
                return !isEmpty;
            }, observableList);

            btnSortOrder.visibleProperty().bind(hasServersBindings);
            btnSort.visibleProperty().bind(hasServersBindings);
            btnServers.visibleProperty().bind(hasServersBindings);
            hbTabs.visibleProperty().bind(hasServersBindings);
            hbTabs.managedProperty().bind(hasServersBindings);
            paneFilters.visibleProperty().bind(hasServersBindings);
            paneFilters.managedProperty().bind(hasServersBindings);

            contentContainer.setDividerPositions(1);
            hasServersBindings.addListener((observable, oldValue, newValue) -> contentContainer.setDividerPositions(newValue ? 0.81 : 1));
        });
    }

    private void createGridView() {
        ObservableList<Node> childrens = spGridHolder.getChildren();
        if (GUArray.isNotEmpty(childrens) && childrens.get(0) instanceof GridView) {
            childrens.remove(0);
        }

        gridView = new GridView<>();
        gridView.setCellHeight(260);
        gridView.setCellWidth(150);
        gridView.setVerticalCellSpacing(4);
        gridView.setHorizontalCellSpacing(4);
        gridView.setPadding(new Insets(0, 0, 0, 5));
        gridView.itemsProperty().addListener((observable, oldValue, newValue) ->
                FXUtils.runLaterDelayed(() -> gridView.setCellFactory(view -> createSerieCell()), 150));
        SerieGridCell.clearAll();

        BorderPane.setAlignment(gridView, Pos.TOP_CENTER);

        childrens.add(0, gridView);
    }

    private void configureButtons() {
        ViewUtils.createOnMouseEnterBorderEffect(btnReload, btnListCategories, btnLockCategories, btnChangeCategory, btnSelectAll, btnClearSelection, btnUsers,
                btnImport, btnCategories, btnAdmin, btnFilters, btnSettings, btnClearFilters);
        ViewUtils.createOnMouseExitedBorderEffect(btnReload, btnListCategories, btnLockCategories, btnChangeCategory, btnSelectAll, btnClearSelection, btnUsers,
                btnImport, btnCategories, btnAdmin, btnFilters, btnSettings, btnClearFilters);
        btnClearFilters.setOnMouseClicked(event -> {
            clearSelectedFilters();
            tvFilters.getSelectionModel().select(treeItemAll);
            handleTreeViewMouseClick();
        });
        setSortButtonTitleAndIcon();
        setServersButtonTitle();
    }

    private void configureActionBar() {
        ViewUtils.setNodeGone(hbActionBar);
        btnChangeCategory.setOnAction(event -> {
            ContentCategoryDialogController.createAndShow(
                    new ArrayList<>(SerieGridCell.SELECTED_ITEMS.values()),
                    (observable, oldValue, success) -> {
                        if (success) {
                            SerieGridCell.SELECTED_ITEMS.clear();
                            ViewUtils.setNodeGone(hbActionBar);
                        }
                    });
        });

        btnSelectAll.setOnAction(event -> {
            SerieGridCell.SELECTED_ITEMS.clear();
            SerieGridCell.SELECTED_ITEMS.putAll(sortedList.stream().collect(Collectors.toMap(Serie::getId, Function.identity())));
            SerieGridCell.updateAll();
        });

        btnClearSelection.setOnAction(event -> {
            SerieGridCell.SELECTED_ITEMS.clear();
            SerieGridCell.updateAll();
            ViewUtils.setNodeGone(hbActionBar);
        });
    }

    private void configureFiltersButton() {
        boolean isShowFilters = Settings.Atsumeru.isShowFilters();
        ivFilters.setGlyphName(isShowFilters ? GLYPH_FILTER_REMOVE : GLYPH_FILTER);
        btnFiltersTooltip.setText(LocaleManager.getString(isShowFilters ? "gui.hide_filters" : "gui.show_filters"));
    }

    private void configureLibraryTabs() {
        // Tab selection for items load/reload in category
        tpLibraryTabs.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        mCurrentCategory = (Category) newValue.getUserData();
                        fetchItems();
                    }
                }
        );

        // Tabs reordering save on server
        tpLibraryTabs.getTabs().addListener((ListChangeListener<Tab>) change -> {
            change.next();
            if (change.wasPermutated()) {
                RXUtils.safeDispose(mOrderCategoriesDisposable);

                AtomicInteger order = new AtomicInteger(0);
                List<Category> categories = change.getList().stream()
                        .map(Tab::getUserData)
                        .map(object -> (Category) object)
                        .peek(category -> category.setOrder(order.getAndIncrement()))
                        .collect(Collectors.toList());

                mOrderCategoriesDisposable = AtsumeruAPI.orderCategories(categories)
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(Schedulers.io())
                        .subscribe(message -> {
                            Snackbar.showSnackBar(
                                    MainController.getSnackbarRoot(),
                                    LocaleManager.getString("atsumeru.categories_reordered"),
                                    Snackbar.Type.SUCCESS
                            );
                            RXUtils.safeDispose(mOrderCategoriesDisposable);
                        }, throwable -> Snackbar.showExceptionSnackbar(MainController.getSnackbarRoot(true), throwable, true));
            }
        });
    }

    private void createContentTab(Category category) {
        ObservableList<Tab> tabs = tpLibraryTabs.getTabs();
        Tab tab = new Tab(category.getName());
        tab.setUserData(category);
        tabs.add(tab);
    }

    private void createGridSliderValueListener() {
        Platform.runLater(() -> StatusBarManager.getGridSliderValueProperty().addListener((observable, oldValue, newValue) -> {
            double oldScale = (double) Math.round(oldValue.doubleValue());
            double newScale = (double) Math.round(newValue.doubleValue());
            if (oldScale != newScale) {
                recalculateGridView();
            }
        }));
    }

    private void loadCustomCategories() {
        showLoading();
        AtomicReference<Disposable> disposable = new AtomicReference<>();
        disposable.set(AtsumeruAPI.getCategoriesList()
                .subscribeOn(Schedulers.newThread())
                .observeOn(Schedulers.io())
                .subscribe(categories -> {
                    Platform.runLater(() -> {
                        this.categories = categories;
                        categories.forEach(this::createContentTab);
                    });

                    RXUtils.safeDispose(disposable);
                }, throwable -> {
                    vbErrorView.showErrorView(
                            throwable,
                            ErrorType.NO_CONNECTION,
                            spinnerLoading,
                            true,
                            this::onServerChange,
                            false,
                            null
                    );
                    Snackbar.showExceptionSnackbar(MainController.getSnackbarRoot(true), throwable, true);
                }));
    }

    private void fetchFullFiltersList() {
        Single<List<Serie>> single = AtsumeruAPI.getBooksList(LibraryPresentation.SERIES_AND_SINGLES, null,
                null, null, Sort.CREATED_AT, true, 1, 999, false, true);

        AtomicReference<Disposable> disposable = new AtomicReference<>();
        disposable.set(single.cache()
                .subscribeOn(Schedulers.newThread())
                .observeOn(Schedulers.io())
                .subscribe(items -> {
                    allFiltersMap = new LinkedHashMap<>();
                    fillTreeMapWithFilters(allFiltersMap, items);
                    RXUtils.safeDispose(disposable);
                }, throwable -> Snackbar.showExceptionSnackbar(MainController.getSnackbarRoot(true), throwable, true)));
    }

    private void fetchItems() {
        isLoadingData.set(true);

        showLoading();
        vbEmptyView.hideEmptyView();
        vbErrorView.hideErrorView();

        RXUtils.safeDispose(mDisposable);

        String cacheTag = mCurrentCategory != null ? mCurrentCategory.getId() : "";
        if (!cache.containsKey(cacheTag)) {
            Single<List<Serie>> single = AtsumeruAPI.getBooksList(libraryPresentation, null, GUString.isEmpty(cacheTag) ? ContentType.UNKNOWN.name() : null,
                    cacheTag, Sort.CREATED_AT, true, 1, 999, true, true);

            mDisposable = single.cache()
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(Schedulers.io())
                    .subscribe(items -> Platform.runLater(() -> {
                        onItemsLoad(items);
                        cache.put(cacheTag, items);
                        RXUtils.safeDispose(mDisposable);
                    }), throwable -> vbErrorView.showErrorView(
                            throwable,
                            ErrorType.NO_CONNECTION,
                            spinnerLoading,
                            true,
                            this::onServerChange,
                            false,
                            null
                    ));
        } else {
            onItemsLoad(cache.get(cacheTag));
        }

        FXUtils.runDelayed(this::startFetchingImporterStatus, 1000);
    }

    private void onItemsLoad(List<Serie> items) {
        initTreeMaps();
        prepareGridView(items);
        initTreeViewData();
        configureTreeView();

        FXUtils.runLaterDelayed(() -> {
            if (!vbEmptyView.checkEmptyItems(items, null, spinnerLoading)) {
                FXUtils.runLaterDelayed(() -> isLoadingData.set(false), 200);
            }

            ViewUtils.setGridViewPannable(gridView);
            if (!WebAPI.isBrowser() || !WebAPI.getWebAPI(contentRoot.getScene()).isMobile()) {
                ViewUtils.setGridViewOnScroll(gridView, 1.5);
            }
        }, 300);
    }

    private void initTreeMaps() {
        filtersMap = new LinkedHashMap<>();
        fillTreeMapWithEnumNames(filtersMap, CONTENT_TYPE_TAG, Arrays.asList(ContentType.values()));
        fillTreeMapWithEnumNames(filtersMap, STATUS_TAG, Status.class);
        fillTreeMapWithEnumNames(filtersMap, TRANSLATION_STATUS_TAG, TranslationStatus.class);
        fillTreeMapWithEnumNames(filtersMap, PLOT_TYPE_TAG, PlotType.class);
        fillTreeMapWithEnumNames(filtersMap, CENSORSHIP_TAG, Censorship.class);
        fillTreeMapWithEnumNames(filtersMap, COLOR_TAG, Color.class);
        fillTreeMapWithEnumNames(filtersMap, AGE_RATING_TAG, AgeRating.class);
    }

    private void fillTreeMapWithFilters(Map<String, Map<String, Integer>> targetMap, List<Serie> serieList) {
        if (GUArray.isNotEmpty(serieList)) {
            for (Serie serie : serieList) {
                for (String enumTag : ENUM_TAGS) {
                    fillTreeMapWithTags(targetMap, enumTag, getTagsForCategory(enumTag, serie));
                }
                for (String stringTag : STRING_TAGS) {
                    fillTreeMapWithTags(targetMap, stringTag, getTagsForCategory(stringTag, serie));
                }
            }
        }
    }

    private FilteredList<Serie> getFilteredSerieList(List<Serie> serieList) {
        return new FilteredList<>(FXCollections.observableArrayList(serieList), predicate -> true);
    }

    private void prepareGridView(List<Serie> serieList) {
        filteredList = getFilteredSerieList(serieList);
        sortedList = new SortedList<>(filteredList);
        filterLibraryList(tfSearch.getText());
        sortLibraryList();
        fillTreeMapWithFilters(filtersMap, serieList);
        createGridView();
        recalculateGridView(50);
        FXUtils.runLaterDelayed(() -> gridView.setItems(sortedList), 100);
    }

    public AgeRating getAgeRating(Serie serie) {
        if (serie.isAdult()) {
            return AgeRating.ADULTS_ONLY;
        } else if (serie.isMature()) {
            return AgeRating.MATURE;
        } else {
            return AgeRating.EVERYONE;
        }
    }

    private void checkIsModifiedSerieCategoriesChanged(List<Serie> series) {
        Serie serie = series.get(0);
        String categoryId = mCurrentCategory.getId();
        ContentType contentType = GUEnum.valueOfOrNull(ContentType.class, mCurrentCategory.getContentType());

        boolean categoryIdChanged = GUString.isEmpty(categoryId) && GUArray.isNotEmpty(serie.getCategories());
        boolean contentTypeChanged = contentType != null && contentType != GUEnum.valueOf(ContentType.class, serie.getContentType());
        boolean categorieNotInSerie = !serie.getCategories().contains(categoryId);

        if (!contentTypeChanged && contentType != null && GUArray.isEmpty(serie.getCategories())) {
            // Assume that category and content type wan not changed
            categorieNotInSerie = false;
        }

        if (categoryIdChanged || categorieNotInSerie || contentTypeChanged) {
            cache.remove(categoryId);
            serie.getCategories().forEach(cache::remove);
            if (contentTypeChanged || GUArray.isEmpty(serie.getCategories())) {
                removeContentFromCacheForContentType(GUEnum.valueOfOrNull(ContentType.class, serie.getContentType()));
            }
            gridView.setItems(FXCollections.observableArrayList());
            this.filteredList.setPredicate(obj -> true);
            FilteredList<Serie> filteredList = this.filteredList.filtered(serie1 -> {
                for (Serie changedSerie : series) {
                    if (GUString.equals(serie1.getId(), changedSerie.getId())) {
                        return false;
                    }
                }
                return true;
            });
            this.filteredList = getFilteredSerieList(filteredList);
            sortedList = new SortedList<>(this.filteredList);
            filterLibraryList(tfSearch.getText());
            sortLibraryList();

            listCopy = new ArrayList<>(sortedList);
            gridView.setItems(sortedList);
        }
    }

    private void removeContentFromCacheForContentType(ContentType contentType) {
        if (contentType != null) {
            Optional<Category> categoryWithContentType = categories.stream()
                    .filter(category -> GUString.equalsIgnoreCase(category.getContentType(), contentType.name()))
                    .findFirst();

            categoryWithContentType.ifPresent(category -> cache.remove(category.getId()));
        }
    }

    public void sortLibraryList() {
        if (sortedList != null) {
            Comparator<Serie> comparator = getSortComparator(currentSort);
            if (reversedOrder) {
                comparator = comparator.reversed();
            }
            sortedList.setComparator(comparator);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private Comparator<Serie> getSortComparator(Sort sort) {
        switch (sort) {
            case TITLE:
                return (item1, item2) -> NaturalStringComparator.compareStrings(item1.getTitle(), item2.getTitle());
            case YEAR:
                return (item1, item2) -> AlphanumComparator.compareStrings(item1.getYear(), item2.getYear());
            case COUNTRY:
                return (item1, item2) -> AlphanumComparator.compareStrings(item1.getCountry(), item2.getCountry());
            case LANGUAGE:
                return (item1, item2) -> AlphanumComparator.compareStrings(GUString.join(",", item1.getLanguages()), GUString.join(",", item2.getLanguages()));
            case PUBLISHER:
                return (item1, item2) -> AlphanumComparator.compareStrings(item1.getPublisher(), item2.getPublisher());
            case SERIE:
                return (item1, item2) -> AlphanumComparator.compareStrings(
                        GUArray.safeGetString(item1.getSeries(), 0, ""),
                        GUArray.safeGetString(item2.getSeries(), 0, "")
                );
            case PARODY:
                return (item1, item2) -> AlphanumComparator.compareStrings(
                        GUArray.safeGetString(item1.getParodies(), 0, ""),
                        GUArray.safeGetString(item2.getParodies(), 0, "")
                );
            case VOLUMES_COUNT:
                return Comparator.comparingLong(Serie::getVolumesCount);
            case CHAPTERS_COUNT:
                return Comparator.comparingLong(Serie::getChaptersCount);
            case SCORE:
                return (item1, item2) -> AlphanumComparator.compareStrings(item1.getScore(), item2.getScore());
            case UPDATED_AT:
                return Comparator.comparingLong(Serie::getUpdatedAt);
            case POPULARITY:
                return Comparator.comparingInt(Serie::getRating);
            case LAST_READ:
                return Comparator.comparingLong(TabAtsumeruLibraryController::getLastReadForBook);
            case CREATED_AT:
            default:
                return Comparator.comparingLong(Serie::getCreatedAt);
        }
    }

    public void filterLibraryList(String filter) {
        if (filteredList != null) {
            filteredList.setPredicate(serie -> GUString.isEmpty(filter)
                    || GUString.containsIgnoreCase(serie.getTitle(), filter)
                    || GUString.containsIgnoreCase(serie.getAltTitle(), filter)
                    || GUString.containsIgnoreCase(serie.getJapTitle(), filter)
                    || GUString.containsIgnoreCase(serie.getKorTitle(), filter)
                    || GUString.containsIgnoreCase(serie.getFolder(), filter)
                    || GUString.containsIgnoreCase(serie.getId(), filter));
        }
    }

    public void filterLibraryListByTag(String tag) {
        List<TreeItem<Object>> eligibleItems = new ArrayList<>();
        findTreeViewLeafsByFilter(tvFilters.getRoot(), eligibleItems, tag);
        eligibleItems.forEach(it -> tvFilters.getSelectionModel().select(it));
        handleTreeViewMouseClick();
    }

    private void findTreeViewLeafsByFilter(TreeItem<Object> root, List<TreeItem<Object>> eligibleItems, String filter) {
        for (TreeItem<Object> child : root.getChildren()) {
            if (child.isLeaf()) {
                if (GUString.equalsIgnoreCase(filter, child.getValue().toString().replaceAll(" \\(\\d+\\)$", ""))) {
                    eligibleItems.add(child);
                }
            } else {
                findTreeViewLeafsByFilter(child, eligibleItems, filter);
            }
        }
    }

    public void initTreeViewData() {
        initTreeViewData(null);
    }

    public void initTreeViewData(String filter) {
        TreeItem<Object> rootTreeItem = new TreeItem<>("Filters");
        rootTreeItem.setExpanded(true);

        ObservableList<TreeItem<Object>> children = rootTreeItem.getChildren();
        children.add(treeItemAll = new TreeItem<>(String.format(LocaleManager.getString("gui.filters_all"), sortedList.size())));
        filtersMap.keySet().forEach(key -> children.add(createTreeItem(filtersMap, key, filter)));
        tvFilters.setRoot(rootTreeItem);
        tvFilters.setCellFactory(value -> new TreeCell<>() {
            @Override
            public void updateItem(Object object, boolean empty) {
                super.updateItem(object, empty);

                if (empty) {
                    setText(null);
                } else {
                    String string = object.toString();
                    if (string.startsWith(TAGS_RES_BUNDLE_PREFIX)) {
                        String resId = string.replaceAll(" \\(\\d+\\)$", "");
                        setText(string.replace(resId, LocaleManager.getString(resId)));
                    } else {
                        setText(string);
                    }
                }
            }
        });
    }

    private TreeItem<Object> createTreeItem(Map<String, Map<String, Integer>> targetMap, String mapTag, String filter) {
        Map<String, Integer> map = getOrCreateFiltersMap(targetMap, mapTag);
        TreeItem<Object> treeItem = new TreeItem<>();
        treeItem.setExpanded(GUString.isNotEmpty(filter));
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            if (GUString.isNotEmpty(filter) && !entry.getKey().replaceAll(" \\(\\d+\\)$", "").toLowerCase().contains(filter.toLowerCase())) {
                continue;
            }
            TreeItem<Object> node = new TreeItem<>();
            if (entry.getValue() > 0) {
                node.setValue(entry.getKey() + " (" + entry.getValue() + ")");
                treeItem.getChildren().add(node);
            }
        }
        treeItem.setValue(String.format("%s (%s)", TAGS_RES_BUNDLE_PREFIX + mapTag, treeItem.getChildren().size()));

        return treeItem;
    }

    public void configureTreeView() {
        listCopy = new ArrayList<>(sortedList);

        // Отключаем выбор Non Leaf TreeItem в TreeView
        ViewUtils.disableNonLeafSelectionForMultipleModeTreeView(tvFilters, mCurrentSelectedTreeItems);

        tvFilters.setShowRoot(false);
        tvFilters.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // По умолчанию выбираем первый элемент списка и сохраняем его в "историю" выбранных элементов
        tvFilters.getSelectionModel().select(0);
        mCurrentSelectedTreeItems.setValue(new ArrayList<>(tvFilters.getSelectionModel().getSelectedItems()));
    }

    private void createSearchListener() {
        tfSearch.textProperty().addListener((observable, oldValue, newValue) -> filterLibraryList(newValue));
        filterLibraryList(tfSearch.getText());
    }

    private void createFilterSearchListener() {
        tfFilterSearch.textProperty().addListener((observable, oldValue, newValue) -> initTreeViewData(newValue));
    }

    private void fillTreeMapWithEnumNames(Map<String, Map<String, Integer>> targetMap, String mapTag, List<? extends Enum<?>> enums) {
        Map<String, Integer> map = getOrCreateFiltersMap(targetMap, mapTag);
        for (Object enumObj : enums) {
            map.put(GUString.capitalize(enumObj.toString().replace("_", " "), true, true), 0);
        }
    }

    private void fillTreeMapWithEnumNames(Map<String, Map<String, Integer>> targetMap, String mapTag, Class<? extends Enum<?>> clazz) {
        Map<String, Integer> map = getOrCreateFiltersMap(targetMap, mapTag);
        String[] enumNames = GUEnum.getNames(clazz);
        for (String enumName : enumNames) {
            map.put(GUString.capitalize(enumName.replace("_", " "), true, true), 0);
        }
    }

    private void fillTreeMapWithTags(Map<String, Map<String, Integer>> targetMap, String mapTag, List<String> tags) {
        Map<String, Integer> map = getOrCreateFiltersMap(targetMap, mapTag);
        for (String tag : tags) {
            if (GUString.isEmpty(tag)) {
                continue;
            }
            Integer count = map.get(tag);
            if (count == null) {
                count = 0;
            }
            map.put(tag, ++count);
        }
    }

    private Map<String, Integer> getOrCreateFiltersMap(Map<String, Map<String, Integer>> targetMap, String mapTag) {
        Map<String, Integer> map;
        if (targetMap.containsKey(mapTag)) {
            map = targetMap.get(mapTag);
        } else {
            map = new TreeMap<>();
            targetMap.put(mapTag, map);
        }
        return map;
    }

    private List<String> getTagsFromSerieItem(TreeItem<Object> selectedRootItem, Serie serie) {
        String rootCategory = selectedRootItem.getParent().getValue().toString()
                .replaceAll(" \\(\\d+\\)$", "")
                .replace(TAGS_RES_BUNDLE_PREFIX, "");
        return getTagsForCategory(rootCategory, serie);
    }

    @SuppressWarnings("ConstantConditions")
    private List<String> getTagsForCategory(String category, Serie serie) {
        switch (category) {
            // Enums
            case CONTENT_TYPE_TAG:
                return getEnumStringLocalizedSingletonList(serie.getContentType());
            case STATUS_TAG:
                return getEnumStringLocalizedSingletonList(serie.getStatus());
            case TRANSLATION_STATUS_TAG:
                return getEnumStringLocalizedSingletonList(serie.getTranslationStatus());
            case PLOT_TYPE_TAG:
                return getEnumStringLocalizedSingletonList(serie.getPlotType());
            case CENSORSHIP_TAG:
                return getEnumStringLocalizedSingletonList(serie.getCensorship());
            case COLOR_TAG:
                return getEnumStringLocalizedSingletonList(serie.getColor());
            case AGE_RATING_TAG:
                return getEnumStringLocalizedSingletonList(getAgeRating(serie).toString());
            // String values
            case AUTHORS_TAG:
                return serie.getAuthors();
            case ARTISTS_TAG:
                return serie.getArtists();
            case PUBLISHERS_TAG:
                return Collections.singletonList(serie.getPublisher());
            case TRANSLATORS_TAG:
                return serie.getTranslators();
            case GENRES_TAG:
                return AtsumeruAdapter.toLocalizedGenre(serie.getGenres());
            case TAGS_TAG:
                return serie.getTags();
            case YEARS_TAG:
                return Collections.singletonList(serie.getYear());
            case COUNTRIES_TAG:
                return Collections.singletonList(serie.getCountry());
            case LANGUAGES_TAG:
                return serie.getLanguages();
            case EVENTS_TAG:
                return Collections.singletonList(serie.getEvent());
            case MAGAZINES_TAG:
                return serie.getMagazines();
            case CHARACTERS_TAG:
                return serie.getCharacters();
            case SERIES_TAG:
                return serie.getSeries();
            case PARODIES_TAG:
                return serie.getParodies();
            case CIRCLES_TAG:
                return serie.getCircles();
            // TODO: Score/Rating?
        }

        return new ArrayList<>();
    }

    private List<String> getEnumStringLocalizedSingletonList(String value) {
        return Collections.singletonList(LocaleManager.getString(GUString.isNotEmpty(value) ? (ENUM_RES_BUNDLE_PREFIX + value.toLowerCase()) : "enum.unknown"));
    }

    private void bindGridVisibilityAndManagedProperties() {
        BooleanProperty isAppResizing = FXApplication.getInstance().getIsAppResizing();
        spGridHolder.visibleProperty().bind(isLoadingData.not().and(isAppResizing.not().and(isRecalculating.not())));
        spGridHolder.managedProperty().bind(isAppResizing.not());

        AtomicBoolean emptyViewShow = new AtomicBoolean();
        spGridHolder.visibleProperty().addListener((observable, oldValue, visible) -> {
            if (!emptyViewShow.get() && (isAppResizing.get() || isRecalculating.get())) {
                setEmptyViewUpdatingUI();
                vbEmptyView.showEmptyView(Kaomoji.EMBARRASSMENT);
                emptyViewShow.set(true);
            }
        });

        spGridHolder.visibleProperty().addListener((observable, oldValue, visible) -> {
            if (visible) {
                new Thread(() -> {
                    int attempts = 0;
                    while (attempts < 100 && (isAppResizing.get() || isRecalculating.get())) {
                        GUArray.sleepThread(20);
                        attempts++;
                    }

                    Platform.runLater(() -> {
                        vbEmptyView.hideEmptyView();
                        setEmptyViewEmptyData();
                        emptyViewShow.set(false);
                    });
                }).start();
            }
        });
    }

    private void setEmptyViewUpdatingUI() {
        vbEmptyView.setEmptyText(LocaleManager.getString("gui.messing_with_data"));
    }

    private void setEmptyViewEmptyData() {
        vbEmptyView.setKaomoji(Kaomoji.SADNESS);
        vbEmptyView.setEmptyText(LocaleManager.getString("gui.error.empty_list"));
    }

    public void recalculateGridView() {
        recalculateGridView(100);
    }

    public void recalculateGridView(int delay) {
        isRecalculating.set(true);
        FXUtils.runLaterDelayed(() -> {
            DoubleProperty columnsProperty = StatusBarManager.getGridSliderValueProperty();
            double columnsAmount = getColumnsAmount(Settings.getGridScaleType(), columnsProperty);
            double cellWidth = (gridView.getWidth() - gridView.getHorizontalCellSpacing() * columnsAmount * 3) / columnsAmount;
            gridView.setCellFactory(view -> new EmptyGridCell<>());
            gridView.setCellWidth(cellWidth);
            gridView.setCellHeight(gridView.cellWidthProperty().multiply(1.333).add(60).doubleValue());
            FXUtils.runLaterDelayed(() -> {
                gridView.setCellFactory(view -> createSerieCell());
                FXUtils.runLaterDelayed(() -> isRecalculating.set(false), 200);
            }, 150);
        }, delay);
    }

    private double getColumnsAmount(GridScaleType gridScaleType, DoubleProperty columnsProperty) {
        double columnsFromPrefs = (double) Math.round(columnsProperty.get());
        if (gridScaleType == GridScaleType.PROPORTIONAL_SCALE) {
            double containerWidth = gridView.widthProperty().get() - gridView.getHorizontalCellSpacing();

            double cardWidth = containerWidth / columnsFromPrefs;
            while (cardWidth > 0 && cardWidth < 100 || cardWidth > 200) {
                columnsFromPrefs = columnsFromPrefs + (cardWidth < 100 ? -1 : 1);
                cardWidth = containerWidth / columnsFromPrefs;
            }

            return columnsFromPrefs;
        }
        return columnsFromPrefs;
    }

    public void hideLoading() {
        ViewUtils.setNodeGone(spinnerLoading);
    }

    public void showLoading() {
        ViewUtils.setNodeVisible(spinnerLoading);
    }

    public void clearSelectedFilters() {
        tfSearch.setText("");
        tvFilters.getSelectionModel().clearSelection();
    }

    @SuppressWarnings("unchecked")
    @FXML
    public void handleTreeViewMouseClick() {
        FilteredList<TreeItem<Object>> selectedItems = tvFilters.getSelectionModel().getSelectedItems()
                .filtered(TreeItem::isLeaf);

        if (GUArray.isNotEmpty(selectedItems)) {
            if (GUArray.isEmpty(mCurrentSelectedTreeItems.get()) || isCurrentAndOldSelectionsNotMatch(selectedItems)) {
                mCurrentSelectedTreeItems.setValue(new ArrayList<>(selectedItems));
                if (selectedItems.contains(tvFilters.getRoot().getChildren().get(0))) {
                    filteredList.getSource().clear();
                    ((ObservableList<Serie>) filteredList.getSource()).addAll(listCopy);
                } else {
                    List<Serie> newList = new ArrayList<>();
                    boolean hasLeaf = false;
                    for (Serie serie : listCopy) {
                        for (TreeItem<Object> selectedItem : selectedItems) {
                            if (selectedItem.isLeaf()) {
                                hasLeaf = true;
                            }

                            List<?> list = getTagsFromSerieItem(selectedItem, serie);
                            if (list.contains(selectedItem.getValue().toString().replaceAll(" \\(\\d+\\)$", ""))) {
                                newList.add(serie);
                                break;
                            }
                        }
                    }
                    if (hasLeaf) {
                        filteredList.getSource().clear();
                        ((ObservableList<Serie>) filteredList.getSource()).addAll(newList);
                    }
                }
            }
        }
    }

    private boolean isCurrentAndOldSelectionsNotMatch(FilteredList<TreeItem<Object>> selectedItems) {
        boolean equalsSizes = mCurrentSelectedTreeItems.get().size() == selectedItems.size();
        String oldFirstSelectedValue = mCurrentSelectedTreeItems.get().get(0).getValue().toString();
        String newFirstSelectedValue = selectedItems.get(0).getValue().toString();
        return !equalsSizes || !GUString.equalsIgnoreCase(oldFirstSelectedValue, newFirstSelectedValue);
    }

    private void showOrHideFilters() {
        ObservableList<Node> nodes = contentContainer.getItems();
        if (nodes.size() == 2) {
            nodes.remove(paneFilters);
            Settings.Atsumeru.putShowFilters(false);
        } else {
            nodes.add(paneFilters);
            contentContainer.setDividerPositions(0.81);
            Settings.Atsumeru.putShowFilters(true);
        }

        FXUtils.runDelayed(this::recalculateGridView, 30);
    }

    public void addOnImportDoneListener(OnImportListener listener) {
        mImportDoneListeners.add(listener);
    }

    private void startFetchingImporterStatus() {
        if (!JavaHelper.isDebug()) {
            RXUtils.safeDispose(mServicesStatusDisposable);

            Flowable<ServicesStatus> flowable = AtsumeruAPI.getServicesStatus()
                    .retryWhen(new RetryWithDelay(999, 1, TimeUnit.SECONDS))
                    .repeatWhen(completed -> completed.delay(1, TimeUnit.SECONDS));

            mServicesStatusDisposable = flowable.cache()
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(Schedulers.io())
                    .doOnError(throwable -> Thread.sleep(1000))
                    .subscribe(
                            this::onServicesStatusGet,
                            throwable -> Snackbar.showExceptionSnackbar(MainController.getSnackbarRoot(true), throwable, true)
                    );
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void onServicesStatusGet(ServicesStatus servicesStatus) {
        boolean wasActiveImport = isImportActive();

        mImporterActive.set(servicesStatus.getImportStatus().isImportActive());
        mMetadataUpdateActive.set(servicesStatus.getMetadataUpdateStatus().isUpdateActive());

        int progress = Math.max(
                Math.max(servicesStatus.getImportStatus().getImported(), servicesStatus.getMetadataUpdateStatus().getUpdated()),
                servicesStatus.getCoversCachingStatus().getSaved()
        );
        int total = Math.max(
                Math.max(servicesStatus.getImportStatus().getTotal(), servicesStatus.getMetadataUpdateStatus().getTotal()),
                servicesStatus.getCoversCachingStatus().getTotal()
        );

        String messageId;
        if (isImportActive()) {
            messageId = "gui.importing";
        } else if (servicesStatus.getCoversCachingStatus().isCoversCachingActive()) {
            messageId = "gui.covers_caching";
        } else {
            messageId = "gui.metadata_updating";
        }

        StatusBarManager.updateStatusBarProgress(progress, total, progress < total ? LocaleManager.getString(messageId, progress, total) : "");

        if (wasActiveImport && !isImportActive() || isImportFinishedSilent(servicesStatus.getImportStatus())) {
            FXUtils.runLaterDelayed(TabAtsumeruLibraryController::reloadItems, 1500);
        }

        mLastImportTime.set(servicesStatus.getImportStatus().getLastStartTime());

        if (!isImportActive()) {
            mImportDoneListeners.forEach(OnImportListener::onImportFinished);
            mImportDoneListeners.clear();
        }
    }

    private void setSortOrderIcon() {
        mdivSortOrder.setGlyphName(reversedOrder
                ? MaterialDesignIcon.SORT_DESCENDING.name()
                : MaterialDesignIcon.SORT_ASCENDING.name());
    }

    private void setSortButtonTitleAndIcon() {
        btnSort.setText(AtsumeruHelper.getSortNameLocalized(currentSort));
        mdivSort.setGlyphName(AtsumeruHelper.getSortIcon(currentSort).name());
    }

    public void changeServer(Integer serverId) {
        AtsumeruAPI.changeServer(serverId);
        Settings.Atsumeru.putCurrentAtsumeruServer(serverId);
        mImporterActive.set(false);
        mMetadataUpdateActive.set(false);

        setServersButtonTitle();
        reloadItems();
    }

    private void setServersButtonTitle() {
        Server server = AtsumeruAPI.getServerManager().getCurrentServer();
        if (server != null) {
            btnServers.setText(server.getName());
        }
    }

    @FXML
    private void handleReversedSortButtonAction() {
        reversedOrder = !reversedOrder;
        Settings.Atsumeru.putCurrentAtsumeruSortOrder(reversedOrder);
        sortLibraryList();
        setSortOrderIcon();
    }

    @FXML
    private void handleSortButtonAction() {
        JFXListView<Node> list = PopupHelper.createEmptyPopupList();
        for (Sort sort : Sort.values()) {
            list.getItems().add(ViewUtils.createLabelWithMaterialDesignIconNode(
                    AtsumeruHelper.getSortNameLocalized(sort),
                    AtsumeruHelper.getSortIcon(sort),
                    290,
                    26,
                    "white",
                    sort)
            );
        }

        JFXCustomPopup popup = PopupHelper.createListViewPopup(list,
                (observable, oldValue, newValue) -> {
                    currentSort = (Sort) newValue.getUserData();
                    Settings.Atsumeru.putCurrentAtsumeruSort(currentSort);
                    sortLibraryList();
                    setSortButtonTitleAndIcon();
                });

        popup.getStyleClass().add("jfx-popup-medium");
        PopupHelper.showPopupForButton(popup, btnSort);
    }

    @FXML
    private void handleServersButtonAction() {
        JFXCustomPopup popup = PopupHelper.createListViewPopup(PopupHelper.createPopupListForAtsumeruServers(),
                (observable, oldValue, newValue) -> changeServer((Integer) newValue.getUserData()));

        PopupHelper.showPopupForButton(popup, btnServers);
    }

    @FXML
    private void handleUsersButtonAction() {
        UserManagerDialogController.createAndShow();
    }

    @FXML
    private void handleImportButtonAction() {
        JFXCustomPopup popup = PopupHelper.createListViewPopup(PopupHelper.createPopupListForAtsumeruImport(),
                (observable, oldValue, newValue) -> {
                    if (isImportActive()) {
                        Snackbar.showSnackBar(
                                MainController.getSnackbarRoot(true),
                                LocaleManager.getString("gui.import.wait_import_finish"),
                                Snackbar.Type.WARNING
                        );
                        return;
                    }

                    Object userData = newValue.getUserData();
                    if (userData instanceof String) {
                        String tag = userData.toString();
                        switch (tag) {
                            case "folders_management":
                                FXUtils.runLaterDelayed(FoldersManagerDialogController::createAndShow, 100);
                                break;
                        }
                    } else if (userData instanceof ImportType) {
                        ImportType importType = (ImportType) userData;
                        Single<AtsumeruMessage> single = null;
                        switch (importType) {
                            case NEW:
                                single = AtsumeruAPI.importerScan();
                                break;
                            case FULL:
                                single = AtsumeruAPI.importerRescan(false);
                                break;
                            case FULL_WITH_COVERS:
                                single = AtsumeruAPI.importerRescan(true);
                                break;
                        }

                        single.cache()
                                .subscribeOn(Schedulers.newThread())
                                .observeOn(Schedulers.io())
                                .subscribe(message ->
                                                Platform.runLater(() -> Snackbar.showSnackBar(
                                                        getContentRoot(),
                                                        LocaleManager.getString(
                                                                importType == ImportType.NEW
                                                                        ? "atsumeru.import.importing_started"
                                                                        : "atsumeru.import.reimporting_started"
                                                        ),
                                                        Snackbar.Type.SUCCESS)
                                                ),
                                        throwable ->
                                                Snackbar.showSnackBar(
                                                        getContentRoot(),
                                                        LocaleManager.getString(
                                                                importType == ImportType.NEW
                                                                        ? "atsumeru.import.unable_to_start_importing"
                                                                        : "atsumeru.import.unable_to_start_reimporting"
                                                        ),
                                                        Snackbar.Type.ERROR
                                                ));
                    }
                });

        popup.getStyleClass().add("jfx-popup-long");
        PopupHelper.showPopupForButton(popup, btnImport);
    }

    private SerieGridCell createSerieCell() {
        return new SerieGridCell(gridView, (action, position, runnable) -> {
            switch (action) {
                case SerieGridCell.ACTION_VIEW:
                    String contentTitle = sortedList.get(position).getTitle();
                    if (!TabPaneManager.selectTabIfPresent(contentTitle)) {
                        Pair<Node, DetailsFragmentController> pair = FXUtils.loadFXML("/fxml/fragments/DetailsFragment.fxml");

                        pair.getSecond().setSerieItem(sortedList.get(position));

                        Tab tab = new Tab(contentTitle, pair.getFirst());
                        TabPaneManager.addTabIntoTabPanePenultimate(tab, true);
                    }
                    break;
                case SerieGridCell.ACTION_EDIT:
                    TabPaneManager.selectEditTab();
                    TabsManager.getTabController(ContentEditDialogController.class).setData(
                            sortedList.get(position),
                            isSeriesOrSinglesPresentation()
                    );
                    runnable.run();

                    if (!isSeriesOrSinglesPresentation()) {
                        Snackbar.showSnackBar(getContentRoot(),
                                LocaleManager.getString("gui.error.unable_save_metadata_into_archive"),
                                Snackbar.Type.ERROR);
                    }
                    break;
                case SerieGridCell.ACTION_CHANGE_CATEGORY:
                    ContentCategoryDialogController.createAndShow(Collections.singletonList(sortedList.get(position)), null);
                    runnable.run();
                    break;
                case SerieGridCell.ACTION_CHANGE_SELECTION:
                    ViewUtils.setNodeVisibleAndManaged(GUArray.isNotEmpty(SerieGridCell.SELECTED_ITEMS), hbActionBar);
                    break;
                case SerieGridCell.ACTION_REMOVE:
                    Serie serie = sortedList.get(position);

                    List<Triple<ButtonType, String, Runnable>> actionTriples = new ArrayList<>();
                    actionTriples.add(new Triple<>(ButtonType.NO, null, null));
                    actionTriples.add(new Triple<>(
                            ButtonType.YES,
                            null,
                            () -> {
                                AtomicReference<Disposable> disposable = new AtomicReference<>();
                                disposable.set(AtsumeruAPI.deleteBook(serie.getId())
                                        .subscribeOn(Schedulers.newThread())
                                        .observeOn(Schedulers.io())
                                        .subscribe(message -> {
                                            Snackbar.showSnackBar(
                                                    MainController.getSnackbarRoot(),
                                                    LocaleManager.getString("gui.content_removed"),
                                                    Snackbar.Type.SUCCESS
                                            );

                                            Platform.runLater(() -> filteredList.getSource().remove(serie));
                                            cache.get(mCurrentCategory.getId()).remove(serie);

                                            RXUtils.safeDispose(disposable);
                                        }, throwable -> {
                                            throwable.printStackTrace();
                                            Snackbar.showSnackBar(
                                                    MainController.getSnackbarRoot(),
                                                    LocaleManager.getString("gui.error.unable_remove_content"),
                                                    Snackbar.Type.ERROR
                                            );
                                        }));
                            }
                    ));

                    DialogsHelper.showConfirmationDialog(
                            contentRoot,
                            JFXCustomDialog.DialogTransition.CENTER,
                            actionTriples,
                            LocaleManager.getString("gui.dialog.remove_from_library_title"),
                            LocaleManager.getStringFormatted("gui.dialog.remove_from_library_header", serie.getTitle()),
                            "",
                            LocaleManager.getString("gui.dialog.remove_from_library_content")
                    );

                    break;
            }
        });
    }

    private boolean isImportActive() {
        return mImporterActive.get();
    }

    private boolean isImportFinishedSilent(ImportStatus importStatus) {
        return mLastImportTime.get() > 0 && mLastImportTime.get() < importStatus.getLastStartTime();
    }

    @FXML
    private void handleReloadButtonAction() {
        changeServer(AtsumeruAPI.getServerManager().getCurrentServer().getId());
    }

    @FXML
    private void handleCategoriesButtonAction() {
        ContentCategoryDialogController.createAndShow(null, null);
    }

    @FXML
    private void handleAdminButtonAction() {
        JFXCustomPopup popup = PopupHelper.createListViewPopup(PopupHelper.createPopupListForAtsumeruAdminFunctions(),
                (observable, oldValue, newValue) -> {
                    Label cantBeUndoneLabel = ViewUtils.createLabel(LocaleManager.getString("gui.this_cant_be_undone"), "#e74c3c");

                    String tag = newValue.getUserData().toString();
                    switch (tag) {
                        // todo: constants
                        case "check_downloaded_from_links":
                            DownloadedLinksCheckerDialogController.createAndShow();
                            break;
                        case "clear_cache":
                            DialogBuilder.create(TabsManager.getTabController(MainController.class).getContentRoot())
                                    .withTransition(JFXCustomDialog.DialogTransition.CENTER)
                                    .withMinWidth(0)
                                    .withHeading(LocaleManager.getString("gui.clear_server_cache"))
                                    .withBody(
                                            ViewUtils.createLabel(
                                                    LocaleManager.getString("gui.clear_server_cache.summary"),
                                                    "#e74c3c"
                                            )
                                    )
                                    .withBody(cantBeUndoneLabel)
                                    .withButton(ButtonType.NO, null, null)
                                    .withButton(ButtonType.YES, null, () -> AtsumeruAPI.clearServerCache()
                                            .cache()
                                            .subscribeOn(Schedulers.newThread())
                                            .observeOn(Schedulers.io())
                                            .subscribe(
                                                    message -> Snackbar.showSnackBar(
                                                            MainController.getSnackbarRoot(),
                                                            LocaleManager.getString(message.isOk() ? "gui.cache_cleared" : "gui.unable_to_clear_cache"),
                                                            message.isOk() ? Snackbar.Type.SUCCESS : Snackbar.Type.ERROR
                                                    ),
                                                    throwable -> Snackbar.showSnackBar(MainController.getSnackbarRoot(), LocaleManager.getString("gui.unable_to_clear_cache"), Snackbar.Type.ERROR)))
                                    .show();
                            break;
                        case "generate_hashes":
                            DialogBuilder.create(TabsManager.getTabController(MainController.class).getContentRoot())
                                    .withTransition(JFXCustomDialog.DialogTransition.CENTER)
                                    .withMinWidth(0)
                                    .withHeading(LocaleManager.getString("gui.generate_unique_archive_hashes"))
                                    .withBody(
                                            ViewUtils.createLabel(
                                                    LocaleManager.getString("gui.generate_unique_archive_hashes.summary"),
                                                    "#e74c3c"
                                            )
                                    )
                                    .withBody("")
                                    .withBody(cantBeUndoneLabel)
                                    .withButton(ButtonType.NO, null, null)
                                    .withButton(ButtonType.YES, null, () -> AtsumeruAPI.createUniqueIds(true, false, false)
                                            .cache()
                                            .subscribeOn(Schedulers.newThread())
                                            .observeOn(Schedulers.io())
                                            .subscribe(
                                                    message -> Snackbar.showSnackBar(
                                                            MainController.getSnackbarRoot(),
                                                            LocaleManager.getString(message.isOk() ? "gui.procedure_launched" : "gui.unable_to_launch_procedure"),
                                                            message.isOk() ? Snackbar.Type.SUCCESS : Snackbar.Type.ERROR
                                                    ),
                                                    throwable -> Snackbar.showSnackBar(MainController.getSnackbarRoot(), LocaleManager.getString("gui.unable_to_launch_procedure"), Snackbar.Type.ERROR))
                                    )
                                    .show();
                            break;
                        case "insert_metadata":
                            DialogBuilder.create(TabsManager.getTabController(MainController.class).getContentRoot())
                                    .withTransition(JFXCustomDialog.DialogTransition.CENTER)
                                    .withMinWidth(0)
                                    .withHeading(LocaleManager.getString("gui.insert_all_metadata_into_archives"))
                                    .withBody(
                                            ViewUtils.createLabel(
                                                    LocaleManager.getString("gui.insert_all_metadata_into_archives.summary"),
                                                    "#e74c3c"
                                            )
                                    )
                                    .withBody("")
                                    .withBody(cantBeUndoneLabel)
                                    .withButton(ButtonType.NO, null, null)
                                    .withButton(ButtonType.YES, null, () -> AtsumeruAPI.injectAllFromDatabase()
                                            .cache()
                                            .subscribeOn(Schedulers.newThread())
                                            .observeOn(Schedulers.io())
                                            .subscribe(
                                                    message -> Snackbar.showSnackBar(
                                                            MainController.getSnackbarRoot(),
                                                            LocaleManager.getString(message.isOk() ? "gui.procedure_launched" : "gui.unable_to_launch_procedure"),
                                                            message.isOk() ? Snackbar.Type.SUCCESS : Snackbar.Type.ERROR
                                                    ),
                                                    throwable -> Snackbar.showSnackBar(MainController.getSnackbarRoot(), LocaleManager.getString("gui.unable_to_launch_procedure"), Snackbar.Type.ERROR))
                                    )
                                    .show();
                            break;
                    }
                });

        popup.getStyleClass().add("jfx-popup-long");
        PopupHelper.showPopupForButton(popup, btnImport);
    }

    @FXML
    private void handleFiltersButtonAction() {
        showOrHideFilters();
        configureFiltersButton();
    }

    @FXML
    private void handleSettingsButtonAction() {
        ServerSettingsDialogController.createAndShow();
    }

    @FXML
    private void handleListCategoriesButtonAction() {
        JFXCustomPopup popup = PopupHelper.createListViewPopup(PopupHelper.createPopupListForCategories(tpLibraryTabs.getTabs()),
                (observable, oldValue, newValue) -> {
                    Object userData = newValue.getUserData();
                    if (userData instanceof Category) {
                        tpLibraryTabs.getTabs()
                                .stream()
                                .filter(tab -> tab.getUserData().equals(userData))
                                .findFirst()
                                .ifPresent(tab -> tpLibraryTabs.getSelectionModel().select(tab));
                    }
                });

        popup.getStyleClass().add("jfx-popup-medium");
        PopupHelper.showPopupForButton(popup, btnListCategories);
    }

    @FXML
    private void handleLockCategoriesButtonAction() {
        Settings.Atsumeru.putLockCategories(!Settings.Atsumeru.isLockCategories());
        setCategoriesLockMode();
    }

    private void setCategoriesLockMode() {
        if (Settings.Atsumeru.isLockCategories()) {
            btnLockCategories.setTooltip(new Tooltip(LocaleManager.getString("gui.unlock_categories")));
            mdivLockCategories.setGlyphName("LOCK");
            tpLibraryTabs.setTabDragPolicy(TabPane.TabDragPolicy.FIXED);
        } else {
            btnLockCategories.setTooltip(new Tooltip(LocaleManager.getString("gui.lock_categories")));
            mdivLockCategories.setGlyphName("LOCK_OPEN");
            tpLibraryTabs.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);
        }
    }

    @Override
    public Pane getContentRoot() {
        return contentRoot;
    }
}
