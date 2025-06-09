package utils;

import com.vladsv.cloud_file_storage.exception.AmbiguousPathException;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PathUtils {

    private static final String AMBIGUOUS_DIRECTORY_PATH_MESSAGE = "Invalid directory path or it's missing";
    private static final String USER_ROOT_DIRECTORY_PREFIX = "user-%s-files/";

    public static String getValidDirectoryPath(String path, Long userId) {
        if (!path.matches("[A-Za-z0-9/]*")) {
            throw new AmbiguousPathException(AMBIGUOUS_DIRECTORY_PATH_MESSAGE);
        }

        path = normalizePath(path);
        return applyDirectorySuffix(applyUserRootDirectoryPrefix(path, userId));
    }

    public static String getValidResourcePath(String path, Long userId) {
        return normalizePath(applyUserRootDirectoryPrefix(path, userId));
    }

    public static String normalizePath(String path) {
        return path.replaceAll("/{2,}", "/").replaceFirst("^/", "");
    }

    public static String applyUserRootDirectoryPrefix(String path, Long userId) {
        return String.format(USER_ROOT_DIRECTORY_PREFIX, userId) + path;
    }

    public static String applyDirectorySuffix(String path) {
        return path.endsWith("/") ? path : path + "/";
    }

    public static String getUserRootDirectoryPrefix(Long userId) {
        return String.format(USER_ROOT_DIRECTORY_PREFIX, userId);
    }

    public static boolean isDir(String path) {
        return path.endsWith("/");
    }

}
