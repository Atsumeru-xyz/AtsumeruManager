package xyz.atsumeru.manager.utils;

import com.jfoenix.controls.base.IFXLabelFloatControl;
import com.jfoenix.validation.RequiredFieldValidator;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIconView;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import kotlin.Pair;
import kotlin.Triple;
import org.controlsfx.control.GridView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import retrofit2.HttpException;
import xyz.atsumeru.manager.BuildProps;
import xyz.atsumeru.manager.FXApplication;
import xyz.atsumeru.manager.controls.jfoenix.JFXCustomDecorator;
import xyz.atsumeru.manager.controls.jfoenix.JFXRoundedRippler;
import xyz.atsumeru.manager.exceptions.ApiParseException;
import xyz.atsumeru.manager.helpers.LocaleManager;
import xyz.atsumeru.manager.utils.globalutils.GUArray;
import xyz.atsumeru.manager.utils.globalutils.GUString;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Optional;

public class ViewUtils {

    public static final String DEFAULT_BORDER_CSS = "border-transparent";
    public static final String HOVER_BORDER_CSS = "border-colored";
    public static final String DEFAULT_BACKGROUND_CSS = "background-transparent";
    public static final String HOVER_BACKGROUND_CSS = "background-colored";

    public static void setNodeVisible(@Nullable Node... nodes) {
        setNodeVisibleAndManaged(true, true, nodes);
    }

    public static void setNodeInvisible(@Nullable Node... nodes) {
        setNodeVisibleAndManaged(false, true, nodes);
    }

    public static void setNodeUnmanaged(@Nullable Node... nodes) {
        setNodeVisibleAndManaged(true, false, nodes);
    }

    public static void setNodeGone(@Nullable Node... nodes) {
        setNodeVisibleAndManaged(false, false, nodes);
    }

    public static void setNodeVisibleAndManaged(boolean isVisibleAndManaged, @Nullable Node... nodes) {
        setNodeVisibleAndManaged(isVisibleAndManaged, isVisibleAndManaged, nodes);
    }

    public static void setNodeVisibleAndManaged(boolean isVisible, boolean isManaged, @Nullable Node... nodes) {
        if (GUArray.isNotEmpty(nodes)) {
            for (Node node : nodes) {
                node.setVisible(isVisible);
                node.setManaged(isManaged);
            }
        }
    }

    public static void setVerticalScrollPaneOnScroll(ScrollPane scrollPane, double deltaDivide) {
        scrollPane.getContent().setOnScroll(scrollEvent -> {
            double deltaY = scrollEvent.getDeltaY() / 60 / deltaDivide;
            scrollPane.setVvalue(scrollPane.getVvalue() - deltaY);
        });
    }

    public static void setVBoxOnScroll(VBox vbox, ScrollPane scrollPane, double deltaFactor) {
        //https://stackoverflow.com/questions/32739269/how-do-i-change-the-amount-by-which-scrollpane-scrolls
        vbox.setOnScroll(e -> {
            double deltaY = e.getDeltaY() * deltaFactor; // *6 to make the scrolling a bit faster
            double width = scrollPane.getContent().getBoundsInLocal().getWidth();
            double vvalue = scrollPane.getVvalue();
            scrollPane.setVvalue(vvalue + -deltaY / width); // deltaY/width to make the scrolling equally fast regardless of the actual width of the component
        });
    }

