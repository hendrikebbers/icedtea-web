package net.sourceforge.jnlp;

/**
 * Represents the security level requested by an applet/application, as specified in its JNLP or HTML.
 */
public enum RequestedPermissionLevel {
    NONE(null, null),
    DEFAULT(null, "default"),
    SANDBOX(null, "sandbox"),
    J2EE("j2ee-application-client-permissions", null),
    ALL("all-permissions", "all-permissions");

    public static final String PERMISSIONS_NAME = "permissions";
    private final String jnlpString, htmlString;

    private RequestedPermissionLevel(final String jnlpString, final String htmlString) {
        this.jnlpString = jnlpString;
        this.htmlString = htmlString;
    }

    /**
     * This permission level, as it would appear requested in a JNLP file. null if this level
     * is NONE (unspecified) or cannot be requested in a JNLP file.
     * @return the String level
     */
    public String toJnlpString() {
        return this.jnlpString;
    }

    /**
     * This permission level, as it would appear requested in an HTML file. null if this level
     * is NONE (unspecified) or cannot be requested in an HTML file.
     * @return the String level
     */
    public String toHtmlString() {
        return this.htmlString;
    }

}
