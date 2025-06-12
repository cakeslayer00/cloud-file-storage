package utils;

import com.vladsv.cloud_file_storage.exception.InvalidDirectoryPathException;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PathUtils {

    private static final String INVALID_DIRECTORY_PATH = "Invalid directory path or it's missing";
    private static final String INVALID_RESOURCE_PATH = "Invalid resource path or its missing";

    private static final String USER_ROOT_DIRECTORY_PREFIX = "user-%s-files/";

    public static String getValidRootDirectoryPath(String path, Long userId) {
        return applyUserRootDirectoryPrefix(getValidDirectoryPath(path), userId);
    }

    public static String getValidRootResourcePath(String path, Long userId) {
        return applyUserRootDirectoryPrefix(normalizePath(path), userId);
    }

    public static String getValidDirectoryPath(String path) {
        if (!path.matches("[A-Za-z0-9/]*")) {
            throw new InvalidDirectoryPathException(INVALID_DIRECTORY_PATH);
        }

        return normalizePath(applyDirectorySuffix(path));
    }

    public static String getValidResourcePath(String path) {
        return normalizePath(path);
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
