package xyz.atsumeru.manager.utils.globalutils;

import xyz.atsumeru.manager.helpers.LocaleManager;

import java.util.Arrays;

public class GUEnum {
    private static final String ENUM_NAME_PATTERN = "^.|.$";

    public static <E extends Enum<E>> String getEnumLocalizedString(E e) {
        return e != null ? LocaleManager.getString("enum." + e.toString().toLowerCase(), e.toString()) : null;
    }

    public static <E extends Enum<E>> String getEnumNotUnknownLocalizedString(E e) {
        return e != null && !e.toString().equalsIgnoreCase("unknown")
                ? LocaleManager.getString("enum." + e.toString().toLowerCase(), e.toString())
                : null;
    }

    /**
     * Returns {@link String[]} of all names for provided Enum class
     * @param e enum class
     * @return {@link String[]} of all enum names
     */
    public static String[] getNames(Class<? extends Enum<?>> e) {
        return Arrays.toString(e.getEnumConstants()).replaceAll(ENUM_NAME_PATTERN, "").split(", ");
    }

    /**
     * Returns enum that match provided {@link String} value
     * @param classE enum class
     * @param name {@link String} enum name
     * @param <E> type of enum class
     * @return enum that match provided {@link String} value
     */
    public static <E extends Enum<E>> E valueOf(Class<E> classE, String name) {
        return valueOf(classE, name, classE.getEnumConstants()[0]);
    }

    public static <E extends Enum<E>> E valueOf(Class<E> classE, String name, E def) {
        E[] values = classE.getEnumConstants();
        for (E value : values) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return def;
    }

    /**
     * Returns enum that match provided {@link String} value or null
     * @param classE enum class
     * @param name {@link String} enum value
     * @param <E> type of enum class
     * @return enum that match provided {@link String} value or null
     */
    public static <E extends Enum<E>> E valueOfOrNull(Class<E> classE, String name) {
        for (E value : classE.getEnumConstants()) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return null;
    }
}
