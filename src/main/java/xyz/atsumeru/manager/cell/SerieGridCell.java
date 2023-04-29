package xyz.atsumeru.manager.cell;

import com.atsumeru.api.model.Serie;
import com.atsumeru.api.model.Volume;
import com.atsumeru.api.model.sync.History;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIconView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import lombok.Getter;
import org.controlsfx.control.GridView;
import xyz.atsumeru.manager.FXApplication;
import xyz.atsumeru.manager.adapter.AtsumeruAdapter;
import xyz.atsumeru.manager.enums.ContentType;
import xyz.atsumeru.manager.enums.Status;
import xyz.atsumeru.manager.helpers.ImageCache;
import xyz.atsumeru.manager.helpers.LocaleManager;
import xyz.atsumeru.manager.listeners.OnButtonClickListener;
import xyz.atsumeru.manager.source.AtsumeruSource;
import xyz.atsumeru.manager.utils.FXUtils;
import xyz.atsumeru.manager.utils.ViewUtils;
import xyz.atsumeru.manager.utils.globalutils.GUArray;
import xyz.atsumeru.manager.utils.globalutils.GUEnum;
import xyz.atsumeru.manager.utils.globalutils.GUString;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SerieGridCell extends BaseGridCell<Serie> implements ImageCache.ImageLoadCallback {
    public static final Map<Integer, SerieGridCell> CELLS = new HashMap<>();
    public static final Map<String, Serie> SELECTED_ITEMS = new HashMap<>();
    private final StackPane selectedPane;
    private final ContextMenu contextMenu;
    private final MenuItem itemView;
    private final MenuItem itemEdit;
    private final MenuItem itemChangeCategory;
    private final MenuItem itemSelect;
    private final MenuItem itemCopyID;
    private final MenuItem itemCopyPath;
    private final MenuItem itemReloadCover;
    private final MenuItem itemRemove;
    @Getter
    private Serie serie;

    private boolean isPanning;

    public SerieGridCell(GridView<Serie> gridView, OnButtonClickListener onButtonClickListener) {
        super(gridView, onButtonClickListener);
        CELLS.put(this.hashCode(), this);
        super.setDimCompletedContent(true);

        selectedPane = new StackPane();
        configureSelectedPane();

        contextMenu = new ContextMenu();
        contextMenu.getItems().addAll(
                itemView = new MenuItem(LocaleManager.getString("gui.button.view"), createContextMenuIconView(MaterialDesignIcon.OPEN_IN_NEW)),
                itemEdit = new MenuItem(LocaleManager.getString("gui.button.edit"), createContextMenuIconView(MaterialDesignIcon.PENCIL)),
                itemChangeCategory = new MenuItem(LocaleManager.getString("gui.button.change_category"), createContextMenuIconView(MaterialDesignIcon.TAG)),
                itemSelect = new MenuItem(LocaleManager.getString("context.select"), createContextMenuIconView(MaterialDesignIcon.SELECTION)),
                itemCopyID = new MenuItem(LocaleManager.getString("atsumeru.context_menu.copy_volume_id"), createContextMenuIconView(MaterialDesignIcon.CONTENT_COPY)),
                itemCopyPath = new MenuItem(LocaleManager.getString("atsumeru.context_menu.copy_volume_folder_path"), createContextMenuIconView(MaterialDesignIcon.CONTENT_COPY)),
                itemReloadCover = new MenuItem(LocaleManager.getString("atsumeru.context_menu.reload_cover"), createContextMenuIconView(MaterialDesignIcon.RELOAD)),
                itemRemove = new MenuItem(LocaleManager.getString("gui.button.remove"), createContextMenuIconView(MaterialDesignIcon.DELETE))
        );

        addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> isPanning = true);
        addEventFilter(MouseEvent.MOUSE_RELEASED, event -> FXUtils.runLaterDelayed(() -> isPanning = false, 150));
    }

    public static void clearAll() {
        CELLS.clear();
        SELECTED_ITEMS.clear();
    }

    public static void updateAll() {
        CELLS.values().forEach(cell -> cell.updateItem(cell.getSerie(), false));
    }

    private void configureSelectedPane() {
        ImageView ivSelected = new ImageView(new Image("/images/icons/round_done_white_48.png", 48, 48, true, true));
        selectedPane.prefWidthProperty().bind(super.getCoverImageView().fitWidthProperty());
        selectedPane.prefHeightProperty().bind(super.getCoverImageView().fitHeightProperty());
        selectedPane.setBackground(new Background(new BackgroundFill(Color.rgb(219, 50, 33, 0.75), CornerRadii.EMPTY, Insets.EMPTY)));
        selectedPane.getChildren().add(ivSelected);
        StackPane.setAlignment(selectedPane, Pos.CENTER);
        super.getSelectionPane().getChildren().add(selectedPane);
    }

    private MaterialDesignIconView createContextMenuIconView(MaterialDesignIcon icon) {
        return ViewUtils.createMaterialDesignIconView(icon, 20, "white");
    }

    @Override
    public void updateItem(Serie item, boolean empty) {
        super.updateItem(item, empty);
        serie = item;

        if (empty || item == null) {
            super.setCoverViewId(null);
            setGraphic(null);
        } else {
            setSelectionCardEffect();
            super.setCoverViewId(item.getCover());
            if (GUString.isNotEmpty(item.getCover())) {
                if (!FXApplication.getInstance().getIsAppResizing().get()) {
                    super.setCoverImage(null, false, true);
                }
                if (super.getCoverImage() == null) {
                    loadPoster(item.getCover(), false);
                }
            }

            super.setTitle(item.getTitle());
            super.setSubtitle(
                    Optional.ofNullable(GUString.getFirstNotEmptyValue(item.getAltTitle(), item.getJapTitle(), item.getKorTitle()))
                            .filter(GUString::isNotEmpty)
                            .filter(title -> !GUString.equalsIgnoreCase(item.getTitle(), title))
                            .orElse(null)
            );

            super.setScore(serie.getScore());
            super.setMature(serie.isMature());
            super.setAdult(serie.isAdult());

            setProgressInfo();
            super.setCoverAccent(serie.getCoverAccent());

            super.bindData();
            setGraphic(super.getRoot());

            super.setOnAction(event -> {
                if (!isPanning) {
                    if (event.getButton() == MouseButton.PRIMARY) {
                        if (!isInSelectionMode()) {
                            onButtonClickListener.onClick(ACTION_VIEW, getIndex(), null);
                        } else {
                            updateSelection();
                        }
                    } else if (event.getButton() == MouseButton.SECONDARY && !isInSelectionMode()) {
                        contextMenu.hide();
                        itemView.setOnAction(event1 -> onButtonClickListener.onClick(ACTION_VIEW, getIndex(), null));
                        itemEdit.setOnAction(event1 -> onButtonClickListener.onClick(ACTION_EDIT, getIndex(), () -> updateItem(item, false)));
                        itemChangeCategory.setOnAction(event1 -> onButtonClickListener.onClick(ACTION_CHANGE_CATEGORY, getIndex(), () -> updateItem(item, false)));
                        itemSelect.setOnAction(event1 -> updateSelection());
                        itemCopyID.setOnAction(event1 -> FXUtils.copyToClipboard(item.getId()));
                        itemCopyPath.setOnAction(event1 -> FXUtils.copyToClipboard(item.getFolder()));
                        itemReloadCover.setOnAction(event1 -> reloadPoster(item.getCover()));
                        itemRemove.setOnAction(event1 -> onButtonClickListener.onClick(ACTION_REMOVE, getIndex(), () -> updateItem(item, false)));

                        if (GUString.isEmpty(item.getFolder())) {
                            contextMenu.getItems().remove(itemCopyPath);
                        }

                        contextMenu.show(super.getRoot(), event.getScreenX(), event.getScreenY());
                    }
                }
            });

            if (GUString.isNotEmpty(item.getFolder())) {
                ViewUtils.addTooltipToNode(super.getContainer(), LocaleManager.getString("atsumeru.tooltip.serie_info", item.getId(), item.getFolder()), 13);
            }
        }
    }

    private void setProgressInfo() {
        int readVolumes = 0;
        int totalVolumes = serie.getVolumes().size();
        int readPages = 0;
        int totalPages = 0;

        for (Volume volume : serie.getVolumes()) {
            totalPages += volume.getPagesCount();

            History history = volume.getHistory();
            if (history.getPagesCount() > 0) {
                if (history.getCurrentPage() == history.getPagesCount()) {
                    readVolumes++;
                }
                readPages += history.getCurrentPage();
            }
        }

        String volumeStr = null;
        boolean twoLineProgress = false;
        Status status = GUEnum.valueOf(Status.class, serie.getStatus());
        if (status == Status.SINGLE) {
            volumeStr = LocaleManager.getString("enum.single");
        } else if (status == Status.ANTHOLOGY && totalVolumes == 1) {
            volumeStr = LocaleManager.getString("enum.anthology");
        } else if (totalVolumes > 0) {
            String message;
            if (GUEnum.valueOf(ContentType.class, serie.getContentType()) == ContentType.COMICS) {
                message = "atsumeru.issues_and_pages_progress";
            } else if (status == Status.MAGAZINE) {
                message = "atsumeru.magazines_and_pages_progress";
            } else {
                message = "atsumeru.volumes_and_pages_progress";
            }

            twoLineProgress = !(totalVolumes == 1 && serie.isSingle());
            volumeStr = twoLineProgress
                    ? LocaleManager.getString(message, readVolumes, totalVolumes, readPages, totalPages)
                    : LocaleManager.getString("atsumeru.pages_progress", readPages, totalPages);
        } else {
            int volumesCount = (int) serie.getVolumesCount();
            if (volumesCount >= 0) {
                volumeStr = LocaleManager.getStringFormatted("gui.volumes_with_number", volumesCount);
            }
        }

        super.setProgress(readPages, totalPages, volumeStr, twoLineProgress);
    }

    private void updateSelection() {
        if (!SELECTED_ITEMS.containsKey(serie.getId())) {
            SELECTED_ITEMS.put(serie.getId(), serie);
        } else {
            SELECTED_ITEMS.remove(serie.getId());
        }
        updateItem(serie, false);
        onButtonClickListener.onClick(ACTION_CHANGE_SELECTION, getIndex(), null);
    }

    private boolean isInSelectionMode() {
        return GUArray.isNotEmpty(SELECTED_ITEMS);
    }

    private void setSelectionCardEffect() {
        ViewUtils.setNodeVisibleAndManaged(isItemSelected(), selectedPane);
    }

    private boolean isItemSelected() {
        return SELECTED_ITEMS.containsKey(serie.getId());
    }

    private void reloadPoster(String imageHash) {
        super.setCoverViewId(imageHash);
        super.setCoverImage(null, false, true);

        String coverHash = AtsumeruAdapter.getCoverHash(imageHash);
        File coverFile = ImageCache.getFile(coverHash, ImageCache.ImageCacheType.THUMBNAIL);
        boolean success = coverFile.delete();
        ImageCache.removeFromCache(coverHash);
        System.out.println("Cover in path " + coverFile + " removed: " + success);
        loadPoster(imageHash, true);
    }

    protected void loadPoster(String imageHash, boolean forceLoad) {
        String coverHash = AtsumeruAdapter.getCoverHash(imageHash);
        String coverLink = AtsumeruAdapter.getCoverLink(imageHash);

        Image inMemoryImage = ImageCache.getFromInMemoryCache(coverHash, ImageCache.ImageCacheType.THUMBNAIL);
        if (inMemoryImage == null || forceLoad) {
            super.showLoading();

            ImageCache.create(coverHash, coverLink)
                    .withContentId(imageHash)
                    .withCacheType(ImageCache.ImageCacheType.THUMBNAIL)
                    .withHeaders(AtsumeruSource.createAuthorizationHeaders())
                    .withBackgroundLoadingFromFS()
                    .withCallback(this)
                    .getAsync();
        } else {
            setImageAndBackground(inMemoryImage, false, true);
        }
    }

    @Override
    public void onLoad(Image image, String contentId, boolean fromCache, boolean loadNow) {
        if (contentId.equals(super.getCoverImageView().getId())) {
            setImageAndBackground(image, !fromCache, loadNow);
        }
    }
}