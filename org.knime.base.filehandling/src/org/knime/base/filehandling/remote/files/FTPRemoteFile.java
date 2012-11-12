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
import org.knime.core.util.KnimeEncryption;

/**
 * Implementation of the FTP remote file.
 * 
 * 
 * @author Patrick Winter, University of Konstanz
 */
public class FTPRemoteFile extends RemoteFile {

    private URI m_uri;

    private ConnectionCredentials m_credentials;

    /**
     * Creates a FTP remote file for the given URI.
     * 
     * 
     * @param uri The URI
     * @param credentials Credentials to the given URI
     */
    FTPRemoteFile(final URI uri, final ConnectionCredentials credentials) {
        m_uri = uri;
        m_credentials = credentials;
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
    public String getType() {
        return "ftp";
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
    public boolean isDirectory() throws Exception {
        boolean isDirectory = false;
        FTPFile ftpFile = getFTPFile();
        if (ftpFile != null) {
            isDirectory = ftpFile.isDirectory();
        }
        return isDirectory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean move(final RemoteFile file) throws Exception {
        boolean success;
        if (file instanceof FTPRemoteFile
                && getIdentifier().equals(file.getIdentifier())) {
            FTPRemoteFile source = (FTPRemoteFile)file;
            FTPClient client = getClient();
            success = client.rename(source.m_uri.getPath(), m_uri.getPath());
        } else {
            write(file);
            success = file.delete();
        }
        return success;
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
        return deleteRecursively(m_uri.getPath());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RemoteFile[] listFiles() throws Exception {
        RemoteFile[] files;
        if (isDirectory()) {
            FTPClient client = getClient();
            FTPFile[] ftpFiles = client.listFiles(m_uri.getPath());
            files = new RemoteFile[ftpFiles.length];
            for (int i = 0; i < ftpFiles.length; i++) {
                URI uri =
                        new URI(m_uri.getScheme() + "://"
                                + m_uri.getAuthority() + ftpFiles[i].getName());
                files[i] = new FTPRemoteFile(uri, m_credentials);
            }
        } else {
            files = new RemoteFile[0];
        }
        return files;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean mkDir() throws Exception {
        FTPClient client = getClient();
        return client.makeDirectory(m_uri.getPath());
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
                break;
            }
        }
        return file;
    }

    /**
     * Deletes files and directories recursively.
     * 
     * 
     * @param path Path to the file or directory
     * @return true if deletion was successful, false otherwise
     */
    private boolean deleteRecursively(final String path) throws Exception {
        boolean deleted = false;
        FTPFile file = null;
        FTPClient client = getClient();
        FTPFile[] files = client.listFiles(path);
        for (int i = 0; i < files.length; i++) {
            FTPFile currentFile = files[i];
            if (currentFile.getName().equals(path)) {
                file = currentFile;
            }
        }
        if (file != null) {
            if (file.isDirectory()) {
                files = client.listFiles(path);
                for (int i = 0; i < files.length; i++) {
                    deleteRecursively(files[i].getName());
                }
                deleted = client.removeDirectory(path);
            } else {
                deleted = client.deleteFile(path);
            }
        }
        return deleted;
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
            String password =
                    KnimeEncryption.decrypt(m_credentials.getPassword());
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
