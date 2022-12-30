package xyz.atsumeru.manager.views;

import com.jfoenix.controls.JFXSnackbar;
import com.jfoenix.controls.JFXSnackbarLayout;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.scene.layout.Pane;

public class Snackbar {
    public enum Type {
        SUCCESS("success-toast"),
        WARNING("warning-toast"),
        ERROR("error-toast");

        private final String className;

        Type(String className) {
            this.className = className;
        }
    }

    public static void showSnackBar(Pane pane, String title, Type snackbarType) {
        Platform.runLater(() -> showSnackBar(pane, title, snackbarType.className));
    }

    public static void showExceptionSnackbar(Pane pane, Throwable throwable, boolean printStackTrace) {
        if (printStackTrace) {
            throwable.printStackTrace();
        }
        showSnackBar(pane, throwable.getMessage(), Type.ERROR);
    }

    private static void showSnackBar(Pane pane, String title, String className) {
        JFXSnackbar bar = new JFXSnackbar(pane);
        bar.setPrefWidth(pane.getWidth());
        bar.fireEvent(createSnackbarEvent(title, className));
    }

    private static JFXSnackbar.SnackbarEvent createSnackbarEvent(String title, String className) {
        return new JFXSnackbar.SnackbarEvent(new JFXSnackbarLayout(title), createSnackbarStyle(className));
    }

    private static JFXSnackbarStyle createSnackbarStyle(String className) {
        return new JFXSnackbarStyle(className);
    }

    private static class JFXSnackbarStyle extends PseudoClass {
        private final String className;

        public JFXSnackbarStyle(String className) {
            this.className = className;
        }

        @Override
        public String getPseudoClassName() {
            return className;
        }
    }
}
