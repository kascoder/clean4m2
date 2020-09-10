package org.kascoder.clean4m2;

import java.io.File;

public class Utils {
    private Utils() {

    }

    public static void removeFileOrDir(File fileOrDir) {
        if (fileOrDir == null) {
            return;
        }

        if (fileOrDir.isDirectory()) {
            File[] children = fileOrDir.listFiles();
            if (children != null) {
                for (File child : children) {
                    removeFileOrDir(child);
                }
            }
        }

        fileOrDir.delete();
    }

    public static boolean isNotBlankString(String s) {
        return s != null && !s.isEmpty();
    }
}
