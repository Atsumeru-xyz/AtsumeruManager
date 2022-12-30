package xyz.atsumeru.manager.helpers;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.json2.JSONArray;
import org.json2.JSONException;
import org.json2.JSONObject;
import xyz.atsumeru.manager.utils.globalutils.GUString;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class JSONHelper {

    public static JSONObject fromString(String string) throws JSONException {
        if (string == null) {
            return null;
        }
        JSONParser p = new JSONParser();
        try {
            p.parse(string);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
        return new JSONObject(string);
    }

    public static String getStringFromFile(final File f) throws IOException {
        if (!f.exists() || f.isDirectory()) {
            return null;
        }
        final StringBuilder str = new StringBuilder();
        final FileInputStream fin = new FileInputStream(f);
        final byte[] buff = new byte[1024];
        int n;
        while ((n = fin.read(buff)) > 0) {
            str.append(new String(buff, 0, n, StandardCharsets.UTF_8));
        }
        fin.close();
        return str.toString();
    }

    public static void putJSON(JSONObject obj, String name, String value) throws JSONException {
        if (!GUString.isEmpty(value)) {
            obj.put(name, value);
        }
    }

    public static String getStringSafe(final JSONObject obj, final String name, final String def) throws JSONException {
        if (obj.has(name)) {
            return obj.getString(name);
        }
        return def;
    }

    public static String getStringSafe(final JSONObject obj, final String name) throws JSONException {
        if (obj.has(name)) {
            return obj.getString(name);
        }
        return null;
    }

    public static String getStringSafe(final JSONObject obj, final String def, final String ... names) throws JSONException {
        for (String name : names) {
            if (obj.has(name)) {
                return obj.getString(name);
            }
        }
        return def;
    }

    public static int getIntSafe(final JSONObject obj, final String name, final int def) throws JSONException {
        if (obj.has(name)) {
            return obj.getInt(name);
        }
        return def;
    }

    public static float getFloatSafe(final JSONObject obj, final String name, final float def) throws JSONException {
        if (obj.has(name)) {
            return (float) obj.getDouble(name);
        }
        return def;
    }

    public static boolean getBooleanSafe(final JSONObject obj, final String name, final boolean def) throws JSONException {
        if (obj.has(name)) {
            return obj.getBoolean(name);
        }
        return def;
    }

    public static Object getSafe(final JSONObject obj, final String name) throws JSONException {
        if (obj.has(name)) {
            return obj.get(name);
        }
        return null;
    }

    public static JSONObject getObjectSafe(JSONObject obj, String... names) throws JSONException {
        JSONObject o = null;
        for (String name : names) {
            o = getObjectSafe(obj, name);

            if (o != null) {
                break;
            }
        }
        return o;
    }

    public static JSONObject getObjectSafe(final JSONObject obj, final String name) throws JSONException {
        if (obj.has(name)) {
            return obj.getJSONObject(name);
        }
        return null;
    }

    public static JSONObject getObjectSafe(final JSONObject obj, final String name, final JSONObject def) throws JSONException {
        if (obj.has(name)) {
            return obj.getJSONObject(name);
        }
        return def;
    }

    public static JSONArray getArraySafe(JSONObject obj, String... names) throws JSONException {
        JSONArray array = null;
        for (String name : names) {
            array = getArraySafe(obj, name);

            if (array != null) {
                break;
            }
        }
        return array;
    }

    public static JSONArray getArraySafe(JSONObject obj, final String name) throws JSONException {
        if (obj.has(name)) {
            return obj.getJSONArray(name);
        }
        return null;
    }

    public static long getLongSafe(final JSONObject obj, final String name, final long def) throws JSONException {
        if (obj.has(name)) {
            return obj.getLong(name);
        }
        return def;
    }

    public static Object get(final JSONObject obj, final String name) throws JSONException {
        if (obj.has(name)) {
            return obj.get(name);
        }
        return null;
    }

    public static List<String> getStringList(JSONObject obj, String name) throws JSONException {
        ArrayList<String> list = new ArrayList<>();
        if (!obj.has(name)) {
            return list;
        }
        Object o = get(obj, name);
        if (o instanceof String) {
            list.add((String)o);
            return list;
        }
        if (o instanceof JSONArray) {
            JSONArray arr = (JSONArray)o;
            for (int i = 0; i < arr.length(); ++i) {
                list.add(arr.getString(i));
            }
            return list;
        }
        return list;
    }
}
