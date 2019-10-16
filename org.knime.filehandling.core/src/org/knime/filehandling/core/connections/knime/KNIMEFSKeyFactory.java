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
 *   Oct 10, 2019 (Tobias Urhaug, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.core.connections.knime;

import java.io.IOException;
import java.net.URI;
import java.net.URL;

import org.knime.filehandling.core.defaultnodesettings.KNIMEConnection;
import org.knime.filehandling.core.util.MountPointIDProviderService;

/**
 * Factory for creating {@link KNIMEFileSystem} URI keys, i.e. "knime://knime.workflow/absolute/path/to/workflow".
 *
 * The keys consist of a schema, host and path, where the path is decided in runtime:
 *
 * Schema
 *  - the KNIME URL schema 'knime'
 * Host
 *  - the host tells if the key points to a workflow relative, node relative, mount point relative, or mount point
 *  absolute {@link KNIMEFileSystem}
 * Path
 *  - the path to the base location of the {@link KNIMEFileSystem}. If fetching the key for a workflow relative file
 *  system, the path will be resolved to the current workflow.
 *
 *  The base location is resolved to be the current workflow, node or mount point location.
 *
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
public class KNIMEFSKeyFactory {

    /**
     * Dynamically fetches a file system key (URI) for the given
     * {@link org.knime.filehandling.core.defaultnodesettings.KNIMEConnection.Type}.
     *
     * I.e. when the connection type is workflow relative, the workflow location will be resolved and the key be:
     *
     * "knime://knime.workflow/path/to/the/current/workflow"
     *
     * @param knimeConnectionType the type of the file system
     * @return the key (URI) of the file system with the relative paths resolved
     * @throws IOException
     */
    public static URI keyOf(final KNIMEConnection.Type knimeConnectionType) throws IOException {
        String schemeAndHost = knimeConnectionType.getSchemeAndHost();
        URL baseLocation = MountPointIDProviderService.instance().resolveKNIMEURL(URI.create(schemeAndHost).toURL());
        return URI.create(schemeAndHost + baseLocation.getPath());
    }

}
