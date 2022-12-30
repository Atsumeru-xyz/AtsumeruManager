package xyz.atsumeru.manager.utils.globalutils;

import net.greypanther.natsort.CaseInsensitiveSimpleNaturalComparator;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class GUFile {
    final private static String ILLEGAL_CHARACTERS = "/\n\r\t\0\f`?*\\<>|\"”:.# ";
    public static int MAX_FILE_LENGTH = 20 + (Integer.MIN_VALUE + "").length();
    private static final int BUFFER_SIZE = 1024;

    public static List<File> getAllFilesFromDirectory(String path, String[] allowedExtensions, boolean recursive) {
        File file = new File(path);
        if (file.isDirectory()) {
            List<File> files = (List<File>) org.apache.commons.io.FileUtils.listFiles(file, allowedExtensions, recursive);
            files.sort((file1, file2) -> CaseInsensitiveSimpleNaturalComparator.getInstance().compare(file1.getPath(), file2.getPath()));
            return files;
        }

        return new ArrayList<>();
    }

    public static void copyFile(File src, File dest) {
        try {
            org.apache.commons.io.FileUtils.copyFile(src, dest);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Add path slash {@link File#separator} into end of path if needed
     * @param str input path
     * @return path with slash in end
     */
    public static String addPathSlash(String str) {
        if (GUString.isNotEmpty(str) && !str.endsWith("/") && !str.endsWith(File.separator)) {
            return str + File.separator;
        }
        return str;
    }

    /**
     * Closes {@link Closeable} quietly
     * @param closeable {@link Closeable} for close
     */
    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) {
                // ignore
            }
        }
    }

    /**
     * Check is {@link File} exists
     * @param file {@link File} for checking
     * @return true if exists
     */
    public static boolean isFileExist(File file) {
        return file != null && file.exists();
    }

    /**
     * Check is {@link File} is Directory
     * @param file {@link File} for checking
     * @return true if file is Directory
     */
    public static boolean isDirectory(File file) {
        return isFileExist(file) && file.isDirectory();
    }

    public static boolean isFile(File file) {
        return isFileExist(file) && file.isFile();
    }

    /**
     * Copies content of {@link InputStream} into {@link OutputStream}
     * @param in {@link InputStream} from which need copy content
     * @param out {@link OutputStream} into which need write content
     * @return length of copied content
     * @throws IOException
     */
    public static long copy(InputStream in, OutputStream out) throws IOException {
        return copy(in, out, BUFFER_SIZE);
    }

    /**
     * Copies content of {@link InputStream} into {@link OutputStream}
     * @param in {@link InputStream} from which need copy content
     * @param out {@link OutputStream} into which need write content
     * @param bufferLength {@link Integer} buffer size
     * @return length of copied content
     * @throws IOException
     */
    public static long copy(InputStream in, OutputStream out, int bufferLength) throws IOException {
        byte[] buffer = new byte[bufferLength];
        long readed = 0L;
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
            readed = readed + (long) read;
        }
        out.flush();
        return readed;
    }

    public static String createValidFileName(String s, boolean isSmartTrim) {
        int i = 0;
        String converted = GUString.decodeUrl(s.replace("%", ""));
        while (i < ILLEGAL_CHARACTERS.length()) {
            if (converted.indexOf(ILLEGAL_CHARACTERS.charAt(i)) >= 0) {
                converted = converted.replace(ILLEGAL_CHARACTERS.charAt(i), '_');
            }
            i = i + 1;
        }
        return isSmartTrim ? smartTrim(converted) : converted;
    }

    public static String smartTrim(String str) {
        if (GUString.isEmpty(str)) {
            return str;
        }
        return (str.length() > MAX_FILE_LENGTH
                ? str.substring(0, MAX_FILE_LENGTH - 1) + str.hashCode()
                : str);
    }

    /**
     * Returns directory name from provided {@link String} path
     * @param path provided {@link String} path
     * @return directory name. Example: /some_dir/my-dir -> my-dir
     */
    public static String getDirName(String path) {
        int indexSeparator = path.lastIndexOf(File.separator);
        int indexSlash = path.lastIndexOf("/");

        if (indexSeparator < 0 && indexSlash < 0) {
            return null;
        }

        return path.substring(0, Math.max(indexSeparator, indexSlash));
    }

    public static String getPath(String path) {
        int indexSlash = path.lastIndexOf("\\");
        int indexBackSlash = path.lastIndexOf("/");

        if (indexSlash >= 0) {
            return path.substring(0, indexSlash);
        } else if (indexBackSlash >= 0) {
            return path.substring(0, indexBackSlash);
        }

        return "";
    }

    /**
     * Returns {@link String} name without extension from provided {@link String} path
     * @param path provided {@link String} path
     * @return {@link String} name without extension. Example: /some_dir/some_file.jpg -> some_file
     */
    public static String getFileName(String path) {
        return getFileName(path, false);
    }

    /**
     * Returns {@link String} name without extension from provided {@link String} path
     * @param path provided {@link String} path
     * @param isLocalFile indicates that path if from FS
     * @return {@link String} name without extension. Example: /some_dir/some_file.jpg -> some_file
     */
    public static String getFileName(String path, boolean isLocalFile) {
        int indexSlash = path.lastIndexOf(!isLocalFile ? "/" : File.separator);
        int indexDot = path.lastIndexOf(".");

        if (indexSlash >= 0 && indexDot > indexSlash) {
            return path.substring(indexSlash + 1, indexDot);
        }
        if (indexSlash >= 0) {
            return path.substring(indexSlash + 1);
        }
        if (indexDot >= 0) {
            return path.substring(0, indexDot);
        }

        return path;
    }

    /**
     * Returns {@link String} name with extension from provided {@link String} path
     * @param path provided {@link String} path
     * @return {@link String} name without extension. Example: /some_dir/some_file.jpg -> some_file
     */
    public static String getFileNameWithExt(String path) {
        return getFileNameWithExt(path, false);
    }

    /**
     * Returns {@link String} name with extension from provided {@link String} path
     * @param path provided {@link String} path
     * @param isLocalFile indicates that path if from FS
     * @return {@link String} name with extension. Example: /some_dir/some_file.jpg -> some_file.jpg
     */
    public static String getFileNameWithExt(String path, boolean isLocalFile) {
        int indexSlash = path.lastIndexOf(!isLocalFile ? "/" : File.separator);
        return indexSlash >= 0 ? path.substring(indexSlash + 1) : path;
    }

    /**
     * Returns {@link File} extension from {@link String} path
     * @param path provided {@link String} path
     * @return {@link File} extension. Example: logo.jpg -> jpg
     */
    public static String getFileExt(String path) {
        if (path == null) {
            return "";
        }

        int indexSlash = path.lastIndexOf("/");
        int indexDot = path.lastIndexOf(".");

        return indexSlash >= 0 && indexDot >= 0 && indexSlash < indexDot || indexSlash < 0 && indexDot >= 0
                ? path.substring(indexDot + 1)
                : "";
    }

    /**
     * Returns {@link File} extension from {@link String} url
     * @param url provided {@link String} url
     * @return {@link File} extension. Example: example.com/logo.jpg -> jpg
     */
    public static String getFileExtFromUrl(String url) {
        url = GULinks.getPath(url);
        if (url == null) {
            return null;
        }
        int specialStart = url.lastIndexOf("#");
        int queryStart = url.lastIndexOf("?");

        if (specialStart >= 0 && queryStart >= 0) {
            queryStart = Math.min(specialStart, queryStart);
        } else if (queryStart < 0) {
            queryStart = (specialStart >= 0) ? specialStart : url.length();
        }

        int index = url.lastIndexOf(".", queryStart);
        if (index <= 0) {
            return null;
        }
        return url.substring(index + 1, queryStart);
    }

    public static boolean writeStringToFile(File file, String content) {
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(file);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            printWriter.print(content);
            printWriter.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeQuietly(fileWriter);
        }
    }
}