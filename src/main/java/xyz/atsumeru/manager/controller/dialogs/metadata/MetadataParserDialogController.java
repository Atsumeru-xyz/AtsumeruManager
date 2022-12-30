package xyz.atsumeru.manager.controller.dialogs.metadata;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXTextArea;
import com.jfoenix.controls.JFXTextField;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.fxml.FXML;
import javafx.scene.Node;
import kotlin.Pair;
import lombok.Setter;
import xyz.atsumeru.manager.controller.dialogs.BaseDialogController;
import xyz.atsumeru.manager.helpers.DialogsHelper;
import xyz.atsumeru.manager.helpers.LocaleManager;
import xyz.atsumeru.manager.listeners.OnDialogInputListener;
import xyz.atsumeru.manager.utils.FXUtils;
import xyz.atsumeru.manager.utils.globalutils.GUString;
import xyz.atsumeru.manager.validation.ParserLinkValidator;

public class MetadataParserDialogController extends BaseDialogController<Void> {
    @FXML
    private JFXTextField tfContentLink;
    @FXML
    private JFXTextArea taHtmlCode;
    @FXML
    private JFXButton btnFetch;
    @FXML
    private JFXCheckBox chbOverrideNonEmptyFields;

    @Setter
    private OnDialogInputListener callback;

    public static void createAndShow(OnDialogInputListener callback) {
        Pair<Node, MetadataParserDialogController> pair = FXUtils.loadFXML("/fxml/atsumeru/dialogs/metadata/MetadataParserDialog.fxml");
        MetadataParserDialogController controller = pair.getSecond();
        controller.setCallback(callback);

        controller.show(LocaleManager.getString("gui.fetch_metadata"), pair.getFirst());
    }

    @FXML
    protected void initialize() {
        Platform.runLater(() -> tfContentLink.requestFocus());

        BooleanBinding isValid = Bindings.createBooleanBinding(() ->
                !DialogsHelper.isInvalidParserLink(tfContentLink.getText())
                        || taHtmlCode.textProperty().isNotEmpty().get() && !DialogsHelper.isInvalidParserLink(tfContentLink.getText()),
                tfContentLink.textProperty(), taHtmlCode.textProperty());
        btnFetch.disableProperty().bind(isValid.not());

        ParserLinkValidator validator = new ParserLinkValidator(LocaleManager.getString("gui.parser_link_not_supported"));
        validator.setIcon(FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.EXCLAMATION_TRIANGLE));
        tfContentLink.getValidators().add(validator);
        tfContentLink.textProperty().addListener((o, oldVal, newVal) -> tfContentLink.validate());
    }

    @FXML
    void closeDialog() {
        close();
    }

    @FXML
    void fetch() {
        if (GUString.isNotEmpty(tfContentLink.getText()) && GUString.isNotEmpty(taHtmlCode.getText())) {
            callback.onConfirmInput(tfContentLink.getText(), taHtmlCode.getText(), chbOverrideNonEmptyFields.isSelected());
            closeDialog();
        } else if (GUString.isNotEmpty(tfContentLink.getText())) {
            callback.onConfirmInput(tfContentLink.getText(), chbOverrideNonEmptyFields.isSelected());
            closeDialog();
        }
    }

    @Override
    protected int minDialogWidth() {
        return 630;
    }
}