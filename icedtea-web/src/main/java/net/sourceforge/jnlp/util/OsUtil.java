package net.sourceforge.jnlp.util;

public class OsUtil {
    /**
     * @return {@code true} if running on a Unix or Unix-like system (including
     * Linux and *BSD)
     */
    @Deprecated
    public static boolean isUnix() {
        String sep = System.getProperty("file.separator");
        return (sep != null && sep.equals("/"));
    }

    /**
     * @return {@code true} if running on Windows
     */
    public static boolean isWindows() {
        String os = System.getProperty("os.name");
        return (os != null && os.startsWith("Windows"));
    }
}
