package net.sourceforge.jnlp.security;

import net.sourceforge.jnlp.JNLPFile;
import net.sourceforge.jnlp.cache.Resource;
import net.sourceforge.jnlp.runtime.SecurityDelegate;
import net.sourceforge.jnlp.security.dialogresults.AccessWarningPaneComplexReturn;
import net.sourceforge.jnlp.security.dialogresults.NamePassword;
import net.sourceforge.jnlp.security.dialogresults.YesNoSandbox;
import net.sourceforge.jnlp.security.dialogresults.YesNoSandboxLimited;

import java.net.URL;
import java.util.Set;

public interface SecurityUserInteraction {

    AccessWarningPaneComplexReturn showAccessWarning(final AccessType accessType, final JNLPFile file, final Object[] extras);

    YesNoSandboxLimited showUnsignedWarning(JNLPFile file);

    YesNoSandbox showCertWarning(AccessType accessType,
                                 JNLPFile file, CertVerifier certVerifier, SecurityDelegate securityDelegate);

    YesNoSandbox showPartiallySignedWarning(JNLPFile file, CertVerifier certVerifier,
                                            SecurityDelegate securityDelegate);

    NamePassword showAuthenicationPrompt(String host, int port, String prompt, String type);

    boolean showMissingALACAttribute(JNLPFile file, URL codeBase, Set<URL> remoteUrls);

    boolean showMatchingALACAttribute(JNLPFile file, URL documentBase, Set<URL> remoteUrls);

    boolean showMissingPermissionsAttribute(JNLPFile file);

    boolean show511(Resource r);

    static SecurityUserInteraction getInstance() {
        try {
            return (SecurityUserInteraction) Class.forName("net.sourceforge.jnlp.security.SwingSecurityUserInteraction").newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Dialog UI not on Classpath!",e);
        }
    }

}
