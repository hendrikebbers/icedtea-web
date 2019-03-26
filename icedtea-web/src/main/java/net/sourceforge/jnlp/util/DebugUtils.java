package net.sourceforge.jnlp.util;

import net.sourceforge.jnlp.util.logging.OutputController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
}
