package xyz.atsumeru.manager.cell;

import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import org.controlsfx.control.GridCell;
import org.controlsfx.control.GridView;
import xyz.atsumeru.manager.controller.cards.CardContentController;
import xyz.atsumeru.manager.listeners.OnButtonClickListener;
import xyz.atsumeru.manager.utils.FXUtils;
import xyz.atsumeru.manager.utils.ViewUtils;

public abstract class BaseGridCell<T> extends GridCell<T> {
    public static final String ACTION_VIEW = "view";
    public static final String ACTION_EDIT = "edit";
    public static final String ACTION_CHANGE_CATEGORY = "change_category";
    public static final String ACTION_CHANGE_SELECTION = "change_selection";
    public static final String ACTION_REMOVE = "remove";

    private final CardContentController controller;

    protected final GridView<T> gridView;
    protected final OnButtonClickListener onButtonClickListener;

    public BaseGridCell(GridView<T> gridView, OnButtonClickListener onButtonClickListener) {
        this.gridView = gridView;
        this.onButtonClickListener = onButtonClickListener;

        this.controller = FXUtils.loadFXMLAndGetController("/fxml/cards/CardContent.fxml");
        this.controller.setContainerNode(gridView);
    }

    protected Node getRoot() {
        return controller.root;
    }

    protected Node getContainer() {
        return controller.container;
    }

    protected void bindData() {
        controller.bindData();
    }

    protected void setContentId(String contentId) {
        controller.setContentId(contentId);
    }

    protected void setTitle(String title) {
        controller.setTitle(title);
    }

    protected void setSubtitle(String subtitle) {
        controller.setSubtitle(subtitle);
    }

    protected void setScore(String score) {
        controller.setScore(score);
    }

    protected void setMature(boolean isMature) {
        controller.setMature(isMature);
    }

    protected void setAdult(boolean isAdult) {
        controller.setAdult(isAdult);
    }

    protected void setCoverViewId(String id) {
        controller.ivPoster.setId(id);
    }

    protected void setCoverImage(Image coverImage, boolean withAnimations, boolean loadNow) {
        if (coverImage != null) {
            Platform.runLater(() -> ViewUtils.setNodeVisible(controller.spCover));
            controller.setSetCoverWithAnimations(withAnimations);
            controller.onLoad(coverImage, null, false, loadNow);
        } else {
            Platform.runLater(() -> ViewUtils.setNodeInvisible(controller.spCover));
            controller.ivPoster.setImage(null);
            controller.ivLandscapePoster.setImage(null);
        }
    }

    protected Image getCoverImage() {
        return controller.ivPoster.getImage();
    }

    protected ImageView getCoverImageView() {
        return controller.ivPoster;
    }

    protected void setCoverAccent(String coverAccent) {
        controller.setCoverAccent(coverAccent);
    }

    protected void showLoading() {
        ViewUtils.setNodeVisible(controller.spinnerLoading);
    }

    protected void setProgress(int progressCurrent, int progressTotal, String progressInfo, boolean twoLineProgress) {
        controller.setProgressCurrent(progressCurrent);
        controller.setProgressTotal(progressTotal);
        controller.setProgressInfo(progressInfo);
        controller.setTwoLineProgress(twoLineProgress);
    }

    protected void setDimCompletedContent(boolean dimCompletedContent) {
        controller.setDimCompletedContent(dimCompletedContent);
    }

    protected void setOnAction(EventHandler<? super MouseEvent> onMouseClicked) {
        controller.setOnMouseClicked(onMouseClicked);
    }

    protected StackPane getSelectionPane() {
        return controller.selection;
    }

    protected void setImageAndBackground(Image coverImage, boolean withAnimations, boolean loadNow) {
        setCoverImage(coverImage, withAnimations, loadNow);
    }
}