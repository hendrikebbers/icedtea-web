/**
 * License unknown.
 * based on  Alexander Potochkin's "Debugging Swing, the final summary"
 * when oracle acquired sun, this blog post was removed, and lives only in copies.
 * most complex was found:
http://web.archive.org/web/20150523152453/https://weblogs.java.net/blog/alexfromsun/archive/2006/02/debugging_swing.html
 */
package net.sourceforge.swing;

import javax.swing.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static net.sourceforge.swing.SwingUtils.trace;

/**
 * For usage of this class, please refer to http://weblogs.java.net/blog/alexfromsun/archive/2006/02/debugging_swing.html
 * <p> To use it, call RepaintManager.setCurrentManager(new ThreadCheckingRepaintManager()) then watch the print out
 * from the console of all threading violations. </p>
 */
public final class ThreadCheckingRepaintManager extends RepaintManager {

    // it is recommended to pass the complete check
    private boolean completeCheck = true;
    private boolean checkIsShowing = false;

    /**
     * Creates ThreadCheckingRepaintManager. You can create one and set it using RepaintManager.setCurrentManager(new
     * ThreadCheckingRepaintManager()).
     */
    public ThreadCheckingRepaintManager() {
        super();
    }

    @Override
    public synchronized void addInvalidComponent(JComponent jComponent) {
        checkThreadViolations(jComponent);
        super.addInvalidComponent(jComponent);
    }

    @Override
    public synchronized void addDirtyRegion(JComponent jComponent, int i, int i1, int i2, int i3) {
        checkThreadViolations(jComponent);
        super.addDirtyRegion(jComponent, i, i1, i2, i3);
    }

    private void checkThreadViolations(JComponent c) {
        if (!SwingUtils.isEventDispatchThread() && (completeCheck || checkIsShowing(c))) {
            Exception exception = new Exception();
            boolean repaint = false;
            boolean fromSwing = false;
            StackTraceElement[] stackTrace = exception.getStackTrace();
            for (StackTraceElement st : stackTrace) {
                if (repaint && st.getClassName().startsWith("javax.swing.")) {
                    fromSwing = true;
                }
                if ("repaint".equals(st.getMethodName())) {
                    repaint = true;
                }
            }
            if (repaint && !fromSwing) {
                //no problems here, since repaint() is thread safe
                return;
            }
            trace("----------Wrong Thread START");
            trace(getStrackTraceAsString(exception));
            trace("----------Wrong Thread END");
        }
    }

    @SuppressWarnings({"SimplifiableIfStatement"})
    private boolean checkIsShowing(JComponent c) {
        if (this.checkIsShowing) {
            return c.isShowing();
        } else {
            return true;
        }
    }

    private String getStrackTraceAsString(Exception e) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(byteArrayOutputStream);
        e.printStackTrace(printStream);
        printStream.flush();
        return byteArrayOutputStream.toString();
    }
}
