package xyz.atsumeru.manager.managers;

import lombok.Getter;
import xyz.atsumeru.manager.controller.BaseController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings("unchecked")
public class TabsManager {
    private static TabsManager instance;
    @Getter private final Map<String, BaseController> controllers;

    private TabsManager() {
        controllers = new HashMap<>();
    }

    public static TabsManager getInstance() {
        if (TabsManager.instance == null) {
            TabsManager.instance = new TabsManager();
        }
        return TabsManager.instance;
    }

    public static void addTabController(String id, BaseController controller) {
        TabsManager.getInstance().getControllers().put(id, controller);
    }

    public static void removeTabController(String id) {
        TabsManager.getInstance().getControllers().remove(id);
    }

    public static <T extends BaseController> boolean hasTabController(Class<T> clazz) {
        return TabsManager.getInstance().getControllers().get(clazz.getCanonicalName()) != null;
    }

    public static <T extends BaseController> T getTabController(Class<T> clazz) {
        return (T) TabsManager.getInstance().getControllers().get(clazz.getCanonicalName());
    }

    public static <T extends BaseController> Optional<T> getTabControllerOptional(Class<T> clazz) {
        return Optional.ofNullable(TabsManager.getInstance().getControllers().get(clazz.getCanonicalName())).map(controller -> (T) controller);
    }
}
