package xyz.atsumeru.manager.controller.cards;

import com.atsumeru.api.model.server.Server;
import io.github.palexdev.materialfx.controls.MFXProgressSpinner;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import lombok.Getter;
import lombok.Setter;
import org.controlsfx.control.GridView;
import xyz.atsumeru.manager.enums.GridScaleType;
import xyz.atsumeru.manager.helpers.ImageCache;
import xyz.atsumeru.manager.helpers.LayoutHelpers;
import xyz.atsumeru.manager.helpers.LocaleManager;
import xyz.atsumeru.manager.managers.Settings;
import xyz.atsumeru.manager.managers.StatusBarManager;
import xyz.atsumeru.manager.source.AtsumeruSource;
import xyz.atsumeru.manager.utils.ColorUtils;
import xyz.atsumeru.manager.utils.FXUtils;
import xyz.atsumeru.manager.utils.ViewUtils;
import xyz.atsumeru.manager.utils.globalutils.GUString;

import java.io.File;
import java.util.Optional;

public class CardContentController implements ImageCache.ImageLoadCallback {
    private static final int PROGRESS_BAR_HEIGHT = 6;

    @FXML
    public VBox root;
    @FXML
    public StackPane container;
    @FXML
    public StackPane selection;
    @FXML
    public Pane pnInfoMask;
    @FXML
    public StackPane spProgressInfo;
    @FXML
    public Label lblProgressInfo;
    @FXML
    public ProgressBar pbProgress;
    @FXML
    public MFXProgressSpinner spinnerLoading;
    @FXML
    public StackPane spCover;
    @FXML
    public ImageView ivPoster;
    @FXML
    public ImageView ivLandscapePoster;
    @FXML
    public Label lblScore;
    @FXML
    public Label lblTitle;
    @FXML
    public Label lblSubtitle;
    @FXML
    public Label lblSubSubtitle;
    @FXML
    public ImageView ivAgeRatingBadge;

    @Setter
    private Node containerNode;
    @Getter
    @Setter
    private Server server;
    @Getter
    @Setter
    private String contentId;
    @Setter
    private String title;
    @Setter
    private String subtitle;
    @Setter
    private String subSubtitle;
    @Setter
    private String score;
    @Setter
    private boolean isMature;
    @Setter
    private boolean isAdult;
    @Setter
    private String coverUrl;
    @Setter
    private String coverAccent;
    @Setter
    private int progressCurrent;
    @Setter
    private int progressTotal;
    @Setter
    private String progressInfo;
    @Setter
    private boolean twoLineProgress;
    @Setter
    private boolean setCoverWithAnimations = true;
    @Setter
    private boolean dimCompletedContent;
    private EventHandler<? super MouseEvent> onMouseClicked;

    private DoubleBinding fixedColumnsBinding = null;
    private DoubleBinding proportionalColumnsBinding = null;

    private boolean accentColorSet;

    @FXML
    private void initialize() {
        ivLandscapePoster.setEffect(LayoutHelpers.BACKGROUND_ADJUST_BOX_BLUR);

        Platform.runLater(() -> {
            Color col = Color.web("#2d2d2d");
            CornerRadii corn = new CornerRadii(8);
            Background background = new Background(new BackgroundFill(col, corn, new Insets(3, 0, 3, 0)));
            lblScore.setBackground(background);

            setHoverEffects();

            Rectangle clipRectangle = createRectangle(206);
            container.setClip(clipRectangle);

            Rectangle clipPosterRectangle = null;
            if (pbProgress.isManaged()) {
                clipPosterRectangle = createRectangle(200);
                ivPoster.setClip(clipPosterRectangle);
            }

            bindWidthProperty(Settings.getGridScaleType(), clipRectangle, clipPosterRectangle);
            root.setOnMouseClicked(event -> onMouseClicked.handle(event));
        });

        bindData();
    }

