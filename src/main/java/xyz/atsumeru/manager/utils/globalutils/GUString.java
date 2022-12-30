package xyz.atsumeru.manager.utils.globalutils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;

public class GUString {

    public static String getMHash2(String hashTag, String link) {
        String hash = getUriHash2(hashTag, link,false);
        if (hashTag != null && hash != null) {
            return hashTag + hash;
        }
        return hash;
    }

    public static String getUriHash2(String hashTag, String link, boolean hashWithParams) {
        String path = hashWithParams ? getPathWithParams(link) : getPath(link);
        if (path == null) {
            return GUString.md5Hex(link);
        }
        path = path.replace("//", "/");
        if (hashTag == null) {
            return GUString.md5Hex(path);
        }
        return GUString.md5Hex(hashTag + path);
    }

    public static String getPathWithParams(String link) {
        int h = link.indexOf("#");
        int end;
        if (link.endsWith("/")) {
            link = link.substring(0, link.lastIndexOf("/"));
        }
        if (h >= 0) {
            end = h;
        } else {
            end = link.length();
        }
        int scheme = link.contains("scheme")
                ? link.indexOf("//")
                : link.indexOf("://");
        int start = 0;
        if (scheme >= 0) {
            start = link.lastIndexOf("/") + 1;
        }
        if (start < 0) {
            return null;
        }
        return link.substring(start, end);
    }

    public static String getPath(String link) {
        int q = link.indexOf("?");
        int h = link.indexOf("#");
        int end;
        if (q < 0 && h < 0) {
            end = link.length();
        } else if (q >= 0 && h >= 0) {
            end = Math.min(q, h);
        } else if (q >= 0) {
            end = q;
        } else {
            end = h;
        }
        int scheme = link.contains("scheme") ? link.indexOf("//") : link.indexOf("://");
        int start = 0;
        if (scheme >= 0) {
            start = link.indexOf(47, scheme + 3);
        }
        if (start < 0) {
            return null;
        }
        return link.substring(start, end);
    }

    public static boolean containsIgnoreCase(String firstValue, String secondValue) {
        return isNotEmpty(firstValue) && isNotEmpty(secondValue) && firstValue.toLowerCase().contains(secondValue.toLowerCase());
    }

    public static String join(String delimiter, Iterable<?> tokens) {
        if (GUArray.isEmpty(tokens)) {
            return "";
        }

        final Iterator<?> it = tokens.iterator();
        if (!it.hasNext()) {
            return "";
        }
        final StringBuilder sb = new StringBuilder();
        sb.append(it.next());
        while (it.hasNext()) {
            sb.append(delimiter);
            sb.append(it.next());
        }
        return sb.toString();
    }

    public static boolean equals(String s1, String s2) {
        if (s1 == null)
            s1 = "";
        return s1.equals(s2);
    }

    public static boolean equalsIgnoreCase(String s1, String s2) {
        if (s1 == null) {
            s1 = "";
        }
        return s1.equalsIgnoreCase(s2);
    }

    public static boolean startsWithIgnoreCase(String first, String second) {
        return isNotEmpty(first) && isNotEmpty(second) && first.toLowerCase().startsWith(second.toLowerCase());
    }

    public static boolean endsWithIgnoreCase(String first, String second) {
        return isNotEmpty(first) && isNotEmpty(second) && first.toLowerCase().endsWith(second.toLowerCase());
    }

    /**
     * Checks is string null/empty
     * @param str - input string
     * @return boolean. true if null or empty, false if none of this
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().length() == 0;
    }

    /**
     * Checks is string not null/not empty
     * @param str - input string
     * @return boolean. true if not null and not empty, false if null or empty
     */
    public static boolean isNotEmpty(String str) {
        return str != null && str.trim().length() > 0;
    }

    /**
     * Checks is {@link CharSequence} not null
     * @param charSequence - input {@link CharSequence}
     * @return boolean. true if not null, otherwise - false
     */
    public static boolean isNotNull(CharSequence charSequence) {
        return charSequence != null;
    }

