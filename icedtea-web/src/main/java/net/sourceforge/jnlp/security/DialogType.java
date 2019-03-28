package net.sourceforge.jnlp.security;

public enum DialogType {
    CERT_WARNING,
    MORE_INFO,
    CERT_INFO,
    SINGLE_CERT_INFO,
    ACCESS_WARNING,
    PARTIALLYSIGNED_WARNING,
    UNSIGNED_WARNING, /* requires confirmation with 'high-security' setting */
    APPLET_WARNING,
    AUTHENTICATION,
    UNSIGNED_EAS_NO_PERMISSIONS_WARNING, /* when Extended applet security is at High Security and no permission attribute is find, */
    MISSING_ALACA, /*alaca - Application-Library-Allowable-Codebase Attribute*/
    MATCHING_ALACA,
    SECURITY_511;
}