    public void bindData() {
        Platform.runLater(() -> {
            setProgress();
            ViewUtils.setNodeInvisible(pbProgress);

            spProgressInfo.prefHeightProperty().set(twoLineProgress ? 40 : 20);

            if (GUString.isNotEmpty(coverUrl)) {
                loadPoster();
            }

            lblTitle.setText(title);
            lblTitle.setTooltip(new Tooltip(title));

            ViewUtils.setTextWithTooltipOrMakeGone(lblSubtitle, subtitle);
            ViewUtils.setTextWithTooltipOrMakeGone(lblSubSubtitle, subSubtitle);

            if (isMature) {
                ivAgeRatingBadge.setImage(new Image("/images/mature.png"));
            } else if (isAdult) {
                ivAgeRatingBadge.setImage(new Image("/images/adults_only.png"));
            }
            ViewUtils.setNodeVisibleAndManaged(isMature || isAdult, ivAgeRatingBadge);

            boolean isHasScore = GUString.isNotEmpty(score);
            ViewUtils.setNodeVisibleAndManaged(isHasScore, isHasScore, lblScore);
            lblScore.setText(String.format("★ %s", Optional.ofNullable(score).map(score -> score.replace("★", "")).orElse("")));

            StackPane.setMargin(pnInfoMask, new Insets(0, 0, pbProgress.isVisible() ? 6 : 0, 0));
            FXUtils.runLaterDelayed(() -> setAccentColorForNodes(getCoverAccentColor()), 50);
        });
    }

    public void setHoverEffects() {
        container.setOnMouseEntered(event -> {
            ivPoster.setEffect(isProgressFull() && dimCompletedContent ? LayoutHelpers.COVER_ADJUST_MONOCHROME_HOVER : LayoutHelpers.COVER_ADJUST_HOVER);
            ivLandscapePoster.setEffect(LayoutHelpers.BACKGROUND_ADJUST_BOX_BLUR_HOVER);
        });

        container.setOnMouseExited(event -> {
            ivPoster.setEffect(isProgressFull() && dimCompletedContent ? LayoutHelpers.COVER_ADJUST_MONOCHROME : LayoutHelpers.COVER_ADJUST_DEFAULT);
            ivLandscapePoster.setEffect(LayoutHelpers.BACKGROUND_ADJUST_BOX_BLUR);
        });
    }

    public void setProgress() {
        if (progressTotal > 0) {
            pbProgress.setProgress((float) progressCurrent / progressTotal);
        }
        lblProgressInfo.setText(progressInfo);
    }

    private boolean isProgressFull() {
        return pbProgress.getProgress() >= 1.0;
    }

    public void resetProgress() {
        progressCurrent = 0;
        progressInfo = LocaleManager.getString("gui.read_progress.pages", progressCurrent, progressTotal);
        setProgress();
    }

    public void reloadPoster() {
        ivPoster.setImage(null);
        File coverFile = ImageCache.getFile(contentId, ImageCache.ImageCacheType.THUMBNAIL);
        boolean success = coverFile.delete();
        ImageCache.removeFromCache(contentId);
        System.out.println("Cover in path " + coverFile + " removed: " + success);
        loadPoster();
    }

    private void loadPoster() {
        Platform.runLater(() -> {
            clearPosterImageView();
            ivPoster.setCache(true);
            ViewUtils.setNodeVisible(spinnerLoading);
        });

        ImageCache.create(contentId, coverUrl)
                .withCacheType(ImageCache.ImageCacheType.THUMBNAIL)
                .withHeaders(AtsumeruSource.createAuthorizationHeaders())
                .withCallback(this)
                .getAsync();
    }

    public void clearPosterImageView() {
        ivPoster.setCache(false);
        ivPoster.setImage(null);
    }

    private Rectangle createRectangle(int height) {
        Rectangle rectangle = new Rectangle();
        rectangle.setArcHeight(16);
        rectangle.setArcWidth(16);
        rectangle.setWidth(150.0);
        rectangle.setHeight(height);
        return rectangle;
    }

    private void bindWidthProperty(GridScaleType gridScaleType, Rectangle clipRectangle, Rectangle clipPosterRectangle) {
        DoubleBinding columnsBinding = gridScaleType == GridScaleType.FIXED_SCALE ? getFixedWidthBinding() : getProportionalWidthBinding();
        bindViewsWidthProperties(clipRectangle, clipPosterRectangle, columnsBinding);
    }

