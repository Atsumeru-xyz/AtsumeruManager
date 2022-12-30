package xyz.atsumeru.manager.controller.dialogs;

import com.atsumeru.api.AtsumeruAPI;
import com.atsumeru.api.manager.ServerManager;
import com.atsumeru.api.model.server.Server;
import com.jfoenix.controls.JFXPasswordField;
import com.jfoenix.controls.JFXTextField;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import kotlin.Pair;
import xyz.atsumeru.manager.helpers.LocaleManager;
import xyz.atsumeru.manager.source.AtsumeruSource;
import xyz.atsumeru.manager.utils.FXUtils;
import xyz.atsumeru.manager.utils.ViewUtils;
import xyz.atsumeru.manager.utils.globalutils.GULinks;
import xyz.atsumeru.manager.views.Snackbar;

public class EditServerDialogController extends BaseDialogController<Server> {
    @FXML
    VBox container;
    @FXML
    JFXTextField tfServerName;
    @FXML
    JFXTextField tfServerHost;
    @FXML
    JFXTextField tfServerLogin;
    @FXML
    JFXPasswordField tfServerPassword;

    private int newServerId;

    public static void createAndShow(Server server, ChangeListener<Server> changeListener) {
        Pair<Node, EditServerDialogController> pair = FXUtils.loadFXML("/fxml/atsumeru/dialogs/EditServerDialog.fxml");
        EditServerDialogController controller = pair.getSecond();

        String heading = LocaleManager.getString(server != null ? "atsumeru.edit_server" : "atsumeru.add_server");
        controller.show(server, heading, pair.getFirst(), changeListener);
    }

    @FXML
    public void initialize() {
        fillServerData();
        addTextFieldValidators();
        Platform.runLater(() -> container.requestFocus());
    }

    public void fillServerData() {
        Platform.runLater(() -> {
            Server server = property.getValue();
            if (server != null) {
                tfServerName.setText(server.getName());
                tfServerHost.setText(server.getHost());
                tfServerLogin.setText(server.getBasicCredentials().getFirst());
                tfServerPassword.setText(server.getBasicCredentials().getSecond());
            } else {
                newServerId = AtsumeruAPI.getServerManager().createNewServerId();
            }
        });
    }

    private void addTextFieldValidators() {
        ViewUtils.addTextFieldValidator(tfServerName, ViewUtils.createNotEmptyValidator());
        ViewUtils.addTextFieldValidator(tfServerHost, ViewUtils.createNotEmptyValidator());
        ViewUtils.addTextFieldValidator(tfServerLogin, ViewUtils.createNotEmptyValidator());
        ViewUtils.addTextFieldValidator(tfServerPassword, ViewUtils.createNotEmptyValidator());
    }

    private boolean validateFields() {
        return tfServerName.validate()
                && tfServerHost.validate()
                && tfServerLogin.validate()
                && tfServerPassword.validate();
    }

    @FXML
    void closeDialog() {
        close();
    }

    @FXML
    void save() {
        if (validateFields()) {
            ServerManager serverManager = AtsumeruAPI.getServerManager();

            String serverName = tfServerName.getText();
            String serverHost = tfServerHost.getText();
            Pair<String, String> credentials = new Pair<>(tfServerLogin.getText(), tfServerPassword.getText());

            Server server = property.getValue();
            property.setValue(null);
            if (server == null) {
                Server newServer = new Server(
                        newServerId,
                        serverName,
                        serverHost,
                        credentials,
                        false
                );

                Single<Boolean> single = Single.create(subscriber ->
                        subscriber.onSuccess(GULinks.isURLReachable(newServer.getPingUrl(),
                                credentials.getFirst(), credentials.getSecond(), 3000)));

                single.cache().subscribeOn(Schedulers.newThread())
                        .observeOn(Schedulers.io())
                        .subscribe(success -> {
                            if (success) {
                                property.setValue(newServer);
                                serverManager.addServer(newServer);
                                saveServersAndCloseDialog();
                            } else {
                                Snackbar.showSnackBar(container, LocaleManager.getString("atsumeru.server_not_responding"), Snackbar.Type.ERROR);
                            }
                        }, throwable -> Snackbar.showSnackBar(container, LocaleManager.getString("atsumeru.server_not_responding"), Snackbar.Type.ERROR));
            } else {
                server.setName(serverName);
                server.setHost(serverHost);
                server.setBasicCredentials(credentials);

                serverManager.removeServer(server);
                serverManager.addServer(server);

                property.setValue(server);

                saveServersAndCloseDialog();
            }
        }
    }

    private void saveServersAndCloseDialog() {
        AtsumeruSource.saveServers();
        closeDialog();
    }

    @Override
    protected int minDialogWidth() {
        return 600;
    }
}