    @SuppressWarnings("rawtypes")
    public static void setGridViewPannable(GridView gridView) {
        ObservableList<Node> nodes = gridView.getChildrenUnmodifiable();
        for (Node node : nodes) {
            if (node instanceof VirtualFlow) {
                VirtualFlow flow = (VirtualFlow) node;
                flow.setPannable(true);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    public static void setGridViewOnScroll(GridView gridView, double deltaFactor) {
        ObservableList<Node> nodes = gridView.getChildrenUnmodifiable();
        for (Node node : nodes) {
            if (node instanceof VirtualFlow) {
                VirtualFlow flow = (VirtualFlow) node;
                flow.setOnScroll(scrollEvent -> {
                    double deltaY = scrollEvent.getDeltaY() * deltaFactor;
                    flow.scrollPixels(-deltaY);
                });
            }
        }
    }

    public static Stage createStageWithAppIcon() {
        Stage stage = new Stage();
        addAppIconToStage(stage);
        return stage;
    }

    public static void addAppIconToStage(Stage stage) {
        stage.getIcons().add(new Image("/images/icons/" + BuildProps.getAppIconName() + ".png"));
    }

    public static void addStylesheetToScene(Scene scene) {
        final ObservableList<String> stylesheets = scene.getStylesheets();
        stylesheets.addAll(
                FXApplication.getResource("/css/theme.css").toExternalForm(),
                FXApplication.getResource("/css/dark/materialfx.css").toExternalForm()
        );
    }

    public static void addStylesheetToSceneWithAccent(Scene scene, Node rootNode, String accentColor) {
        addStylesheetToScene(scene);
        setAppAccentColor(rootNode, accentColor);
    }

    public static void setAppAccentColor(Node rootNode, String hexColor) {
        rootNode.setStyle(String.format("-fx-accent-color: %s;", hexColor));
    }

    public static ImageView createAppIconImageView() {
        ImageView ivAppIcon = new ImageView(createAppIconImage());
        ivAppIcon.setFitWidth(28);
        ivAppIcon.setFitHeight(28);
        return ivAppIcon;
    }

    public static Image createAppIconImage() {
        return new Image("/images/" + BuildProps.getAppIconName() + ".png");
    }

    public static Pair<JFXCustomDecorator, Scene> createDecoratorWithScene(Stage stage, Node root, int width, int height, boolean withIcon) {
        return createDecoratedScene(stage, root, true, true, true, true, width, height, withIcon, null);
    }

    public static Scene createDecoratedScene(Stage stage, Node root, int width, int height) {
        return createDecoratedScene(stage, root, true, true, true, true, width, height, false, null).getSecond();
    }

    public static Scene createDecoratedScene(Stage stage, Node root, boolean customMaximize, boolean fullScreen,
                                             boolean max, boolean min, int width, int height) {
        return createDecoratedScene(stage, root, customMaximize, fullScreen, max, min, width, height, false, null).getSecond();
    }

    public static Pair<JFXCustomDecorator, Scene> createDecoratedScene(Stage stage, Node root, boolean customMaximize, boolean fullScreen, boolean max,
                                                                       boolean min, int width, int height, boolean withIcon, @Nullable Runnable onCloseButtonAction) {
        JFXCustomDecorator decorator = new JFXCustomDecorator(stage, root, fullScreen, max, min);
        decorator.setCustomMaximize(customMaximize);
        if (withIcon) {
            decorator.setGraphic(createAppIconImageView());
        }

        if (onCloseButtonAction != null) {
            decorator.setOnCloseButtonAction(onCloseButtonAction);
        }
        return new Pair<>(decorator, createScene(stage, StageStyle.TRANSPARENT, decorator, width, height));
    }

    public static Scene createScene(Stage stage, StageStyle stageStyle, Parent root, int width, int height) {
        Scene scene = new Scene(root, width, height);
        ViewUtils.addStylesheetToScene(scene);
        stage.initStyle(stageStyle);
        scene.setFill(Color.TRANSPARENT);
        return scene;
    }

    public static Tab createTab(String fxmlPath, String tabId, @Nullable Node graphic, String tooltipText, boolean closeable) {
        Tab tab = new Tab(null, FXUtils.loadFXMLNode(fxmlPath));
        tab.setId(tabId);
        tab.setGraphic(graphic);
        if (GUString.isNotEmpty(tooltipText)) {
            tab.setTooltip(new Tooltip(tooltipText));
        }
        tab.setClosable(closeable);
        return tab;
    }

    public static void setNotEmptyTextToTextInput(TextInputControl textInputControl, String value) {
        if (GUString.isNotEmpty(value)) {
            textInputControl.setText(value);
        }
    }

    public static void resetImagePosition(ImageView imageView) {
        imageView.setX(0);
        imageView.setY(0);
    }

    public static void centerImage(ImageView imageView) {
        Image img = imageView.getImage();
        if (img != null) {
            double w;
            double h;

            double ratioX = imageView.getFitWidth() / img.getWidth();
            double ratioY = imageView.getFitHeight() / img.getHeight();

            double reducCoeff = Math.min(ratioX, ratioY);

            w = img.getWidth() * reducCoeff;
            h = img.getHeight() * reducCoeff;

            imageView.setX((imageView.getFitWidth() - w) / 2);
            imageView.setY((imageView.getFitHeight() - h) / 2);
        }
    }

    public static void disableNonLeafSelectionForMultipleModeTreeView(TreeView<Object> treeView, ObjectProperty<List<TreeItem<Object>>> selectionListProperty) {
        treeView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.isLeaf()) {
                Platform.runLater(() -> {
                    treeView.getSelectionModel().clearSelection();
                    if (GUArray.isNotEmpty(selectionListProperty.get())) {
                        selectionListProperty.get().forEach(it -> treeView.getSelectionModel().select(it));
                    }
                });
            }
        });
    }

