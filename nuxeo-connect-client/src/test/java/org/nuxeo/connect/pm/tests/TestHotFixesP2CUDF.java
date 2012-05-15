/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Julien Carsique
 *
 */

package org.nuxeo.connect.pm.tests;

import org.nuxeo.connect.packages.PackageManager;

/**
 * @since 1.4
 */
public class TestHotFixesP2CUDF extends TestHotFixes {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        pm.setResolver(PackageManager.P2CUDF_DEPENDENCY_RESOLVER);
    }

}
