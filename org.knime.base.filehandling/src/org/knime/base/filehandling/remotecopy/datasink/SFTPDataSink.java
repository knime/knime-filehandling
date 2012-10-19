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
 *   Oct 19, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.remotecopy.datasink;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

import org.knime.base.filehandling.remotecopy.connections.ConnectionMonitor;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

/**
 * Data sink for URIs that have the scheme "sftp".
 * 
 * @author Patrick Winter, University of Konstanz
 */
public class SFTPDataSink implements DataSink {

    private ChannelSftp m_channel;

    private OutputStream m_stream;

    /**
     * Creates a data source that uses the stream from
     * <code>com.jcraft.jsch.ChannelSftp</code>.
     * 
     * 
     * @param uri URI that determines the resource used
     * @param monitor Monitor for connection reuse
     * @throws Exception If the resource is not reachable
     */
    public SFTPDataSink(final URI uri, final ConnectionMonitor monitor)
            throws Exception {
        m_channel = (ChannelSftp)monitor.getConnection(uri);
        if (m_channel == null || !m_channel.isConnected()) {
            openConnection(uri);
            monitor.registerConnection(uri, m_channel);
        }
        String path = uri.getPath();
        m_stream = m_channel.put(path);
        if (m_stream == null) {
            throw new Exception("Path not reachable");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(final byte[] buffer, final int length) throws IOException {
        m_stream.write(buffer, 0, length);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        m_stream.close();
    }

    /**
     * Opens a new connection for the given URI.
     * 
     * 
     * @param uri Contains the connection information
     * @throws Exception If connection was not possible
     */
    private void openConnection(final URI uri) throws Exception {
        // Read attributes
        String host = uri.getHost();
        int port = uri.getPort() != -1 ? uri.getPort() : 22;
        String user = uri.getUserInfo();
        String password = "password";
        JSch jsch = new JSch();
        Session session = jsch.getSession(user, host, port);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();
        m_channel = (ChannelSftp)session.openChannel("sftp");
        m_channel.connect();
    }

}
