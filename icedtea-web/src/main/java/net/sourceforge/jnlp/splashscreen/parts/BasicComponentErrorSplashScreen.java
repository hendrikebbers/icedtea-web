/* BasicComponentSplashScreen.java
Copyright (C) 2012 Red Hat, Inc.

This file is part of IcedTea.

IcedTea is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2, or (at your option)
any later version.

IcedTea is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with IcedTea; see the file COPYING.  If not, write to the
Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
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
exception statement from your version. */
package net.sourceforge.jnlp.splashscreen.parts;

import net.sourceforge.jnlp.splashscreen.SplashErrorPanel;
import net.sourceforge.swing.SwingUtils;

public abstract class BasicComponentErrorSplashScreen extends BasicComponentSplashScreen implements SplashErrorPanel {

    /**
     * When applet loading fails, then dying stacktrace can be stted, and is then shown to user on demand.
     */
    private Throwable loadingException;
   

    /**
     * @return the loadingException
     */
    public Throwable getLoadingException() {
        return loadingException;
    }

    /**
     * @param loadingException the loadingException to set
     */
    @Override
    public void setLoadingException(Throwable loadingException) {
        this.loadingException = loadingException;
    }

   

    protected void raiseExceptionDialogNOW() {
        JEditorPaneBasedExceptionDialog dialog = new JEditorPaneBasedExceptionDialog(null, true, getLoadingException(), getInformationElement(), createAditionalInfo());
        dialog.setVisible(true);
    }

    protected void raiseExceptionDialogQUEUED() {
        SwingUtils.invokeLater(new Runnable() {
            @Override
            public void run() {
                raiseExceptionDialogNOW();
            }
        });
    }

    protected void raiseExceptionDialog() {
        raiseExceptionDialogQUEUED();
    }
}
