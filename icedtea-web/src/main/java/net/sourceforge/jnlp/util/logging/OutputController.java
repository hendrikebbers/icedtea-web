/*Copyright (C) 2013 Red Hat, Inc.

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

import net.sourceforge.jnlp.runtime.JNLPRuntime;
import net.sourceforge.jnlp.util.OsUtil;
import net.sourceforge.jnlp.util.logging.headers.MessageWithHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;


/**
 * 
 * OutputController class (thread) must NOT call JNLPRuntime.getConfiguraion()
 * 
 */
public class OutputController {

    private final static Logger LOG = LoggerFactory.getLogger(OutputController.class);

    /*
     * singleton instance
     */
    private static final String NULL_OBJECT = "Trying to log null object";
    private PrintStreamLogger outLog;
    private PrintStreamLogger errLog;
    private final List<MessageWithHeader> messageQue = new LinkedList<>();
    private final MessageQueConsumer messageQueConsumer = new MessageQueConsumer();
    Thread consumerThread;
     /*stdin reader for headless dialogues*/
    private BufferedReader br;

    //bounded to instance
    private class MessageQueConsumer implements Runnable {

        @Override
        public void run() {
            while (true) {
                try {
                    synchronized (OutputController.this) {
                        OutputController.this.wait(1000);
                        if (!(OutputController.this == null || messageQue.isEmpty())) {
                            flush();
                        }
                    }

                } catch (Throwable t) {
                    LOG.error("ERROR", t);
                }
            }
        }
    };

    public synchronized void flush() {

        while (!messageQue.isEmpty()) {
            consume();
        }
    }
    
    public void close() throws Exception {
        flush();
        if (LogConfig.getLogConfig().isLogToFile()){
            getFileLog().close();
        }
    }

    private void consume() {
        MessageWithHeader s = messageQue.get(0);
        messageQue.remove(0);
        //filtering is done in console during runtime
        if (LogConfig.getLogConfig().isLogToConsole()) {
            JavaConsole.getConsole().addMessage(s);
        }
        //clients app's messages are reprinted only to console
        if (s.getHeader().isClientApp){
            if (LogConfig.getLogConfig().isLogToFile() && LogConfig.getLogConfig().isLogToFileForClientApp()) {
                getAppFileLog().log(proceedHeader(s));
            }
            return;
        }
        if (!JNLPRuntime.isDebug() && (s.getHeader().level == MessageLevel.MESSAGE_DEBUG
                || s.getHeader().level == MessageLevel.WARNING_DEBUG
                || s.getHeader().level == MessageLevel.ERROR_DEBUG)) {
            //filter out debug messages
            //must be here to prevent deadlock, casued by exception form jnlpruntime, loggers or configs themselves
            return;
        }
        String message = proceedHeader(s);
        if (LogConfig.getLogConfig().isLogToStreams()) {
            if (s.getHeader().level.isOutput()) {
                outLog.log(message);
            }
            if (s.getHeader().level.isError()) {
                errLog.log(message);
            }
        }
        if (LogConfig.getLogConfig().isLogToFile()) {
            getFileLog().log(message);
        }
        //only crucial stuff is going to system log
        //only java messages handled here, plugin is onhis own
        if (LogConfig.getLogConfig().isLogToSysLog() && 
                (s.getHeader().level.equals(MessageLevel.ERROR_ALL) || s.getHeader().level.equals(MessageLevel.WARNING_ALL)) &&
                s.getHeader().isC == false) {
            //no headers here
            getSystemLog().log(s.getMessage());
        }

    }

    private String proceedHeader(MessageWithHeader s) {
        String message = s.getMessage();
        if (LogConfig.getLogConfig().isEnableHeaders()) {
            if (message.contains("\n")) {
                message = s.getHeader().toString() + "\n" + message;
            } else {
                message = s.getHeader().toString() + " " + message;
            }
        }
        return message;
    }

    private OutputController() {
        this(System.out, System.err);
    }
    
    
    private static class OutputControllerHolder {

        //https://en.wikipedia.org/wiki/Initialization_on_demand_holder_idiom
        //https://en.wikipedia.org/wiki/Double-checked_locking#Usage_in_Java
        private static final OutputController INSTANCE = new OutputController();
    }

