package xyz.atsumeru.manager.controller;

import javafx.fxml.FXML;
import javafx.scene.layout.Pane;
import xyz.atsumeru.manager.managers.TabsManager;
import xyz.atsumeru.manager.utils.FXUtils;

import java.io.IOException;

public abstract class BaseController {

    @FXML
    protected void initialize() throws IOException {
        registerTab();
        FXUtils.runLaterDelayed(() -> {
            if (getContentRoot() != null) {
                getContentRoot().requestFocus();
            }
        }, 200);
    }

    private void registerTab() {
        TabsManager.addTabController(this.getClass().getCanonicalName(), this);
    }

    protected void unregisterTab() {
        TabsManager.removeTabController(this.getClass().getCanonicalName());
    }

    protected Pane getContentRoot() {
        return null;
    }
}
