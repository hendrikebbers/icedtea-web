package net.sourceforge.jnlp.runtime;

import net.sourceforge.jnlp.JARDesc;
import net.sourceforge.jnlp.LaunchException;
import net.sourceforge.jnlp.SecurityDesc;

import java.net.URL;
import java.security.Permission;
import java.util.Collection;

/**
 * SecurityDelegate, in real usage, relies on having a "parent"
 * JNLPClassLoader instance. However, JNLPClassLoaders are very large,
 * heavyweight, difficult-to-mock objects, which means that unit testing on
 * anything that uses a SecurityDelegate can become very difficult. For
 * example, JarCertVerifier is designed separated from the ClassLoader so it
 * can be tested in isolation. However, JCV needs some sort of access back
 * to JNLPClassLoader instances to be able to invoke setRunInSandbox(). The
 * SecurityDelegate handles this, allowing JCV to be tested without
 * instantiating JNLPClassLoaders, by creating a fake SecurityDelegate that
 * does not require one.
 */
public interface SecurityDelegate {

    public boolean userPromptedForSandbox();

    public SecurityDesc getCodebaseSecurityDesc(final JARDesc jarDesc, final URL codebaseHost);

    public SecurityDesc getClassLoaderSecurity(final URL codebaseHost) throws LaunchException;

    public SecurityDesc getJarPermissions(final URL codebaseHost);

    public void promptUserOnPartialSigning() throws LaunchException;

    public void setRunInSandbox() throws LaunchException;

    public boolean getRunInSandbox();

    public void addPermissions(final Collection<Permission> perms);
}
