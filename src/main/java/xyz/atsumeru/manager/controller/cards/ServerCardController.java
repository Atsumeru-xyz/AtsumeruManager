package xyz.atsumeru.manager.controller.cards;

import com.atsumeru.api.model.server.Server;
import com.jfoenix.controls.JFXButton;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.Setter;
import xyz.atsumeru.manager.controller.dialogs.EditServerDialogController;
import xyz.atsumeru.manager.helpers.LocaleManager;
import xyz.atsumeru.manager.listeners.OnItemClickListener;
import xyz.atsumeru.manager.utils.FXUtils;
import xyz.atsumeru.manager.utils.ViewUtils;

public class ServerCardController {
    public static final String ACTION_EDIT = "edit";
    public static final String ACTION_DELETE = "delete";

    @FXML
    Pane root;
    @FXML
    Pane pane;
    @FXML
    Pane pRippler;
    @FXML
    HBox hbRoot;
    @FXML
    VBox vbRoot;

    @FXML
    Label lblServiceName;
    @FXML
    Label lblServiceLink;
    @FXML
    Label lblServiceSlot;
    @FXML
    JFXButton btnDeleteServer;

    @Setter
    private Server server;
    @Setter
    private OnItemClickListener onItemClickListener;

    public static ServerCardController createCard(Server server) {
        FXMLLoader fxmlLoader = FXUtils.loadFXMLAndGetLoader("/fxml/atsumeru/cards/ServerCard.fxml");
        ServerCardController controller = fxmlLoader.getController();
        controller.setServer(server);
        return controller;
    }

    @FXML
    public void initialize() {
        ViewUtils.createRoundedRippler(pRippler, pane, 12, 12);

        root.setOnMouseClicked(event -> EditServerDialogController.createAndShow(server, (observable, oldServer, server) -> {
            if (server != null) {
                this.server = server;
                updateServerInfo();
            }
        }));

        btnDeleteServer.setOnMouseClicked(event -> onItemClickListener.onClick(ACTION_DELETE, server.getId(), server));

        ViewUtils.createOnMouseEnterBorderEffect(pane, btnDeleteServer);
        ViewUtils.createOnMouseExitedBorderEffect(pane, btnDeleteServer);

        updateServerInfo();
    }

    public void updateServerInfo() {
        Platform.runLater(() -> {
            lblServiceName.setText(server.getName());
            lblServiceLink.setText(server.getHost());
            lblServiceSlot.setText(LocaleManager.getStringFormatted("atsumeru.slot", String.valueOf(server.getId())));
        });
    }

    public Node getRootNode() {
        return root;
    }
}