    private DoubleProperty getColumnsProperty() {
        return server != null
                ? new SimpleDoubleProperty(4)
                : StatusBarManager.getGridSliderValueProperty();
    }

    private DoubleBinding getProportionalWidthBinding() {
        DoubleProperty columnsProperty = getColumnsProperty();
        if (proportionalColumnsBinding == null) {
            ReadOnlyDoubleProperty widthProperty = getContainerWidthProperty();
            proportionalColumnsBinding = Bindings.createDoubleBinding(() -> {
                double columnsFromPrefs = (double) Math.round(columnsProperty.get());
                double containerWidth = widthProperty.get() - getContainerHGap();

                int maxAttempts = 10;
                double cardWidth = containerWidth / columnsFromPrefs;
                while ((cardWidth < 100 || cardWidth > 200) && maxAttempts > 0) {
                    columnsFromPrefs = columnsFromPrefs + (cardWidth < 100 ? -1 : 1);
                    cardWidth = containerWidth / columnsFromPrefs;
                    maxAttempts--;
                }

                return columnsFromPrefs;
            }, columnsProperty, widthProperty);
        }
        return proportionalColumnsBinding;
    }

    private DoubleBinding getFixedWidthBinding() {
        DoubleProperty columnsProperty = getColumnsProperty();
        return Optional.ofNullable(fixedColumnsBinding)
                .orElseGet(() -> fixedColumnsBinding = Bindings.createDoubleBinding(() -> (double) Math.round(columnsProperty.get()), columnsProperty));
    }

    private void bindViewsWidthProperties(Rectangle clipRectangle, Rectangle clipPosterRectangle, DoubleBinding columnsBinding) {
        ivPoster.fitWidthProperty().bind(getContainerWidthProperty().divide(columnsBinding).subtract(getContainerHGap()));
        DoubleBinding height = ivPoster.fitWidthProperty().add(getContainerHGap()).multiply(1.333);
        ivPoster.fitHeightProperty().bind(height);

        ivLandscapePoster.fitWidthProperty().bind(ivPoster.fitWidthProperty());
        ivLandscapePoster.fitHeightProperty().bind(ivPoster.fitHeightProperty());

        boolean hasProgress = pbProgress.isManaged();
        if (hasProgress) {
            pbProgress.prefWidthProperty().bind(ivPoster.fitWidthProperty());
            pbProgress.prefHeightProperty().bind(height.add(PROGRESS_BAR_HEIGHT));
            pbProgress.minHeightProperty().bind(height.add(PROGRESS_BAR_HEIGHT));
        }

        root.prefWidthProperty().bind(ivPoster.fitWidthProperty());
        root.prefHeightProperty().bind(height);

        container.prefWidthProperty().bind(ivPoster.fitWidthProperty());
        container.prefHeightProperty().bind(hasProgress ? height.add(PROGRESS_BAR_HEIGHT) : height);

        clipRectangle.widthProperty().bind(ivPoster.fitWidthProperty());
        clipRectangle.heightProperty().bind(hasProgress ? height.add(PROGRESS_BAR_HEIGHT) : height);

        clipRectangle.arcWidthProperty().bind(clipRectangle.widthProperty().divide(150).multiply(16));
        clipRectangle.arcHeightProperty().bind(clipRectangle.heightProperty().divide(200).multiply(16));

        if (clipPosterRectangle != null) {
            clipPosterRectangle.widthProperty().bind(clipRectangle.widthProperty());
            clipPosterRectangle.heightProperty().bind(height);

            clipPosterRectangle.arcWidthProperty().bind(clipRectangle.arcWidthProperty());
            clipPosterRectangle.arcHeightProperty().bind(clipRectangle.arcHeightProperty());
        }

        container.prefWidthProperty().addListener((observable, oldValue, newValue) -> {
            container.setClip(clipRectangle);
            if (clipPosterRectangle != null) {
                ivPoster.setClip(clipPosterRectangle);
            }
        });
    }

