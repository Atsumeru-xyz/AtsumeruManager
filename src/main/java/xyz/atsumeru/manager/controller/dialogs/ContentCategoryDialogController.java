package xyz.atsumeru.manager.controller.dialogs;

import com.atsumeru.api.AtsumeruAPI;
import com.atsumeru.api.model.AtsumeruMessage;
import com.atsumeru.api.model.Serie;
import com.atsumeru.api.model.category.Category;
import com.jfoenix.controls.JFXButton;
import io.github.palexdev.materialfx.controls.MFXProgressSpinner;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import kotlin.Pair;
import kotlin.Triple;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.atsumeru.manager.controller.MainController;
import xyz.atsumeru.manager.controller.tabs.TabAtsumeruLibraryController;
import xyz.atsumeru.manager.controls.CategoryEntry;
import xyz.atsumeru.manager.controls.EmptyView;
import xyz.atsumeru.manager.controls.ErrorView;
import xyz.atsumeru.manager.controls.jfoenix.JFXCustomDialog;
import xyz.atsumeru.manager.enums.ErrorType;
import xyz.atsumeru.manager.helpers.DialogsHelper;
import xyz.atsumeru.manager.helpers.LocaleManager;
import xyz.atsumeru.manager.helpers.RXUtils;
import xyz.atsumeru.manager.utils.FXUtils;
import xyz.atsumeru.manager.utils.ViewUtils;
import xyz.atsumeru.manager.utils.globalutils.GUArray;
import xyz.atsumeru.manager.utils.globalutils.GUString;
import xyz.atsumeru.manager.views.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class ContentCategoryDialogController extends BaseDialogController<Boolean> {
    @FXML
    private StackPane contentRoot;
    @FXML
    private ScrollPane spRoot;
    @FXML
    private VBox vbRoot;
    @FXML
    private JFXButton btnSave;

    @FXML
    MFXProgressSpinner spinnerLoading;

    @FXML
    EmptyView evEmptyView;
    @FXML
    ErrorView evErrorView;

    @Setter
    private List<Serie> series;
    private List<CategoryEntry> categoryEntries;

    private boolean isSaved;
    private boolean changedCategoriesList;

    public static void createAndShow(@Nullable List<Serie> series, ChangeListener<Boolean> changeListener) {
        Pair<Node, ContentCategoryDialogController> pair = FXUtils.loadFXML("/fxml/atsumeru/dialogs/ContentCategoryDialog.fxml");
        ContentCategoryDialogController controller = pair.getSecond();
        controller.setSeries(series);

        String heading = LocaleManager.getString(GUArray.isNotEmpty(series) ? "gui.button.change_category" : "gui.library_categories_editor");
        controller.show(false, heading, pair.getFirst(), changeListener);
    }

    @FXML
    protected void initialize() {
        loadCategories();
        ViewUtils.setVBoxOnScroll(vbRoot, spRoot, 1);
        Platform.runLater(() -> {
            ViewUtils.setNodeVisibleAndManaged(GUArray.isNotEmpty(series), btnSave);
            FXUtils.requestFocus(contentRoot);
            FXUtils.setOnHiding(contentRoot, event -> closeDialog());
        });
    }

    private void loadCategories() {
        ViewUtils.setNodeGone(spRoot);

        showLoading();
        evEmptyView.hideEmptyView();
        evErrorView.hideErrorView();

        AtomicReference<Disposable> disposable = new AtomicReference<>();
        disposable.set(AtsumeruAPI.getCategoriesList()
                .subscribeOn(Schedulers.newThread())
                .observeOn(Schedulers.io())
                .subscribe(categories -> {
                    Platform.runLater(() -> onCategoriesLoad(categories));

                    RXUtils.safeDispose(disposable);
                }, throwable -> {
                    throwable.printStackTrace();
                    evErrorView.showErrorView(throwable, ErrorType.LOAD_CONTENT, spinnerLoading,
                            true, this::loadCategories, false, null);
                }));
    }

    private void onCategoriesLoad(List<Category> categories) {
        categoryEntries = categories.stream()
                .filter(category -> GUString.isEmpty(category.getContentType()))
                .map(category -> {
                    CategoryEntry entry = new CategoryEntry(category);
                    entry.setOnEditAction(event -> createOrEditCategory(category));
                    entry.setOnDeleteAction(event -> deleteCategory(category));
                    if (GUArray.isNotEmpty(series)) {
                        entry.setSelected(series.stream().anyMatch(serie -> GUArray.isNotEmpty(serie.getCategories()) && serie.getCategories().contains(category.getId())));
                    }
                    return entry;
                })
                .collect(Collectors.toList());

        vbRoot.getChildren().clear();
        vbRoot.getChildren().addAll(categoryEntries);
        evEmptyView.checkEmptyItems(categoryEntries, spRoot, spinnerLoading);
    }

    public void showLoading() {
        ViewUtils.setNodeVisible(spinnerLoading);
    }

    private void createOrEditCategory(@Nullable Category category) {
        String categoryName = DialogsHelper.showTextInputDialog(
                LocaleManager.getString(category != null ? "gui.edit_category" : "gui.create_category"),
                LocaleManager.getString("gui.create_category.hint"),
                null,
                category != null ? category.getName() : ""
        );

        if (GUString.isNotEmpty(categoryName)) {
            AtomicReference<Disposable> disposable = new AtomicReference<>();
            disposable.set(getCreateOrEditSingle(category, categoryName)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(Schedulers.io())
                    .subscribe(message -> {
                        Snackbar.showSnackBar(
                                MainController.getSnackbarRoot(true),
                                message.getMessage(),
                                message.isOk() ? Snackbar.Type.SUCCESS : Snackbar.Type.ERROR
                        );

                        loadCategories();
                        changedCategoriesList = true;

                        RXUtils.safeDispose(disposable);
                    }, throwable -> Snackbar.showExceptionSnackbar(MainController.getSnackbarRoot(true), throwable, true)));
        }
    }

    private Single<AtsumeruMessage> getCreateOrEditSingle(@Nullable Category category, String categoryName) {
        return category != null
                ? AtsumeruAPI.editCategory(category.getId(), categoryName)
                : AtsumeruAPI.createCategory(categoryName);
    }

    private void deleteCategory(Category category) {
        List<Triple<ButtonType, String, Runnable>> actionPairs = new ArrayList<>();
        actionPairs.add(new Triple<>(ButtonType.NO, null, null));
        actionPairs.add(new Triple<>(ButtonType.YES, null, () -> executeDeleteCategory(category)));

        DialogsHelper.showConfirmationDialog(
                contentRoot,
                JFXCustomDialog.DialogTransition.CENTER,
                actionPairs,
                LocaleManager.getString("gui.delete_category"),
                LocaleManager.getStringFormatted("gui.delete_category.header", category.getName())
        );
    }

    private void executeDeleteCategory(@NotNull Category category) {
        AtsumeruAPI.deleteCategory(category.getId())
                .cache()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(message -> {
                    Snackbar.showSnackBar(
                            MainController.getSnackbarRoot(true),
                            message.getMessage(),
                            message.isOk() ? Snackbar.Type.SUCCESS : Snackbar.Type.ERROR
                    );

                    loadCategories();
                    changedCategoriesList = true;
                }, throwable -> Snackbar.showExceptionSnackbar(MainController.getSnackbarRoot(true), throwable, true));
    }

    @FXML
    private void create() {
        createOrEditCategory(null);
    }

    @FXML
    private void save() {
        List<String> categoriesIds = categoryEntries.stream()
                .filter(CategoryEntry::isSelected)
                .map(entry -> entry.getCategory().getId())
                .collect(Collectors.toList());

        Map<String, String> contentIdsWithCategories = series.stream()
                .peek(serie -> serie.setCategories(categoriesIds))
                .collect(Collectors.toMap(Serie::getId, serie -> GUString.join(",", serie.getCategories())));

        AtomicReference<Disposable> disposable = new AtomicReference<>();
        disposable.set(AtsumeruAPI.setCategories(contentIdsWithCategories)
                .subscribeOn(Schedulers.newThread())
                .observeOn(Schedulers.io())
                .subscribe(message -> {
                    Snackbar.showSnackBar(
                            MainController.getSnackbarRoot(true),
                            message.getMessage(),
                            Snackbar.Type.SUCCESS
                    );

                    isSaved = true;
                    closeDialog();

                    RXUtils.safeDispose(disposable);
                }, throwable -> Snackbar.showExceptionSnackbar(MainController.getSnackbarRoot(true), throwable, true)));
    }

    @FXML
    private void closeDialog() {
        Platform.runLater(() -> {
            if (GUArray.isNotEmpty(series)) {
                TabAtsumeruLibraryController.notifyItemUpdate(series);
            }
            if (changedCategoriesList) {
                TabAtsumeruLibraryController.reloadItems();
            }
            property.setValue(isSaved);
            close();
        });
    }

    @Override
    protected int minDialogWidth() {
        return 400;
    }
}
