// Copyright (C) 2001-2003 Jon A. Maxwell (JAM)
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

package net.sourceforge.jnlp.runtime;

import net.sourceforge.jnlp.DefaultLaunchHandler;
import net.sourceforge.jnlp.GuiLaunchHandler;
import net.sourceforge.jnlp.JnlpRuntimeState;
import net.sourceforge.jnlp.LaunchHandler;
import net.sourceforge.jnlp.browser.BrowserAwareProxySelector;
import net.sourceforge.jnlp.cache.CacheUtil;
import net.sourceforge.jnlp.cache.DefaultDownloadIndicator;
import net.sourceforge.jnlp.cache.DownloadIndicator;
import net.sourceforge.jnlp.cache.UpdatePolicy;
import net.sourceforge.jnlp.config.DeploymentConfiguration;
import net.sourceforge.jnlp.config.PathsAndFiles;
import net.sourceforge.jnlp.security.JNLPAuthenticator;
import net.sourceforge.jnlp.security.KeyStores;
import net.sourceforge.jnlp.security.SecurityDialogMessageHandler;
import net.sourceforge.jnlp.security.SecurityUtil;
import net.sourceforge.jnlp.services.XServiceManagerStub;
import net.sourceforge.jnlp.util.BasicExceptionDialog;
import net.sourceforge.jnlp.util.DebugUtils;
import net.sourceforge.jnlp.util.FileUtils;
import net.sourceforge.jnlp.util.logging.JavaConsole;
import net.sourceforge.jnlp.util.logging.LogConfig;
import net.sourceforge.jnlp.util.logging.OutputController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.net.www.protocol.jar.URLJarFile;

import javax.jnlp.ServiceManager;
import javax.naming.ConfigurationException;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.Authenticator;
import java.net.ProxySelector;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.security.AllPermission;
import java.security.KeyStore;
import java.security.Policy;
import java.security.Security;

import static net.sourceforge.jnlp.runtime.Translator.R;

/**
 * <p>
 * Configure and access the runtime environment.  This class
 * stores global jnlp properties such as default download
 * indicators, the install/base directory, the default resource
 * update policy, etc.  Some settings, such as the base directory,
 * cannot be changed once the runtime has been initialized.
 * </p>
 * <p>
 * The JNLP runtime can be locked to prevent further changes to
 * the runtime environment except by a specified class.  If set,
 * only instances of the <i>exit class</i> can exit the JVM or
 * change the JNLP runtime settings once the runtime has been
 * initialized.
 * </p>
 *
 * @author <a href="mailto:jmaxwell@users.sourceforge.net">Jon A. Maxwell (JAM)</a> - initial author
 * @version $Revision: 1.19 $
 */
public class JNLPRuntime {

    private final static Logger LOG = LoggerFactory.getLogger(JNLPRuntime.class);


    public static JNLPSecurityManager security;
    /** the security policy */
    public static JNLPPolicy policy;
    /** handles all security message to show appropriate security dialogs */
    public static SecurityDialogMessageHandler securityDialogMessageHandler;
    /** a default launch handler */
    public static LaunchHandler handler = null;
    /** default download indicator */
    public static DownloadIndicator indicator = null;
    /** update policy that controls when to check for updates */
    public static UpdatePolicy updatePolicy = UpdatePolicy.ALWAYS;


    /**
     * Initialize the JNLP runtime environment by installing the
     * security manager and security policy, initializing the JNLP
     * standard services, etc.
     * <p>
     * This method should be called from the main AppContext/Thread.
     * </p>
     * <p>
     * This method cannot be called more than once. Once
     * initialized, methods that alter the runtime can only be
     * called by the exit class.
     * </p>
     *
     * @param isApplication is {@code true} if a webstart application is being
     * initialized
     * @throws IllegalStateException if the runtime was previously initialized
     */
    public static void initialize(boolean isApplication) throws IllegalStateException {
        JnlpRuntimeState.checkInitialized();

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            LOG.debug("Unable to set system look and feel");
        }

        if (JavaConsole.canShowOnStartup(isApplication)) {
            JavaConsole.getConsole().showConsoleLater();
        }
        /* exit if there is a fatal exception loading the configuration */
        if (getConfiguration().getLoadingException() != null) {
            if (getConfiguration().getLoadingException() instanceof ConfigurationException){
                // ConfigurationException is thrown only if deployment.config's field
                // deployment.system.config.mandatory is true, and the destination
                //where deployment.system.config points is not readable
                throw new RuntimeException(getConfiguration().getLoadingException());
            }
            LOG.debug(R("RConfigurationError")+": "+getConfiguration().getLoadingException().getMessage());
        }

