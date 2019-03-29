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

    /**
     * The JNLP permission level corresponding to the given String. If null is given, null comes
     * back. If there is no permission level that can be granted in JNLP matching the given String,
     * null is also returned.
     * @param jnlpString the JNLP permission String
     * @return the matching RequestedPermissionLevel
     */
    public RequestedPermissionLevel fromJnlpString(final String jnlpString) {
        for (final RequestedPermissionLevel level : RequestedPermissionLevel.values()) {
            if (level.jnlpString != null && level.jnlpString.equals(jnlpString)) {
                return level;
            }
        }
        return null;
    }

    /**
     * The HTML permission level corresponding to the given String. If null is given, null comes
     * back. If there is no permission level that can be granted in HTML matching the given String,
     * null is also returned.
     * @param htmlString the JNLP permission String
     * @return the matching RequestedPermissionLevel
     */
    public RequestedPermissionLevel fromHtmlString(final String htmlString) {
        for (final RequestedPermissionLevel level : RequestedPermissionLevel.values()) {
            if (level.htmlString != null && level.htmlString.equals(htmlString)) {
                return level;
            }
        }
        return null;
    }
}
