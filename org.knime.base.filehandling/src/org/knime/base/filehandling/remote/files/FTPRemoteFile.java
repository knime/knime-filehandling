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
import java.util.Arrays;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformation;
import org.knime.core.util.KnimeEncryption;

/**
 * Implementation of the FTP remote file.
 * 
 * 
 * @author Patrick Winter, University of Konstanz
 */
public class FTPRemoteFile extends RemoteFile {

    /**
     * Creates a FTP remote file for the given URI.
     * 
     * 
     * @param uri The URI
     * @param connectionInformation Connection information to the given URI
     */
    FTPRemoteFile(final URI uri,
            final ConnectionInformation connectionInformation) {
        super(uri, connectionInformation);
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
    public String getType() {
        return "ftp";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() throws Exception {
        String name;
        if (isDirectory()) {
            // Remove '/' from path and separate name
            name =
                    FilenameUtils.getName(FilenameUtils
                            .normalizeNoEndSeparator(getPath()));
        } else {
            // Use name from URI
            name = FilenameUtils.getName(getURI().getPath());
        }
        return FilenameUtils.normalize(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPath() throws Exception {
        FTPClient client = getClient();
        String path = getURI().getPath();
        // If path is empty use working directory
        if (path == null || path.length() == 0) {
            path = client.printWorkingDirectory();
        }
        boolean changed = client.changeWorkingDirectory(path);
        // If directory has not changed the path pointed to a file
        if (!changed) {
            path = FilenameUtils.getFullPath(path);
        }
        // Make sure that the path ends with a '/'
        if (!path.endsWith("/")) {
            path += "/";
        }
        return FilenameUtils.normalize(path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exists() throws Exception {
        // In case of the root directory there is no FTP file available but
        // isDirectory returns true
        return getFTPFile() != null || isDirectory();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDirectory() throws Exception {
        boolean isDirectory = false;
        FTPClient client = getClient();
        // Use path from URI
        String path = getURI().getPath();
        if (path != null && path.length() > 0) {
            // If path is not missing, try to change to it
            isDirectory = client.changeWorkingDirectory(path);
        } else {
            // If path is missing, interpret this file as root directory, so it
            // is always a directory
            isDirectory = true;
        }
        return isDirectory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void move(final RemoteFile file) throws Exception {
        // If the file is also an FTP remote file and over the same connection
        // it can be moved
        if (file instanceof FTPRemoteFile
                && getIdentifier().equals(file.getIdentifier())) {
            FTPRemoteFile source = (FTPRemoteFile)file;
            FTPClient client = getClient();
            boolean success =
                    client.rename(source.getURI().getPath(), getURI().getPath());
            if (!success) {
                throw new Exception("Move operation failed");
            }
        } else {
            super.move(file);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream openInputStream() throws Exception {
        FTPClient client = getClient();
        String path = getURI().getPath();
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
        String path = getURI().getPath();
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
        // Assume missing
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
            time = ftpFile.getTimestamp().getTimeInMillis() / 1000;
        }
        return time;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean delete() throws Exception {
        // Delete can only be true if the file exists
        boolean result = exists();
        FTPClient client = getClient();
        String path = getFullName();
        if (exists()) {
            if (isDirectory()) {
                // Delete inner files first
                RemoteFile[] files = listFiles();
                for (int i = 0; i < files.length; i++) {
                    files[i].delete();
                }
                // Delete this directory
                client.rmd(path);
                result = result && !exists();
            } else {
                // Delete this file
                client.deleteFile(path);
                result = result && !exists();
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RemoteFile[] listFiles() throws Exception {
        RemoteFile[] files;
        if (isDirectory()) {
            FTPClient client = getClient();
            client.changeWorkingDirectory(getPath());
            // Get FTP files
            FTPFile[] ftpFiles = client.listFiles();
            files = new RemoteFile[ftpFiles.length];
            // Generate remote file for each file
            for (int i = 0; i < ftpFiles.length; i++) {
                // Build URI
                URI uri =
                        new URI(getURI().getScheme() + "://"
                                + getURI().getAuthority() + getPath()
                                + ftpFiles[i].getName());
                // Create remote file and open it
                files[i] = new FTPRemoteFile(uri, getConnectionInformation());
                files[i].open();
            }
        } else {
            // Return 0 files
            files = new RemoteFile[0];
        }
        // Sort results
        Arrays.sort(files);
        return files;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean mkDir() throws Exception {
        FTPClient client = getClient();
        return client.makeDirectory(getURI().getPath());
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
    private FTPClient getClient() throws Exception {
        // Get FTP connection from super class
        FTPConnection connection = (FTPConnection)getConnection();
        // Change to default working directory
        connection.resetWorkingDir();
        // Return FTP client of the FTP connection
        return connection.getClient();
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
        boolean isDirectory = isDirectory();
        FTPClient client = getClient();
        // Change to this files path
        client.changeWorkingDirectory(getPath());
        // If this file is a directory change to the parent directory
        if (isDirectory) {
            client.changeToParentDirectory();
        }
        // Get all files in working directory
        FTPFile[] files = client.listFiles();
        // Check all files by name
        for (int i = 0; i < files.length; i++) {
            FTPFile currentFile = files[i];
            if (currentFile.getName().equals(
                    FilenameUtils.getName(getURI().getPath()))) {
                // File with the same name is the correct one
                file = currentFile;
                break;
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

        private String m_defaultDir;

        /**
         * {@inheritDoc}
         */
        @Override
        public void open() throws Exception {
            // Create client
            m_client = new FTPClient();
            // Read attributes
            String host = getURI().getHost();
            int port =
                    getURI().getPort() != -1 ? getURI().getPort()
                            : DefaultPortMap.getMap().get(getType());
            String user = getURI().getUserInfo();
            String password = getConnectionInformation().getPassword();
            if (password != null) {
                password = KnimeEncryption.decrypt(password);
            }
            // Open connection
            m_client.connect(host, port);
            // Login
            boolean loggedIn = m_client.login(user, password);
            if (!loggedIn) {
                throw new IOException("Login failed");
            }
            // Find root directory
            String oldDir;
            do {
                oldDir = m_client.printWorkingDirectory();
                m_client.changeToParentDirectory();
                m_defaultDir = m_client.printWorkingDirectory();
            } while (!m_defaultDir.equals(oldDir));
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

        /**
         * Change working directory to root.
         * 
         * 
         * @throws Exception If the operation could not be executed
         */
        public void resetWorkingDir() throws Exception {
            m_client.changeWorkingDirectory(m_defaultDir);
        }

    }

}
