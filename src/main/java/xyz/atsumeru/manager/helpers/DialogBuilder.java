package xyz.atsumeru.manager.helpers;

import com.jfoenix.controls.JFXButton;
import io.github.palexdev.materialfx.controls.MFXProgressBar;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import kotlin.Triple;
import org.jetbrains.annotations.Nullable;
import xyz.atsumeru.manager.controls.jfoenix.JFXCustomDialog;
import xyz.atsumeru.manager.controls.jfoenix.JFXCustomDialogLayout;
import xyz.atsumeru.manager.utils.FXUtils;
import xyz.atsumeru.manager.utils.globalutils.GUApp;
import xyz.atsumeru.manager.utils.globalutils.GUArray;
import xyz.atsumeru.manager.utils.globalutils.GUString;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class DialogBuilder {
    public enum DialogType { NODE, INPUT, WAIT }

    private final JFXCustomDialog dialog;

    private DialogType dialogType;
    private JFXCustomDialog.DialogTransition transition = JFXCustomDialog.DialogTransition.CENTER;
    private int minWidth = 380;
    private Region customRegion;
    private String heading;
    private boolean noButtons = false;
    private boolean closable = true;

    private MFXTextField input;
    private Consumer<String> inputChangeConsumer;

    private MFXProgressBar progressBar;
    private Runnable onProgressDoneRunnable;

    private final List<Node> bodies = new ArrayList<>();
    private final List<Triple<ButtonType, String, Runnable>> actions = new ArrayList<>();

    private Runnable onDialogOpenRunnable;
    private Runnable onDialogCloseRunnable;

    private List<URL> stylesheetUrls;

    public static DialogBuilder create(StackPane container) {
        return new DialogBuilder(container);
    }

    private DialogBuilder(StackPane container) {
        this.dialog = new JFXCustomDialog();
        dialog.setDialogContainer(container);
    }

    public DialogBuilder withDialogType(DialogType dialogType) {
        this.dialogType = dialogType;
        if (dialogType == DialogType.INPUT) {
            input = new MFXTextField();
        } else if (dialogType == DialogType.WAIT) {
            progressBar = new MFXProgressBar();
        }
        return this;
    }

    public DialogBuilder withTransition(JFXCustomDialog.DialogTransition transition) {
        this.transition = transition;
        return this;
    }

    public DialogBuilder withMinWidth(int minWidth) {
        this.minWidth = minWidth;
        return this;
    }

    public DialogBuilder withCustomRegion(Region customRegion) {
        this.customRegion = customRegion;
        return this;
    }

    public DialogBuilder withHeading(String heading) {
        this.heading = heading;
        return this;
    }

    public DialogBuilder withInputFloatingText(String floatingText) {
        Optional.ofNullable(input).ifPresentOrElse(node -> node.setFloatingText(floatingText), () -> System.err.println("Unable to set TextField Floating text. TextField is null!"));
        return this;
    }

    public DialogBuilder withInputChange(Consumer<String> inputChangeConsumer) {
        this.inputChangeConsumer = inputChangeConsumer;
        return this;
    }

    public DialogBuilder withOnProgressDone(Runnable onProgressDoneRunnable) {
        this.onProgressDoneRunnable = onProgressDoneRunnable;
        return this;
    }

    public DialogBuilder withBody(String body) {
        bodies.add(new Label(body));
        return this;
    }

    public DialogBuilder withBody(Node body) {
        bodies.add(body);
        return this;
    }

    public DialogBuilder withNoButtons() {
        noButtons = true;
        return this;
    }

    public DialogBuilder withButton(ButtonType button, @Nullable String title, @Nullable Runnable runnable) {
        actions.add(new Triple<>(button, title, runnable));
        return this;
    }

    public DialogBuilder withClosable(boolean closable) {
        this.closable = closable;
        return this;
    }

    public DialogBuilder withOnOpenRunnable(Runnable onDialogOpenRunnable) {
        this.onDialogOpenRunnable = onDialogOpenRunnable;
        return this;
    }

    public DialogBuilder withOnCloseRunnable(Runnable onDialogCloseRunnable) {
        this.onDialogCloseRunnable = onDialogCloseRunnable;
        return this;
    }

    public DialogBuilder withStylesheets(List<URL> stylesheetUrls) {
        this.stylesheetUrls = stylesheetUrls;
        return this;
    }

    public DialogBuilder build() {
        return this;
    }

    public JFXCustomDialog show() {
        configureDialog(dialog);
        addStylesheets(dialog);

        dialog.show();
        return dialog;
    }

    public void close() {
        dialog.close();
    }

    public void updateProgress(int current, int total) {
        Platform.runLater(() -> {
            progressBar.setProgress((double) current / total);
            if (current == total) {
                GUApp.safeRun(onProgressDoneRunnable);
                dialog.close();
            }
        });
    }

    private void configureDialog(JFXCustomDialog dialog) {
        dialog.setTransitionType(transition);
        dialog.setContent(getContent(dialog));
        dialog.setOnDialogOpened(event -> Optional.ofNullable(onDialogOpenRunnable).ifPresent(Runnable::run));
        dialog.setOnDialogClosed(event -> Optional.ofNullable(onDialogCloseRunnable).ifPresent(Runnable::run));
        dialog.setOverlayClose(closable);
    }

    private Region getContent(JFXCustomDialog dialog) {
        if (customRegion == null) {
            JFXCustomDialogLayout layout = new JFXCustomDialogLayout(noButtons);
            if (GUString.isNotEmpty(heading)) {
                layout.setHeading(new Label(heading));
            }

            VBox vBox = new VBox();
            vBox.setMinWidth(minWidth);

            if (GUArray.isNotEmpty(bodies)) {
                vBox.getChildren().addAll(bodies);
            }

            if (dialogType == DialogType.INPUT) {
                vBox.getChildren().add(input);
                FXUtils.runLaterDelayed(input::requestFocus, 250);
            } else if (dialogType == DialogType.WAIT) {
                vBox.getChildren().add(progressBar);
            }

            vBox.getChildren()
                    .stream()
                    .filter(Region.class::isInstance)
                    .map(Region.class::cast)
                    .forEach(region -> region.minWidthProperty().bind(vBox.minWidthProperty()));

            layout.setBody(vBox);

            setActions(dialog, layout);

            return layout;
        }
        return customRegion;
    }

    private void setActions(JFXCustomDialog dialog, JFXCustomDialogLayout layout) {
        if (GUArray.isNotEmpty(this.actions)) {
            List<Node> actions = new ArrayList<>();
            this.actions.forEach(triple -> {
                ButtonBar.ButtonData buttonData = triple.getFirst().getButtonData();
                String buttonText = triple.getSecond();
                if (GUString.isEmpty(buttonText)) {
                    buttonText = LocaleManager.getString("gui." + buttonData.toString().toLowerCase());
                }

                JFXButton action = new JFXButton(buttonText);
                action.setStyle("-fx-text-fill: white; -fx-font-size: 15; -fx-background-color: "
                        + (buttonData.isCancelButton() ? "transparent;" : "-fx-accent-color;"));

                action.setOnMouseClicked(event -> {
                    GUApp.safeRun(triple.getThird());
                    if (!buttonData.isCancelButton() && dialogType == DialogType.INPUT) {
                        Optional.ofNullable(inputChangeConsumer).ifPresent(consumer -> consumer.accept(input.getText()));
                    }

                    if (closable) {
                        dialog.close();
                    }
                });

                actions.add(action);
            });

            layout.setActions(actions);
        }
    }

    private void addStylesheets(JFXCustomDialog dialog) {
        Optional.ofNullable(stylesheetUrls).ifPresent(urls -> {
            ObservableList<String> stylesheets = dialog.getStylesheets();
            urls.stream()
                    .map(URL::toExternalForm)
                    .forEach(stylesheets::add);
        });
    }
}
