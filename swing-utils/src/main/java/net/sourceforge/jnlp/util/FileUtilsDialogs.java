// Copyright (C) 2009 Red Hat, Inc.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

package net.sourceforge.jnlp.util;

import net.sourceforge.jnlp.util.FileUtils.OpenFileResult;
import net.sourceforge.swing.SwingUtils;

import javax.swing.*;
import java.awt.*;

import static net.sourceforge.jnlp.runtime.Translator.R;


/**
 * This class contains a few file-related utility functions.
 *
 * @author Omair Majid
 */

public final class FileUtilsDialogs {

    /**
     * Show a dialog informing the user that the file is currently read-only
     * @param frame a {@link JFrame} to act as parent to this dialog
     */
    public static void showReadOnlyDialog(final Component frame) {
        SwingUtils.invokeLater(new Runnable() {
            @Override
            public void run() {
                JOptionPane.showMessageDialog(frame, R("RFileReadOnly"), R("Warning"), JOptionPane.WARNING_MESSAGE);
            }
        });
    }

    /**
     * Show a generic error dialog indicating the  file could not be opened
     * @param frame a {@link JFrame} to act as parent to this dialog
     * @param filePath a {@link String} representing the path to the file we failed to open
     */
    public static void showCouldNotOpenFilepathDialog(final Component frame, final String filePath) {
        showCouldNotOpenDialog(frame, R("RCantOpenFile", filePath));
    }

    /**
     * Show an error dialog indicating the file could not be opened, with a particular reason
     * @param frame a {@link JFrame} to act as parent to this dialog
     * @param filePath a {@link String} representing the path to the file we failed to open
     * @param reason a {@link OpenFileResult} specifying more precisely why we failed to open the file
     */
    public static void showCouldNotOpenFileDialog(final Component frame, final String filePath, final OpenFileResult reason) {
        final String message;
        switch (reason) {
            case CANT_CREATE:
                message = R("RCantCreateFile", filePath);
                break;
            case CANT_WRITE:
                message = R("RCantWriteFile", filePath);
                break;
            case NOT_FILE:
                message = R("RExpectedFile", filePath);
                break;
            default:
                message = R("RCantOpenFile", filePath);
                break;
        }
        showCouldNotOpenDialog(frame, message);
    }

    /**
     * Show a dialog informing the user that the file could not be opened
     * @param frame a {@link JFrame} to act as parent to this dialog
     * @param message a {@link String} giving the specific reason the file could not be opened
     */
    public static void showCouldNotOpenDialog(final Component frame, final String message) {
        SwingUtils.invokeLater(new Runnable() {
            @Override
            public void run() {
                JOptionPane.showMessageDialog(frame, message, R("Error"), JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
