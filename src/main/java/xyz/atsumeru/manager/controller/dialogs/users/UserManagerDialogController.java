package xyz.atsumeru.manager.controller.dialogs.users;

import com.atsumeru.api.AtsumeruAPI;
import com.atsumeru.api.model.user.User;
import com.jfoenix.controls.JFXButton;
import io.github.palexdev.materialfx.controls.MFXProgressSpinner;
import io.reactivex.schedulers.Schedulers;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import kotlin.Pair;
import kotlin.Triple;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.atsumeru.manager.controller.MainController;
import xyz.atsumeru.manager.controller.dialogs.BaseDialogController;
import xyz.atsumeru.manager.controls.EmptyView;
import xyz.atsumeru.manager.controls.ErrorView;
import xyz.atsumeru.manager.controls.UserCard;
import xyz.atsumeru.manager.controls.jfoenix.JFXCustomDialog;
import xyz.atsumeru.manager.enums.ErrorType;
import xyz.atsumeru.manager.helpers.DialogsHelper;
import xyz.atsumeru.manager.helpers.LocaleManager;
import xyz.atsumeru.manager.managers.TabsManager;
import xyz.atsumeru.manager.utils.FXUtils;
import xyz.atsumeru.manager.utils.ViewUtils;
import xyz.atsumeru.manager.views.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserManagerDialogController extends BaseDialogController<Void> {
    private static final String DIALOG_TEXT_STYLE = "-fx-fill: white; -fx-font-size: 16;";
    private static final String DIALOG_BOLD_TEXT_STYLE = DIALOG_TEXT_STYLE + " -fx-font-weight: bold;";

    @FXML
    StackPane container;
    @FXML
    VBox vbItemsList;
    @FXML
    JFXButton btnAddUser;
    @FXML
    MFXProgressSpinner spinnerLoading;
    @FXML
    EmptyView evEmptyView;
    @FXML
    ErrorView evErrorView;

    public static void createAndShow() {
        Pair<Node, UserManagerDialogController> pair = FXUtils.loadFXML("/fxml/atsumeru/dialogs/users/UserManagerDialog.fxml");
        UserManagerDialogController controller = pair.getSecond();
        controller.show(LocaleManager.getString("gui.users_management"), pair.getFirst());
    }

    @FXML
    protected void initialize() {
        fetchUsers();
        configureButtons();
        FXUtils.runLaterDelayed(() -> container.requestFocus(), 100);
    }

    private void fetchUsers() {
        ViewUtils.setNodeInvisible(vbItemsList);

        showLoading();
        evEmptyView.hideEmptyView();
        evErrorView.hideErrorView();

        AtsumeruAPI.getUserList()
                .cache().subscribeOn(Schedulers.newThread())
                .observeOn(Schedulers.io())
                .subscribe(usersList -> Platform.runLater(() -> {
                    evEmptyView.checkEmptyItems(usersList, vbItemsList, spinnerLoading);
                    usersList.forEach(this::createUserCard);
                }), throwable -> {
                    throwable.printStackTrace();
                    Platform.runLater(() -> evErrorView.showErrorView(throwable, ErrorType.LOAD_CONTENT, spinnerLoading,
                            true, this::fetchUsers, false, null));
                });
    }

    private void createUserCard(User user) {
        Platform.runLater(() -> {
            UserCard userCard = new UserCard(user);
            userCard.setOnEditUserClick(event -> showEditUserDialog(userCard));
            userCard.setOnDeleteUserClick(event -> showDeleteUserPrompt(userCard));
            addUserNode(userCard);
        });
    }

    private void configureButtons() {
        btnAddUser.setOnMouseClicked(event -> showEditUserDialog(null));
    }

    private void showEditUserDialog(@Nullable UserCard userCard) {
        EditUserDialogController.createAndShow(
                Optional.ofNullable(userCard)
                        .map(UserCard::getUser)
                        .orElse(null),
                (observable, oldUser, user) -> {
                    if (user != null) {
                        Platform.runLater(() -> {
                            if (userCard == null) {
                                createUserCard(user);
                            } else {
                                userCard.setUser(user);
                            }
                        });
                    }
                }
        );
    }

    private void showDeleteUserPrompt(@NotNull UserCard userCard) {
        List<Triple<ButtonType, String, Runnable>> actionTriples = new ArrayList<>();
        actionTriples.add(new Triple<>(ButtonType.NO, null, null));
        actionTriples.add(new Triple<>(
                ButtonType.YES,
                null,
                () -> AtsumeruAPI.deleteUser(userCard.getUser().getId())
                        .cache()
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .subscribe(message -> removeUserNode(userCard),
                                throwable -> {
                                    throwable.printStackTrace();
                                    Snackbar.showSnackBar(
                                            MainController.getSnackbarRoot(),
                                            LocaleManager.getString("atsumeru.unable_delete_user"),
                                            Snackbar.Type.ERROR
                                    );
                                })
        ));

        DialogsHelper.showConfirmationDialog(
                TabsManager.getTabController(MainController.class).getContentRoot(),
                JFXCustomDialog.DialogTransition.CENTER,
                actionTriples,
                LocaleManager.getString("atsumeru.delete_user"),
                LocaleManager.getString("atsumeru.delete_user.header"),
                "",
                createDeleteDialogCombinedStyleTextBody(userCard)
        );
    }

    private TextFlow createDeleteDialogCombinedStyleTextBody(UserCard userCard) {
        TextFlow textFlow = new TextFlow();

        Text start = new Text(LocaleManager.getString("atsumeru.delete_user.content"));
        start.setStyle(DIALOG_TEXT_STYLE);

        Text boldText = new Text(userCard.getUser().getUserName());
        boldText.setStyle(DIALOG_BOLD_TEXT_STYLE);

        Text end = new Text("?");
        end.setStyle(DIALOG_TEXT_STYLE);

        textFlow.getChildren().addAll(start, boldText, end);

        return textFlow;
    }

    private void addUserNode(Node node) {
        vbItemsList.getChildren().add(node);
    }

    private void removeUserNode(Node node) {
        Platform.runLater(() -> vbItemsList.getChildren().remove(node));
    }

    public void showLoading() {
        ViewUtils.setNodeVisible(spinnerLoading);
    }

    @FXML
    private void handleCancelButton() {
        close();
    }

    @Override
    protected int minDialogWidth() {
        return 500;
    }
}
