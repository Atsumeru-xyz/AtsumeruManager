package xyz.atsumeru.manager.helpers;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDialogLayout;
import io.github.palexdev.materialfx.controls.MFXPasswordField;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.enums.FloatMode;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.StringBinding;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Pair;
import kotlin.Triple;
import org.controlsfx.dialog.ProgressDialog;
import org.jetbrains.annotations.Nullable;
import xyz.atsumeru.manager.FXApplication;
import xyz.atsumeru.manager.controls.jfoenix.JFXCustomDialog;
import xyz.atsumeru.manager.utils.ViewUtils;
import xyz.atsumeru.manager.utils.globalutils.GUArray;
import xyz.atsumeru.manager.utils.globalutils.GULinks;
import xyz.atsumeru.manager.utils.globalutils.GUString;

import java.util.*;

public class DialogsHelper {

    @Deprecated
    public static void showConfirmationDialog(StackPane dialogContainer, JFXCustomDialog.DialogTransition dialogTransition,
                                              List<Triple<ButtonType, String, Runnable>> actionTriples, String heading, Object... bodies) {
        JFXCustomDialog dialog = new JFXCustomDialog();

        JFXDialogLayout layout = new JFXDialogLayout();
        layout.setHeading(new Label(heading));

        VBox vBox = new VBox();
        for (Object body : bodies) {
            if (body instanceof String) {
                vBox.getChildren().add(new Label((String) body));
            } else if (body instanceof Node) {
                vBox.getChildren().add((Node) body);
            }
        }

        layout.setBody(vBox);

        setDialogActions(dialog, layout, actionTriples);

        dialog.setDialogContainer(dialogContainer);
        dialog.setTransitionType(dialogTransition);
        dialog.setContent(layout);

        //Загрузка темы
        final ObservableList<String> stylesheets = dialog.getStylesheets();
        stylesheets.addAll(
                FXApplication.getResource("/css/theme.css").toExternalForm(),
                FXApplication.getResource("/css/dark/materialfx.css").toExternalForm()
        );

        dialog.show();
    }

    private static void setDialogActions(JFXCustomDialog dialog, JFXDialogLayout layout, List<Triple<ButtonType, String, Runnable>> actionTriples) {
        if (GUArray.isNotEmpty(actionTriples)) {
            List<Node> actions = new ArrayList<>();
            actionTriples.forEach(triple -> {
                ButtonBar.ButtonData buttonData = triple.getFirst().getButtonData();
                String buttonText = triple.getSecond();
                if (GUString.isEmpty(buttonText)) {
                    buttonText = LocaleManager.getString("gui." + buttonData.toString().toLowerCase());
                }

                JFXButton action = new JFXButton(buttonText);
                action.setStyle("-fx-text-fill: white; -fx-font-size: 15; -fx-background-color: "
                        + (buttonData.isCancelButton() ? "transparent;" : "-fx-accent-color;"));

                action.setOnMouseClicked(event -> {
                    dialog.close();
                    if (triple.getThird() != null) {
                        triple.getThird().run();
                    }
                });
                actions.add(action);
            });

            layout.setActions(actions);
        }
    }

    public static Boolean showProgressDialog(String title, String contentText, Task<Boolean> taskWorker) {
        ProgressDialog dialog = new ProgressDialog(taskWorker);
        dialog.initStyle(StageStyle.TRANSPARENT);
        setDialogStyle(dialog);

        dialog.setGraphic(null);
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.setTitle(title);
        dialog.setContentText(contentText);
        dialog.setHeaderText(null);
        dialog.setGraphic(null);
        dialog.initStyle(StageStyle.UTILITY);
        new Thread(taskWorker).start();
        dialog.showAndWait();
        return taskWorker.getValue();
    }

    public static String showTextInputDialog(String title, String headerText, @Nullable String icon, String defaultValue) {
        TextInputDialog dialog = new TextInputDialog(defaultValue);
        setDialogStyle(dialog);

        dialog.setTitle(title);
        dialog.setHeaderText(headerText);

        // Set the icon (must be included in the project).
        if (GUString.isNotEmpty(icon)) {
            ImageView keyIconView = new ImageView(new Image(icon));
            keyIconView.setFitHeight(32);
            keyIconView.setFitWidth(32);
            dialog.setGraphic(keyIconView);
        }

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        TextField inputField = dialog.getEditor();

        // Disabling OK Button if text field is empty
        BooleanBinding isEmpty = Bindings.createBooleanBinding(() -> inputField.getText().isEmpty(), inputField.textProperty());
        okButton.disableProperty().bind(isEmpty);

        // Show error text if text field is empty
        StringBinding errorBinding = Bindings.createStringBinding(() -> inputField.getText().isEmpty() ? LocaleManager.getString("gui.cant_be_empty") : "", inputField.textProperty());
        dialog.contentTextProperty().bind(errorBinding);

        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }

