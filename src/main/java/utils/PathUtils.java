package utils;

import com.vladsv.cloud_file_storage.exception.AmbiguousPathException;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PathUtils {

    private static final String USER_ROOT_DIRECTORY_PREFIX = "user-%s-files/";
    private static final String AMBIGUOUS_PATH = "fuck you";

    public static String getValidPath(String path) {
        if (path.contains("//")) throw new AmbiguousPathException(AMBIGUOUS_PATH);

        return path.startsWith("/") ? path.substring(1) : path;
    }

    public static String applyUserRootDirectoryPrefix(String path, Long userId) {
        return String.format(USER_ROOT_DIRECTORY_PREFIX + path, userId);
    }

    public static String applyDirectorySuffix(String path) {
        return path.endsWith("/") ? path : path + "/";
    }

    public static String applyDirectoryPrefix(String path) {
        return path.startsWith("/") ? path : "/" + path;
    }

    public static String getUserRootDirectoryPrefix(Long userId) {
        return String.format(USER_ROOT_DIRECTORY_PREFIX, userId);
    }

    public static boolean isDirectory(String path) {
        return path.endsWith("/");
    }

}
