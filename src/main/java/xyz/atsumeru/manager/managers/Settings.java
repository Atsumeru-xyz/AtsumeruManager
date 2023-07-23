package xyz.atsumeru.manager.managers;

import com.atsumeru.api.utils.Sort;
import org.jetbrains.annotations.Nullable;
import xyz.atsumeru.manager.enums.AppCloseBehaviorInMetadataEditor;
import xyz.atsumeru.manager.enums.GridScaleType;
import xyz.atsumeru.manager.utils.globalutils.GUEnum;
import xyz.atsumeru.manager.utils.globalutils.GUString;
import xyz.atsumeru.manager.utils.globalutils.GUType;

import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.Optional;
import java.util.Properties;

public class Settings {
    private static Settings INSTANCE;
    private static Properties properties;
    private static final String PROPERTIES_FILENAME = "app.properties";

    public static void init() {
        properties = new Properties();
        try (FileReader fileReader = new FileReader(WorkspaceManager.WORKING_DIR + PROPERTIES_FILENAME)) {
            properties.load(fileReader);
        } catch (IOException e) {
            System.err.println("Unable to load " + PROPERTIES_FILENAME);
        }
        INSTANCE = new Settings();
    }

    public static Settings getInstance() {
        return INSTANCE;
    }

    public String getString(String key, String def) {
        return properties.getProperty(key, def);
    }

    public int getInt(String key, int def) {
        return GUType.getIntDef(properties.getProperty(key), def);
    }

    public long getLong(String key, long def) {
        return GUType.getLongDef(properties.getProperty(key), def);
    }

    public float getFloat(String key, float def) {
        return GUType.getFloatDef(properties.getProperty(key), def);
    }

    public void putString(String key, String value) {
        properties.setProperty(key, value);
    }

    public void putInt(String key, int value) {
        properties.setProperty(key, String.valueOf(value));
    }

    public void putLong(String key, long value) {
        properties.setProperty(key, String.valueOf(value));
    }

    public void putFloat(String key, float value) {
        properties.setProperty(key, String.valueOf(value));
    }

    public void remove(String key) {
        properties.remove(key);
    }

    public void save() {
        saveProperties();
    }

    private static <E extends Enum<E>> void setProperty(String propertyName, @Nullable E enumValue) {
        setProperty(propertyName, Optional.ofNullable(enumValue).map(Enum::toString).orElse(null));
    }

    private static void setProperty(String propertyName, @Nullable String propertyValue) {
        if (GUString.isNotEmpty(propertyValue)) {
            properties.setProperty(propertyName, propertyValue);
        } else {
            properties.remove(propertyName);
        }
        saveProperties();
    }

