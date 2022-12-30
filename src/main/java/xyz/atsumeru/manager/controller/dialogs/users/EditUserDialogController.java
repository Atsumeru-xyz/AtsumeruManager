package xyz.atsumeru.manager.controller.dialogs.users;

import com.atsumeru.api.AtsumeruAPI;
import com.atsumeru.api.model.AtsumeruMessage;
import com.atsumeru.api.model.category.Category;
import com.atsumeru.api.model.user.User;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXPasswordField;
import com.jfoenix.controls.JFXTextField;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import kotlin.Pair;
import org.jetbrains.annotations.Nullable;
import xyz.atsumeru.manager.controller.MainController;
import xyz.atsumeru.manager.controller.dialogs.BaseDialogController;
import xyz.atsumeru.manager.controls.GenreChip;
import xyz.atsumeru.manager.helpers.AutocompletionHelper;
import xyz.atsumeru.manager.helpers.LocaleManager;
import xyz.atsumeru.manager.helpers.TextFieldContextMenuHelper;
import xyz.atsumeru.manager.managers.Settings;
import xyz.atsumeru.manager.utils.FXUtils;
import xyz.atsumeru.manager.utils.ViewUtils;
import xyz.atsumeru.manager.utils.globalutils.GUArray;
import xyz.atsumeru.manager.utils.globalutils.GUString;
import xyz.atsumeru.manager.utils.globalutils.GUType;
import xyz.atsumeru.manager.views.Snackbar;

import java.util.*;
import java.util.stream.Collectors;

public class EditUserDialogController extends BaseDialogController<User> {
    @FXML
    private VBox container;
    @FXML
    private JFXTextField tfUserName;
    @FXML
    private JFXPasswordField tfUserPassword;
    @FXML
    private VBox vbRoles;
    @FXML
    private VBox vbAuthorities;
    @FXML
    private VBox vbCategoriesAccess;
    @FXML
    private FlowPane fpGenres;
    @FXML
    private JFXTextField tfTags;

    private final List<JFXCheckBox> roles = new ArrayList<>();
    private final List<JFXCheckBox> authorities = new ArrayList<>();
    private final List<JFXCheckBox> categoriesAccess = new ArrayList<>();
    private final List<GenreChip> genresLimit = new ArrayList<>();
    private final Set<String> tagsLimit = new HashSet<>();

    private Disposable disposable;

    public static void createAndShow(@Nullable User user, ChangeListener<User> changeListener) {
        Pair<Node, EditUserDialogController> pair = FXUtils.loadFXML("/fxml/atsumeru/dialogs/users/EditUserDialog.fxml");
        EditUserDialogController controller = pair.getSecond();

        String heading = LocaleManager.getString(user != null ? "atsumeru.edit_user" : "atsumeru.add_user");
        controller.show(user, heading, pair.getFirst(), changeListener, null, null);
    }

    @FXML
    public void initialize() {
        tfTags.setFocusColor(Color.web(Settings.getAppAccentColor()));

        Platform.runLater(() -> {
            addTextFieldValidators();
            container.requestFocus();
        });

        loadUserAccessConstants();
    }

    @SuppressWarnings("ConstantConditions")
    private void loadUserAccessConstants() {
        AtsumeruAPI.getUserAccessConstants()
                .subscribeOn(Schedulers.newThread())
                .observeOn(Schedulers.io())
                .subscribe(accessConstants -> {
                    createAccessConstantsCheckBoxes(accessConstants.getRoles(), roles);
                    createAccessConstantsCheckBoxes(accessConstants.getAuthorities(), authorities);

                    accessConstants.getCategories().forEach(category -> {
                        JFXCheckBox checkBox = new JFXCheckBox(category.getName());
                        checkBox.setFont(Font.font(16));
                        checkBox.setMnemonicParsing(false);
                        checkBox.setUserData(category);
                        categoriesAccess.add(checkBox);
                    });

                    accessConstants.getGenres().forEach(genreModel -> {
                        GenreChip chip = new GenreChip(
                                genreModel.getName(),
                                (action, position) -> genresLimit.stream()
                                        .filter(chip1 -> GUString.equalsIgnoreCase(chip1.getChipText(), action))
                                        .findFirst()
                                        .ifPresent(chip1 -> chip1.setSelected(!chip1.isSelected())));
                        chip.setUserData(genreModel.getId());
                        genresLimit.add(chip);
                    });

                    tagsLimit.addAll(accessConstants.getTags());

                    Platform.runLater(this::fillUserData);
                }, throwable -> Snackbar.showExceptionSnackbar(MainController.getSnackbarRoot(true), throwable, true));
    }

    private void createAccessConstantsCheckBoxes(List<String> constants, List<JFXCheckBox> checkBoxList) {
        constants.forEach(constant -> {
            JFXCheckBox checkBox = new JFXCheckBox(constant);
            checkBox.setFont(Font.font(16));
            checkBox.setMnemonicParsing(false);
            checkBox.setUserData(constant);
            checkBoxList.add(checkBox);
        });
    }

