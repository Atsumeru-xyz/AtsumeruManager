package xyz.atsumeru.manager.controller.tabs;

import com.jpro.webapi.WebAPI;
import io.github.palexdev.materialfx.controls.MFXProgressSpinner;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import xyz.atsumeru.manager.FXApplication;
import xyz.atsumeru.manager.controller.BaseController;
import xyz.atsumeru.manager.controls.EmptyView;
import xyz.atsumeru.manager.controls.ErrorView;
import xyz.atsumeru.manager.enums.ErrorType;
import xyz.atsumeru.manager.exceptions.ApiParseException;
import xyz.atsumeru.manager.helpers.LocaleManager;
import xyz.atsumeru.manager.utils.ViewUtils;
import xyz.atsumeru.manager.utils.globalutils.GUArray;

import java.io.IOException;
import java.util.List;

public class TabRepositoryController extends BaseController {

    @FXML
    Pane root;
    @FXML
    TabPane tabsRepository;
    @FXML
    FlowPane fpItems;
    @FXML
    EmptyView vbEmptyView;
    @FXML
    ErrorView vbErrorView;
    @FXML
    MFXProgressSpinner spinnerLoading;

    private boolean repoDataLoaded = false;

    @FXML
    protected void initialize() throws IOException {
        super.initialize();
    }

    public void loadAvailableRepositories() {
        if (!repoDataLoaded && isSupportsMetadataFetching()) {
            ViewUtils.setNodeGone(vbEmptyView, vbErrorView);
            ViewUtils.setNodeVisible(spinnerLoading);

            FXApplication.getParsersManager().fetchAvailableRepositories(
                    this::insertRepositoryTabs,
                    throwable -> {
                        throwable.printStackTrace();
                        showErrorViewRepositories();
                    }
            );

            repoDataLoaded = true;
        }
    }

    private boolean isSupportsMetadataFetching() {
        if (!FXApplication.getParsersManager().isSupportsMetadataFetching()) {
            ViewUtils.setNodeGone(vbEmptyView);
            vbErrorView.showErrorView(
                    new ApiParseException(LocaleManager.getString(WebAPI.isBrowser() ? "gui.error.unsupported_in_web" : "gui.error.unsupported_in_open_source")),
                    ErrorType.LOAD_CONTENT,
                    spinnerLoading,
                    false,
                    null,
                    false,
                    null
            );
            return false;
        }
        return true;
    }

    public void loadRepositoryData(String repositoryType) {
        ViewUtils.setNodeGone(fpItems, vbEmptyView, vbErrorView);
        ViewUtils.setNodeVisible(spinnerLoading);

        FXApplication.getParsersManager().fetchRepositoryCatalog(
                repositoryType,
                () -> root,
                () -> fpItems,
                nodes -> insertParserCardsIntoRoot(nodes, repositoryType), throwable -> {
                    throwable.printStackTrace();
                    showErrorViewParsersList(repositoryType);
                }
        );
    }

    private void insertRepositoryTabs(List<Tab> tabs) {
        Platform.runLater(() -> {
            ViewUtils.setNodeGone(spinnerLoading);
            if (GUArray.isNotEmpty(tabs)) {
                tabsRepository.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> loadRepositoryData(newValue.getUserData().toString()));
                tabsRepository.getTabs().addAll(tabs);
                tabsRepository.getSelectionModel().select(0);
            }
        });
    }

    private void insertParserCardsIntoRoot(List<Node> nodes, String repositoryType) {
        Platform.runLater(() -> {
            fpItems.getChildren().clear();
            if (GUArray.isNotEmpty(nodes)) {
                fpItems.getChildren().clear();
                fpItems.getChildren().addAll(nodes);
                ViewUtils.setNodeGone(spinnerLoading);
                vbEmptyView.checkEmptyItems(fpItems.getChildren(), fpItems, null);
            } else {
                showErrorViewParsersList(repositoryType);
            }
        });
    }

    private void showErrorViewRepositories() {
       Platform.runLater(() -> vbErrorView.showErrorView(
               new ApiParseException(LocaleManager.getString("gui.error.unable_to_load_repository_list")),
               ErrorType.NO_CONNECTION,
               null,
               true,
               this::loadAvailableRepositories,
               false,
               null
       ));
       repoDataLoaded = false;
    }

    private void showErrorViewParsersList(String repositoryType) {
        Platform.runLater(() -> vbErrorView.showErrorView(
                new ApiParseException(LocaleManager.getString("gui.error.unable_to_load_parsers_list")),
                ErrorType.NO_CONNECTION,
                spinnerLoading,
                true,
                () -> loadRepositoryData(repositoryType),
                false,
                null
        ));
    }
}