    public void setOnMouseClicked(EventHandler<? super MouseEvent> onMouseClicked) {
        this.onMouseClicked = onMouseClicked;
    }

    private Color getCoverAccentColor() {
        return Optional.ofNullable(coverAccent)
                .filter(GUString::isNotEmpty)
                .map(Color::web)
                .orElse(null);
    }

    private void setAccentColorForNodes(Color accentColor) {
        if (!accentColorSet) {
            if (accentColor != null) {
                String backgroundStyle = "-fx-background-color: " + FXUtils.toHexString(accentColor);
                String desaturatedBackgroundStyle = "-fx-background-color: " + FXUtils.toHexString(accentColor.desaturate().darker().darker());

                spProgressInfo.setStyle("-fx-background-radius: 6 6 0 0; " + backgroundStyle);
                if (accentColor.getBrightness() >= 0.75) {
                    lblProgressInfo.setStyle("-fx-text-fill: black");
                }

                pbProgress.setBackground(new Background(new BackgroundFill(accentColor, CornerRadii.EMPTY, Insets.EMPTY)));
                Node bar = pbProgress.lookup(".bar");
                if (bar != null) {
                    bar.setStyle(backgroundStyle);
                    accentColorSet = true;
                }

                Node track = pbProgress.lookup(".track");
                if (track != null) {
                    track.setStyle(desaturatedBackgroundStyle);
                    accentColorSet = true;
                }
            }
        }

        ViewUtils.setNodeVisibleAndManaged(GUString.isNotEmpty(progressInfo), true, spProgressInfo);
    }

    private ReadOnlyDoubleProperty getContainerWidthProperty() {
        return ((Region) containerNode).widthProperty();
    }

    private double getContainerHGap() {
        if (containerNode instanceof FlowPane) {
            return ((FlowPane) containerNode).getHgap();
        } else if (containerNode instanceof GridView) {
            return ((GridView<?>) containerNode).getHorizontalCellSpacing() * 2;
        }
        throw new IllegalArgumentException("This Node type is not supported!");
    }

    @Override
    public void onLoad(Image image, String contentId, boolean fromCache, boolean loadNow) {
        if (GUString.isEmpty(coverAccent)) {
            coverAccent = ColorUtils.getImageAccent(image);
            accentColorSet = false;
        }

        Color accentColor = getCoverAccentColor();
        if (loadNow) {
            onLoad(image, accentColor);
        } else {
            Platform.runLater(() -> onLoad(image, accentColor));
        }
    }

    public void onLoad(Image image, Color accentColor) {
        ViewUtils.setNodeGone(spinnerLoading);
        ViewUtils.setNodeVisible(spCover);

        boolean isLandscapeImage = image.getHeight() < image.getWidth();
        if (isLandscapeImage) {
            ViewUtils.setNodeVisible(ivLandscapePoster);
            ivLandscapePoster.setImage(image);
            ViewUtils.centerImage(ivPoster);
        } else {
            ViewUtils.setNodeGone(ivLandscapePoster);
            ViewUtils.resetImagePosition(ivPoster);
        }

        ivPoster.setPreserveRatio(isLandscapeImage);
        ivPoster.setEffect(isProgressFull() && dimCompletedContent ? LayoutHelpers.COVER_ADJUST_MONOCHROME : LayoutHelpers.COVER_ADJUST_DEFAULT);
        ivPoster.setImage(image);

        if (setCoverWithAnimations) {
            FXUtils.animateImageSet(ivPoster, Duration.seconds(0.2));
            FXUtils.runLaterDelayed(() -> ViewUtils.setNodeVisibleAndManaged(progressTotal > 0 && accentColorSet, pbProgress), 150);
        }

        FXUtils.runLaterDelayed(() -> setAccentColorForNodes(accentColor), 50);
        if (!setCoverWithAnimations) {
            ViewUtils.setNodeVisibleAndManaged(progressTotal > 0 && accentColorSet, pbProgress);
        }
    }
}