    /**
     * This should be the only legal way to get logger for ITW
     *
     * @return logging singleton
     */
    public static OutputController getInputOutputController() {
        return OutputControllerHolder.INSTANCE;
    }

    /**
     * for testing purposes the logger with custom streams can be created
     * otherwise only getInputOutputController()'s singleton can be called.
     */
     public OutputController(PrintStream out, PrintStream err) {
        if (out == null || err == null) {
            throw new IllegalArgumentException("No stream can be null");
        }
        outLog = new PrintStreamLogger(out);
        errLog = new PrintStreamLogger(err);
        //itw logger have to be fully initialised before start
        consumerThread = new Thread(messageQueConsumer, "Output controller consumer daemon");
        consumerThread.setDaemon(true);
        //is started in JNLPRuntime.getConfig() after config is laoded
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                flush();
            }
        }));
    }
     
    public void startConsumer() {
        consumerThread.start();
        //some messages were probably posted before start of consumer
        synchronized (this) {
            this.notifyAll();
        }
    }

    /**
     *
     * @return current stream for std.out reprint
     */
    public PrintStream getOut() {
        flush();
        return outLog.getStream();
    }

    /**
     *
     * @return current stream for std.err reprint
     */
    public PrintStream getErr() {
        flush();
        return errLog.getStream();
    }

    /**
     * Some tests may require set the output stream and check the output. This
     * is the gate for it.
     */
    public void setOut(PrintStream out) {
        flush();
        this.outLog.setStream(out);
    }

    /**
     * Some tests may require set the output stream and check the output. This
     * is the gate for it.
     */
    public void setErr(PrintStream err) {
        flush();
        this.errLog.setStream(err);
    }


    private static class FileLogHolder {
        
        //https://en.wikipedia.org/wiki/Double-checked_locking#Usage_in_Java
        //https://en.wikipedia.org/wiki/Initialization_on_demand_holder_idiom
        private static volatile SingleStreamLogger INSTANCE = FileLog.createFileLog();
    }

    private SingleStreamLogger getFileLog() {
        return FileLogHolder.INSTANCE;
    }
    
    
    private static class AppFileLogHolder {
        
        //https://en.wikipedia.org/wiki/Double-checked_locking#Usage_in_Java
        //https://en.wikipedia.org/wiki/Initialization_on_demand_holder_idiom
        private static volatile SingleStreamLogger INSTANCE = FileLog.createAppFileLog();
    }

    private SingleStreamLogger getAppFileLog() {
        return AppFileLogHolder.INSTANCE;
    }

    private static class SystemLogHolder {

        //https://en.wikipedia.org/wiki/Double-checked_locking#Usage_in_Java
        //https://en.wikipedia.org/wiki/Initialization_on_demand_holder_idiom
        private static volatile SingleStreamLogger INSTANCE = initSystemLogger();

        private static SingleStreamLogger initSystemLogger() {
            if (OsUtil.isWindows()) {
                return new WinSystemLog();
            } else {
                return new UnixSystemLog();
            }
        }
    }

    private SingleStreamLogger getSystemLog() {
        return SystemLogHolder.INSTANCE;
    }

    public void printErrorLn(String e) {
        getErr().println(e);

    }

    public void printOutLn(String e) {
        getOut().println(e);

    }

    public void printWarningLn(String e) {
        printOutLn(e);
        printErrorLn(e);
    }

    public void printError(String e) {
        getErr().print(e);

    }

    public void printOut(String e) {
        getOut().print(e);

    }

    public void printWarning(String e) {
        printOut(e);
        printError(e);
    }
    
   //package private setters for testing

    void setErrLog(PrintStreamLogger errLog) {
        this.errLog = errLog;
    }

    void setFileLog(SingleStreamLogger fileLog) {
        FileLogHolder.INSTANCE = fileLog;
    }
    
    void setAppFileLog(SingleStreamLogger fileLog) {
        AppFileLogHolder.INSTANCE = fileLog;
    }

    void setOutLog(PrintStreamLogger outLog) {
        this.outLog = outLog;
    }

    void setSysLog(SingleStreamLogger sysLog) {
        SystemLogHolder.INSTANCE = sysLog;
    }
    
    public synchronized String readLine() throws IOException {
        if (br == null) {
            br = new BufferedReader(new InputStreamReader(System.in));
        }
        return br.readLine();
    }
    
    
}
