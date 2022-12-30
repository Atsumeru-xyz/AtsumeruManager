package xyz.atsumeru.manager.controller.dialogs;

import com.atsumeru.api.AtsumeruAPI;
import com.jfoenix.controls.JFXTextArea;
import io.github.palexdev.materialfx.controls.MFXProgressSpinner;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import kotlin.Pair;
import xyz.atsumeru.manager.controller.MainController;
import xyz.atsumeru.manager.helpers.LocaleManager;
import xyz.atsumeru.manager.helpers.RXUtils;
import xyz.atsumeru.manager.utils.FXUtils;
import xyz.atsumeru.manager.utils.ViewUtils;
import xyz.atsumeru.manager.utils.globalutils.GUArray;
import xyz.atsumeru.manager.utils.globalutils.GUString;
import xyz.atsumeru.manager.views.Snackbar;

import java.util.List;

public class DownloadedLinksCheckerDialogController extends BaseDialogController<Void> {
    @FXML
    private VBox vbCheckLinks;
    @FXML
    private VBox vbResult;
    @FXML
    private MFXProgressSpinner spinnerLoading;

    @FXML
    private JFXTextArea taLinks;
    @FXML
    private JFXTextArea taLinksDownloaded;
    @FXML
    private JFXTextArea taLinksNotDownloaded;

    private Disposable disposable;

    public static void createAndShow() {
        Pair<Node, DownloadedLinksCheckerDialogController> pair = FXUtils.loadFXML("/fxml/atsumeru/dialogs/DownloadedLinksCheckerDialog.fxml");
        DownloadedLinksCheckerDialogController controller = pair.getSecond();
        controller.show(LocaleManager.getString("gui.check_downloaded_from_links"), pair.getFirst());
    }

    @FXML
    protected void initialize() {
        ViewUtils.setNodeGone(vbResult, spinnerLoading);
    }

    @FXML
    void closeDialog() {
        close();
    }

    @FXML
    void reset() {
        taLinks.clear();
        taLinksDownloaded.clear();
        taLinksNotDownloaded.clear();

        ViewUtils.setNodeGone(vbResult);
        ViewUtils.setNodeVisible(vbCheckLinks);
    }

    @FXML
    void check() {
        if (GUString.isNotEmpty(taLinks.getText())) {
            ViewUtils.setNodeVisible(spinnerLoading);

            List<String> links = GUArray.splitString(taLinks.getText(),"\\n");
            disposable = AtsumeruAPI.checkLinksDownloaded(links)
                    .cache()
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(Schedulers.io())
                    .subscribe(downloadedLinks -> {
                        taLinksDownloaded.setText(GUString.join("\n", downloadedLinks.getDownloaded()));
                        taLinksNotDownloaded.setText(GUString.join("\n", downloadedLinks.getNotDownloaded()));

                        ViewUtils.setNodeGone(spinnerLoading, vbCheckLinks);
                        ViewUtils.setNodeVisible(vbResult);

                        RXUtils.safeDispose(disposable);
                    }, throwable -> {
                        ViewUtils.setNodeGone(spinnerLoading);
                        ViewUtils.setNodeVisible(vbCheckLinks);
                        Snackbar.showExceptionSnackbar(MainController.getSnackbarRoot(true), throwable, true);

                        RXUtils.safeDispose(disposable);
                    });
        }
    }

    @Override
    protected int minDialogWidth() {
        return 550;
    }
}