        JnlpRuntimeState.isWebstartApplication = isApplication;

        //Setting the system property for javawebstart's version.
        //The version stored will be the same as java's version.
        System.setProperty("javawebstart.version", "javaws-" +
                System.getProperty("java.version"));

        if (!isHeadless() && indicator == null)
            indicator = new DefaultDownloadIndicator();

        if (handler == null) {
            if (isHeadless()) {
                handler = new DefaultLaunchHandler(OutputController.getInputOutputController());
            } else {
                handler = new GuiLaunchHandler(OutputController.getInputOutputController());
            }
        }

        ServiceManager.setServiceManagerStub(new XServiceManagerStub()); // ignored if we're running under Web Start

        policy = new JNLPPolicy();
        security = new JNLPSecurityManager(); // side effect: create JWindow

        JnlpRuntimeState.doMainAppContextHacks();

        if (JnlpRuntimeState.securityEnabled) {
            Policy.setPolicy(policy); // do first b/c our SM blocks setPolicy
            System.setSecurityManager(security);
        }

        securityDialogMessageHandler = startSecurityThreads();

        // wire in custom authenticator for SSL connections
        try {
            SSLSocketFactory sslSocketFactory;
            SSLContext context = SSLContext.getInstance("SSL");
            KeyStore ks = KeyStores.getKeyStore(KeyStores.Level.USER, KeyStores.Type.CLIENT_CERTS).getKs();
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            SecurityUtil.initKeyManagerFactory(kmf, ks);
            TrustManager[] trust = new TrustManager[] { getSSLSocketTrustManager() };
            context.init(kmf.getKeyManagers(), trust, null);
            sslSocketFactory = context.getSocketFactory();

            HttpsURLConnection.setDefaultSSLSocketFactory(sslSocketFactory);
        } catch (Exception e) {
            LOG.debug("Unable to set SSLSocketfactory (may _prevent_ access to sites that should be trusted)! Continuing anyway...");
            LOG.error("ERROR", e);
        }

        // plug in a custom authenticator and proxy selector
        Authenticator.setDefault(new JNLPAuthenticator());
        BrowserAwareProxySelector proxySelector = new BrowserAwareProxySelector(getConfiguration());
        proxySelector.initialize();
        ProxySelector.setDefault(proxySelector);

        // Restrict access to netx classes
        Security.setProperty("package.access", 
                             Security.getProperty("package.access")+",net.sourceforge.jnlp");

        URLJarFile.setCallBack(CachedJarFileCallback.getInstance());