    /**
     * Calculates MD5 for input string
     * @param str - input string
     * @return - MD5 hash string
     */
    public static String md5Hex(String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(str.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                final String hex = Integer.toHexString(0xFF & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NullPointerException | NoSuchAlgorithmException ignored) {
        }
        return "";
    }

    @Deprecated
    public static String getWrongStringToMD5(String s) {
        try {
            MessageDigest a = MessageDigest.getInstance("MD5");
            a.update(s.getBytes());
            byte[] a0 = a.digest();
            StringBuilder a1 = new StringBuilder();
            int i = 0;
            while (i < a0.length) {
                int i0 = a0[i];
                a1.append(Integer.toHexString(i0 & 255));
                i = i + 1;
            }
            return a1.toString();
        } catch (NoSuchAlgorithmException a2) {
            a2.printStackTrace();
            return "";
        }
    }

    /**
     * Decodes a {@code application/x-www-form-urlencoded} string using a specific
     * encoding scheme.
     * @param url the {@code String} to decode
     * @return the newly decoded {@code String}
     */
    public static String decodeUrl(String url) {
        try {
            return URLDecoder.decode(url, StandardCharsets.UTF_8);
        } catch (NullPointerException ex2) {
            return url;
        }
    }

    /**
     * Removes all html tags from input string leaving only readable text
     * @param htmlStr - input html string
     * @return string without html tags
     */
    public static String fromHtmlToString(String htmlStr) {
        if (isEmpty(htmlStr)) {
            return "";
        }
        htmlStr = htmlStr.replace("<br />", "\n");
        StringBuilder str = new StringBuilder();
        int indexStart = 0;
        int indexEnd = htmlStr.indexOf("<", indexStart);
        if (indexEnd >= 0) {
            str.append(htmlStr, indexStart, indexEnd);
            while (indexStart >= 0 && indexEnd >= 0) {
                indexStart = htmlStr.indexOf(">", indexEnd);
                indexEnd = htmlStr.indexOf("<", indexStart);
                boolean flBreak = false;
                if (indexEnd < 0) {
                    indexEnd = htmlStr.length();
                    flBreak = true;
                }
                str.append(htmlStr, indexStart + 1, indexEnd);
                if (flBreak) {
                    break;
                }
            }
        } else {
            str.append(htmlStr);
        }
        return str.toString().trim();
    }

    public static String capitalizeFully(String str, char[] delimiters) {
        int delimLen = (delimiters == null ? -1 : delimiters.length);
        if (str == null || str.length() == 0 || delimLen == 0) {
            return str;
        }
        str = str.toLowerCase();
        return capitalize(str, delimiters, false, true);
    }

    public static String capitalize(String str, boolean isLowercaseBeforeCapitalize, boolean capitalizeAllBetweenDelimiters) {
        return capitalize(str, null, isLowercaseBeforeCapitalize, capitalizeAllBetweenDelimiters);
    }

    public static String capitalize(String str, char[] delimiters, boolean isLowercaseBeforeCapitalize, boolean capitalizeAllBetweenDelimiters) {
        if (isLowercaseBeforeCapitalize) {
            str = str.toLowerCase();
        }

        int delimLen = (delimiters == null ? -1 : delimiters.length);
        if (str == null || str.length() == 0 || delimLen == 0) {
            return str;
        }
        int strLen = str.length();
        StringBuffer buffer = new StringBuffer(strLen);
        boolean capitalizeNext = true;
        for (int i = 0; i < strLen; i++) {
            char ch = str.charAt(i);

            if (isDelimiter(ch, delimiters)) {
                buffer.append(ch);
                capitalizeNext = capitalizeAllBetweenDelimiters;
            } else if (capitalizeNext) {
                buffer.append(Character.toTitleCase(ch));
                capitalizeNext = false;
            } else {
                buffer.append(ch);
            }
        }
        return buffer.toString();
    }

    private static boolean isDelimiter(char ch, char[] delimiters) {
        if (delimiters == null) {
            return Character.isWhitespace(ch);
        }
        for (char delimiter : delimiters) {
            if (ch == delimiter) {
                return true;
            }
        }
        return false;
    }

    public static String getFirstNotEmptyValue(String... values) {
        for (String value : values) {
            if (GUString.isNotEmpty(value)) {
                return value;
            }
        }
        return null;
    }

}
