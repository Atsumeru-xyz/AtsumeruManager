package xyz.atsumeru.manager.managers;

import com.jfoenix.controls.JFXTabPane;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIconView;
import javafx.geometry.Side;
import javafx.scene.control.Tab;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import xyz.atsumeru.manager.controller.dialogs.ContentEditDialogController;
import xyz.atsumeru.manager.controller.tabs.TabRepositoryController;
import xyz.atsumeru.manager.helpers.LocaleManager;
import xyz.atsumeru.manager.utils.ViewUtils;
import xyz.atsumeru.manager.utils.globalutils.GUString;

import java.util.Optional;

public class TabPaneManager {
    @Getter private static TabPaneManager instance;
    private final JFXTabPane tabPane;
    @Getter private final MaterialDesignIconView glyphJobCount;

    private TabPaneManager(JFXTabPane tabPane) {
        this.tabPane = tabPane;
        this.glyphJobCount = ViewUtils.createTabIconView(MaterialDesignIcon.NUMERIC_0_BOX);
    }

    public static void init(JFXTabPane tabPane) {
        instance = new TabPaneManager(tabPane);
        instance.configureTabPane();
        instance.createTabs();
    }

    public static boolean selectTabIfPresent(String tabTitle) {
        for (Tab tabInPane : getInstance().tabPane.getTabs()) {
            if (GUString.equalsIgnoreCase(tabInPane.getText(), tabTitle)) {
                getInstance().tabPane.getSelectionModel().select(tabInPane);
                return true;
            }
        }
        return false;
    }

    public static void selectTab(int tabIndex) {
        getInstance().tabPane.getSelectionModel().select(tabIndex);
    }

    public static void selectTab(Tab tab) {
        getInstance().tabPane.getSelectionModel().select(tab);
    }

    public static void selectHomeTab() {
        selectTab(0);
    }

    public static void selectEditTab() {
        getInstance().tabPane.getTabs()
                .stream()
                .filter(tab -> GUString.equalsIgnoreCase(tab.getId(), "tabContentEdit"))
                .findFirst()
                .ifPresent(TabPaneManager::selectTab);
    }

    public static String getSelectedTabId() {
        return getInstance().tabPane.getSelectionModel().getSelectedItem().getId();
    }

    private void configureTabPane() {
        tabPane.setSide(Side.TOP);
        tabPane.setTabMinHeight(45);
        tabPane.setTabMaxHeight(45);
        tabPane.setTabMinWidth(45);
        tabPane.setTabMaxWidth(200);

        // Добавляем к TabLayout листенер изменения табов реализации отключения кнопок смены LibraryPresentation
        // и видимости GridScaleSlider вне Каталогов и скрытие "кнопки" Назад, если показано описание контента
        addTabChangeListener();
    }

    private void createTabs() {
        addTabIntoTabPane(ViewUtils.createTab(
                "/fxml/atsumeru/tabs/TabAtsumeruLibrary.fxml",
                "tabAtsumeruLibrary",
                ViewUtils.createMaterialDesignIconView(MaterialDesignIcon.COMPASS, 25, "#ecf0f1"),
                LocaleManager.getString("gui.tooltip.settings"),
                false
        ), true);

        addTabIntoTabPane(ViewUtils.createTab(
                "/fxml/atsumeru/dialogs/ContentEditDialog.fxml",
                "tabContentEdit",
                ViewUtils.createMaterialDesignIconView(MaterialDesignIcon.PENCIL, 25, "#ecf0f1"),
                LocaleManager.getString("gui.button.edit"),
                false
        ), false);

        addTabIntoTabPane(ViewUtils.createTab(
                "/fxml/tabs/TabRepository.fxml",
                "tabRepository",
                ViewUtils.createMaterialDesignIconView(MaterialDesignIcon.EARTH, 25, "#ecf0f1"),
                LocaleManager.getString("gui.tooltip.repository"),
                false
        ), false);

        addTabIntoTabPane(ViewUtils.createTab(
                "/fxml/atsumeru/tabs/settings/TabAtsumeruSettings.fxml",
                "tabAtsumeruSettings",
                ViewUtils.createMaterialDesignIconView(MaterialDesignIcon.SETTINGS, 25, "#ecf0f1"),
                LocaleManager.getString("gui.tooltip.settings"),
                false
        ), false);
    }

    private void addTabChangeListener() {
        // Add Tab ChangeListener
        tabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, selectedTab) -> {
            String previousTabId = Optional.ofNullable(oldValue).map(Tab::getId).orElse("");
            String selectedTabId = selectedTab.getId();
            if (GUString.isNotEmpty(selectedTabId)) {
                boolean isPreviousEditContentTab = GUString.equalsIgnoreCase(previousTabId, "tabContentEdit");

                if (isPreviousEditContentTab) {
                    TabsManager.getTabController(ContentEditDialogController.class).setData(null, false);
                }

                if ("tabRepository".equals(selectedTabId)) {
                    if (selectedTab.getContent() != null) {
                        TabRepositoryController controller = TabsManager.getTabController(TabRepositoryController.class);
                        controller.loadAvailableRepositories();
                    }
                }
            }
        });
    }

    public static void addTabIntoTabPane(Tab tab, boolean isSelect) {
        getInstance().addTabIntoTabPane(tab, null, isSelect);
    }

    public static void addTabIntoTabPanePenultimate(Tab tab, boolean isSelect) {
        TabPaneManager manager = getInstance();
        manager.addTabIntoTabPane(tab, manager.tabPane.getTabs().size() - 3, isSelect);
    }

    public void addTabIntoTabPane(Tab tab, @Nullable Integer position, boolean isSelect) {
        tab.setOnCloseRequest(event -> {
            if (tabPane.getTabs().size() <= 4) {
                selectTab(0);
            }
        });

        if (position != null) {
            tabPane.getTabs().add(position, tab);
        } else {
            tabPane.getTabs().add(tab);
        }

        if (isSelect) {
            tabPane.getSelectionModel().select(tab);
        }
    }
}