        JnlpRuntimeState.initialized = true;

    }

    public static void reloadPolicy() {
        policy.refresh();
    }

    /**
     * Returns a TrustManager ideal for the running VM.
     *
     * @return TrustManager the trust manager to use for verifying https certificates
     */
    private static TrustManager getSSLSocketTrustManager() throws
                                ClassNotFoundException, IllegalAccessException, InstantiationException, InvocationTargetException {

        try {

            Class<?> trustManagerClass;
            Constructor<?> tmCtor;

            if (System.getProperty("java.version").startsWith("1.6")) { // Java 6
                try {
                    trustManagerClass = Class.forName("net.sourceforge.jnlp.security.VariableX509TrustManagerJDK6");
                 } catch (ClassNotFoundException cnfe) {
                     LOG.debug("Unable to find class net.sourceforge.jnlp.security.VariableX509TrustManagerJDK6");
                     return null;
                 }
            } else { // Java 7 or more (technically could be <= 1.5 but <= 1.5 is unsupported)
                try {
                    trustManagerClass = Class.forName("net.sourceforge.jnlp.security.VariableX509TrustManagerJDK7");
                 } catch (ClassNotFoundException cnfe) {
                     LOG.debug("Unable to find class net.sourceforge.jnlp.security.VariableX509TrustManagerJDK7");
                     return null;
                 }
            }

            Constructor<?>[] tmCtors = trustManagerClass.getDeclaredConstructors();
            tmCtor = tmCtors[0];

            for (Constructor<?> ctor : tmCtors) {
                if (tmCtor.getGenericParameterTypes().length == 0) {
                    tmCtor = ctor;
                    break;
                }
            }

            return (TrustManager) tmCtor.newInstance();
        } catch (RuntimeException e) {
            LOG.debug("Unable to load JDK-specific TrustManager. Was this version of IcedTea-Web compiled with JDK 6 or 7?");
            LOG.error("ERROR", e);
            throw e;
        }
    }

    /**
     * This must NOT be called form the application ThreadGroup. An application
     * can inject events into its {@link EventQueue} and bypass the security
     * dialogs.
     *
     * @return a {@link SecurityDialogMessageHandler} that can be used to post
     * security messages
     */
    private static SecurityDialogMessageHandler startSecurityThreads() {
        ThreadGroup securityThreadGroup = new ThreadGroup("NetxSecurityThreadGroup");
        SecurityDialogMessageHandler runner = new SecurityDialogMessageHandler();
        Thread securityThread = new Thread(securityThreadGroup, runner, "NetxSecurityThread");
        securityThread.setDaemon(true);
        securityThread.start();
        return runner;
    }


    /**
     * @return whether debug statements for the JNLP client code
     * should be printed.
     */
    public static boolean isDebug() {
        return DebugUtils.isSetDebug() ||  DebugUtils.isPluginDebug() || LogConfig.getLogConfig().isEnableLogging();
    }

    /**
     * see <a href="https://en.wikipedia.org/wiki/Double-checked_locking#Usage_in_Java">Double-checked locking in Java</a>
     * for cases how not to do lazy initialization
     * and <a href="https://en.wikipedia.org/wiki/Initialization_on_demand_holder_idiom">Initialization on demand holder idiom</a>
     * for ITW approach
     */
    private static class DeploymentConfigurationHolder {

        private static final DeploymentConfiguration INSTANCE = initConfiguration();

        private static DeploymentConfiguration initConfiguration() {
            DeploymentConfiguration config = new DeploymentConfiguration();
            try {
                config.load();
                config.copyTo(System.getProperties());
            } catch (ConfigurationException ex) {
                LOG.debug(R("RConfigurationError"));
                //mark this exceptionas we can die on it later
                config.setLoadingException(ex);
                //to be sure - we MUST die - http://docs.oracle.com/javase/6/docs/technotes/guides/deployment/deployment-guide/properties.html
            }catch(Exception t){
                //all exceptions are causing InstantiatizationError so this do it much more readble
                LOG.error("ERROR", t);
                LOG.debug(R("RFailingToDefault"));
                if (!JNLPRuntime.isHeadless()){
                    JOptionPane.showMessageDialog(null, R("RFailingToDefault")+"\n"+t.toString());
                }
                //try to survive this unlikely exception
                config.resetToDefaults();
            } finally {
                OutputController.getInputOutputController().startConsumer();
            }
            return config;
        }
    }

    /**
     * Gets the Configuration associated with this runtime
     *
     * @return a {@link DeploymentConfiguration} object that can be queried to
     * find relevant configuration settings
     */
    public static DeploymentConfiguration getConfiguration() {
        return DeploymentConfigurationHolder.INSTANCE;
    }

    /**
     * @return whether the JNLP client will use any AWT/Swing
     * components.
     */
    public static boolean isHeadless() {
        if (!JnlpRuntimeState.headless && !JnlpRuntimeState.headlessChecked) {
            checkHeadless();

        }
        return JnlpRuntimeState.headless;
    }


    /**
     *
     * @return the {@link SecurityDialogMessageHandler} that should be used to
     * post security dialog messages
     */
    public static SecurityDialogMessageHandler getSecurityDialogHandler() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new AllPermission());
        }
        return securityDialogMessageHandler;
    }

    /**
     * @return the current Application, or null if none can be
     * determined.
     */
    public static ApplicationInstance getApplication() {
        return security.getApplication();
    }

    /**
     * Sets whether debug statements for the JNLP client code
     * should be printed to the standard output.
     *
     * @param enabled set to true if you need full debug output
     * @throws IllegalStateException if caller is not the exit class
     */
    public static void setDebug(boolean enabled) {
        checkExitClass();
        DebugUtils.debug = enabled;
    }

  
    /**
     * Sets the default update policy.
     *
     * @param policy global update policy of environment
     * @throws IllegalStateException if caller is not the exit class
     */
    public static void setDefaultUpdatePolicy(UpdatePolicy policy) {
        checkExitClass();
        updatePolicy = policy;
    }

    /**
     * @return the default update policy.
     */
    public static UpdatePolicy getDefaultUpdatePolicy() {
        return updatePolicy;
    }

    /**
     * Returns the default launch handler.
     * @return default handler
     */
    public static LaunchHandler getDefaultLaunchHandler() {
        return handler;
    }

    /**
     * Sets the default download indicator.
     *
     * @param indicator where to show progress
     * @throws IllegalStateException if caller is not the exit class
     */
    public static void setDefaultDownloadIndicator(DownloadIndicator indicator) {
        checkExitClass();
        indicator = indicator;
    }

    /**
     * @return the default download indicator.
     */
    public static DownloadIndicator getDefaultDownloadIndicator() {
        return indicator;
    }

    /**
     * Throws an exception if called with security enabled but a caller is not
     * the exit class and the runtime has been initialized.
     */
    private static void checkExitClass() {
        if (JnlpRuntimeState.securityEnabled && JnlpRuntimeState.initialized)
            if (!security.isExitClass())
                throw new IllegalStateException("Caller is not the exit class");
    }

    /**
     * Check whether the VM is in headless mode.
     */
    private static void checkHeadless() {
        //if (GraphicsEnvironment.isHeadless()) // jdk1.4+ only
        //    headless = true;
        try {
            if ("true".equalsIgnoreCase(System.getProperty("java.awt.headless"))) {
                JnlpRuntimeState.headless = true;
            }
            if (!JnlpRuntimeState.headless) {
                boolean noCheck = Boolean.valueOf(JNLPRuntime.getConfiguration().getProperty(DeploymentConfiguration.IGNORE_HEADLESS_CHECK));
                if (noCheck) {
                    JnlpRuntimeState.headless = false;
                    LOG.debug(DeploymentConfiguration.IGNORE_HEADLESS_CHECK + " set to " + noCheck + ". Avoding headless check.");
                } else {
                    try {
                        if (GraphicsEnvironment.isHeadless()) {
                            throw new HeadlessException();
                        }
                    } catch (HeadlessException ex) {
                        JnlpRuntimeState.headless = true;
                        LOG.error("ERROR", ex);
                    }
                }
            }
        } catch (SecurityException ex) {
        } finally {
            JnlpRuntimeState.headlessChecked = true;
        }
    }

    /**
     * Indicate that netx is running by creating the
     * {@link DeploymentConfiguration#KEY_USER_NETX_RUNNING_FILE} and
     * acquiring a shared lock on it
     */
    public synchronized static void markNetxRunning() {
        if (JnlpRuntimeState.fileLock != null) return;
        try {
            String message = "This file is used to check if netx is running";

            File netxRunningFile = PathsAndFiles.MAIN_LOCK.getFile();
            if (!netxRunningFile.exists()) {
                FileUtils.createParentDir(netxRunningFile);
                FileUtils.createRestrictedFile(netxRunningFile, true);
                try (FileOutputStream fos = new FileOutputStream(netxRunningFile)) {
                    fos.write(message.getBytes());
                }
            }

            FileInputStream is = new FileInputStream(netxRunningFile);
            FileChannel channel = is.getChannel();
            JnlpRuntimeState.fileLock = channel.lock(0, 1, true);
            if (!JnlpRuntimeState.fileLock.isShared()){ // We know shared locks aren't offered on this system.
                FileLock temp = null;
                for (long pos = 1; temp == null && pos < Long.MAX_VALUE - 1; pos++){
                    temp = channel.tryLock(pos, 1, false); // No point in requesting for shared lock.
                }
                JnlpRuntimeState.fileLock.release(); // We can release now, since we hold another lock.
                JnlpRuntimeState.fileLock = temp; // Keep the new lock so we can release later.
            }
            
            if (JnlpRuntimeState.fileLock != null && JnlpRuntimeState.fileLock.isShared()) {
                LOG.debug("Acquired shared lock on " +
                            netxRunningFile.toString() + " to indicate javaws is running");
            }
        } catch (IOException e) {
            LOG.error("ERROR", e);
        }

        Runtime.getRuntime().addShutdownHook(new Thread("JNLPRuntimeShutdownHookThread") {
            @Override
            public void run() {
                JnlpRuntimeState.markNetxStopped();
                CacheUtil.cleanCache();
            }
        });
    }

    public static void exit(int i) {
        try {
            OutputController.getInputOutputController().close();
            while (BasicExceptionDialog.areShown()){
                Thread.sleep(100);
            }
        } catch (Exception ex) {
            //to late
        }
        System.exit(i);
    }


}
