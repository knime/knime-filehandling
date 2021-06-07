/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   2020-11-18 (Bjoern Lohrmann): created
 */
package org.knime.ext.http.filehandling.fs;

import java.net.URI;
import java.nio.file.Path;

import org.knime.filehandling.core.connections.base.UnixStylePath;

/**
 * {@link Path} implementation for the HTTP(S) file system.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
final class HttpPath extends UnixStylePath {

    /**
     * @param fileSystem
     *            The file system.
     * @param first
     *            The first name component.
     * @param more
     *            More name components. the string representation of the path.
     */
    HttpPath(final HttpFileSystem fileSystem, final String first, final String[] more) {
        super(fileSystem, first, more);
    }

    @Override
    public HttpFileSystem getFileSystem() {
        return (HttpFileSystem) super.getFileSystem();
    }

    /**
     * @return the URL for the HTTP request to make, when trying to access this file
     *         over HTTP.
     */
    public String getRequestUrl() {
        @SuppressWarnings("resource")
        final URI baseUrl = URI.create(getFileSystem().getBaseUrl());
        final StringBuilder url = new StringBuilder(
                String.format("%s://%s", baseUrl.getScheme(), baseUrl.getAuthority()));
        if (url.charAt(url.length() - 1) == '/') {
            url.deleteCharAt(url.length() - 1);
        }
        // note: we are not doing any percent-encoding here, hence the user has to
        // provide correctly percent-encoded paths. This allows the user to embed query
        // and fragment components into the request URL by appending them to the path.
        // Percent encoding the user-provided path would also encode the reserved
        // characters in the query and fragment, thus breaking that feature.
        url.append(toAbsolutePath().toString());
        return url.toString();
    }
}
