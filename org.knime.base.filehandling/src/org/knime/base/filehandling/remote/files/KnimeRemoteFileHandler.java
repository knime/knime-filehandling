/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 */

package org.knime.base.filehandling.remote.files;


import java.io.File;
import java.io.IOException;
import java.net.URI;

import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformation;
import org.knime.core.util.FileUtil;
import org.knime.core.util.pathresolve.ResolverUtil;


/**
 * {@link RemoteFileHandler} for URIs with the knime:// protocol.
 *
 * @author Patrick Winter, Konstanz University, Konstanz, Germany
 * @since 3.3
 */
public class KnimeRemoteFileHandler implements RemoteFileHandler<Connection> {

    /**The {@link Protocol} of this {@link RemoteFileHandler}.*/
    public static final Protocol PROTOCOL =
            new Protocol("knime", 0, true, false, false, true, true, false, true, false);

    /**
     * {@inheritDoc}
     */
    @Override
    public Protocol[] getSupportedProtocols() {
        return new Protocol[] {PROTOCOL};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RemoteFile<Connection> createRemoteFile(final URI uri,
            final ConnectionInformation connectionInformation,
            final ConnectionMonitor<Connection> connectionMonitor) throws Exception {
        // Check if the URI points to a local or a remote file
        File localFile = ResolverUtil.resolveURItoLocalFile(uri);
        if(localFile!=null) {
            return new FileRemoteFile(localFile.toURI());
        } else if (FileUtil.isRelativeKnimeURL(uri.toURL())) {
            return new KnimeRemoteFile(uri);
        } else {
            throw new IOException("knime.workflow".equals(uri.getHost()) ? "URL accesses workflow contents: " + uri
                : "Unsupported host: '" + uri.getHost() + "'.");
        }
    }
}
