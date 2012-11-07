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
 *   Nov 2, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.remote.files;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

/**
 * Implementation of the FTP remote file.
 * 
 * 
 * @author Patrick Winter, University of Konstanz
 */
public class FTPRemoteFile extends RemoteFile {

    private URI m_uri;

    /**
     * Creates a FTP remote file for the given URI.
     * 
     * 
     * @param uri The URI
     */
    FTPRemoteFile(final URI uri) {
        m_uri = uri;
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
        return new FTPConnection();
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
        return 21;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exists() throws Exception {
        return getFTPFile() != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(final RemoteFile file) throws Exception {
        byte[] buffer = new byte[1024];
        InputStream in = file.openInputStream();
        OutputStream out = openOutputStream();
        int length;
        while (((length = in.read(buffer)) > 0)) {
            out.write(buffer, 0, length);
        }
        in.close();
        out.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream openInputStream() throws Exception {
        FTPClient client = getClient();
        String path = m_uri.getPath();
        // Open stream (null if stream could not be opened)
        InputStream stream = client.retrieveFileStream(path);
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
        FTPClient client = getClient();
        String path = m_uri.getPath();
        // Open stream (null if stream could not be opened)
        OutputStream stream = client.storeFileStream(path);
        if (stream == null) {
            throw new Exception("Path not reachable");
        }
        return stream;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSize() throws Exception {
        long size = 0;
        FTPFile ftpFile = getFTPFile();
        if (ftpFile != null && ftpFile.getSize() > 0) {
            size = ftpFile.getSize();
        }
        return size;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long lastModified() throws Exception {
        // Assume missing
        long time = 0;
        FTPFile ftpFile = getFTPFile();
        if (ftpFile != null) {
            time = ftpFile.getTimestamp().getTimeInMillis();
        }
        return time;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean delete() throws Exception {
        FTPClient client = getClient();
        String path = m_uri.getPath();
        return client.deleteFile(path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws Exception {
        FTPClient client = getClient();
        // Complete all operations
        boolean success = client.completePendingCommand();
        if (!success) {
            throw new IOException("Could not finalize the operation");
        }
    }

    /**
     * Convenience method to get the FTP Client from the connection.
     * 
     * 
     * @return The FTP Client
     */
    private FTPClient getClient() {
        return ((FTPConnection)getConnection()).getClient();
    }

    /**
     * Returns the FTPFile to this file.
     * 
     * 
     * @return FTPFile to this file or null if not existing
     * @throws Exception If the operation could not be executed
     */
    private FTPFile getFTPFile() throws Exception {
        FTPFile file = null;
        FTPClient client = getClient();
        String path = m_uri.getPath();
        FTPFile[] files = client.listFiles(path);
        for (int i = 0; i < files.length; i++) {
            FTPFile currentFile = files[i];
            if (currentFile.getName().equals(path)) {
                file = currentFile;
            }
        }
        return file;
    }

    /**
     * Connection over FTP.
     * 
     * 
     * @author Patrick Winter, University of Konstanz
     */
    private class FTPConnection extends Connection {

        private FTPClient m_client;

        /**
         * {@inheritDoc}
         */
        @Override
        public void open() throws Exception {
            // Create client
            m_client = new FTPClient();
            // Read attributes
            String host = m_uri.getHost();
            int port =
                    m_uri.getPort() != -1 ? m_uri.getPort() : getDefaultPort();
            String user = m_uri.getUserInfo();
            String password = "password";
            // Open connection
            m_client.connect(host, port);
            // Login
            boolean loggedIn = m_client.login(user, password);
            if (!loggedIn) {
                throw new IOException("Login failed");
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isOpen() {
            boolean open = false;
            if (m_client != null && m_client.isConnected()) {
                open = true;
            }
            return open;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void close() throws Exception {
            m_client.logout();
            m_client.disconnect();
        }

        /**
         * Return the client of this connection.
         * 
         * 
         * @return The FTP Client
         */
        public FTPClient getClient() {
            return m_client;
        }

    }

}
