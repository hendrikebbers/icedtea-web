/* LogConfig.java
 Copyright (C) 2011, 2013 Red Hat, Inc.

 This file is part of IcedTea.

 IcedTea is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License as published by
 the Free Software Foundation, version 2.

 IcedTea is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with IcedTea; see the file COPYING.  If not, write to
 the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 02110-1301 USA.

 Linking this library statically or dynamically with other modules is
 making a combined work based on this library.  Thus, the terms and
 conditions of the GNU General Public License cover the whole
 combination.

 As a special exception, the copyright holders of this library give you
 permission to link this library with independent modules to produce an
 executable, regardless of the license terms of these independent
 modules, and to copy and distribute the resulting executable under
 terms of your choice, provided that you also meet, for each linked
 independent module, the terms and conditions of the license of that
 module.  An independent module is a module which is not derived from
 or based on this library.  If you modify this library, you may extend
 this exception to your version of the library, but you are not
 obligated to do so.  If you do not wish to do so, delete this
 exception statement from your version.
 */
package net.sourceforge.jnlp.util.logging;

import net.sourceforge.jnlp.config.DeploymentConfiguration;
import net.sourceforge.jnlp.config.PathsAndFiles;
import net.sourceforge.jnlp.runtime.JNLPRuntime;

import java.io.File;

/**
 * This file provides the information required to do logging.
 *
 */
public class LogConfig {

    // Directory where the logs are stored.
    private String icedteaLogDir;
    private boolean enableLogging;
    private boolean enableHeaders;
    private boolean logToFile;
    private boolean logClientAppToFile;
    private boolean logToStreams;
    private boolean logToSysLog;
    private boolean legacyLogaAsedFileLog;

    private LogConfig() {
        DeploymentConfiguration config = JNLPRuntime.getConfiguration();
        // Check whether logging and tracing is enabled.
        enableLogging = Boolean.parseBoolean(config.getProperty(DeploymentConfiguration.KEY_ENABLE_LOGGING));
        //enagle disable headers
        enableHeaders = Boolean.parseBoolean(config.getProperty(DeploymentConfiguration.KEY_ENABLE_LOGGING_HEADERS));
        //enable/disable individual channels
        logToFile = Boolean.parseBoolean(config.getProperty(DeploymentConfiguration.KEY_ENABLE_LOGGING_TOFILE));
        logToStreams = Boolean.parseBoolean(config.getProperty(DeploymentConfiguration.KEY_ENABLE_LOGGING_TOSTREAMS));
        logToSysLog = Boolean.parseBoolean(config.getProperty(DeploymentConfiguration.KEY_ENABLE_LOGGING_TOSYSTEMLOG));
        legacyLogaAsedFileLog = Boolean.parseBoolean(config.getProperty(DeploymentConfiguration.KEY_ENABLE_LEGACY_LOGBASEDFILELOG));
        logClientAppToFile = Boolean.parseBoolean(config.getProperty(DeploymentConfiguration.KEY_ENABLE_APPLICATION_LOGGING_TOFILE));

        // Get log directory, create it if it doesn't exist. If unable to create and doesn't exist, don't log.
        icedteaLogDir = PathsAndFiles.LOG_DIR.getFullPath();
        if (icedteaLogDir != null) {
            File f = new File(icedteaLogDir);
            if (f.isDirectory() || f.mkdirs()) {
                icedteaLogDir += File.separator;
            } else {
                enableLogging = false;
            }
        } else {
            enableLogging = false;
        }
    }

    private static class LogConfigHolder {

        //https://en.wikipedia.org/wiki/Double-checked_locking#Usage_in_Java
        //https://en.wikipedia.org/wiki/Initialization_on_demand_holder_idiom
        private static volatile LogConfig INSTANCE = new LogConfig();
    }

    public static LogConfig getLogConfig() {
        return LogConfigHolder.INSTANCE;
    }

    public String getIcedteaLogDir() {
        return icedteaLogDir;
    }

    public boolean isEnableLogging() {
        return enableLogging;
    }

    public boolean isLogToFile() {
        return logToFile;
    }

    public boolean isLogToStreams() {
        return logToStreams;
    }

    public boolean isLogToSysLog() {
        return logToSysLog;
    }

    public boolean isEnableHeaders() {
        return enableHeaders;
    }

    boolean isLogToConsole() {
        return JavaConsole.isEnabled();
    }

    boolean isLegacyLogBasedFileLog() {
        return legacyLogaAsedFileLog;
    }

    boolean isLogToFileForClientApp() {
        return logClientAppToFile;
    }

}
