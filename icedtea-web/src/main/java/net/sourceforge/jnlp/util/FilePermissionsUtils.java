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

import net.sourceforge.jnlp.config.DirectoryValidator;
import net.sourceforge.jnlp.config.DirectoryValidator.DirectoryCheckResults;
import net.sourceforge.jnlp.util.FileUtils.OpenFileResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class contains a few file-related utility functions.
 *
 * @author Omair Majid
 */

public final class FilePermissionsUtils {
    private final static Logger LOG = LoggerFactory.getLogger(FilePermissionsUtils.class);

    /**
     * Ensure that the parent directory of the file exists and that we are
     * able to create and access files within this directory
     * @param file the {@link File} representing a Java Policy file to test
     * @return a {@link DirectoryCheckResults} object representing the results of the test
     */
    public static DirectoryCheckResults testDirectoryPermissions(File file) {
        try {
            file = file.getCanonicalFile();
        } catch (final IOException e) {
            LOG.error("ERROR",e);
            return null;
        }
        if (file == null || file.getParentFile() == null || !file.getParentFile().exists()) {
            return null;
        }
        final List<File> policyDirectory = new ArrayList<>();
        policyDirectory.add(file.getParentFile());
        final DirectoryValidator validator = new DirectoryValidator(policyDirectory);
        final DirectoryCheckResults result = validator.ensureDirs();

        return result;
    }

    /**
     * Verify that a given file object points to a real, accessible plain file.
     * @param file the {@link File} to verify
     * @return an {@link OpenFileResult} representing the accessibility level of the file
     */
    public static OpenFileResult testFilePermissions(File file) {
        if (file == null || !file.exists()) {
            return OpenFileResult.FAILURE;
        }
        try {
            file = file.getCanonicalFile();
        } catch (final IOException e) {
            return OpenFileResult.FAILURE;
        }
        final DirectoryCheckResults dcr = FilePermissionsUtils.testDirectoryPermissions(file);
        if (dcr != null && dcr.getFailures() == 0) {
            if (file.isDirectory())
                return OpenFileResult.NOT_FILE;
            try {
                if (!file.exists() && !file.createNewFile()) {
                    return OpenFileResult.CANT_CREATE;
                }
            } catch (IOException e) {
                return OpenFileResult.CANT_CREATE;
            }
            final boolean read = file.canRead(), write = file.canWrite();
            if (read && write)
                return OpenFileResult.SUCCESS;
            else if (read)
                return OpenFileResult.CANT_WRITE;
            else
                return OpenFileResult.FAILURE;
        }
        return OpenFileResult.FAILURE;
    }
}
