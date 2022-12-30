package xyz.atsumeru.manager.managers;

import com.jfoenix.controls.JFXSlider;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import lombok.Getter;
import org.controlsfx.control.StatusBar;
import xyz.atsumeru.manager.utils.ViewUtils;

import java.util.Optional;

public class StatusBarManager {
    @Getter
    private static StatusBarManager instance;
    private final StatusBar statusBar;

    private Label lblProgress;
    private ProgressBar pbProgress;
    @Getter
    private JFXSlider gridSlider;

    private StatusBarManager(StatusBar statusBar) {
        this.statusBar = statusBar;
    }

    public static void init(StatusBar statusBar) {
        instance = new StatusBarManager(statusBar);
        instance.initStatusBar();
    }

    public static void updateStatusBarProgress(int count, int total, String text) {
        getInstance().setStatusBarProgress(count, total, text);
    }

    public static DoubleProperty getGridSliderValueProperty() {
        return getInstance().gridSlider.valueProperty();
    }

    private void initStatusBar() {
        lblProgress = new Label();
        lblProgress.setMinWidth(Region.USE_PREF_SIZE);
        HBox.setHgrow(lblProgress, Priority.ALWAYS);
        HBox.setMargin(lblProgress, new Insets(0, 10, 0, 10));

        pbProgress = new ProgressBar();
        HBox.setMargin(pbProgress, new Insets(2, 0, 0, 10));
        ViewUtils.setNodeGone(pbProgress);

        createGridSlider();
        Optional.ofNullable(statusBar).ifPresent(bar -> bar.getRightItems().addAll(lblProgress, pbProgress));
    }

    public void setStatusBarProgress(int count, int total, String text) {
        Platform.runLater(() -> {
            if (total == 0) {
                lblProgress.setText("");
                ViewUtils.setNodeGone(pbProgress);
                return;
            }

            double progress = (double) count / total;
            boolean showProgress = progress < 1;
            pbProgress.setProgress(progress);
            lblProgress.setText(showProgress ? text : "");
            ViewUtils.setNodeVisibleAndManaged(showProgress, showProgress, pbProgress);
        });
    }

    public void createGridSlider() {
        gridSlider = new JFXSlider(3, 8, Settings.getGridScale());
        gridSlider.setPadding(new Insets(4, 0, 0, 4));
        gridSlider.setMajorTickUnit(1);
        gridSlider.setMinorTickCount(0);
        gridSlider.setSnapToTicks(true);
        gridSlider.setShowTickMarks(true);
        gridSlider.setPrefWidth(100);
        gridSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            double newScale = (double) Math.round(newValue.doubleValue());
            if (Settings.getGridScale() != newScale) {
                Settings.putGridScale(newScale);
            }
        });
    }
}
