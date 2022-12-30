package xyz.atsumeru.manager.utils.globalutils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class GULinks {

    public static String addPathSlashToEnd(String str) {
        if (GUString.isEmpty(str)) {
            return "";
        }
        str = str.trim();
        if (!str.endsWith("/")) {
            str = str + "/";
        }

        return str;
    }

    public static boolean isUrl(String url) {
        return GUString.isNotEmpty(url) && url.toLowerCase().startsWith("http");
    }

    /**
     * https://sub.example.com/test -> example
     */
    public static String getHostName(String link) {
        String host = getHost(link);
        if (host == null) {
            return "";
        }
        int levelTop = host.lastIndexOf(46);
        if (levelTop >= 0) {
            host = host.substring(0, levelTop);
        }
        if (host.contains(".")) {
            host = host.substring(host.lastIndexOf(".") + 1);
        }
        return host;
    }

    public static String getHost(String link) {
        final int scheme = link.indexOf("://");
        if (scheme < 0) {
            return null;
        }
        final int start = scheme + 3;
        int end = link.indexOf(47, start);
        if (end < 0) {
            end = link.length();
        }
        return link.substring(start, end);
    }

    public static String getPath(String url) {
        if (url.startsWith("/")) {
            return url;
        }
        int i1 = url.indexOf("://");
        if (i1 < 0) {
            return null;
        }
        i1 += 3;
        i1 = url.indexOf(47, i1);
        if (i1 < 0) {
            return null;
        }
        int i2 = url.lastIndexOf(63);
        final int i3 = url.lastIndexOf(35);
        if (i2 >= 0 && i3 >= 0) {
            i2 = Math.min(i2, i3);
        } else if (i3 >= 0) {
            i2 = i3;
        } else if (i2 < 0) {
            i2 = url.length();
        }
        return url.substring(i1, i2).replace("//", "/");
    }

    public static String addGetQueryParam(StringBuilder link, String name, String value) {
        return addGetQueryParam(link, name, value, true, true);
    }

    public static String addGetQueryParam(StringBuilder link, String name, String value, boolean isEncodeName, boolean isEncodeValue) {
        if (value == null)
            return link.toString();
        if (link.indexOf("?") >= 0)
            link.append("&");
        else
            link.append("?");
        return link.append(isEncodeName ? URLEncoder.encode(name, StandardCharsets.UTF_8) : name)
                .append('=')
                .append(isEncodeValue ? URLEncoder.encode(value, StandardCharsets.UTF_8) : value)
                .toString();
    }

    public static boolean isURLReachable(String testUrl, String user, String password, int timeout) {
        try {
            URL url = new URL(testUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(timeout);
            if (GUString.isNotEmpty(user) && GUString.isNotEmpty(password)) {
                connection.setRequestProperty("Authorization", "Basic " + Base64.getEncoder().encodeToString((user + ":" + password).getBytes()));
            }
            connection.connect();
            return connection.getResponseCode() == 200;
        } catch (IOException ex) {
            return false;
        }
    }
}
