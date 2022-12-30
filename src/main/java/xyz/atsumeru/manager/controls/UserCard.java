package xyz.atsumeru.manager.controls;

import com.atsumeru.api.model.user.User;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXRippler;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import lombok.Getter;
import xyz.atsumeru.manager.helpers.LocaleManager;
import xyz.atsumeru.manager.source.AtsumeruSource;
import xyz.atsumeru.manager.utils.FXUtils;
import xyz.atsumeru.manager.utils.ViewUtils;
import xyz.atsumeru.manager.utils.globalutils.GUArray;
import xyz.atsumeru.manager.utils.globalutils.GUString;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("unused")
public class UserCard extends HBox {
    @FXML
    private StackPane pane;
    @FXML
    private Pane rippler;
    @FXML
    private Label lblUsername;
    @FXML
    private Label lblRoles;
    @FXML
    private Label lblAuthorities;
    @FXML
    private Label lblAllowedCategories;
    @FXML
    private Label lblDisallowedGenres;
    @FXML
    private Label lblDisallowedTags;
    @FXML
    private JFXButton btnEditUser;
    @FXML
    private JFXButton btnDeleteUser;

    @Getter private User user;

    public UserCard(User user) {
        this.user = user;
        FXUtils.loadComponent(this, "/fxml/atsumeru/controls/UserCard.fxml");
    }

    @FXML
    private void initialize() {
        createRippler();
        setUserInfo();

        ViewUtils.createOnMouseEnterBorderEffect(pane, btnEditUser, btnDeleteUser);
        ViewUtils.createOnMouseExitedBorderEffect(pane, btnEditUser, btnDeleteUser);
    }

    private void createRippler() {
        JFXRippler rippler = new JFXRippler(this.rippler);
        rippler.setRipplerFill(Color.WHITE);
        pane.getChildren().add(rippler);
    }

    public void setUser(User user) {
        this.user = user;
        setUserInfo();
    }

    @SuppressWarnings("ConstantConditions")
    private void setUserInfo() {
        lblUsername.setText(user.getUserName());
        lblRoles.setText(GUString.join(", ", user.getRoles()));
        lblAuthorities.setText(GUString.join(", ", user.getAuthorities()));

        lblAllowedCategories.setText(getLimitsListToStringOrPlaceholder(user.getAllowedCategories(), "all_caps"));
        lblDisallowedGenres.setText(getLimitsListToStringOrPlaceholder(user.getDisallowedGenres(), "no_limits_caps"));
        lblDisallowedTags.setText(getLimitsListToStringOrPlaceholder(user.getDisallowedTags(), "no_limits_caps"));

        boolean isCurrentUser = user.getUserName().equalsIgnoreCase(AtsumeruSource.getCurrentUserName());
        if (isCurrentUser) {
            ViewUtils.setNodeGone(btnDeleteUser);
        }
    }

    private String getLimitsListToStringOrPlaceholder(List<String> limits, String placeholderRes) {
        return Optional.ofNullable(limits)
                .filter(GUArray::isNotEmpty)
                .map(list -> GUString.join(", ", list))
                .orElseGet(() -> LocaleManager.getString(placeholderRes));
    }

    public void setOnEditUserClick(EventHandler<MouseEvent> eventHandler) {
        pane.setOnMouseClicked(eventHandler);
        btnEditUser.setOnMouseClicked(eventHandler);
    }

    public void setOnDeleteUserClick(EventHandler<MouseEvent> eventHandler) {
        btnDeleteUser.setOnMouseClicked(eventHandler);
    }
}
