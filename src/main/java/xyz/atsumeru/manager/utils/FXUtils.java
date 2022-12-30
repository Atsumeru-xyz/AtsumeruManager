package xyz.atsumeru.manager.utils;

import com.jpro.webapi.WebAPI;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import kotlin.Pair;
import lombok.SneakyThrows;
import xyz.atsumeru.manager.FXApplication;
import xyz.atsumeru.manager.helpers.LocaleManager;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class FXUtils {

    public static void setAppAccentColor(Node rootNode, String hexColor) {
        rootNode.setStyle(String.format("-fx-accent-color: %s;", hexColor));
    }

    public static void loadFont(String fontPath) {
        Font.loadFont(FXApplication.getResource(fontPath).toExternalForm(), -1);
    }

    public static void loadComponent(Object rootAndController, String fxmlPath) {
        FXMLLoader fxmlLoader = new FXMLLoader(FXApplication.getResource(fxmlPath), LocaleManager.getResourceBundle());
        fxmlLoader.setRoot(rootAndController);
        fxmlLoader.setController(rootAndController);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public static Node loadFXMLNode(String fxmlPath) {
        return loadFXML(fxmlPath).getFirst();
    }

    @SneakyThrows
    public static <T> Pair<Node, T> loadFXML(String fxmlPath) {
        FXMLLoader loader = getLoader(fxmlPath);
        return new Pair<>(loader.load(), loader.getController());
    }

    @SneakyThrows
    public static FXMLLoader getLoader(String fxmlPath) {
        return new FXMLLoader(FXApplication.class.getResource(fxmlPath), LocaleManager.getResourceBundle());
    }

    @SneakyThrows
    public static FXMLLoader loadFXMLAndGetLoader(String fxmlPath) {
        FXMLLoader loader = getLoader(fxmlPath);
        loader.load();
        return loader;
    }

    @SneakyThrows
    public static <T> T loadFXMLAndGetController(String fxmlPath) {
        FXMLLoader loader = getLoader(fxmlPath);
        loader.load();
        return loader.getController();
    }

    public static void copyToClipboard(String clip) {
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(clip);
        clipboard.setContent(content);
    }

    public static void setTaskbarAppIcon(Stage primaryStage, Image appIconImage) {
        primaryStage.getIcons().add(appIconImage);
    }

    public static void runDelayed(Runnable runnable, long delay) {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                runnable.run();
            }
        };
        new Timer().schedule(task, delay);
    }

    public static void runLaterDelayed(Runnable runnable, long delay) {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(runnable);
            }
        };
        new Timer().schedule(task, delay);
    }

    public static String toHexString(Color value) {
        return "#" + (format(value.getRed()) + format(value.getGreen()) + format(value.getBlue()) + format(value.getOpacity()))
                .toUpperCase();
    }

    private static String format(double val) {
        String in = Integer.toHexString((int) Math.round(val * 255));
        return in.length() == 1 ? "0" + in : in;
    }

    public static void requestFocus(Node node) {
        Platform.runLater(node::requestFocus);
    }

    public static void setOnHiding(Node node, EventHandler<WindowEvent> eventHandler) {
        setOnHiding(node.getScene(), eventHandler);
    }

    public static void setOnHiding(Scene scene, EventHandler<WindowEvent> eventHandler) {
        scene.getWindow().setOnHiding(eventHandler);
    }

    public static void animateImageSet(ImageView imageView, Duration duration) {
        imageView.setOpacity(0);
        FadeTransition transition = new FadeTransition(duration, imageView);
        transition.setCycleCount(1);
        transition.setAutoReverse(false);
        transition.setRate(-1);
        transition.setFromValue(0);
        transition.setToValue(1);

        transition.setRate(transition.getRate() * -1);
        transition.play();
    }

    public static void showStage(Stage stage) {
        if (WebAPI.isBrowser()) {
            WebAPI.getWebAPI(FXApplication.getInstance().getCurrentStage()).openStageAsPopup(stage);
        } else {
            stage.show();
        }
    }
}