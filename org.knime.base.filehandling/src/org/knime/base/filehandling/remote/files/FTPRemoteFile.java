/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   Nov 2, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.remote.files;

import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPException;
import it.sauronsoftware.ftp4j.FTPFile;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Arrays;
import org.apache.commons.io.FilenameUtils;
import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformation;
import org.knime.core.node.ExecutionContext;
import org.knime.core.util.FileUtil;

/**
 * Implementation of the FTP remote file.
 *
 *
 * @author Patrick Winter, KNIME.com, Zurich, Switzerland
 */
public class FTPRemoteFile extends RemoteFile<FTPConnection> {

    private String m_nameCache = null;

    private String m_pathCache = null;

    private Boolean m_existsCache = null;

    private Boolean m_isdirCache = null;

    private FTPFile m_ftpfileCache = null;

    private Long m_sizeCache = null;

    private Long m_modifiedCache = null;

    private void resetCache() {
        m_pathCache = null;
        m_nameCache = null;
        m_existsCache = null;
        m_isdirCache = null;
        m_ftpfileCache = null;
        m_sizeCache = null;
        m_modifiedCache = null;
    }

    /**
     * Creates a FTP remote file for the given URI.
     *
     *
     * @param uri The URI
     * @param connectionInformation Connection information to the given URI
     * @param connectionMonitor Monitor for the connection
     */
    FTPRemoteFile(final URI uri, final ConnectionInformation connectionInformation,
            final ConnectionMonitor<FTPConnection> connectionMonitor) {
        super(uri, connectionInformation, connectionMonitor);
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
     * @since 2.11
     */
    @Override
    protected FTPConnection createConnection() {
        return new FTPConnection(this);
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
        if (m_nameCache == null) {
            String name;
            if (isDirectory()) {
                // Remove '/' from path and separate name
                String path = getPath();
                path = path.substring(0, path.length() - 1);
                name = FilenameUtils.getName(path);
            } else {
                // Use name from URI
                name = FilenameUtils.getName(getURI().getPath());
            }
            m_nameCache = name;
        }
        return m_nameCache;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPath() throws Exception {
        if (m_pathCache == null) {
            final FTPClient client = getClient();
            String path = getURI().getPath();
            // If path is empty use working directory
            if (path == null || path.length() == 0) {
                path = client.currentDirectory();
            }
            boolean changed = true;
            try {
                client.changeDirectory(path);
            } catch (final FTPException e) {
                changed = false;
            }
            // If directory has not changed the path pointed to a file
            if (!changed) {
                path = FilenameUtils.getFullPath(path);
            }
            // Make sure that the path ends with a '/'
            if (!path.endsWith("/")) {
                path += "/";
            }
            // Make sure that the path starts with a '/'
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
            m_pathCache = path;
        }
        return m_pathCache;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exists() throws Exception {
        if (m_existsCache == null) {
            // In case of the root directory there is no FTP file available but
            // isDirectory returns true
            m_existsCache = getFTPFile() != null || isDirectory();
        }
        return m_existsCache;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDirectory() throws Exception {
        if (m_isdirCache == null) {
            boolean isDirectory = false;
            final FTPClient client = getClient();
            // Use path from URI
            final String path = getURI().getPath();
            if (path != null && path.length() > 0) {
                // If path is not missing, try to change to it
                isDirectory = true;
                try {
                    client.changeDirectory(path);
                } catch (final FTPException e) {
                    isDirectory = false;
                }
            } else {
                // If path is missing, interpret this file as root directory, so
                // it
                // is always a directory
                isDirectory = true;
            }
            m_isdirCache = isDirectory;
        }
        return m_isdirCache;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void move(final RemoteFile<FTPConnection> file, final ExecutionContext exec) throws Exception {
        // If the file is also an FTP remote file and over the same connection
        // it can be moved
        if (file instanceof FTPRemoteFile && getIdentifier().equals(file.getIdentifier())) {
            final FTPRemoteFile source = (FTPRemoteFile)file;
            final FTPClient client = getClient();
            boolean success = true;
            try {
                client.rename(source.getURI().getPath(), getURI().getPath());
            } catch (final FTPException e) {
                success = false;
            }
            resetCache();
            if (!success) {
                throw new Exception("Move operation failed");
            }
        } else {
            super.move(file, exec);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream openInputStream() throws Exception {
        final FTPClient client = getClient();
        final String path = getURI().getPath();
        final File tempFile = FileUtil.createTempFile("ftp-" + getName(), "");
        client.download(path, tempFile);
        final InputStream stream = new FTPInputStream(tempFile);
        return stream;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputStream openOutputStream() throws Exception {
        final File tempFile = FileUtil.createTempFile("ftp-" + getName(), "");
        final OutputStream stream = new FTPOutputStream(tempFile);
        resetCache();
        return stream;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSize() throws Exception {
        if (m_sizeCache == null) {
            // Assume missing
            long size = 0;
            final FTPFile ftpFile = getFTPFile();
            if (ftpFile != null && ftpFile.getSize() > 0) {
                size = ftpFile.getSize();
            }
            m_sizeCache = size;
        }
        return m_sizeCache;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long lastModified() throws Exception {
        if (m_modifiedCache == null) {
            // Assume missing
            long time = 0;
            final FTPFile ftpFile = getFTPFile();
            if (ftpFile != null) {
                time = ftpFile.getModifiedDate().getTime() / 1000;
            }
            m_modifiedCache = time;
        }
        return m_modifiedCache;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean delete() throws Exception {
        // Delete can only be true if the file exists
        boolean result = exists();
        final FTPClient client = getClient();
        final String path = getFullName();
        if (exists()) {
            if (isDirectory()) {
                // Delete inner files first
                final RemoteFile<FTPConnection>[] files = listFiles();
                for (final RemoteFile<FTPConnection> file : files) {
                    file.delete();
                }
                // Delete this directory
                client.deleteDirectory(path);
                resetCache();
                result = result && !exists();
            } else {
                // Delete this file
                client.deleteFile(path);
                resetCache();
                result = result && !exists();
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RemoteFile<FTPConnection>[] listFiles() throws Exception {
        RemoteFile<FTPConnection>[] files;
        if (isDirectory()) {
            final FTPClient client = getClient();
            client.changeDirectory(getPath());
            // Get FTP files
            final FTPFile[] ftpFiles = client.list();
            files = new FTPRemoteFile[ftpFiles.length];
            final URI thisUri = getURI();
            // Generate remote file for each file
            for (int i = 0; i < ftpFiles.length; i++) {
                // Build URI
                final URI uri = new URI(thisUri.getScheme(), thisUri.getUserInfo(), thisUri.getHost(),
                    thisUri.getPort(), getPath() + ftpFiles[i].getName(), thisUri.getQuery(), thisUri.getFragment());
                // Create remote file and open it
                files[i] = new FTPRemoteFile(uri, getConnectionInformation(), getConnectionMonitor());
                files[i].open();
            }
        } else {
            // Return 0 files
            files = new FTPRemoteFile[0];
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
        final FTPClient client = getClient();
        boolean created = true;
        try {
            client.createDirectory(getURI().getPath());
        } catch (final FTPException e) {
            created = false;
        }
        resetCache();
        return created;
    }

    /**
     * Convenience method to get the FTP Client from the connection.
     *
     *
     * @return The FTP Client
     */
    private FTPClient getClient() throws Exception {
        // Get FTP connection from super class
        final FTPConnection connection = getConnection();
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
        if (m_ftpfileCache == null) {
            FTPFile file = null;
            final boolean isDirectory = isDirectory();
            final FTPClient client = getClient();
            // Change to this files path
            try {
                client.changeDirectory(getPath());
                // If this file is a directory change to the parent directory
                if (isDirectory) {
                    client.changeDirectoryUp();
                }
                // Get all files in working directory
                final FTPFile[] files = client.list();
                // Check all files by name
                for (final FTPFile currentFile : files) {
                    if (currentFile.getName().equals(getName())) {
                        // File with the same name is the correct one
                        file = currentFile;
                        break;
                    }
                }
                m_ftpfileCache = file;
            } catch (final FTPException e) {
                // return with null
            }
        }
        return m_ftpfileCache;
    }

    private class FTPInputStream extends FileInputStream {

        private final File m_file;

        /**
         * @param file
         * @throws FileNotFoundException
         */
        public FTPInputStream(final File file) throws FileNotFoundException {
            super(file);
            m_file = file;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void close() throws IOException {
            super.close();
            m_file.delete();
        }

    }

    private class FTPOutputStream extends FileOutputStream {

        private final File m_file;

        /**
         * @param file
         * @throws FileNotFoundException
         */
        public FTPOutputStream(final File file) throws FileNotFoundException {
            super(file);
            m_file = file;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void close() throws IOException {
            super.close();
            try {
                final String name = getName();
                final FTPClient client = getClient();
                client.changeDirectory(getPath());
                final InputStream in = new FileInputStream(m_file);
                client.upload(name, in, 0, 0, null);
                in.close();
            } catch (final Exception e) {
                throw new IOException(e);
            } finally {
                m_file.delete();
            }
        }

    }

}
