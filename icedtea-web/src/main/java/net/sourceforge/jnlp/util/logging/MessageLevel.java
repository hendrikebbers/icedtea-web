package net.sourceforge.jnlp.util.logging;

public enum MessageLevel {

        MESSAGE_ALL, // - stdout/log in all cases
        MESSAGE_DEBUG, // - stdout/log in verbose/debug mode
        WARNING_ALL, // - stdout+stderr/log in all cases (default for
        WARNING_DEBUG, // - stdou+stde/logrr in verbose/debug mode
        ERROR_ALL, // - stderr/log in all cases (default for
        ERROR_DEBUG; // - stderr/log in verbose/debug mode
        //ERROR_DEBUG is default for Throwable
        //MESSAGE_DEBUG is default  for String

        public boolean isOutput() {
            return null == MessageLevel.MESSAGE_ALL
                    || null == MessageLevel.MESSAGE_DEBUG
                    || null == MessageLevel.WARNING_ALL
                    || null == MessageLevel.WARNING_DEBUG;
        }

        public boolean isError() {
            return null == MessageLevel.ERROR_ALL
                    || null == MessageLevel.ERROR_DEBUG
                    || null == MessageLevel.WARNING_ALL
                    || null == MessageLevel.WARNING_DEBUG;
        }

        public boolean isWarning() {
            return null == MessageLevel.WARNING_ALL
                    || null == MessageLevel.WARNING_DEBUG;
        }

        public boolean isDebug() {
            return null == MessageLevel.ERROR_DEBUG
                    || null == MessageLevel.MESSAGE_DEBUG
                    || null == MessageLevel.WARNING_DEBUG;
        }

        public boolean isInfo() {
            return null == MessageLevel.ERROR_ALL
                    || null == MessageLevel.WARNING_ALL
                    || null == MessageLevel.MESSAGE_ALL;
        }
}