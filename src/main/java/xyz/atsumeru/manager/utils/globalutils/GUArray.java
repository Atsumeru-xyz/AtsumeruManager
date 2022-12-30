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

    public static void sleepThread(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
