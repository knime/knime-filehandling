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
package org.knime.base.filehandling.remotecopy.datasource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import org.knime.base.filehandling.remotecopy.ConnectionMonitor;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

/**
 * Data source for URIs that have the scheme "scp".
 * 
 * 
 * @author Patrick Winter, University of Konstanz
 */
public class SCPDataSource implements DataSource {

    private Session m_session;

    private ChannelExec m_channel;

    private InputStream m_streamIn;

    private OutputStream m_streamOut;

    private long m_bytesLeft;

    private long m_size;

    /**
     * Creates a data source that uses the stream from
     * <code>com.jcraft.jsch.ChannelExec</code>.
     * 
     * 
     * @param uri URI that determines the resource used
     * @param monitor Monitor for connection reuse
     * @throws Exception If the resource is not reachable
     */
    public SCPDataSource(final URI uri, final ConnectionMonitor monitor)
            throws Exception {
        String skipped;
        byte[] buffer = new byte[1];
        String path = uri.getPath();
        m_session = (Session)monitor.getConnection(uri);
        if (m_session == null || !m_session.isConnected()) {
            openConnection(uri);
            monitor.registerConnection(uri, m_session);
        }
        m_channel = (ChannelExec)m_session.openChannel("exec");
        m_channel.setCommand("scp -f " + path);
        m_streamIn = m_channel.getInputStream();
        m_streamOut = m_channel.getOutputStream();
        m_channel.connect();
        // write '0'
        buffer[0] = 0;
        m_streamOut.write(buffer);
        m_streamOut.flush();
        m_streamIn.read(buffer);
        if (buffer[0] != 0) {
            throw new IOException("SCP error");
        }
        // skip first token
        skipped = skipTo(' ');
        // look for error message
        if (skipped.contains("scp:")) {
            throw new IOException("File not found");
        }
        m_bytesLeft = 0L;
        // read filesize in single digits
        while (true) {
            int length = m_streamIn.read(buffer);
            if (length < 0) {
                break;
            }
            if (buffer[0] == ' ') {
                break;
            }
            m_bytesLeft = m_bytesLeft * 10L + (buffer[0] - '0');
        }
        m_size = m_bytesLeft;
        // skip filename
        skipTo('\n');
        // write '0'
        buffer[0] = 0;
        m_streamOut.write(buffer);
        m_streamOut.flush();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read(final byte[] buffer) throws IOException {
        int result = -1;
        if (m_bytesLeft > 0L) {
            int length;
            if (buffer.length < m_bytesLeft) {
                length = buffer.length;
            } else {
                length = (int)m_bytesLeft;
            }
            result = m_streamIn.read(buffer, 0, length);
            m_bytesLeft -= length;
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        byte[] buffer = new byte[1];
        m_streamIn.read(buffer);
        if (buffer[0] != 0) {
            throw new IOException("SCP error");
        }
        buffer[0] = 0;
        m_streamOut.write(buffer);
        m_streamOut.flush();
        m_streamIn.close();
        m_streamOut.close();
        m_channel.disconnect();
    }

    /**
     * Opens a new connection for the given URI.
     * 
     * 
     * @param uri Contains the connection information
     * @throws Exception If connection was not possible
     */
    private void openConnection(final URI uri) throws Exception {
        String host = uri.getHost();
        int port = uri.getPort() != -1 ? uri.getPort() : 22;
        String user = uri.getUserInfo();
        String password = "password";
        JSch jsch = new JSch();
        Session session = jsch.getSession(user, host, port);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();
        m_session = session;
    }

    /**
     * Consumes all bytes from the input stream up to and including the given
     * character.
     * 
     * 
     * @param character The character that will be searched for
     * @return All consumed characters (excluding the given character)
     * @throws IOException If the input stream does not contain the given
     *             character
     */
    private String skipTo(final char character) throws IOException {
        StringBuffer result = new StringBuffer();
        byte[] buffer = new byte[1];
        do {
            int length = m_streamIn.read(buffer, 0, 1);
            if (length < 0) {
                throw new IOException("Reached end of stream and found no '"
                        + character + "'");
            }
            result.append(new String(buffer));
        } while (buffer[0] != character);
        result.deleteCharAt(result.length() - 1);
        return result.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSize() {
        return m_size;
    }

}
