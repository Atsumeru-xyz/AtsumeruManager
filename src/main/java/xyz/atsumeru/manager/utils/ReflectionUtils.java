package xyz.atsumeru.manager.utils;

import java.lang.reflect.Field;

public class ReflectionUtils {

    public static Object getAccessibleField(Field field, Object target) {
        try {
            field.setAccessible(true);
            return field.get(target);
        } catch (IllegalAccessException ignored) {
        }
        return null;
    }
}
