package net.sourceforge.jnlp.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;

public class DebugUtils {

    private final static Logger LOG = LoggerFactory.getLogger(DebugUtils.class);


    /** whether debug mode is on */
    public static boolean debug = false;
    /**
     * whether plugin debug mode is on
     */
    public static Boolean pluginDebug = null;

    public static boolean isSetDebug() {
       return debug;
   }

    public static boolean isPluginDebug() {
        if (pluginDebug == null) {
            try {
                //there are cases when this itself is not allowed by security manager, and so
                //throws exception. Under some conditions it can couse deadlock
                pluginDebug = System.getenv().containsKey("ICEDTEAPLUGIN_DEBUG");
            } catch (Exception ex) {
                pluginDebug = false;
                LOG.error("ERROR", ex);
            }
        }
        return pluginDebug;
    }

    public static String exceptionToString(Throwable t) {
        if (t == null) {
            return null;
        }
        String s = "Error during processing of exception";
        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            s = sw.toString();
            pw.close();
            sw.close();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return s;
    }
}
