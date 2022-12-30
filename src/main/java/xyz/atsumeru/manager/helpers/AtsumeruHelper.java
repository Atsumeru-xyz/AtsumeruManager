package xyz.atsumeru.manager.helpers;

import com.atsumeru.api.utils.Sort;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import xyz.atsumeru.manager.controller.tabs.TabAtsumeruLibraryController;
import xyz.atsumeru.manager.listeners.OnImportListener;
import xyz.atsumeru.manager.managers.TabsManager;

public class AtsumeruHelper {

    public static void addOnImportDoneListener(OnImportListener listener) {
        TabsManager.getTabController(TabAtsumeruLibraryController.class).addOnImportDoneListener(listener);
    }

    public static String getSortNameLocalized(Sort sort) {
        return LocaleManager.getString("atsumeru.sort." + sort.toString().toLowerCase());
    }

    public static MaterialDesignIcon getSortIcon(Sort sort) {
        switch (sort) {
            case TITLE:
                return MaterialDesignIcon.SORT_ALPHABETICAL;
            case YEAR:
                return MaterialDesignIcon.SORT_NUMERIC;
            case COUNTRY:
                return MaterialDesignIcon.EARTH;
            case LANGUAGE:
                return MaterialDesignIcon.FORMAT_COLOR_TEXT;
            case PUBLISHER:
                return MaterialDesignIcon.ACCOUNT_LOCATION;
            case PARODY:
                return MaterialDesignIcon.BOOK_VARIANT;
            case VOLUMES_COUNT:
                return MaterialDesignIcon.SORT_NUMERIC;
            case CHAPTERS_COUNT:
                return MaterialDesignIcon.SORT_NUMERIC;
            case SCORE:
                return MaterialDesignIcon.NUMERIC;
            case CREATED_AT:
                return MaterialDesignIcon.UPDATE;
            case UPDATED_AT:
                return MaterialDesignIcon.UPDATE;
            case POPULARITY:
                return MaterialDesignIcon.CHART_HISTOGRAM;
            case LAST_READ:
                return MaterialDesignIcon.BOOK_OPEN;
            default:
                return MaterialDesignIcon.ANCHOR;
        }
    }
}
