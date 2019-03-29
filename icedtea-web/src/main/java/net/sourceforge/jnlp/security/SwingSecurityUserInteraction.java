package net.sourceforge.jnlp.security;

import net.sourceforge.jnlp.JNLPFile;
import net.sourceforge.jnlp.cache.Resource;
import net.sourceforge.jnlp.runtime.JNLPRuntime;
import net.sourceforge.jnlp.runtime.SecurityDelegate;
import net.sourceforge.jnlp.security.dialogresults.AccessWarningPaneComplexReturn;
import net.sourceforge.jnlp.security.dialogresults.DialogResult;
import net.sourceforge.jnlp.security.dialogresults.NamePassword;
import net.sourceforge.jnlp.security.dialogresults.YesCancel;
import net.sourceforge.jnlp.security.dialogresults.YesNoSandbox;
import net.sourceforge.jnlp.security.dialogresults.YesNoSandboxLimited;
import net.sourceforge.jnlp.util.UrlUtils;
import net.sourceforge.swing.SwingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.NetPermission;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Set;
import java.util.concurrent.Semaphore;

public class SwingSecurityUserInteraction implements SecurityUserInteraction {

    private final static Logger LOG = LoggerFactory.getLogger(SwingSecurityUserInteraction.class);

    /**
     * Shows a warning dialog for different types of system access (i.e. file
     * open/save, clipboard read/write, printing, etc).
     *
     * @param accessType the type of system access requested.
     * @param file the jnlp file associated with the requesting application.
     * @param extras array of objects used as extra.toString or similarly later
     * @return true if permission was granted by the user, false otherwise.
     */
    public AccessWarningPaneComplexReturn showAccessWarning(final AccessType accessType,
                                                                         final JNLPFile file, final Object[] extras) {

        final SecurityDialogMessage message = new SecurityDialogMessage(file);

        message.dialogType = DialogType.ACCESS_WARNING;
        message.accessType = accessType;
        message.extras = extras;

        return (AccessWarningPaneComplexReturn) getUserResponse(message);

    }

    /**
     * Shows a warning dialog for when a plugin applet is unsigned. This is used
     * with 'high-security' setting.
     *
     * @param file the file to be base as information source for this dialogue
     * @return true if permission was granted by the user, false otherwise.
     */
    public YesNoSandboxLimited showUnsignedWarning(JNLPFile file) {

        final SecurityDialogMessage message = new SecurityDialogMessage(file);
        message.dialogType = DialogType.UNSIGNED_WARNING;
        message.accessType = AccessType.UNSIGNED;

        DialogResult r = getUserResponse(message);

        return (YesNoSandboxLimited) r;
    }

    /**
     * Shows a security warning dialog according to the specified type of
     * access. If {@code accessType} is one of {@link AccessType#VERIFIED} or
     * {@link AccessType#UNVERIFIED}, extra details will be available with
     * regards to code signing and signing certificates.
     *
     * @param accessType the type of warning dialog to show
     * @param file the JNLPFile associated with this warning
     * @param certVerifier the JarCertVerifier used to verify this application
     * @param securityDelegate the delegate for security atts.
     *
     * @return RUN if the user accepted the certificate, SANDBOX if the user
     * wants the applet to run with only sandbox permissions, or CANCEL if the
     * user did not accept running the applet
     */
    public YesNoSandbox showCertWarning(AccessType accessType,
                                                     JNLPFile file, CertVerifier certVerifier, SecurityDelegate securityDelegate) {

        final SecurityDialogMessage message = new SecurityDialogMessage(file);
        message.dialogType = DialogType.CERT_WARNING;
        message.accessType = accessType;
        message.certVerifier = certVerifier;
        message.extras = new Object[]{securityDelegate};

        DialogResult selectedValue = getUserResponse(message);

        return (YesNoSandbox) selectedValue;
    }

    /**
     * Shows a warning dialog for when an applet or application is partially
     * signed.
     *
     * @param file the JNLPFile associated with this warning
     * @param certVerifier the JarCertVerifier used to verify this application
     * @param securityDelegate the delegate for security atts.
     * @return true if permission was granted by the user, false otherwise.
     */
    public YesNoSandbox showPartiallySignedWarning(JNLPFile file, CertVerifier certVerifier,
                                                                SecurityDelegate securityDelegate) {

        final SecurityDialogMessage message = new SecurityDialogMessage(file);
        message.dialogType = DialogType.PARTIALLYSIGNED_WARNING;
        message.accessType = AccessType.PARTIALLYSIGNED;
        message.certVerifier = certVerifier;
        message.extras = new Object[]{securityDelegate};

        DialogResult r = getUserResponse(message);
        return (YesNoSandbox) r;
    }

    /**
     * Present a dialog to the user asking them for authentication information,
     * and returns the user's response. The caller must have
     * NetPermission("requestPasswordAuthentication") for this to work.
     *
     * @param host The host for with authentication is needed
     * @param port The port being accessed
     * @param prompt The prompt (realm) as presented by the server
     * @param type The type of server (proxy/web)
     * @return an array of objects representing user's authentication tokens
     * @throws SecurityException if the caller does not have the appropriate
     * permissions.
     */
    public NamePassword showAuthenicationPrompt(String host, int port, String prompt, String type) {

        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            NetPermission requestPermission
                    = new NetPermission("requestPasswordAuthentication");
            sm.checkPermission(requestPermission);
        }

        final SecurityDialogMessage message = new SecurityDialogMessage(null);