    public static void createOnMouseEnterExitedBorderEffect(Node... nodes) {
        createOnMouseEnterBorderEffect(nodes);
        createOnMouseExitedBorderEffect(nodes);
    }

    public static void createOnMouseEnterBorderEffect(Node... nodes) {
        createOnMouseEnterBorderEffect(DEFAULT_BORDER_CSS, HOVER_BORDER_CSS, nodes);
    }

    public static void createOnMouseExitedBorderEffect(Node... nodes) {
        createOnMouseExitedBorderEffect(DEFAULT_BORDER_CSS, HOVER_BORDER_CSS, nodes);
    }

    public static void createOnMouseEnterBorderEffect(String defaultStyleName, String hoverBorderEffect, Node... nodes) {
        for (Node node : nodes) {
            node.setOnMouseEntered(event -> {
                ObservableList<String> styleClass = node.getStyleClass();
                styleClass.remove(defaultStyleName);
                styleClass.add(hoverBorderEffect);
            });
        }
    }

    public static void createOnMouseExitedBorderEffect(String defaultStyleName, String hoverBorderEffect, Node... nodes) {
        for (Node node : nodes) {
            node.setOnMouseExited(event -> {
                ObservableList<String> styleClass = node.getStyleClass();
                styleClass.remove(hoverBorderEffect);
                styleClass.add(defaultStyleName);
            });
        }
    }

    public static void createOnMouseEnterBorderEffect(String hoverStyle, Node... nodes) {
        for (Node node : nodes) {
            node.setOnMouseEntered(event -> node.setStyle(hoverStyle));
        }
    }

    public static void createOnMouseExitedBorderEffect(String defaultStyle, Node... nodes) {
        for (Node node : nodes) {
            node.setOnMouseExited(event -> node.setStyle(defaultStyle));
        }
    }

    public static void createOnMouseEnterBackgroundEffect(Node node) {
        node.setOnMouseEntered(event -> {
            ObservableList<String> styleClass = node.getStyleClass();
            styleClass.remove(DEFAULT_BACKGROUND_CSS);
            styleClass.add(HOVER_BACKGROUND_CSS);
        });
    }

    public static void createOnMouseExitedBackgroundEffect(Node node) {
        node.setOnMouseExited(event -> {
            ObservableList<String> styleClass = node.getStyleClass();
            styleClass.remove(HOVER_BACKGROUND_CSS);
            styleClass.add(DEFAULT_BACKGROUND_CSS);
        });
    }

    public static HBox createTwoLabelWithMaterialDesignIconNode(String firstLabelText, String secondLabelText, MaterialDesignIcon icon,
                                                                int iconSize, String iconColor, Object userData) {
        HBox root = new HBox();
        root.setAlignment(Pos.CENTER_LEFT);
        root.setStyle("-fx-pref-width: -fx-popup-pref-width;");
        root.setPadding(new Insets(8));
        root.setUserData(userData);

        VBox vbLabels = new VBox();
        vbLabels.setPadding(new Insets(0, 0, 0, 6));

        Label firstLabel = new Label(firstLabelText);
        firstLabel.setFont(Font.font("OpenSans", FontWeight.BOLD, 15));
        firstLabel.setStyle("fx-font-weight: bold;");
        firstLabel.setMnemonicParsing(false);

        Label secondLabel = new Label(secondLabelText);
        secondLabel.setMnemonicParsing(false);

        vbLabels.getChildren().addAll(firstLabel, secondLabel);
        root.getChildren().addAll(createMaterialDesignIconView(icon, iconSize, iconColor), vbLabels);

        return root;
    }

