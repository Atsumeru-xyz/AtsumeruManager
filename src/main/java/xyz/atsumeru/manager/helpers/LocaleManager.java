package xyz.atsumeru.manager.helpers;

import lombok.Getter;
import xyz.atsumeru.manager.utils.globalutils.GUString;

import java.util.Locale;
import java.util.ResourceBundle;

public class LocaleManager {
    @Getter
    private static ResourceBundle resourceBundle;

    public static String getSystemLanguageCode() {
        return Locale.getDefault().getLanguage();
    }

    public static boolean isCurrentSystemLocaleIsCyrillic() {
        String systemLanguageCode = getSystemLanguageCode();
        return systemLanguageCode.equalsIgnoreCase("ru") || systemLanguageCode.equalsIgnoreCase("ua");
    }

    public static void loadResourceBundle() {
        resourceBundle = ResourceBundle.getBundle("lang", java.util.Locale.getDefault());
    }

    public static void setLocale(String langCode) {
        if (GUString.isNotEmpty(langCode)) {
            Locale.setDefault(new Locale(langCode));
        }
    }

    public static String getString(String key) {
        try {
            return resourceBundle.getString(key);
        } catch (Exception ex) {
            if (JavaHelper.isDebug()) {
                System.err.println(ex.getMessage());
            }
            return "placeholder";
        }
    }

    public static String getString(String key, String def) {
        try {
            return getString(key);
        } catch (Exception ex) {
            return def;
        }
    }

    public static String getString(String key, Object... formatArgs) {
        return String.format(getString(key), formatArgs);
    }

    public static String getStringFormatted(String key, Object... formatArgs) {
        return getString(key, formatArgs);
    }

    public static boolean isContainsKey(String key) {
        return resourceBundle.containsKey(key);
    }
}
