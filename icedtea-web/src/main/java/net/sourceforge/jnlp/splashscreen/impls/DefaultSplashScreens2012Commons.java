/* DefaultSplashScreensCommons2012.java
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
package net.sourceforge.jnlp.splashscreen.impls;

import net.sourceforge.jnlp.about.AboutDialog;
import net.sourceforge.jnlp.splashscreen.impls.defaultsplashscreen2012.BasePainter;
import net.sourceforge.jnlp.splashscreen.parts.BasicComponentSplashScreen;
import net.sourceforge.jnlp.util.docprovider.TextsProvider;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public final class DefaultSplashScreens2012Commons {

    private final BasicComponentSplashScreen parent;
    private final BasePainter painter;
   

    public DefaultSplashScreens2012Commons(BasePainter painterr, BasicComponentSplashScreen parentt) {
        this.painter = painterr;
        this.parent = parentt;
        parent.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                painter.increaseAnimationPosition();
                parent.repaint();
            }
        });
        parent.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getY() < painter.getAboutOfset().y && e.getX() > (painter.getAboutOfset().x)) {
                    AboutDialog.display(TextsProvider.ITW_PLUGIN);
                }
            }
        });
        // Add a new listener for resizes
        parent.addComponentListener(new ComponentAdapter() {
            // Re-adjust variables based on size

            @Override
            public void componentResized(ComponentEvent e) {
                parent.setSplashWidth(parent.getWidth());
                parent.setSplashHeight(parent.getHeight());
                parent.adjustForSize();
                parent.repaint();
            }
        });
    }

    public void paintTo(Graphics g) {
        painter.paint(g);


    }

    public void adjustForSize() {
        painter.adjustForSize(parent.getSplashWidth(), parent.getSplashHeight());
    }

    public void stopAnimation() {
        parent.setAnimationRunning(false);
    }

    /**
     * Methods to start the animation in the splash panel.
     *
     * This method exits after starting a new thread to do the animation. It
     * is synchronized to prevent multiple startAnimation threads from being created.
     */
    public synchronized  void startAnimation() {
        if (parent.isAnimationRunning()) {
            return;
        }
        parent.setAnimationRunning(true);
        painter.startAnimationThreads();

    }

    public void setPercentage(int done) {
        painter.clearCachedWaterTextImage();
        painter.setWaterLevel(done);
    }

    public int getPercentage() {
        return painter.getWaterLevel();
    }

   
}
