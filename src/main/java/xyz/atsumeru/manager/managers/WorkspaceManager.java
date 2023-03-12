package xyz.atsumeru.manager.managers;

import org.apache.commons.io.IOUtils;
import xyz.atsumeru.manager.BuildProps;
import xyz.atsumeru.manager.FXApplication;
import xyz.atsumeru.manager.helpers.JavaHelper;
import xyz.atsumeru.manager.utils.globalutils.GUFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class WorkspaceManager {
    public static final String WORKING_DIR = getWorkingDir();

    public static final String DOWNLOADS_DIR = WORKING_DIR + "downloads" + File.separator;
    public static final String PARSERS_DIR = WORKING_DIR + "parsers" + File.separator;
    public static final String CACHE_DIR = WORKING_DIR + "cache" + File.separator;
    public static final String TMP_DIR = WORKING_DIR + "tmp" + File.separator;

    final private static String[] FOLDERS_TO_CHECK = new String[]{"parsers", "cache", "tmp"};

    public static void configureWorkspace() {
        checkWorkspace();
    }

    private static void checkWorkspace() {
        for (String checkFolder : FOLDERS_TO_CHECK) {
            if (!Files.isDirectory(Paths.get(WORKING_DIR + checkFolder))) {
                (new File(WORKING_DIR + checkFolder)).mkdirs();
            }
        }
    }

    private static String getWorkingDir() {
        File workDir = new File(System.getProperty("user.dir"));
        String workDirString = workDir.toString();
        if (workDir.isFile()) {
            workDirString = workDirString.substring(0, workDirString.lastIndexOf(File.separator));
        }
        return workDirString + File.separator;
    }

    public static void unpackLibraries() {
        if (FXApplication.isNativeImage() && BuildProps.getSystemType() == JavaHelper.SystemType.WINDOWS) {
            unpackLibrary("jvm/Windows-amd64/awt.dll");
            unpackLibrary("jvm/Windows-amd64/java.dll");
            unpackLibrary("jvm/Windows-amd64/javajpeg.dll");
            unpackLibrary("jvm/Windows-amd64/jvm.dll");
            unpackLibrary("jvm/Windows-amd64/verify.dll");
            unpackLibrary("jvm/Windows-amd64/lcms.dll");
            unpackLibrary("jvm/Windows-amd64/GRAY.pf");
        }
    }

    private static void unpackLibrary(String libraryPath) {
        File libraryFile = new File(getWorkingDir() + new File(libraryPath).getName());
        if (!GUFile.isFileExist(libraryFile)) {
            try (InputStream is = WorkspaceManager.class.getResourceAsStream(libraryPath);
                 OutputStream out = new FileOutputStream(libraryFile)) {
                IOUtils.copy(is, out);
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        }
    }
}
