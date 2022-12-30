package xyz.atsumeru.manager.validation;

import com.jfoenix.validation.base.ValidatorBase;
import javafx.scene.control.TextInputControl;
import xyz.atsumeru.manager.FXApplication;
import xyz.atsumeru.manager.utils.globalutils.GULinks;

public class ParserLinkValidator extends ValidatorBase {

    public ParserLinkValidator(String message) {
        super(message);
    }

    @Override
    protected void eval() {
        if (srcControl.get() instanceof TextInputControl) {
            evalTextInputField();
        }
    }

    private void evalTextInputField() {
        TextInputControl textField = (TextInputControl) srcControl.get();
        hasErrors.set(!FXApplication.getParsersManager().containsParser(GULinks.getHostName(textField.getText())));
    }
}