        message.dialogType = DialogType.AUTHENTICATION;
        message.extras = new Object[]{host, port, prompt, type};

        DialogResult response = getUserResponse(message);
        LOG.debug("Decided action for matching alaca at  was " + response);
        return (NamePassword) response;
    }

    public boolean showMissingALACAttribute(JNLPFile file, URL codeBase, Set<URL> remoteUrls) {

        SecurityDialogMessage message = new SecurityDialogMessage(file);
        message.dialogType = DialogType.MISSING_ALACA;
        String urlToShow = file.getNotNullProbalbeCodeBase().toExternalForm();
        if (codeBase != null) {
            urlToShow = codeBase.toString();
        } else {
            LOG.debug("Warning, null codebase wants to show in ALACA!");
        }
        message.extras = new Object[]{urlToShow, UrlUtils.setOfUrlsToHtmlList(remoteUrls)};
        DialogResult selectedValue = getUserResponse(message);

        LOG.debug("Decided action for matching alaca at " + file.getCodeBase() + " was " + selectedValue);

        if (selectedValue == null) {
            return false;
        }
        return selectedValue.toBoolean();
    }

    public boolean showMatchingALACAttribute(JNLPFile file, URL documentBase, Set<URL> remoteUrls) {

        SecurityDialogMessage message = new SecurityDialogMessage(file);
        message.dialogType = DialogType.MATCHING_ALACA;
        String docBaseString = "null-documentbase";
        if (documentBase != null) {
            docBaseString = documentBase.toString();
        }
        message.extras = new Object[]{docBaseString, UrlUtils.setOfUrlsToHtmlList(remoteUrls)};
        DialogResult selectedValue = getUserResponse(message);

        LOG.debug("Decided action for matching alaca at " + file.getCodeBase() + " was " + selectedValue);

        if (selectedValue != null) {
            return selectedValue.toBoolean();
        }

        return false;

    }

    public boolean showMissingPermissionsAttribute(JNLPFile file) {

        SecurityDialogMessage message = new SecurityDialogMessage(file);
        message.dialogType = DialogType.UNSIGNED_EAS_NO_PERMISSIONS_WARNING;
        DialogResult selectedValue = getUserResponse(message);
        LOG.debug("Decided action for missing permissions at " + file.getCodeBase() + " was " + selectedValue);

        if (selectedValue != null) {
            return selectedValue.toBoolean();
        }

        return false;
    }

    /**
     * Posts the message to the SecurityThread and gets the response. Blocks
     * until a response has been recieved. It's safe to call this from an
     * EventDispatchThread.
     *
     * @param message the SecuritDialogMessage indicating what type of dialog to
     * display
     * @return The user's response. Can be null. The exact answer depends on the
     * type of message, but generally an Integer corresponding to the value 0
     * indicates success/proceed, and everything else indicates failure
     */
    private DialogResult getUserResponse(final SecurityDialogMessage message) {
        /*
         * Want to show a security warning, while blocking the client
         * application. This would be easy except there is a bug in showing
         * modal JDialogs in a different AppContext. The source EventQueue -
         * that sends the message to the (destination) EventQueue which is
         * supposed to actually show the dialog - must not block. If the source
         * EventQueue blocks, the destination EventQueue stops responding. So we
         * have a hack here to work around it.
         */

        /*
         * If this is the event dispatch thread the use the hack
         */
        if (SwingUtils.isEventDispatchThread()) {
            /*
             * Create a tiny modal dialog (which creates a new EventQueue for
             * this AppContext, but blocks the original client EventQueue) and
             * then post the message - this makes the source EventQueue continue
             * running - but dot not allow the actual applet/application to
             * continue processing
             */
            final JDialog fakeDialog = new JDialog();
            fakeDialog.setName("FakeDialog");
            SwingUtils.info(fakeDialog);
            fakeDialog.setSize(0, 0);
            fakeDialog.setResizable(false);
            fakeDialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
            fakeDialog.addWindowListener(new WindowAdapter() {

                @Override
                public void windowOpened(WindowEvent e) {
                    message.toDispose = fakeDialog;
                    message.lock = null;
                    AccessController.doPrivileged(new PrivilegedAction<Void>() {
                        @Override
                        public Void run() {
                            JNLPRuntime.getSecurityDialogHandler().postMessage(message);
                            return null;
                        }
                    });
                }
            });

            /* this dialog will be disposed/hidden when the user closes the security prompt */
            fakeDialog.setVisible(true);
        } else {
            /*
             * Otherwise do it the normal way. Post a message to the security
             * thread to make it show the security dialog. Wait until it tells us
             * to proceed.
             */
            message.toDispose = null;
            message.lock = new Semaphore(0);
            JNLPRuntime.getSecurityDialogHandler().postMessage(message);

            boolean done = false;
            while (!done) {
                try {
                    message.lock.acquire();
                    done = true;
                } catch (InterruptedException e) {
                    // ignore; retry
                }
            }

        }
        return message.userResponse;
    }

    // false = termiante ITW
    // true = continue
    public boolean show511(Resource r) {
        SecurityDialogMessage message = new SecurityDialogMessage(null);
        message.dialogType = DialogType.SECURITY_511;
        message.extras = new Object[]{r.getLocation()};
        DialogResult selectedValue = getUserResponse(message);
        if (selectedValue != null && selectedValue.equals(YesCancel.cancel())) {
            return false; //kill command
        }
        return true;
    }
}