package xyz.atsumeru.manager.controls;

import com.jfoenix.controls.JFXButton;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import kotlin.Triple;
import org.jetbrains.annotations.Nullable;
import xyz.atsumeru.manager.enums.ErrorType;
import xyz.atsumeru.manager.exceptions.ApiParseException;
import xyz.atsumeru.manager.utils.FXUtils;
import xyz.atsumeru.manager.utils.ViewUtils;

@SuppressWarnings("unused")
public class ErrorView extends VBox {
    @FXML
    private VBox vbErrorView;
    @FXML
    private ImageView ivErrorImage;
    @FXML
    private Label lblErrorTitle;
    @FXML
    private Label lblErrorSubtitle;
    @FXML
    private Label lblErrorException;
    @FXML
    private JFXButton btnErrorRetry;
    @FXML
    private JFXButton btnErrorOpenInWebView;

    public ErrorView() {
        FXUtils.loadComponent(this, "/fxml/controls/ErrorView.fxml");
    }

    public void hideErrorView() {
        ViewUtils.setNodeGone(vbErrorView);
    }

    public void showErrorView(Throwable throwable, ErrorType errorType, @Nullable ProgressIndicator loadingPane, boolean showRetryButton,
                              @Nullable Runnable retryRunnable, boolean showOpenInWebViewButton, @Nullable Runnable openInWebViewRunnable) {
        if (showRetryButton && retryRunnable == null || showOpenInWebViewButton && openInWebViewRunnable == null) {
            throw new IllegalArgumentException("Runnable can't be null for enabled button");
        }

        if (throwable != null && !(throwable instanceof ApiParseException)) {
            throwable.printStackTrace();
        }

        Platform.runLater(() -> {
            ViewUtils.setNodeGone(loadingPane);
            configureErrorView(throwable, errorType, showRetryButton, showOpenInWebViewButton, retryRunnable, openInWebViewRunnable);
        });
    }

    private void configureErrorView(Throwable throwable, ErrorType errorType, boolean showRetryButton,
                                    boolean showOpenInWebViewButton, Runnable retryRunnable, Runnable openInWebViewRunnable) {
        Triple<String, String, String> errorTriple = ViewUtils.getErrorViewTitleAndSubtitleTriple(throwable);
        lblErrorTitle.setText(errorTriple.getFirst());
        lblErrorSubtitle.setText(errorTriple.getSecond());
        lblErrorException.setText(errorTriple.getThird());

        boolean hasErrorException = errorTriple.getThird() != null;
        ViewUtils.setNodeVisibleAndManaged(hasErrorException, hasErrorException, lblErrorException);

        configureErrorView(ivErrorImage, errorType);

        ViewUtils.setNodeVisibleAndManaged(showRetryButton, showRetryButton, btnErrorRetry);
        btnErrorRetry.setOnMouseClicked(event -> retryRunnable.run());

        if (btnErrorOpenInWebView != null) {
            ViewUtils.setNodeVisibleAndManaged(showOpenInWebViewButton, showOpenInWebViewButton, btnErrorOpenInWebView);
            btnErrorOpenInWebView.setOnMouseClicked(event -> openInWebViewRunnable.run());
        }

        ViewUtils.setNodeVisible(vbErrorView);
    }

    private static void configureErrorView(ImageView errorImage, ErrorType errorType) {
        switch (errorType) {
            case NO_PARSERS:
                errorImage.setImage(new Image("/images/errors/error_download.png"));
                break;
            case NO_CONNECTION:
                errorImage.setImage(new Image("/images/errors/error_no_connection.png"));
                break;
            case NO_SERVER:
                errorImage.setImage(new Image("/images/errors/error_cable.png"));
                break;
            case SEARCH_QUERY_TOO_SHORT:
                errorImage.setImage(new Image("/images/errors/error_search.png"));
                break;
            case LOAD_CONTENT:
                errorImage.setImage(new Image("/images/errors/error_view_cloud.png"));
                break;
            case TIP:
                errorImage.setImage(new Image("/images/errors/error_tip.png"));
                break;
        }
    }
}
