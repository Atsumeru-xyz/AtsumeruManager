package xyz.atsumeru.manager.helpers;

import java.io.IOException;
import java.net.JarURLConnection;
import java.text.SimpleDateFormat;

public class JavaHelper {
    public enum SystemType {
        WINDOWS("win"),
        LINUX("unix"),
        MACOS("mac");

        public final String codename;

        SystemType(String codename) {
            this.codename = codename;
        }
    }

    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyy-MM-dd");
    private static Boolean DEBUG = null;

    public static boolean isWindows() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("win");
    }

    public static boolean isMac() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("mac");
    }

    public static boolean isUnix() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("nix") || os.contains("nux");
    }

    public static void setDebug(boolean debug) {
        DEBUG = debug;
    }

    public static boolean isDebug() {
        return DEBUG == null ? DEBUG = getAppVersion(JavaHelper.class).equals("debug") : DEBUG;
    }

    public static String getAppVersion(Class<?> cls) {
        String jarVersion = cls.getPackage().getImplementationVersion();
        String result;
        if (jarVersion != null && jarVersion.length() > 0) {
            result = jarVersion;
        } else {
            result = "debug";
        }
        try {
            String rn = cls.getName().replace('.', '/') + ".class";
            JarURLConnection j = (JarURLConnection)ClassLoader.getSystemResource(rn).openConnection();
            long time = j.getJarFile().getEntry("META-INF/MANIFEST.MF").getTime();
            return result + "-" + JavaHelper.SIMPLE_DATE_FORMAT.format(time);
        } catch (Exception e) {
            return result;
        }
    }

    public static void openURL(String url, SystemType systemType) {
        Runtime rt = Runtime.getRuntime();
        try {
            if (systemType == SystemType.WINDOWS) {
                rt.exec("rundll32 url.dll,FileProtocolHandler " + url).waitFor();
            } else if (systemType == SystemType.MACOS) {
                String[] cmd = {"open", url};
                rt.exec(cmd).waitFor();
                String[] cmd2 = {"xdg-open", url};
                rt.exec(cmd2).waitFor();
            } else if (systemType == SystemType.LINUX) {
                String[] cmd = {"xdg-open", url};
                rt.exec(cmd).waitFor();
            } else {
                try {
                    throw new IllegalStateException();
                } catch (IllegalStateException e1) {
                    e1.printStackTrace();
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void openDirectory(String dir, SystemType systemType) {
        Runtime rt = Runtime.getRuntime();
        try {
            if (systemType == SystemType.WINDOWS) {
                rt.exec("explorer " + dir).waitFor();
            } else if (systemType == SystemType.MACOS) {
                rt.exec("open -R " + dir).waitFor();
            } else if (systemType == SystemType.LINUX) {
                rt.exec("xdg-open " + dir).waitFor();
            } else {
                try {
                    throw new IllegalStateException();
                } catch (IllegalStateException e1) {
                    e1.printStackTrace();
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
