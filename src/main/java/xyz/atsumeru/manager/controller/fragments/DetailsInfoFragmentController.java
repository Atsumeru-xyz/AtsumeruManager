package xyz.atsumeru.manager.controller.fragments;

import io.github.palexdev.materialfx.controls.MFXProgressSpinner;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextFlow;
import org.jetbrains.annotations.Nullable;
import xyz.atsumeru.manager.controller.tabs.TabAtsumeruLibraryController;
import xyz.atsumeru.manager.controls.GenreChip;
import xyz.atsumeru.manager.enums.AgeRating;
import xyz.atsumeru.manager.helpers.ImageCache;
import xyz.atsumeru.manager.helpers.LocaleManager;
import xyz.atsumeru.manager.listeners.OnClickListener;
import xyz.atsumeru.manager.managers.TabPaneManager;
import xyz.atsumeru.manager.managers.TabsManager;
import xyz.atsumeru.manager.models.content.Content;
import xyz.atsumeru.manager.models.content.Info;
import xyz.atsumeru.manager.source.AtsumeruSource;
import xyz.atsumeru.manager.utils.ViewUtils;
import xyz.atsumeru.manager.utils.globalutils.GUArray;
import xyz.atsumeru.manager.utils.globalutils.GUEnum;
import xyz.atsumeru.manager.utils.globalutils.GUString;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DetailsInfoFragmentController implements ImageCache.ImageLoadCallback {
    private static final String LABEL_TEXT_STYLE = "-fx-fill: white; -fx-font-size: 13; -fx-padding: 0 0 0 6;";

    @FXML
    public Rectangle shContentCover;

    @FXML
    public MFXProgressSpinner spinnerLoading;
    @FXML
    public ScrollPane spInfo;
    @FXML
    public VBox vbInfo;

    @FXML
    public Label lblContentTypeWithScore;
    @FXML
    public ImageView ivAgeRating;

    @FXML
    public Label lblTitle;
    @FXML
    public Label lblAltTitle;
    @FXML
    public Label lblJapTitle;
    @FXML
    private Label lblKorTitle;

    // Info
    @FXML
    private TextFlow tfAuthor;
    @FXML
    private TextFlow tfArtists;
    @FXML
    private TextFlow tfPublisher;
    @FXML
    private TextFlow tfCountry;
    @FXML
    private TextFlow tfYear;
    @FXML
    private TextFlow tfLanguages;
    @FXML
    private TextFlow tfTranslators;
    @FXML
    private TextFlow tfMagazines;
    @FXML
    private TextFlow tfCharacters;
    @FXML
    private TextFlow tfParodies;
    @FXML
    private TextFlow tfEvent;
    @FXML
    private TextFlow tfCircles;
    @FXML
    private TextFlow tfCensorship;
    @FXML
    private TextFlow tfColor;
    @FXML
    private TextFlow tfStatus;
    @FXML
    private TextFlow tfTranslationStatus;

    @FXML
    private ImageView ivGenresIcon;
    @FXML
    private ImageView ivTagsIcon;
    @FXML
    private FlowPane fpGenres;
    @FXML
    private FlowPane fpTags;

    @FXML
    public Label lblDescriptionHeader;
    @FXML
    public Label lblDescription;

    private final OnClickListener tagClickListener = (action, position) -> {
        TabAtsumeruLibraryController controller = TabsManager.getTabController(TabAtsumeruLibraryController.class);
        controller.clearSelectedFilters();
        controller.filterLibraryListByTag(action);
        TabPaneManager.selectHomeTab();
    };

    @FXML
    public void initialize() {
        ViewUtils.setVBoxOnScroll(vbInfo, spInfo, 5);
        ViewUtils.setNodeGone(vbInfo);
    }

    public void fillContentInfo(Content content) {
        // Секция "Основная информация"
        lblTitle.setText(content.getTitle());

        safeSetLabelText(content.getAltTitle(), lblAltTitle);
        safeSetLabelTextWithAppend(content.getJapTitle(), "JP: ", lblJapTitle);
        safeSetLabelTextWithAppend(content.getKoreanTitle(), "KOR: ", lblKorTitle);

        Info info = content.getInfo();

        safeCreateTextFlowInfo(tfAuthor, info.getAuthors());
        safeCreateTextFlowInfo(tfArtists, info.getArtists());
        safeCreateTextFlowInfo(tfPublisher, info.getPublisher());
        safeCreateTextFlowInfo(tfCountry, info.getCountry());
        safeCreateTextFlowInfo(tfYear, info.getYear());
        safeCreateTextFlowInfo(tfLanguages, info.getLanguage());
        safeCreateTextFlowInfo(tfTranslators, info.getTranslators());
        safeCreateTextFlowInfo(tfMagazines, info.getMagazines());
        safeCreateTextFlowInfo(tfCharacters, info.getCharacters());
        safeCreateTextFlowInfo(tfParodies, info.getParodies());
        safeCreateTextFlowInfo(tfEvent, info.getEvent());
        safeCreateTextFlowInfo(tfCircles, info.getCircles());

        safeCreateTextFlowInfo(tfStatus, GUEnum.getEnumLocalizedString(info.getStatus()));
        safeCreateTextFlowInfo(tfTranslationStatus, GUEnum.getEnumLocalizedString(info.getMangaTranslationStatus()));
        safeCreateTextFlowInfo(tfCensorship, GUEnum.getEnumNotUnknownLocalizedString(info.getCensorship()));
        safeCreateTextFlowInfo(tfColor, GUEnum.getEnumNotUnknownLocalizedString(info.getColor()));

        safeSetLabelText(GUString.fromHtmlToString(content.getDescription()), lblDescription);
        if (lblDescription.textProperty().isEmpty().get()) {
            ViewUtils.setNodeGone(lblDescriptionHeader);
        }

        if (GUString.isEmpty(content.getInfo().getScore())) {
            safeSetLabelText(GUEnum.getEnumLocalizedString(info.getContentType()), lblContentTypeWithScore);
        } else {
            String contentTypeWithScore = GUString.join(" • ",
                    Arrays.asList(GUEnum.getEnumLocalizedString(content.getInfo().getContentType()), String.format("\uD83D\uDFCA%s", content.getInfo().getScore())));
            safeSetLabelText(contentTypeWithScore, lblContentTypeWithScore);
        }

        ivGenresIcon.setImage(new Image("/images/icons/round_sell_white_18.png"));
        ivTagsIcon.setImage(new Image("/images/icons/round_theater_comedy_white_18.png"));

        ViewUtils.setNodeVisibleAndManaged(GUArray.isNotEmpty(info.getGenres()), fpGenres);
        ViewUtils.setNodeVisibleAndManaged(GUArray.isNotEmpty(info.getTags()), fpTags);

        clearPaneChildrensExceptFirst(fpGenres);
        clearPaneChildrensExceptFirst(fpTags);

        if (GUArray.isNotEmpty(info.getGenres())) {
            info.getGenres()
                    .stream()
                    .map(String::toLowerCase)
                    .map(genre -> LocaleManager.getString("genre_" + genre, genre))
                    .map(genreLocalized -> new GenreChip(genreLocalized, tagClickListener))
                    .forEach(genreChip -> fpGenres.getChildren().add(genreChip));
        }
        if (GUArray.isNotEmpty(info.getTags())) {
            info.getTags()
                    .stream()
                    .map(tag -> new GenreChip(tag, tagClickListener))
                    .forEach(genreChip -> fpTags.getChildren().add(genreChip));
        }

        configureAgeRatingBadge(getAgeRating(content));

        //В новом потоке получаем изображение из кеша (или сначала грузим его туда и уже потом берем из кеша) и отображаем
        //его в Rectangle Shape. Добавляем тень с заокругливанием
        if (!info.getImages().getThumbnail().isEmpty()) {
            loadCover(content);
        }

        ViewUtils.setNodeGone(spinnerLoading);
        ViewUtils.setNodeVisible(vbInfo);
    }

    private AgeRating getAgeRating(Content content) {
        if (content.getInfo().isAdult()) {
            return AgeRating.ADULTS_ONLY;
        } else if (content.getInfo().isMature()) {
            return AgeRating.MATURE;
        }
        return null;
    }

    private void configureAgeRatingBadge(@Nullable AgeRating ageRating) {
        if (ageRating == null) {
            ViewUtils.setNodeGone(ivAgeRating);
            return;
        }

        if (ageRating == AgeRating.MATURE) {
            ivAgeRating.setImage(new Image("/images/mature.png"));
        } else if (ageRating == AgeRating.ADULTS_ONLY) {
            ivAgeRating.setImage(new Image("/images/adults_only.png"));
        }
    }

    private void loadCover(Content content) {
        ImageCache.create(content.getContentId(), content.getInfo().getImages().getOriginal())
                .withCacheType(ImageCache.ImageCacheType.ORIGINAL)
                .withPreserveRatio()
                .withSmooth()
                .withHeaders(AtsumeruSource.createAuthorizationHeaders())
                .withCallback(this)
                .getAsync();
    }

    private void setContentCover(Image contentCover) {
        shContentCover.setFill(new ImagePattern(contentCover));
        shContentCover.setEffect(new DropShadow(10, Color.BLACK));
        ViewUtils.addCoverTooltip(shContentCover, contentCover, 400);
    }

    private void clearPaneChildrensExceptFirst(Pane pane) {
        if (pane.getChildren().size() > 1) {
            ObservableList<Node> childrens = pane.getChildren();
            Node firstNode = childrens.get(0);
            childrens.clear();
            childrens.add(firstNode);
        }
    }

    private void safeCreateTextFlowInfo(TextFlow textFlow, List<String> textList) {
        ViewUtils.setNodeVisibleAndManaged(GUArray.isNotEmpty(textList), textFlow);
        if (GUArray.isNotEmpty(textList)) {
            clearPaneChildrensExceptFirst(textFlow);
            for (int i = 0; i < textList.size(); i++) {
                createTextFlowLabel(textFlow, textList.get(i), i < textList.size() - 1);
            }
        }
    }

    private void safeCreateTextFlowInfo(TextFlow textFlow, String text) {
        ViewUtils.setNodeVisibleAndManaged(GUString.isNotEmpty(text), textFlow);
        if (GUString.isNotEmpty(text)) {
            clearPaneChildrensExceptFirst(textFlow);
            createTextFlowLabel(textFlow, text, false);
        }
    }

    private void createTextFlowLabel(TextFlow textFlow, String text, boolean appendComma) {
        Label label = new Label(!appendComma ? text : text + ",");
        label.setStyle(LABEL_TEXT_STYLE);
        label.setOnMouseClicked(event -> tagClickListener.onClick(text, -1));
        textFlow.getChildren().add(label);
    }

    private void safeSetLabelText(String text, Label targetLabel) {
        safeSetLabelText(text, null, targetLabel);
    }

    private void safeSetLabelTextWithAppend(String text, String append, Label targetLabel) {
        safeSetLabelText(text, append, null, targetLabel);
    }

    private void safeSetLabelText(String text, Label hintLabel, Label targetLabel) {
        safeSetLabelText(text, null, hintLabel, targetLabel);
    }

    private void safeSetLabelText(String text, String append, Label hintLabel, Label targetLabel) {
        boolean hasText = GUString.isNotEmpty(text);
        targetLabel.setText(GUString.isNotEmpty(append) ? append + text : text);
        ViewUtils.setNodeVisibleAndManaged(hasText, hasText, hintLabel == null
                ? Collections.singletonList(targetLabel).toArray(new Node[0])
                : Arrays.asList(hintLabel, targetLabel).toArray(new Node[0]));
    }

    @Override
    public void onLoad(Image image, String contentId, boolean fromCache, boolean loadNow) {
        // Если изображение уже загружено из кеша - сразу же рендерим его
        // В ином случае - добавляем "слушатель" прогресса скачивания После успешной загрузки изображение будет отрендерено
        Platform.runLater(() -> setContentCover(image));
    }
}