    public static boolean isInvalidParserLink(String text) {
        return !FXApplication.getParsersManager().containsParser(GULinks.getHostName(text));
    }

    public static void showErrorDialog(String title, String headerText, String contentText, String buttonText) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        setDialogStyle(alert);

        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);

        Button okButton = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setText(buttonText);

        Image image = new Image("/images/icons/exclamation.png");
        ImageView exclamationIconView = new ImageView(image);
        exclamationIconView.setFitHeight(48);
        exclamationIconView.setFitWidth(48);
        alert.setGraphic(exclamationIconView);

        alert.showAndWait();
    }

    public static Map<String, String> showAuthDialog(String title, String headerText, String usernameValue, String okButtonText, String cancelButtonText,
                                                     String icon, boolean isRetry) {
        // Create the custom dialog
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        setDialogStyle(dialog);

        dialog.setTitle(title); //"Login Dialog"
        dialog.setHeaderText(headerText); //"Look, a Custom Login Dialog"

        // Set the icon (must be included in the project).
        ImageView keyIconView = new ImageView(new Image(icon));
        keyIconView.setFitHeight(32);
        keyIconView.setFitWidth(32);
        dialog.setGraphic(keyIconView);

        // Set the button types.
        ButtonType loginButtonType = new ButtonType(okButtonText, ButtonBar.ButtonData.OK_DONE); //"Login"
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.setText(cancelButtonText);

        // Create the username and password labels and fields.
        VBox vBox = new VBox();
        vBox.setSpacing(8);
        vBox.setMinWidth(350);
        vBox.setPadding(new Insets(20, 10, 10, 10));

        MFXTextField username = new MFXTextField();
        username.setFloatMode(FloatMode.BORDER);
        username.setPromptText(LocaleManager.getString("gui.auth.enter_username")); //"Username"
        username.setPrefWidth(330);
        if (GUString.isNotEmpty(usernameValue)) {
            username.setText(usernameValue);
        }

        MFXPasswordField password = new MFXPasswordField();
        password.setFloatMode(FloatMode.BORDER);
        password.setPromptText(LocaleManager.getString("gui.auth.enter_password")); //"Password"
        password.setPrefWidth(330);

        // TODO: 08.04.2022 !!!
//        if (isRetry) {
//            username.setUnFocusColor(Paint.valueOf("#d32f2f"));
//            password.setUnFocusColor(Paint.valueOf("#d32f2f"));
//        }

        vBox.getChildren().addAll(username, password);

        // Enable/Disable login button depending on whether a username was entered.
        Node loginButton = dialog.getDialogPane().lookupButton(loginButtonType);
        loginButton.setDisable(true);

        // Do some validation (using the Java 8 lambda syntax).
        username.textProperty().addListener((observable, oldValue, newValue) -> {
            loginButton.setDisable(newValue.trim().isEmpty());
        });

        dialog.getDialogPane().setContent(vBox);

        // Request focus on the username field by default.
        Platform.runLater(username::requestFocus);

        // Convert the result to a username-password-pair when the login button is clicked.
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return new Pair<>(username.getText(), password.getText());
            }
            return null;
        });

        Optional<Pair<String, String>> result = dialog.showAndWait();

        Map<String, String> valuesMap = new HashMap<>();

        result.ifPresent(usernamePassword -> {
            valuesMap.put("login", usernamePassword.getKey());
            valuesMap.put("password", usernamePassword.getValue());

        });

        return valuesMap;
    }

    private static <T> void setDialogStyle(Dialog<T> dialog) {
        DialogPane dialogPane = dialog.getDialogPane();

        dialogPane.getStylesheets().addAll(
                FXApplication.getResource("/css/theme.css").toExternalForm(),
                FXApplication.getResource("/css/dark/materialfx.css").toExternalForm()
        );
        dialogPane.getStyleClass().add("dialogDefault");

        Scene scene = dialog.getDialogPane().getScene();
        Stage stage = (Stage) scene.getWindow();
        ViewUtils.addAppIconToStage(stage);
    }
}
