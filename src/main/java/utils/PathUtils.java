package utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PathUtils {

    public static final String USER_ROOT_DIR_PATTERN = "user-%s-files/";

    public static String applyDirectorySuffix(String path) {
        return path.endsWith("/") ? path : path + "/";
    }

    public static String normalizePath(String path) {
        return path.replaceAll("/{2,}", "/").replaceFirst("^/", "");
    }

    public static String getUserRootDirectoryPattern(Long userId) {
        return USER_ROOT_DIR_PATTERN.formatted(userId);
    }

    public static String getResourceNameFromPath(String resource) {
        String trimmed = isDir(resource) ? resource.substring(0, resource.length() - 1) :  resource;
        int lastIndexOfSlash = trimmed.lastIndexOf("/");
        String name = lastIndexOfSlash >= 0 ? trimmed.substring(lastIndexOfSlash + 1) : trimmed;
        return isDir(resource) ? name + "/" : name;
    }

    public static String getPathToResource(String resource) {
        String trimmed = isDir(resource) ? resource.substring(0, resource.length() - 1) :  resource;
        int lastIndexOfSlash = trimmed.lastIndexOf("/");
        return lastIndexOfSlash > 0 ? trimmed.substring(0, lastIndexOfSlash + 1) : "/";
    }

    public static boolean isDir(String path) {
        return path.endsWith("/");
    }
}