    public static Label createLabel(String labelText, String textColor) {
        Label label = new Label(labelText);

        label.setStyle(String.format("-fx-text-fill: %s", textColor));
        label.setMnemonicParsing(false);

        return label;
    }

    public static Label createLabelWithInsets(String labelText, String textColor, int width, Object userData) {
        Label label = new Label(labelText);

        label.setStyle(String.format("-fx-pref-width: %spx; -fx-text-fill: %s", width, textColor));
        label.setPadding(new Insets(8));
        label.setUserData(userData);
        label.setMnemonicParsing(false);

        return label;
    }

    public static Label createLabelWithMaterialDesignIconNode(String labelText, MaterialDesignIcon icon, int iconSize, String iconColor, Object userData) {
        return createLabelWithMaterialDesignIconNode(labelText, icon, 250, iconSize, iconColor, userData);
    }

    public static Label createLabelWithMaterialDesignIconNode(String labelText, MaterialDesignIcon icon, int width, int iconSize, String iconColor, Object userData) {
        return createLabelWithMaterialDesignIconNode(labelText, "#ffffff", icon, width, iconSize, iconColor, userData);
    }

    public static Label createLabelWithMaterialDesignIconNode(String labelText, String textColor, MaterialDesignIcon icon, int width, int iconSize, String iconColor, Object userData) {
        Label label = createLabelWithInsets(labelText, textColor, width, userData);
        label.setGraphic(createMaterialDesignIconView(icon, iconSize, iconColor));

        return label;
    }

    public static MaterialDesignIconView createTabIconView(MaterialDesignIcon icon) {
        return createMaterialDesignIconView(icon, 30, "#ecf0f1", "-fx-rotate: 90");
    }

    public static MaterialDesignIconView createMaterialDesignIconView(MaterialDesignIcon icon, int iconSize, String iconColor) {
        return createMaterialDesignIconView(icon, iconSize, iconColor, "");
    }

    public static MaterialDesignIconView createMaterialDesignIconView(MaterialDesignIcon icon, int iconSize, String iconColor, @NotNull String style) {
        MaterialDesignIconView mdiv = new MaterialDesignIconView(icon);
        mdiv.setStyle(String.format("-fx-font-family: 'Material Design Icons'; -fx-font-size: %s; -fx-fill: %s; %s;", iconSize, iconColor, style));
        return mdiv;
    }

    public static FontAwesomeIconView createFontAwesomeIconView(FontAwesomeIcon icon, int iconSize, String iconColor, @NotNull String style) {
        FontAwesomeIconView faiv = new FontAwesomeIconView(icon);
        faiv.setStyle(String.format("-fx-font-family: 'FontAwesome'; -fx-font-size: %s; -fx-fill: %s; %s;", iconSize, iconColor, style));
        return faiv;
    }