    private static void saveProperties() {
        try (FileOutputStream fout = new FileOutputStream(WorkspaceManager.WORKING_DIR + PROPERTIES_FILENAME)) {
            properties.store(fout, "Auto save properties: " + new Date());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static final String KEY_APP_LANGUAGE = "app_language";
    private static final String DEFAULT_APP_LANGUAGE = "";

    public static String getDefaultAppLanguageCode() {
        return properties.getProperty(KEY_APP_LANGUAGE, DEFAULT_APP_LANGUAGE);
    }

    public static void putDefaultAppLanguageCode(String value) {
        setProperty(KEY_APP_LANGUAGE, value);
    }

    private static final String KEY_APP_CLOSE_BEHAVIOR_IN_METADATA_EDITOR = "app_close_behavior_in_metadata_editor";
    private static final AppCloseBehaviorInMetadataEditor DEFAULT_APP_CLOSE_BEHAVIOR_IN_METADATA_EDITOR = AppCloseBehaviorInMetadataEditor.CLOSE_EDITOR;

    public static AppCloseBehaviorInMetadataEditor getAppCloseBehaviorInMetadataEditor() {
        return GUEnum.valueOf(AppCloseBehaviorInMetadataEditor.class, properties.getProperty(KEY_APP_CLOSE_BEHAVIOR_IN_METADATA_EDITOR, ""), DEFAULT_APP_CLOSE_BEHAVIOR_IN_METADATA_EDITOR);
    }

    public static void putAppCloseBehaviorInMetadataEditor(AppCloseBehaviorInMetadataEditor value) {
        setProperty(KEY_APP_CLOSE_BEHAVIOR_IN_METADATA_EDITOR, value.toString());
    }

    private static final String KEY_UPDATE_BRANCH = "update_branch";
    private static final UpdatesChecker.UpdateBranch DEFAULT_UPDATE_BRANCH = UpdatesChecker.UpdateBranch.RELEASE;

    public static UpdatesChecker.UpdateBranch getUpdateBranch() {
        return GUEnum.valueOf(UpdatesChecker.UpdateBranch.class, properties.getProperty(KEY_UPDATE_BRANCH, ""), DEFAULT_UPDATE_BRANCH);
    }

    public static void putUpdateBranch(UpdatesChecker.UpdateBranch value) {
        setProperty(KEY_UPDATE_BRANCH, value.name());
    }

    private static final String KEY_APP_ACCENT_COLOR = "app_accent_color";
    private static final String DEFAULT_APP_ACCENT_COLOR = "#B5305F";

    public static String getAppAccentColor() {
        return properties.getProperty(KEY_APP_ACCENT_COLOR, DEFAULT_APP_ACCENT_COLOR);
    }

    public static void putAppAccentColor(String value) {
        setProperty(KEY_APP_ACCENT_COLOR, value);
    }

    private static final String KEY_GRID_SCALE_TYPE = "grid_scale_type";
    private static final GridScaleType DEFAULT_GRID_SCALE_TYPE = GridScaleType.PROPORTIONAL_SCALE;

    public static GridScaleType getGridScaleType() {
        return GUEnum.valueOf(GridScaleType.class, properties.getProperty(KEY_GRID_SCALE_TYPE), DEFAULT_GRID_SCALE_TYPE);
    }

    public static void putGridScaleType(GridScaleType value) {
        setProperty(KEY_GRID_SCALE_TYPE, value);
    }

    private static final String KEY_GRID_SCALE = "grid_scale";
    private static final double DEFAULT_GRID_SCALE = 6;

    public static double getGridScale() {
        return GUType.getDoubleDef(properties.getProperty(KEY_GRID_SCALE), DEFAULT_GRID_SCALE);
    }

    public static void putGridScale(double value) {
        setProperty(KEY_GRID_SCALE, String.valueOf(value));
    }

    /**
     * Atsumeru related Settings
     */
    public static class Atsumeru {
        private static final String KEY_ATSUMERU_SERVERS = "atsumeru_servers";
        private static final String DEFAULT_ATSUMERU_SERVERS = "";

        public static String getAtsumeruServers() {
            return properties.getProperty(KEY_ATSUMERU_SERVERS, DEFAULT_ATSUMERU_SERVERS);
        }

        public static void putAtsumeruServers(String value) {
            setProperty(KEY_ATSUMERU_SERVERS, value);
        }

        private static final String KEY_CURRENT_ATSUMERU_SERVER = "current_atsumeru_server";
        private static final int DEFAULT_CURRENT_ATSUMERU_SERVER = -1;

        public static int getCurrentAtsumeruServer() {
            return GUType.getIntDef(properties.getProperty(KEY_CURRENT_ATSUMERU_SERVER), DEFAULT_CURRENT_ATSUMERU_SERVER);
        }

        public static void putCurrentAtsumeruServer(int value) {
            setProperty(KEY_CURRENT_ATSUMERU_SERVER, String.valueOf(value));
        }

        private static final String KEY_CURRENT_ATSUMERU_SORT = "current_atsumeru_sort";
        private static final Sort DEFAULT_CURRENT_ATSUMERU_SORT = Sort.CREATED_AT;

        public static Sort getCurrentAtsumeruSort() {
            return GUEnum.valueOf(Sort.class, properties.getProperty(KEY_CURRENT_ATSUMERU_SORT), DEFAULT_CURRENT_ATSUMERU_SORT);
        }

        public static void putCurrentAtsumeruSort(Sort value) {
            setProperty(KEY_CURRENT_ATSUMERU_SORT, value.toString());
        }

        private static final String KEY_CURRENT_ATSUMERU_SORT_ORDER = "current_atsumeru_sort_order";
        private static final boolean DEFAULT_CURRENT_ATSUMERU_SORT_ORDER = false;

        public static boolean getCurrentAtsumeruSortOrder() {
            return GUType.getBoolDef(properties.getProperty(KEY_CURRENT_ATSUMERU_SORT_ORDER), DEFAULT_CURRENT_ATSUMERU_SORT_ORDER);
        }

        public static void putCurrentAtsumeruSortOrder(boolean value) {
            setProperty(KEY_CURRENT_ATSUMERU_SORT_ORDER, String.valueOf(value));
        }

        private static final String KEY_IS_SHOW_FILTERS = "atsumeru_is_show_filters";
        private static final boolean DEFAULT_IS_SHOW_FILTERS = true;

        public static boolean isShowFilters() {
            return GUType.getBoolDef(properties.getProperty(KEY_IS_SHOW_FILTERS), DEFAULT_IS_SHOW_FILTERS);
        }

        public static void putShowFilters(boolean value) {
            setProperty(KEY_IS_SHOW_FILTERS, String.valueOf(value));
        }

        private static final String KEY_LOCK_CATEGORIES = "atsumeru_lock_categories";
        private static final boolean DEFAULT_LOCK_CATEGORIES = true;

        public static boolean isLockCategories() {
            return GUType.getBoolDef(properties.getProperty(KEY_LOCK_CATEGORIES), DEFAULT_LOCK_CATEGORIES);
        }

        public static void putLockCategories(boolean value) {
            setProperty(KEY_LOCK_CATEGORIES, String.valueOf(value));
        }

        private static final String KEY_SAVE_METADATA_INTO_LINKED_ARCHIVES = "atsumeru_save_metadata_into_linked_archives";
        private static final boolean SAVE_METADATA_INTO_LINKED_ARCHIVES = true;

        public static boolean isSaveMetadataIntoLinkedArchives() {
            return GUType.getBoolDef(properties.getProperty(KEY_SAVE_METADATA_INTO_LINKED_ARCHIVES), SAVE_METADATA_INTO_LINKED_ARCHIVES);
        }

        public static void putSaveMetadataIntoLinkedArchives(boolean value) {
            setProperty(KEY_SAVE_METADATA_INTO_LINKED_ARCHIVES, String.valueOf(value));
        }

        private static final String KEY_SAVE_METADATA_INTO_DB_ONLY = "atsumeru_save_metadata_into_db_only";
        private static final boolean SAVE_METADATA_INTO_DB_ONLY = false;

        public static boolean isSaveMetadataIntoDBOnly() {
            return GUType.getBoolDef(properties.getProperty(KEY_SAVE_METADATA_INTO_DB_ONLY), SAVE_METADATA_INTO_DB_ONLY);
        }

        public static void putSaveMetadataIntoDBOnly(boolean value) {
            setProperty(KEY_SAVE_METADATA_INTO_DB_ONLY, String.valueOf(value));
        }

        private static final String KEY_IS_DISABLE_PROGRESS_SYNC = "is_disable_progress_sync";
        private static final boolean DEFAULT_IS_DISABLE_PROGRESS_SYNC = false;

        public static boolean isDisableProgressSync() {
            return GUType.getBoolDef(properties.getProperty(KEY_IS_DISABLE_PROGRESS_SYNC), DEFAULT_IS_DISABLE_PROGRESS_SYNC);
        }

        public static void putDisableProgressSync(boolean value) {
            setProperty(KEY_IS_DISABLE_PROGRESS_SYNC, String.valueOf(value));
        }

        private static final String KEY_ATSUMERU_EDIT_BUTTON_LOCKED = "atsumeru_edit_button_locked_";
        private static final boolean DEFAULT_ATSUMERU_EDIT_BUTTON_LOCKED = false;

        public static boolean isButtonLocked(String buttonName) {
            return GUType.getBoolDef(properties.getProperty(KEY_ATSUMERU_EDIT_BUTTON_LOCKED + buttonName.toLowerCase()), DEFAULT_ATSUMERU_EDIT_BUTTON_LOCKED);
        }

        public static void putButtonLocked(String buttonName, boolean value) {
            setProperty(KEY_ATSUMERU_EDIT_BUTTON_LOCKED + buttonName.toLowerCase(), String.valueOf(value));
        }
    }

    /**
     * Metadata related Settings
     */
    public static class Metadata {
        private static final String KEY_COMICVINE_API_KEY = "comicvine_api_key";
        private static final String DEFAULT_KEY_COMICVINE_API_KEY = "";

        public static String getComicVineApiKey() {
            return properties.getProperty(KEY_COMICVINE_API_KEY, DEFAULT_KEY_COMICVINE_API_KEY);
        }

        public static void putComicVineApiKey(String value) {
            setProperty(KEY_COMICVINE_API_KEY, value);
        }
    }
}
