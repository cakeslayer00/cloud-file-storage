package utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PathUtils {

    public static final String USER_ROOT_DIR_PATTERN = "user-%s-files/";

    public static String getUserRootDirectoryPatternWithNoSlash(Long id) {
        return USER_ROOT_DIR_PATTERN.formatted(id).replace("/", "");
    }

    public static String getValidRootDirectoryPath(String path, Long userId) {
        return applyUserRootDirectoryPattern(getValidDirectoryPath(path), userId);
    }

    public static String getValidRootResourcePath(String path, Long userId) {
        return applyUserRootDirectoryPattern(normalizePath(path), userId);
    }

    public static String getValidDirectoryPath(String path) {
        return normalizePath(applyDirectorySuffix(path));
    }

    public static String getValidResourcePath(String path) {
        return normalizePath(path);
    }

    public static String normalizePath(String path) {
        return path.replaceAll("/{2,}", "/").replaceFirst("^/", "");
    }

    public static String applyUserRootDirectoryPattern(String path, Long id) {
        return USER_ROOT_DIR_PATTERN.formatted(id) + path;
    }

    public static String applyDirectorySuffix(String path) {
        return path.endsWith("/") ? path : path + "/";
    }

    public static String getUserRootDirectoryPattern(Long userId) {
        return String.format(USER_ROOT_DIR_PATTERN, userId);
    }

    public static String getDirectoryName(String path) {
        return getFileName(path.substring(0, path.lastIndexOf("/"))) + "/";
    }

    public static String getFileName(String file) {
        return file.substring(file.lastIndexOf("/") + 1);
    }

    public static boolean isDir(String path) {
        return path.endsWith("/");
    }

}
