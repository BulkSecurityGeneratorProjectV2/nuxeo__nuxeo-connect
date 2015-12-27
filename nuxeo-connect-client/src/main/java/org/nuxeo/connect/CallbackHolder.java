/*
 * (C) Copyright 2010-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.connect;

import org.nuxeo.connect.update.PackageUpdateService;

public interface CallbackHolder {

    PackageUpdateService getUpdateService();

    boolean isTestModeSet();

    String getProperty(String key, String defaultValue);

    String getHomePath();

}