    @SuppressWarnings("ConstantConditions")
    private void fillUserData() {
        vbRoles.getChildren().addAll(roles);
        vbAuthorities.getChildren().addAll(authorities);
        vbCategoriesAccess.getChildren().addAll(categoriesAccess);
        fpGenres.getChildren().addAll(genresLimit);

        User user = property.getValue();
        if (user != null) {
            tfUserName.setText(user.getUserName());
            tfUserPassword.setPromptText(LocaleManager.getString("atsumeru.password_leave_empty"));

            roles.forEach(chbRole -> {
                for (String role : user.getRoles()) {
                    if (GUString.equalsIgnoreCase(chbRole.getText(), role)) {
                        chbRole.setSelected(true);
                        break;
                    }
                }
            });

            authorities.forEach(chbAuthority -> {
                for (String authority : user.getAuthorities()) {
                    if (GUString.equalsIgnoreCase(chbAuthority.getText(), authority)) {
                        chbAuthority.setSelected(true);
                        break;
                    }
                }
            });

            categoriesAccess.forEach(chbCategoryAccess -> {
                for (String allowedCategory : user.getAllowedCategories()) {
                    if (GUString.equalsIgnoreCase(((Category) chbCategoryAccess.getUserData()).getId(), allowedCategory)) {
                        chbCategoryAccess.setSelected(true);
                        break;
                    }
                }
            });

            genresLimit.forEach(chip -> user.getDisallowedGenres()
                    .stream()
                    .filter(genre -> GUString.equalsIgnoreCase(genre, String.valueOf(chip.getUserData())))
                    .findFirst()
                    .ifPresent(unused -> chip.setSelected(true)));

            ViewUtils.setNotEmptyTextToTextInput(tfTags, GUString.join(",", user.getDisallowedTags()));
            AutocompletionHelper.bindMultiAutocompletion(tfTags, tagsLimit);
            TextFieldContextMenuHelper.addMenuItemsIntoDefaultMenu(tfTags);
        }
    }

    private void addTextFieldValidators() {
        ViewUtils.addTextFieldValidator(tfUserName, ViewUtils.createNotEmptyValidator());
        if (property.getValue() == null) {
            ViewUtils.addTextFieldValidator(tfUserPassword, ViewUtils.createNotEmptyValidator());
        }
    }

    private boolean validateFields() {
        return tfUserName.validate() && (property.getValue() != null || tfUserPassword.validate());
    }

    @FXML
    private void closeDialog() {
        if (disposable != null) {
            disposable.dispose();
        }
        Platform.runLater(this::close);
    }

    @FXML
    private void save() {
        if (validateFields()) {
            User user;
            Single<AtsumeruMessage> single;
            boolean isCreatingUser = property.getValue() == null;
            if (isCreatingUser) {
                fillUserData(user = new User());
                single = AtsumeruAPI.createUser(user);
            } else {
                user = property.getValue();
                property.setValue(null);
                fillUserData(user);
                single = AtsumeruAPI.updateUser(user);
            }

            disposable = single.cache()
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(Schedulers.io())
                    .subscribe(message -> {
                        if (message.isOk()) {
                            if (isCreatingUser) {
                                user.setId(GUType.getLongDef(message.getMessage(), -1));
                            }
                            property.setValue(user);
                            closeDialog();
                        } else {
                            onError(message.getMessage(), isCreatingUser);
                        }
                    }, throwable -> onError(throwable.getMessage(), isCreatingUser));
        }
    }

    private void onError(String message, boolean isCreatingUser) {
        if (isCreatingUser) {
            property.setValue(null);
        }

        Snackbar.showSnackBar(
                MainController.getSnackbarRoot(),
                LocaleManager.getStringFormatted(isCreatingUser ? "atsumeru.unable_create_user" : "atsumeru.unable_update_user", message),
                Snackbar.Type.ERROR
        );

        if (disposable != null) {
            disposable.dispose();
        }
    }

    private void fillUserData(User user) {
        user.setUserName(tfUserName.getText());
        user.setRoles(roles.stream()
                .filter(CheckBox::isSelected)
                .map(checkBox -> checkBox.getUserData().toString())
                .collect(Collectors.toList()));

        user.setAuthorities(authorities.stream()
                .filter(CheckBox::isSelected)
                .map(checkBox -> checkBox.getUserData().toString())
                .collect(Collectors.toList()));

        user.setAllowedCategories(categoriesAccess.stream()
                .filter(CheckBox::isSelected)
                .map(checkBox -> ((Category) checkBox.getUserData()).getId())
                .collect(Collectors.toList()));

        user.setDisallowedGenres(
                genresLimit.stream()
                        .filter(GenreChip::isSelected)
                        .map(Node::getUserData)
                        .map(Object::toString)
                        .collect(Collectors.toList())
        );

        user.setDisallowedTags(
                Optional.of(tfTags.getText())
                        .filter(GUString::isNotEmpty)
                        .map(GUArray::splitString)
                        .map(Collection::stream)
                        .map(stream -> stream.map(String::trim))
                        .map(stream -> stream.collect(Collectors.toList()))
                        .orElse(new ArrayList<>())
        );

        if (GUString.isNotEmpty(tfUserPassword.getText())) {
            user.setPassword(tfUserPassword.getText());
        }
    }

    @Override
    protected int minDialogWidth() {
        return 600;
    }
}
