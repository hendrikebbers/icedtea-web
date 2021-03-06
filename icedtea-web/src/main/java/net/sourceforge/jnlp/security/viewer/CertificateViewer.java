/* CertificateViewer.java
   Copyright (C) 2008 Red Hat, Inc.

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

package net.sourceforge.jnlp.security.viewer;

import net.sourceforge.jnlp.runtime.JNLPRuntime;
import net.sourceforge.jnlp.util.ImageResources;
import net.sourceforge.jnlp.util.ScreenFinder;
import net.sourceforge.swing.SwingUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import static net.sourceforge.jnlp.runtime.Translator.R;

public class CertificateViewer extends JFrame {

    private boolean initialized = false;
    private static final String dialogTitle = R("CVCertificateViewer");

    CertificatePane panel;

    public CertificateViewer() {
        super(dialogTitle);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setName("CertificateViewer");
        SwingUtils.info(this);
        setIconImages(ImageResources.INSTANCE.getApplicationImages());

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        panel = new CertificatePane(this);

        add(panel);

        pack();

        WindowAdapter adapter = new WindowAdapter() {
            private boolean gotFocus = false;

            public void windowGainedFocus(WindowEvent we) {
                // Once window gets focus, set initial focus
                if (!gotFocus) {
                    panel.focusOnDefaultButton();
                    gotFocus = true;
                }
            }
        };
        addWindowFocusListener(adapter);

        initialized = true;
    }

    public boolean isInitialized() {
        return initialized;
    }

    private void centerDialog() {
        ScreenFinder.centerWindowsToCurrentScreen(this);
    }

    private static void showCertificateViewer() {
        JNLPRuntime.initialize(true);

        CertificateViewer cv = new CertificateViewer();
        cv.setResizable(true);
        cv.centerDialog();
        cv.setVisible(true);
    }

    public static void main(String[] args) throws Exception {
        SwingUtils.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                CertificateViewer.showCertificateViewer();
            }
        });
    }
}
