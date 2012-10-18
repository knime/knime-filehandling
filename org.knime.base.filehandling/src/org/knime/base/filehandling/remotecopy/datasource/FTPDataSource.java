/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 * 
 * History
 *   Oct 18, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.remotecopy.datasource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.apache.commons.net.ftp.FTPClient;

/**
 * Data source for URIs that have the scheme "ftp".
 * 
 * 
 * @author Patrick Winter, University of Konstanz
 */
public class FTPDataSource implements DataSource {

    private FTPClient m_client;

    private InputStream m_stream;

    /**
     * Creates a data source that uses the stream from
     * <code>org.apache.commons.net.ftp.FTPClient</code>.
     * 
     * 
     * @param uri URI that determines the resource used
     * @throws Exception If the resource is not reachable
     */
    public FTPDataSource(final URI uri) throws Exception {
        // Create client
        m_client = new FTPClient();
        // Read attributes
        String host = uri.getHost();
        int port = uri.getPort() != -1 ? uri.getPort() : 21;
        String path = uri.getPath().replaceFirst("/", "");
        String user = uri.getUserInfo();
        String password = "password";
        // Open connection
        m_client.connect(host, port);
        // Login
        boolean loggedIn = m_client.login(user, password);
        if (!loggedIn) {
            throw new IOException("Login failed");
        }
        // Open stream (null if stream could not be opened)
        m_stream = m_client.retrieveFileStream(path);
        if (m_stream == null) {
            throw new Exception("Path not reachable");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read(final byte[] buffer) throws IOException {
        return m_stream.read(buffer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        m_stream.close();
        // Complete all operations
        boolean success = m_client.completePendingCommand();
        m_client.logout();
        m_client.disconnect();
        if (!success) {
            throw new IOException("Could not finalize the operation");
        }
    }

}
