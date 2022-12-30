package xyz.atsumeru.manager.controller.dialogs;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import org.jetbrains.annotations.Nullable;
import xyz.atsumeru.manager.FXApplication;
import xyz.atsumeru.manager.controller.BaseController;
import xyz.atsumeru.manager.controller.MainController;
import xyz.atsumeru.manager.controls.jfoenix.JFXCustomDialog;
import xyz.atsumeru.manager.helpers.DialogBuilder;
import xyz.atsumeru.manager.managers.TabsManager;
import xyz.atsumeru.manager.utils.globalutils.GUApp;

public abstract class BaseDialogController<T> extends BaseController {
    private DialogBuilder dialogBuilder;
    protected ObjectProperty<T> property = new SimpleObjectProperty<>();

    protected abstract int minDialogWidth();

    protected boolean isClosable() {
        return true;
    }

    protected void show(String heading, Node dialogBody) {
        show(null, heading, dialogBody, null, () -> FXApplication.dimContent(true), () -> FXApplication.dimContent(false));
    }

    protected void show(@Nullable T initialValue, String heading, Node dialogBody, @Nullable ChangeListener<T> changeListener) {
        show(initialValue, heading, dialogBody, changeListener, () -> FXApplication.dimContent(true), () -> FXApplication.dimContent(false));
    }

    protected void show(@Nullable T initialValue, String heading, Node dialogBody, @Nullable ChangeListener<T> changeListener, @Nullable Runnable beforeShowRunnable, @Nullable Runnable afterDismissRunnable) {
        property.setValue(initialValue);
        if (changeListener != null) {
            property.addListener(changeListener);
        }

        dialogBuilder = DialogBuilder.create(TabsManager.getTabController(MainController.class).getContentRoot())
                .withDialogType(DialogBuilder.DialogType.NODE)
                .withTransition(JFXCustomDialog.DialogTransition.CENTER)
                .withMinWidth(minDialogWidth())
                .withHeading(heading)
                .withBody(dialogBody)
                .withNoButtons()
                .withClosable(isClosable())
                .withOnOpenRunnable(() -> GUApp.safeRun(beforeShowRunnable))
                .withOnCloseRunnable(() -> GUApp.safeRun(afterDismissRunnable))
                .build();
        dialogBuilder.show();
    }

    protected void close() {
        dialogBuilder.close();
    }
}
