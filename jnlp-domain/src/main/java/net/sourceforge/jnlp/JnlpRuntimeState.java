package net.sourceforge.jnlp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.text.html.parser.ParserDelegator;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.channels.FileLock;
import java.security.AllPermission;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class JnlpRuntimeState {

    private final static Logger LOG = LoggerFactory.getLogger(JnlpRuntimeState.class);


    /**
     * java-abrt-connector can print out specific application String method, it is good to save visited urls for reproduce purposes.
     * For javaws we can read the destination jnlp from commandline
     * However for plugin (url arrive via pipes). Also for plugin we can not be sure which opened tab/window
     * have caused the crash. Thats why the individual urls are added, not replaced.
     */
    public static String history = "";
    /** the security manager */

    /** whether initialized */
    public static boolean initialized = false;
    /** whether netx is in command-line mode (headless) */
    public static boolean headless = false;
    public static boolean headlessChecked = false;
    /** whether we'll be checking for jar signing */
    public static boolean verify = true;
    /** whether the runtime uses security */
    public static boolean securityEnabled = true;
    /** mutex to wait on, for initialization */
    public static Object initMutex = new Object();
    /** set to true if this is a webstart application. */
    public static boolean isWebstartApplication;
    /** set to false to indicate another JVM should not be spawned, even if necessary */
    public static boolean forksAllowed = true;
    /** all security dialogs will be consumed and pretented as being verified by user and allowed.*/
    public static boolean trustAll=false;
    /** flag keeping rest of jnlpruntime live that javaws was lunched as -html */
    public static boolean html=false;
    /** all security dialogs will be consumed and we will pretend the Sandbox option was chosen */
    public static boolean trustNone = false;
    /** allows 301.302.303.307.308 redirects to be followed when downloading resources*/
    public static boolean allowRedirect = false;
    /** when this is true, ITW will not attempt any inet connections and will work only with what is in cache*/
    public static boolean offlineForced = false;
    public static Boolean onlineDetected = null;
    /**
     * Header is not checked and so eg
     * <a href="https://en.wikipedia.org/wiki/Gifar">gifar</a> exploit is
     * possible.<br/>
     * However if jar file is a bit corrupted, then it sometimes can work so
     * this switch can disable the header check.
     * @see <a href="https://en.wikipedia.org/wiki/Gifar">Gifar attack</a>
     */
    public static boolean ignoreHeaders=false;
    /** contains the arguments passed to the jnlp runtime */
    public static List<String> initialArguments;
    /** a lock which is held to indicate that an instance of netx is running */
    public static FileLock fileLock;

    /**
     * Returns whether the JNLP runtime environment has been
     * initialized. Once initialized, some properties such as the
     * base directory cannot be changed. Before
     * @return whether this runtime was already initialilsed
     */
    public static boolean isInitialized() {
        return initialized;
    }

    public static void setOfflineForced(boolean b) {
        offlineForced = b;
        LOG.debug("Forcing of offline set to: " + offlineForced);
    }

    public static boolean isOfflineForced() {
        return offlineForced;
    }

    public static void saveHistory(String documentBase) {
        history += " " + documentBase + " ";
    }

    public static boolean isAllowRedirect() {
        return allowRedirect;
    }

    /**
     * Returns whether the secure runtime environment is enabled.
     * @return true if security manager is created
     */
    public static boolean isSecurityEnabled() {
        return securityEnabled;
    }

    public static void setHtml(boolean html) {
        JnlpRuntimeState.html = html;
    }

    public static boolean isHtml() {
        return html;
    }

    public static void setTrustAll(boolean b) {
        trustAll =b;
    }

    public static boolean isTrustAll() {
        return trustAll;
    }

    public static void setTrustNone(final boolean b) {
        trustNone = b;
    }

    public static boolean isTrustNone() {
        return trustNone;
    }

    public static boolean isIgnoreHeaders() {
        return ignoreHeaders;
    }

    public static void setIgnoreHeaders(boolean ignoreHeaders) {
        JnlpRuntimeState.ignoreHeaders = ignoreHeaders;
    }

    public static List<String> getInitialArguments() {
        return initialArguments;
    }

    public static String getLocalisedTimeStamp(Date timestamp) {
        return DateFormat.getInstance().format(timestamp);
    }

    /**
     * Performs a few hacks that are needed for the main AppContext
     *
     */
    public static void doMainAppContextHacks() {

        /*
         * With OpenJDK6 (but not with 7) a per-AppContext dtd is maintained.
         * This dtd is created by the ParserDelgate. However, the code in
         * HTMLEditorKit (used to render HTML in labels and textpanes) creates
         * the ParserDelegate only if there are no existing ParserDelegates. The
         * result is that all other AppContexts see a null dtd.
         */
        new ParserDelegator();
    }

    public static void setOnlineDetected(boolean online) {
        onlineDetected = online;
        LOG.debug("Detected online set to: " + onlineDetected);
    }

    public static boolean isConnectable(URL location) {
        if (location.getProtocol().equals("file")) {
            return true;
        }

        try {
            InetAddress.getByName(location.getHost());
        } catch (UnknownHostException e) {
            LOG.debug("The host of " + location.toExternalForm() + " file seems down, or you are simply offline.");
            return false;
        }

        return true;
    }

    public static void detectOnline(URL location) {
        if (onlineDetected != null) {
            return;
        }

        setOnlineDetected(isConnectable(location));
    }

    public static boolean isOnlineDetected() {
        if (onlineDetected == null) {
            //"file" protocol do not do online check
            //sugest online for this case
            return true;
        }
        return onlineDetected;
    }

    public static boolean isOnline() {
        if (isOfflineForced()) {
            return false;
        }
        return isOnlineDetected();
    }

    /**
     * @return true if a webstart application has been initialized, and false
     * for a plugin applet.
     */
    public static boolean isWebstartApplication() {
        return isWebstartApplication;
    }

    /**
     * @return whether we are verifying code signing.
     */
    public static boolean isVerifying() {
        return verify;
    }

    /**
     * Indicate that netx is stopped by releasing the shared lock on
     */
    public static void markNetxStopped() {
        if (fileLock == null) {
            return;
        }
        try {
            fileLock.release();
            fileLock.channel().close();
            fileLock = null;
        } catch (IOException e) {
            LOG.error("ERROR", e);
        }
    }

    /**
     * Throws an exception if called when the runtime is already initialized.
     */
    public static void checkInitialized() {
        if (initialized)
            throw new IllegalStateException("JNLPRuntime already initialized.");
    }

    /**
     * @return {@code true} if the current runtime will fork
     */
    public static boolean getForksAllowed() {
        return forksAllowed;
    }

    public static void setForksAllowed(boolean value) {
        checkInitialized();
        forksAllowed = value;
    }

    /**
     * Sets whether the JNLP client will use any AWT/Swing
     * components.  In headless mode, client features that use the
     * AWT are disabled such that the client can be used in
     * headless mode ({@code java.awt.headless=true}).
     *
     * @param enabled true if application do not wont/need gui or X at all
     * @throws IllegalStateException if the runtime was previously initialized
     */
    public static void setHeadless(boolean enabled) {
        checkInitialized();
        headless = enabled;
    }

    public static void setAllowRedirect(boolean enabled) {
        checkInitialized();
        allowRedirect = enabled;
    }

    /**
     * Sets whether we will verify code signing.
     *
     * @param enabled true if app should verify signatures
     * @throws IllegalStateException if the runtime was previously initialized
     */
    public static void setVerify(boolean enabled) {
        checkInitialized();
        verify = enabled;
    }

    /**
     * Sets whether to enable the secure runtime environment.
     * Disabling security can increase performance for some
     * applications, and can be used to use netx with other code
     * that uses its own security manager or policy.
     * <p>
     * Disabling security is not recommended and should only be
     * used if the JNLP files opened are trusted. This method can
     * only be called before initalizing the runtime.
     * </p>
     *
     * @param enabled whether security should be enabled
     * @throws IllegalStateException if the runtime is already initialized
     */
    public static void setSecurityEnabled(boolean enabled) {
        checkInitialized();
        securityEnabled = enabled;
    }

    public static void setInitialArgments(List<String> args) {
        checkInitialized();
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null)
            securityManager.checkPermission(new AllPermission());
        initialArguments = args;
    }
}
