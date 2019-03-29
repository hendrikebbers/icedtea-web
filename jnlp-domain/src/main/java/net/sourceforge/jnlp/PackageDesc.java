// Copyright (C) 2001-2003 Jon A. Maxwell (JAM)
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

package net.sourceforge.jnlp;

/**
 * The package element.
 *
 * @author <a href="mailto:jmaxwell@users.sourceforge.net">Jon A. Maxwell (JAM)</a> - initial author
 * @version $Revision: 1.6 $
 */
public class PackageDesc {

    /** the package name */
    private final String name;

    /** the part required by the package */
    private final String part;

    /** whether the package includes subpackages */
    private final boolean recursive;

    /**
     * Create a package descriptor.
     *
     * @param name the package name
     * @param part the part required by the package
     * @param recursive whether the package includes subpackages
     */
    public PackageDesc(String name, String part, boolean recursive) {
        this.name = name;
        this.part = part;
        this.recursive = recursive;
    }

    /**
     * @return whether the specified class is part of this package.
     *
     * @param className the fully qualified class name

     */
    public boolean matches(String className) {
        // form 1: exact class
        if (name.equals(className))
            return true;

        // form 2: package.*
        if (name.endsWith(".*")) {
            String pkName = name.substring(0, name.length() - 1);

            if (className.startsWith(pkName)) {
                String postfix = className.substring(pkName.length() + 1);

                if (recursive || -1 == postfix.indexOf("."))
                    return true;
            }
        }

        return false;
    }

}