    public static RequiredFieldValidator createNotEmptyValidator() {
        RequiredFieldValidator validator = new RequiredFieldValidator();
        validator.setMessage(LocaleManager.getString("gui.cant_be_empty"));
        validator.setIcon(FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.EXCLAMATION_TRIANGLE));
        return validator;
    }

    public static void addTextFieldValidator(IFXLabelFloatControl textField, RequiredFieldValidator validator) {
        addTextFieldValidator(textField, validator, true);
    }

    public static void addTextFieldValidator(IFXLabelFloatControl textField, RequiredFieldValidator validator, boolean withValidationOnFocusChange) {
        textField.getValidators().add(validator);
        if (withValidationOnFocusChange) {
            ((TextField) textField).focusedProperty().addListener((o, oldVal, newVal) -> {
                if (!newVal) {
                    textField.validate();
                }
            });
        }
    }

    public static void createRoundedRippler(Pane ripplerPane, Pane root, int maskArcHeight, int maskArcWidth) {
        JFXRoundedRippler rippler = new JFXRoundedRippler(ripplerPane, maskArcHeight, maskArcWidth);
        rippler.setRipplerFill(Color.WHITE);
        root.getChildren().add(rippler);
    }

    public static void clipRoundedCorners(Node node, double width, double height, int arcWidth, int arcHeight) {
        Rectangle rectangle = new Rectangle();
        rectangle.setWidth(width);
        rectangle.setHeight(height);
        rectangle.setArcWidth(arcWidth);
        rectangle.setArcHeight(arcHeight);
        node.setClip(rectangle);
    }

    public static Tooltip addTooltipToNode(Node layout, Node tooltipGraphic) {
        return addTooltipToNode(layout, tooltipGraphic, null);
    }

    public static Tooltip addTooltipToNode(Node layout, Node tooltipGraphic, @Nullable Duration showDelay) {
        Tooltip nodeTooltip = new Tooltip();
        nodeTooltip.setGraphic(tooltipGraphic);
        if (showDelay != null) {
            nodeTooltip.setShowDelay(showDelay);
        }
        Tooltip.install(layout, nodeTooltip);
        return nodeTooltip;
    }

    public static void removeTooltipFromNode(Node layout, @Nullable Tooltip tooltip) {
        if (tooltip != null) {
            Tooltip.uninstall(layout, tooltip);
        }
    }

    public static void addTooltipToNode(Node layout, String tooltipText, int fontSize) {
        addTooltipToNode(layout, tooltipText, fontSize, null);
    }

    public static void addTooltipToNode(Node layout, String tooltipText, int fontSize, @Nullable Duration showDelay) {
        Tooltip nodeTooltip = new Tooltip(tooltipText);
        nodeTooltip.setFont(Font.font(fontSize));
        if (showDelay != null) {
            nodeTooltip.setShowDelay(showDelay);
        }
        Tooltip.install(layout, nodeTooltip);
    }

    public static Tooltip addCoverTooltip(Node layout, Image image, int width) {
        if (image != null && image.getHeight() > 0) {
            ImageView ivTooltip = new ImageView(image);
            ivTooltip.setFitWidth(width);
            ivTooltip.setFitHeight(ivTooltip.getFitWidth() * (image.getHeight() / image.getWidth()));
            return ViewUtils.addTooltipToNode(layout, ivTooltip);
        }
        return null;
    }

    public static void setTextWithTooltipOrMakeGone(Label label, String text) {
        Optional.ofNullable(text)
                .filter(GUString::isNotEmpty)
                .ifPresentOrElse(str -> {
                    label.setText(str);
                    label.setTooltip(new Tooltip(str));
                    setNodeVisible(label);
                }, () -> setNodeGone(label));
    }

    public static Triple<String, String, String> getErrorViewTitleAndSubtitleTriple(Throwable throwable) {
        String title;
        String subtitle;
        String subtitleText = null;
        if (throwable instanceof ApiParseException || throwable instanceof HttpException) {
            title = Optional.of(throwable)
                    .filter(thr -> thr instanceof ApiParseException)
                    .map(ApiParseException.class::cast)
                    .map(ApiParseException::getErrorTitle)
                    .filter(GUString::isNotEmpty)
                    .orElseGet(() -> LocaleManager.getString("gui.error.information"));
            subtitle = Optional.of(throwable)
                    .filter(thr -> thr instanceof ApiParseException)
                    .map(ApiParseException.class::cast)
                    .map(ApiParseException::getErrorDescription)
                    .filter(GUString::isNotEmpty)
                    .orElseGet(() -> LocaleManager.getString("gui.reader.error_occurred"));
            subtitleText = throwable.getLocalizedMessage();
        } else if (throwable instanceof SocketTimeoutException) {
            // Таймаут
            title = LocaleManager.getString("gui.error.no_connection");
            subtitle = LocaleManager.getString("gui.error.no_connection.server_offline");
        } else if (throwable instanceof IOException) {
            title = LocaleManager.getString("gui.error.no_connection");
            subtitle = LocaleManager.getString("gui.error.no_connection.no_internet");
        } else if (throwable instanceof IllegalStateException) {
            // ConversationError
            title = LocaleManager.getString("gui.error.server_exception");
            subtitle = LocaleManager.getString("gui.error.no_connection.server_offline");
        } else {
            title = LocaleManager.getString("gui.error.wtf");
            subtitle = LocaleManager.getString("gui.error.wtf.subtitle");
        }

        return new Triple<>(title, subtitle, subtitleText);
    }
}
