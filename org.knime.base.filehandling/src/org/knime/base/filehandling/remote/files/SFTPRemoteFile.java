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
 *   Nov 5, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.remote.files;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Vector;

import org.knime.base.filehandling.remote.Connection;
import org.knime.base.filehandling.remote.RemoteFile;
import org.knime.base.filehandling.remote.SSHConnection;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.ChannelSftp.LsEntry;

/**
 * Implementation of the SFTP remote file.
 * 
 * 
 * @author Patrick Winter, University of Konstanz
 */
public class SFTPRemoteFile extends RemoteFile {

    private URI m_uri;

    private ChannelSftp m_channel;

    /**
     * Creates a SFTP remote file for the given URI.
     * 
     * 
     * @param uri The URI
     */
    public SFTPRemoteFile(final URI uri) {
        // Change protocol to general SSH
        try {
            m_uri = new URI(uri.toString().replaceFirst("sftp", "ssh"));
        } catch (URISyntaxException e) {
            // should not happen
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean usesConnection() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Connection createConnection() {
        // Use general SSH connection
        return new SSHConnection(m_uri);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getIdentifier() {
        return buildIdentifier(m_uri);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getDefaultPort() {
        return 22;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream openInputStream() throws Exception {
        openChannel();
        String path = m_uri.getPath();
        InputStream stream = m_channel.get(path);
        if (stream == null) {
            throw new Exception("Path not reachable");
        }
        return stream;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputStream openOutputStream() throws Exception {
        openChannel();
        String path = m_uri.getPath();
        OutputStream stream = m_channel.put(path);
        if (stream == null) {
            throw new Exception("Path not reachable");
        }
        return stream;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public long getSize() throws Exception {
        openChannel();
        String path = m_uri.getPath();
        // Get attributes for the file
        Vector<LsEntry> vector = m_channel.ls(path);
        SftpATTRS attributes = vector.get(0).getAttrs();
        return attributes.getSize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws Exception {
        if (m_channel != null) {
            m_channel.disconnect();
        }
    }

    /**
     * Opens the SFTP channel if it is not already open.
     * 
     * 
     * @throws Exception If the channel could not be opened
     */
    private void openChannel() throws Exception {
        if (m_channel == null || !m_channel.isConnected()) {
            Session session = ((SSHConnection)getConnection()).getSession();
            m_channel = (ChannelSftp)session.openChannel("sftp");
            m_channel.connect();
        }
    }

}
