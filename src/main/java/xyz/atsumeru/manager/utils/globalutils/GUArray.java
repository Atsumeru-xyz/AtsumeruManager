package xyz.atsumeru.manager.utils.globalutils;

import java.util.*;

public class GUArray {

    public static List<String> splitString(String str) {
        return splitString(str, ",");
    }

    public static List<String> splitString(String str, String regex) {
        try {
            return new ArrayList<>(Arrays.asList(str.split(regex)));
        } catch (NullPointerException ex) {
            return new ArrayList<>();
        }
    }

    public static String safeGetString(List<String> list, int index, String def) {
        try {
            return list.get(index);
        } catch (Exception ex) {
            return def;
        }
    }

    /** Check is collection empty
     * @param collectionMapArray Collection, Map or Array
     * @return boolean - true if empty / false if not
     */
    public static boolean isEmpty(Object collectionMapArray, Integer... lengthArr) {
        int length = lengthArr != null && lengthArr.length > 0 ? lengthArr[0] : 1;
        if (collectionMapArray == null) {
            return true;
        } else if (collectionMapArray instanceof Collection) {
            return ((Collection) collectionMapArray).size() < length;
        } else if (collectionMapArray instanceof Map) {
            return ((Map) collectionMapArray).size() < length;
        } else if (collectionMapArray instanceof Object[]) {
            return ((Object[]) collectionMapArray).length < length || ((Object[]) collectionMapArray)[length - 1] == null;
        } else return true;
    }

    /** Acts like {@link #isEmpty(Object, Integer...) isEmpty} method but reverse
     * @param collectionMapArray Collection, Map or Array
     * @return boolean - false if empty / true if not
     */
    public static boolean isNotEmpty(Object collectionMapArray) {
        return !isEmpty(collectionMapArray);
    }

    public static boolean mergeArraysWithoutDuplicates(List<String> dest, List<String> values) {
        return mergeArraysWithoutDuplicates(values, dest, Integer.MAX_VALUE, Integer.MAX_VALUE, false);
    }

    public static boolean mergeArraysWithoutDuplicates(List<String> values, List<String> dest, int maxValueLength, int maxArraySize, boolean lowercase) {
        if (GUArray.isEmpty(values)) {
            return false;
        }
        boolean merged = false;
        for (String value : values) {
            if (lowercase) {
                value = value.toLowerCase();
            }
            if (addStringIntoArrayIfAbsent(value, dest, maxValueLength, maxArraySize, false)) {
                merged = true;
            }
        }
        return merged;
    }

    public static boolean addStringIntoArrayIfAbsent(String value, List<String> dest, int maxValueLength, int maxArraySize,  boolean lowercase) {
        if (GUString.isEmpty(value) || value.length() > maxValueLength || dest.size() > maxArraySize || GUArray.containsIgnoreCase(dest, value)) {
            return false;
        }
        if (lowercase) {
            value = value.toLowerCase();
        }
        dest.add(value);
        return true;
    }

    public static boolean containsIgnoreCase(Collection<String> collection, String value) {
        for (String valueInCollection : collection) {
            if (valueInCollection.equalsIgnoreCase(value)) {
                return true;
            }
        }

        return false;
    }
}
