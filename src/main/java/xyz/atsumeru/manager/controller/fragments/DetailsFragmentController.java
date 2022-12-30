package xyz.atsumeru.manager.controller.fragments;

import com.atsumeru.api.AtsumeruAPI;
import com.atsumeru.api.model.Readable;
import com.atsumeru.api.model.Serie;
import com.atsumeru.api.model.Volume;
import com.atsumeru.api.model.server.Server;
import com.atsumeru.api.utils.ReadableType;
import com.jfoenix.controls.JFXButton;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIconView;
import de.jensd.fx.glyphs.materialicons.MaterialIconView;
import io.github.palexdev.materialfx.controls.MFXProgressSpinner;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import kotlin.Pair;
import xyz.atsumeru.manager.FXApplication;
import xyz.atsumeru.manager.adapter.AtsumeruAdapter;
import xyz.atsumeru.manager.callback.OnImporterCallback;
import xyz.atsumeru.manager.controller.MainController;
import xyz.atsumeru.manager.controller.cards.CardContentController;
import xyz.atsumeru.manager.controller.dialogs.ContentEditDialogController;
import xyz.atsumeru.manager.controller.dialogs.uploader.UploadFilesDialogController;
import xyz.atsumeru.manager.controller.stages.StageReaderController;
import xyz.atsumeru.manager.controller.tabs.TabAtsumeruLibraryController;
import xyz.atsumeru.manager.helpers.AtsumeruHelper;
import xyz.atsumeru.manager.helpers.LocaleManager;
import xyz.atsumeru.manager.helpers.RXUtils;
import xyz.atsumeru.manager.listeners.DetailsListener;
import xyz.atsumeru.manager.models.content.Content;
import xyz.atsumeru.manager.models.manga.Chapter;
import xyz.atsumeru.manager.source.AtsumeruSource;
import xyz.atsumeru.manager.utils.FXUtils;
import xyz.atsumeru.manager.utils.ViewUtils;
import xyz.atsumeru.manager.utils.globalutils.GUArray;
import xyz.atsumeru.manager.utils.globalutils.GUDate;
import xyz.atsumeru.manager.utils.globalutils.GUString;
import xyz.atsumeru.manager.views.Snackbar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class DetailsFragmentController implements DetailsListener, OnImporterCallback { //extends BaseController
    private static final String LABEL_SELECTED_STYLE = "-fx-border-color: -fx-accent-color; -fx-border-style: hidden hidden solid hidden; -fx-border-width: 0 0 2 0;";
    private static final String LABEL_DESELECTED_STYLE = "-fx-border-color: transparent; -fx-border-style: hidden hidden solid hidden; -fx-border-width: 0 0 2 0;";
    private final List<Pair<String, List<Node>>> backstack = new ArrayList<>();
    private final List<ContentFileItemCardController> controllers = new ArrayList<>();
    private final Map<String, CardContentController> cardControllersMap = new HashMap<>();
    @FXML
    Pane contentRoot;
    @FXML
    ScrollPane spContentDetails;
    @FXML
    ScrollPane spItemsList;
    @FXML
    VBox vbItemsRoot;
    @FXML
    HBox hbHeader;
    @FXML
    FlowPane fpItemsList;
    @FXML
    MFXProgressSpinner spinnerLoading;
    @FXML
    Label lblVolumes;
    @FXML
    Label lblChapters;
    @FXML
    JFXButton btnUpload;
    @FXML
    JFXButton btnRescan;
    @FXML
    JFXButton btnEditMetadata;
    @FXML
    JFXButton bntGoBack;
    @FXML
    MaterialIconView iconSelectAll;
    private Server mServer;
    private Serie mSerie;
    private Content mContent;
    private ReadableType mAtsumeruReadableType = ReadableType.VOLUMES;
    private ContextMenu contextMenu;
    private MenuItem itemEdit;
    private MenuItem itemCopyID;
    private MenuItem itemCopyName;
    private MenuItem itemCopyPath;
    private MenuItem itemReloadCover;
    private MenuItem itemDeleteHistory;
    private List<List<Chapter>> mChapters;

    private volatile boolean openContentInAction;

    public void setSerieItem(Serie serie) {
        this.mSerie = serie;
        setContentItem(
                AtsumeruSource.getAdapter(TabAtsumeruLibraryController.getLibraryPresentation())
                        .toContent(serie, AtsumeruSource.getCurrentServer(), -1L)
                        .getContent()
        );
    }

    public void setContentItem(Content content) {
        this.mContent = content;
    }

    @FXML
    protected void initialize() throws IOException {
        ViewUtils.setVBoxOnScroll(vbItemsRoot, spItemsList, 0.05);
        ViewUtils.setNodeGone(fpItemsList, iconSelectAll);
        hideFileListAndBackNode();

        FXMLLoader loader = FXUtils.getLoader("/fxml/fragments/DetailsInfoFragment.fxml");
        spContentDetails.setContent(loader.load());
        DetailsInfoFragmentController controller = loader.getController();

        Platform.runLater(() -> {
            if (mContent != null) {
                mServer = AtsumeruSource.getServerById(Math.toIntExact(mContent.getParserId()));
            }

            ViewUtils.createOnMouseEnterExitedBorderEffect(btnUpload, btnRescan, btnEditMetadata, bntGoBack);

            ViewUtils.setNodeVisibleAndManaged(true, lblVolumes);
            ViewUtils.setNodeVisibleAndManaged(true, btnUpload, btnRescan, btnEditMetadata); // TODO: 15.08.2021 и проверка наличия authority
            btnUpload.setOnMouseClicked(event -> UploadFilesDialogController.createAndShow(mContent.getTitle(), mContent.getContentId(), this));
            btnRescan.setOnMouseClicked(event -> AtsumeruSource.requestSerieRescan(mContent.getContentId(), () -> FXUtils.runDelayed(this::onImportDone, 1000)));
            btnEditMetadata.setOnMouseClicked(event -> {
                ContentEditDialogController.createAndShow(
                        mSerie,
                        (observable, oldSerie, serie) -> {
                            if (serie != null) {
                                Platform.runLater(() -> {
                                    setSerieItem(serie);
                                    controller.fillContentInfo(mContent);
                                });
                            }
                        },
                        TabAtsumeruLibraryController.isSeriesOrSinglesPresentation(),
                        null
                );
            });

            // Styles
            lblVolumes.setStyle(LABEL_SELECTED_STYLE);
            lblVolumes.setOnMouseClicked(event -> {
                mAtsumeruReadableType = ReadableType.VOLUMES;
                lblVolumes.setStyle(LABEL_SELECTED_STYLE);
                lblChapters.setStyle(LABEL_DESELECTED_STYLE);
                loadContentFiles();
            });
            lblChapters.setOnMouseClicked(event -> {
                mAtsumeruReadableType = ReadableType.CHAPTERS;
                lblChapters.setStyle(LABEL_SELECTED_STYLE);
                lblVolumes.setStyle(LABEL_DESELECTED_STYLE);
                loadContentFiles();
            });

            // Context menu
            contextMenu = new ContextMenu();
            contextMenu.getItems().addAll(
                    itemEdit = new MenuItem(LocaleManager.getString("gui.button.edit"), createContextMenuIconView(MaterialDesignIcon.PENCIL)),
                    itemCopyID = new MenuItem(LocaleManager.getString("atsumeru.context_menu.copy_volume_id"), createContextMenuIconView(MaterialDesignIcon.CONTENT_COPY)),
                    itemCopyName = new MenuItem(LocaleManager.getString("atsumeru.context_menu.copy_volume_file_name"), createContextMenuIconView(MaterialDesignIcon.CONTENT_COPY)),
                    itemCopyPath = new MenuItem(LocaleManager.getString("atsumeru.context_menu.copy_volume_file_path"), createContextMenuIconView(MaterialDesignIcon.CONTENT_COPY)),
                    itemReloadCover = new MenuItem(LocaleManager.getString("atsumeru.context_menu.reload_cover"), createContextMenuIconView(MaterialDesignIcon.RELOAD)),
                    itemDeleteHistory = new MenuItem(LocaleManager.getString("atsumeru.context_menu.delete_history"), createContextMenuIconView(MaterialDesignIcon.DELETE))
            );

            controller.fillContentInfo(mContent);
            loadContentFiles();
        });
    }

    private MaterialDesignIconView createContextMenuIconView(MaterialDesignIcon icon) {
        return ViewUtils.createMaterialDesignIconView(icon, 20, "white");
    }

    @FXML
    void goBack() {
        if (backstack.size() > 0) {
            Pair<String, List<Node>> backstackEntry = backstack.get(backstack.size() - 1);
            fpItemsList.getChildren().clear();
            fpItemsList.getChildren().addAll(backstackEntry.getSecond());

            backstack.remove(backstack.size() - 1);
            setFileListLabelTextFormatted();
            ViewUtils.setNodeVisibleAndManaged(backstack.size() > 0, backstack.size() > 0, bntGoBack);
        }
    }

    private void loadContentFiles() {
        showLoading();
        // TODO: errors
        if (mAtsumeruReadableType == ReadableType.VOLUMES) {
            AtsumeruAPI.getBookVolumes(mContent.getContentId())
                    .cache()
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(Schedulers.io())
                    .subscribe(this::updateAdapter, Throwable::printStackTrace);
        } else {
            AtsumeruAPI.getBookChapters(mContent.getContentId())
                    .cache()
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(Schedulers.io())
                    .subscribe(this::updateAdapterWithChapters, Throwable::printStackTrace);
        }
    }

    public <T extends Readable> void updateAdapter(List<T> readableList) {
        Platform.runLater(() -> {
            List<Parent> layouts = new ArrayList<>();
            readableList.forEach(readable -> {
                try {
                    int currentPage = Math.max(readable.getHistory().getCurrentPage(), 0);

                    FXMLLoader fxmlLoader = new FXMLLoader(FXApplication.class.getResource("/fxml/cards/CardContent.fxml"), LocaleManager.getResourceBundle());
                    Parent layout = fxmlLoader.load();
                    CardContentController controller = fxmlLoader.getController();
                    setCardInfoTooltip(controller.container, readable);
                    controller.setServer(mServer);
                    controller.setContentId(readable.getId());
                    controller.setTitle(readable.getTitle());
                    if (readable instanceof Volume) {
                        Volume volume = (Volume) readable;
                        controller.setSubtitle(volume.getAdditionalTitle());
                        controller.setSubSubtitle(GUString.isNotEmpty(volume.getYear())
                                ? volume.getYear()
                                : GUDate.DateFormat.format(GUDate.DateFormat.Complete, readable.getCreatedAt()));
                    } else {
                        controller.setSubSubtitle(GUDate.DateFormat.format(GUDate.DateFormat.Complete, readable.getCreatedAt()));
                    }
                    controller.setCoverUrl(AtsumeruAdapter.getCoverLink(readable.getId()));
                    controller.setCoverAccent(readable.getCoverAccent());
                    controller.setProgressCurrent(currentPage);
                    controller.setProgressTotal(readable.getPagesCount());
                    controller.setProgressInfo(LocaleManager.getString("gui.read_progress.pages", currentPage, readable.getPagesCount()));
                    controller.setDimCompletedContent(true);
                    controller.setContainerNode(fpItemsList);
                    controller.setOnMouseClicked(event -> {
                        if (event.getButton() == MouseButton.PRIMARY) {
                            if (!openContentInAction) {
                                openContentInAction = true;
                                List<List<Chapter>> chapters = AtsumeruSource.getAdapter(null).toContentChapters(readableList, mContent, mServer);
                                StageReaderController.showReader(
                                        mContent.getTitle(),
                                        chapters,
                                        chapters.stream()
                                                .filter(chapter -> chapter.get(0).getCHash().equals(readable.getId()))
                                                .collect(Collectors.toList())
                                                .get(0)
                                                .get(0),
                                        (chash, readedPages, countPages) -> {
                                            CardContentController currentController = cardControllersMap.get(chash);
                                            readable.getHistory().setCurrentPage(readedPages);
                                            currentController.setProgressCurrent(readedPages);
                                            currentController.setProgressInfo(LocaleManager.getString("gui.read_progress.pages", readedPages, countPages));
                                            currentController.setDimCompletedContent(true);
                                            currentController.setProgress();
                                        }
                                );
                                openContentInAction = false;
                            }
                        } else if (event.getButton() == MouseButton.SECONDARY) {
                            showReadableContextMenu(event, layout, readable);
                        }
                    });

                    cardControllersMap.put(readable.getId(), controller);
                    layouts.add(layout);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });

            fpItemsList.setHgap(10);
            fpItemsList.setVgap(10);

            Platform.runLater(() -> {
                fpItemsList.getChildren().clear();
                fpItemsList.getChildren().addAll(layouts);

                showFilesList();
            });
        });
    }

    private void updateAdapterWithChapters(List<com.atsumeru.api.model.Chapter> chapters) {
        Platform.runLater(() -> fpItemsList.getChildren().clear());

        mChapters = chapters.stream()
                .map(chapter -> AtsumeruAdapter.mapToChapter(chapter, mContent, mServer))
                .collect(Collectors.toList());

        mChapters.forEach(this::createFileCard);

        Platform.runLater(this::showFilesList);
    }

    private void setCardInfoTooltip(Node node, Readable readable) {
        if (readable instanceof Volume) {
            Volume volume = (Volume) readable;
            if (GUString.isNotEmpty(volume.getFileName())) {
                ViewUtils.addTooltipToNode(
                        node,
                        LocaleManager.getString(
                                "atsumeru.tooltip.volume_info",
                                volume.getId(),
                                volume.getFileName(),
                                volume.getFilePath()
                        ),
                        13);
            }
        }
    }

    private void showReadableContextMenu(MouseEvent event, Parent layout, Readable readable) {
        contextMenu.hide();

        itemEdit.setOnAction(event1 -> {
            AtomicReference<Disposable> disposable = new AtomicReference<>();
            disposable.set(
                    AtsumeruAPI.getBookDetails(readable.getId())
                            .subscribeOn(Schedulers.newThread())
                            .observeOn(Schedulers.io())
                            .subscribe(serie -> {
                                Platform.runLater(() -> ContentEditDialogController.createAndShow(serie, null, false, this::loadContentFiles));
                                RXUtils.safeDispose(disposable);
                            }, throwable -> {
                                throwable.printStackTrace();
                                Snackbar.showSnackBar(
                                        MainController.getSnackbarRoot(),
                                        LocaleManager.getString("gui.error.unable_to_get_data"),
                                        Snackbar.Type.ERROR
                                );
                            })
            );
        });
        itemCopyID.setOnAction(event1 -> FXUtils.copyToClipboard(readable.getId()));
        if (readable instanceof Volume) {
            Volume volume = (Volume) readable;
            if (GUString.isNotEmpty(volume.getFileName())) {
                itemCopyName.setOnAction(event1 -> FXUtils.copyToClipboard(volume.getFileName()));
                itemCopyPath.setOnAction(event1 -> FXUtils.copyToClipboard(volume.getFilePath()));
            }
        }
        itemReloadCover.setOnAction(event1 -> cardControllersMap.get(readable.getId()).reloadPoster());
        itemDeleteHistory.setOnAction(event1 -> {
            AtsumeruSource.getSource(mServer).deleteSyncHistory(readable.getId());
            cardControllersMap.get(readable.getId()).resetProgress();
        });

        contextMenu.show(layout, event.getScreenX(), event.getScreenY());
    }

    private void createFileCard(List<Chapter> chapter) {
        ContentFileItemCardController controller = ContentFileItemCardController.createNode(fpItemsList.widthProperty(), this, chapter);
        controllers.add(controller);
        Platform.runLater(() -> fpItemsList.getChildren().add(controller.getRoot()));
    }

    private void showFilesList() {
        ViewUtils.setNodeGone(spinnerLoading);
        new Thread(() -> FXUtils.runLaterDelayed(() -> {
            if (fpItemsList.getChildren().size() > 12) {
                spItemsList.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
            }
            ViewUtils.setVerticalScrollPaneOnScroll(spItemsList, fpItemsList.getChildren().size() / 3f);
            showFileListNodes();
        }, 100)).start();
    }

    private void setFileListLabelTextFormatted() {
        List<String> backstackNames = backstack.stream()
                .map(Pair::getFirst)
                .collect(Collectors.toList());

        if (GUArray.isNotEmpty(backstackNames)) {
            lblChapters.setText(String.format("%s (%s)", lblChapters.getText(), GUString.join(" > ", backstackNames)));
        }
    }

    private void showLoading() {
        ViewUtils.setNodeGone(fpItemsList);
        ViewUtils.setNodeVisible(spinnerLoading);
    }

    private void showFileListNodes() {
        ViewUtils.setNodeVisible(hbHeader, fpItemsList);
        ViewUtils.setNodeGone(spinnerLoading);
    }

    private void hideFileListAndBackNode() {
        ViewUtils.setNodeInvisible(hbHeader, fpItemsList);
        ViewUtils.setNodeGone(bntGoBack);
    }

    public Pane getContentRoot() {
        return contentRoot;
    }

    @Override
    public String getContentTitle() {
        return mContent.getTitle();
    }

    @Override
    public Content getContent() {
        return mContent;
    }

    @Override
    public List<List<Chapter>> getChapters() {
        return mChapters;
    }

    public void setChapters(List<List<Chapter>> chapters) {
        this.mChapters = chapters;
    }

    @Override
    public void onImportDone() {
        AtsumeruHelper.addOnImportDoneListener(() -> Platform.runLater(this::loadContentFiles));
    }
}
