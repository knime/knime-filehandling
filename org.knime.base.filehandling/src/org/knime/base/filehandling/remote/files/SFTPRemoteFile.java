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
 * ------------------------------------------------------------------------
 *
 * History
 *   Nov 5, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.remote.files;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileAlreadyExistsException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformation;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.util.CheckUtils;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

/**
 * Implementation of the SFTP remote file.
 *
 *
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 */
public class SFTPRemoteFile extends RemoteFile<SSHConnection> {

    private ChannelSftp m_channel = null;

    private String m_path = null;

    private String m_nameCache = null;

    private String m_pathCache = null;

    private Boolean m_existsCache = null;

    private Boolean m_isdirCache = null;

    private LsEntry m_entryCache = null;

    private Long m_sizeCache = null;

    private Long m_modifiedCache = null;

    private void resetCache() {
        // Empty cache
        m_pathCache = null;
        m_nameCache = null;
        m_existsCache = null;
        m_isdirCache = null;
        m_entryCache = null;
        m_sizeCache = null;
        m_modifiedCache = null;
    }

    /**
     * Creates a SFTP remote file for the given URI.
     *
     *
     * @param uri The URI
     * @param connectionInformation Connection information to the given URI
     * @param connectionMonitor Monitor for the connection
     */
    SFTPRemoteFile(final URI uri, final ConnectionInformation connectionInformation,
            final ConnectionMonitor<SSHConnection> connectionMonitor) {
        super(uri, connectionInformation, connectionMonitor);
        CheckUtils.checkArgumentNotNull(connectionInformation, "Connection information must not be null");
    }


    /**
     * Creates a SFTP remote file for the given URI. It uses information from an "ls" call to populate the metadata
     * such as filename, size, etc.
     *
     *
     * @param uri The URI
     * @param parentPath the path of the parent directory
     * @param lsEntry the LsEntry from a ls call
     * @param connectionInformation Connection information to the given URI
     * @param connectionMonitor Monitor for the connection
     * @throws Exception if an error occurs
     */
    private SFTPRemoteFile(final URI uri, final String parentPath, final LsEntry lsEntry,
        final ConnectionInformation connectionInformation, final ConnectionMonitor<SSHConnection> connectionMonitor)
        throws Exception {
        super(uri, connectionInformation, connectionMonitor);
        SftpATTRS attrs = lsEntry.getAttrs();
        m_existsCache = Boolean.TRUE;
        m_modifiedCache = (long) attrs.getMTime();
        m_nameCache = lsEntry.getFilename();
        if (attrs.isDir()) {
            m_isdirCache = true;
        } else if (attrs.isLink()) {
            open();
            openChannel(); // this sets m_isDirCache
        } else {
            m_isdirCache = false;
        }
        m_pathCache = m_isdirCache ? (parentPath + lsEntry.getFilename() + "/") : parentPath;
        m_sizeCache = attrs.getSize();
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
    protected SSHConnection createConnection() {
        // Use general SSH connection
        return new SSHConnection(getURI(), getConnectionInformation());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getType() {
        return "sftp";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() throws Exception {
        if (m_nameCache == null) {
            try {
                openChannel();
                internalGetName();
            } finally {
                closeChannel();
            }
        }
        return m_nameCache;
    }

    private String internalGetName() throws Exception {
        if (m_nameCache == null) {
            String name;
            if (internalIsDirectory()) {
                // Remove '/' from path and separate name
                String path = internalGetPath();
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
            try {
                openChannel();
                internalGetPath();
            } finally {
                closeChannel();
            }
        }
        return m_pathCache;
    }

    private String internalGetPath() throws Exception {
        if (m_pathCache == null) {
            cd(m_path);
            String path = getURI().getPath();
            // If path is empty use working directory
            if (path == null || path.length() == 0) {
                // Use path determined through first run of openChannel()
                path = m_path;
            }
            final boolean changed = cd(path);
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
            try {
                openChannel();
                internalExists();
            } finally {
                closeChannel();
            }
        }
        return m_existsCache;
    }

    private boolean internalExists() throws Exception {
        if (m_existsCache == null) {
            // In case of the root directory there is no ls entry available but
            // isDirectory returns true
            m_existsCache = getLsEntry() != null || internalIsDirectory();
        }
        return m_existsCache;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDirectory() throws Exception {
        if (m_isdirCache == null) {
            try {
                openChannel();
                internalIsDirectory();
            } finally {
                closeChannel();
            }
        }
        return m_isdirCache;
    }

    private boolean internalIsDirectory() throws Exception {
        if (m_isdirCache == null) {
            boolean isDirectory = false;
            openChannel();
            // Use path from URI
            final String path = getURI().getPath();
            if (path != null && path.length() > 0) {
                // If path is not missing, try to cd to it
                isDirectory = cd(path);
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
    public void move(final RemoteFile<SSHConnection> file, final ExecutionContext exec) throws Exception {
        // If the file is also an SFTP remote file and over the same connection
        // it can be moved
        if (file instanceof SFTPRemoteFile && getIdentifier().equals(file.getIdentifier())) {
            try {
                final SFTPRemoteFile source = (SFTPRemoteFile)file;
                // Remember if file existed before
                RemoteFile<SSHConnection>[] dirContents = listFiles();
                final boolean existed = Arrays.asList(dirContents).contains(source);
                openChannel();
                // Move file
                m_channel.rename(source.getFullName(), getFullName() + source.getName());
                resetCache();

                dirContents = listFiles();
                final boolean exists = Arrays.asList(dirContents).contains(source);
                // Success if target did not exist and now exists and the source
                // does not exist anymore
                final boolean success = !existed && exists;
                if (!success) {
                    throw new Exception("Move operation failed");
                }
            } finally {
                closeChannel();
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
        InputStream stream;
        openChannel();
        final String path = getURI().getPath();
        stream = m_channel.get(path);
        // Open stream (null if stream could not be opened)
        if (stream == null) {
            closeChannel();
            throw new Exception("Path not reachable");
        }
        stream = new SFTPInputStream(stream, m_channel);
        // Closing the channel is now the responsibility of the stream, this
        // file should use a new channel for other operations
        m_channel = null;
        return stream;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputStream openOutputStream() throws Exception {
        OutputStream stream;
        openChannel();
        final String path = getURI().getPath();
        stream = m_channel.put(path);
        // Open stream (null if stream could not be opened)
        if (stream == null) {
            closeChannel();
            throw new Exception("Path not reachable");
        }
        resetCache();
        stream = new SFTPOutputStream(stream, m_channel);
        // Closing the channel is now the responsibility of the stream, this
        // file should use a new channel for other operations
        m_channel = null;
        return stream;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSize() throws Exception {
        if (m_sizeCache == null) {
            try {
                openChannel();
                internalGetSize();
            } finally {
                closeChannel();
            }
        }
        return m_sizeCache;
    }

    private long internalGetSize() throws Exception {
        if (m_sizeCache == null) {
            // Assume missing
            long size = 0;
            final LsEntry entry = getLsEntry();
            if (entry != null) {
                size = entry.getAttrs().getSize();
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
            try {
                openChannel();
                internalLastModified();
            } finally {
                closeChannel();
            }
        }
        return m_modifiedCache;
    }

    private long internalLastModified() throws Exception {
        if (m_modifiedCache == null) {
            // Assume missing
            long time = 0;
            final LsEntry entry = getLsEntry();
            if (entry != null) {
                time = entry.getAttrs().getMTime();
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
        boolean result;
        try {
            // Delete can only be true if the file exists
            result = internalExists();
            final String path = getFullName();
            //getFullName() closes the channel -> reopen it
            openChannel();
            if (internalExists()) {
                if (internalIsDirectory()) {
                    // Delete inner files first
                    final RemoteFile<? extends Connection>[] files = internalListFiles();
                    for (final RemoteFile<? extends Connection> file : files) {
                        file.delete();
                    }
                    // move to parent directory
                    m_channel.cd("..");
                    // Delete this directory
                    m_channel.rmdir(path);
                    resetCache();
                    result = result && !internalExists();
                } else {
                    // Delete this file
                    openChannel();
                    m_channel.rm(path);
                    resetCache();
                    result = result && !internalExists();
                }
            }
        } finally {
            closeChannel();
        }
        return result;
    }

    /**
     * {@inheritDoc}
     * @since 2.11
     */
    @Override
    public SFTPRemoteFile[] listFiles() throws Exception {
        SFTPRemoteFile[] files;
        try {
            openChannel();
            files = internalListFiles();
        } finally {
            closeChannel();
        }
        return files;
    }

    private SFTPRemoteFile[] internalListFiles() throws Exception {
        final List<RemoteFile<SSHConnection>> files = new LinkedList<>();
        openChannel();
        if (internalIsDirectory()) {
            try {
                cd(m_path);
                // Get ls entries
                @SuppressWarnings("unchecked")
                final List<LsEntry> entries = m_channel.ls(".");
                final URI thisUri = getURI();
                // Generate remote file for each entry that is a file
                for (int i = 0; i < entries.size(); i++) {
                    // . and .. will return null after normalization
                    String filename = entries.get(i).getFilename();
                    if ((filename != null) && !(filename.equals(".") || filename.equals(".."))) {
                        try {
                            // Build URI
                            final URI uri = new URI(thisUri.getScheme(), thisUri.getUserInfo(), thisUri.getHost(),
                                thisUri.getPort(), internalGetPath() + filename,
                                thisUri.getQuery(), thisUri.getFragment());
                            // Create remote file and open it
                            final SFTPRemoteFile file =
                                new SFTPRemoteFile(uri, internalGetPath(), entries.get(i), getConnectionInformation(),
                                    getConnectionMonitor());
                            file.open();
                            // Add remote file to the result list
                            files.add(file);
                        } catch (final URISyntaxException e) {
                            // ignore files that are not representable
                        }
                    }
                }
                SFTPRemoteFile[] outFiles = files.toArray(new SFTPRemoteFile[files.size()]);
                // Sort results
                Arrays.sort(outFiles);
                return outFiles;
            } catch (final SftpException e) {
                // Return 0 files
                return new SFTPRemoteFile[0];
            }
        } else {
            return new SFTPRemoteFile[0];
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean mkDir() throws Exception {
        boolean result = false;
        if (exists()) {
            if (!isDirectory()) {
                throw new FileAlreadyExistsException("File with the same name already exists: " + getFullName());
            } else {
                return false;
            }
        }
        try {
            openChannel();
            m_channel.mkdir(getName() + "/");
            resetCache();
            getLsEntry();
            m_path = m_pathCache;
            result = true;
        } catch (final Exception e) {
            // result stays false
        } finally {
            closeChannel();
        }
        return result;
    }

    /**
     * Opens the SFTP channel if it is not already open.
     *
     *
     * @throws Exception If the channel could not be opened
     */
    private void openChannel() throws Exception {
        if (getConnection() == null) {
            open();
        }
        // Check if channel is ready
        if (m_channel == null || !m_channel.getSession().isConnected() || !m_channel.isConnected()) {
            final Session session = getConnection().getSession();
            if (!session.isConnected()) {
                session.connect();
            }
            m_channel = (ChannelSftp)session.openChannel("sftp");
            m_channel.connect();
        }
        // Check if path is initialized
        if (m_path == null) {
            boolean pathSet = false;
            String path = getURI().getPath();
            // If URI has path
            if (path != null && !path.isEmpty()) {
                if (cd(path)) {
                    // Path points to directory
                    m_path = path;
                    pathSet = true;
                    if (m_isdirCache == null) {
                        m_isdirCache = true;
                    }
                } else {
                    if (m_isdirCache == null) {
                        m_isdirCache = false;
                    }
                    // Path points to file
                    path = FilenameUtils.getFullPath(path);
                    if (cd(path)) {
                        m_path = path;
                        pathSet = true;
                    }
                }
            }
            if (!pathSet) {
                // Use root directory
                String oldDir;
                String newDir;
                // Change directory to parent until the path does not change
                // anymore
                do {
                    oldDir = m_channel.pwd();
                    cd("..");
                    newDir = m_channel.pwd();
                } while (!newDir.equals(oldDir));
                m_path = newDir;
            }
        }
        // Change to correct directory
        cd(m_path);
    }

    private void closeChannel() {
        if (m_channel != null) {
            m_channel.disconnect();
        }
    }

    /**
     * Returns the LsEntry to this file.
     *
     *
     * @return LsEntry to this file or null if not existing
     * @throws Exception If the operation could not be executed
     */
    private LsEntry getLsEntry() throws Exception {
        if (m_entryCache == null) {
            openChannel();
            // Assume missing
            LsEntry entry = null;
            // If this is a directory change to parent
            if (internalIsDirectory()) {
                m_channel.cd("..");
            }
            // Get all entries in working directory
            @SuppressWarnings("unchecked")
            final List<LsEntry> entries = m_channel.ls(".");
            // Check all entries by name
            for (int i = 0; i < entries.size(); i++) {
                final LsEntry currentEntry = entries.get(i);
                if (currentEntry.getFilename().equals(internalGetName())) {
                    // Entry with the same name is the correct one
                    entry = currentEntry;
                    break;
                }
            }
            m_entryCache = entry;
        }
        return m_entryCache;
    }

    /**
     * Change to another directory.
     *
     *
     * @param path Path to the new directory
     * @return true if the cd was successful, false otherwise
     */
    private boolean cd(final String path) {
        boolean result = false;
        try {
            m_channel.cd(path);
            result = true;
        } catch (final SftpException e) {
            // return false if cd was not possible
        }
        return result;
    }

    private static class SFTPInputStream extends InputStream {

        private final InputStream m_stream;

        private final ChannelSftp m_channel;

        SFTPInputStream(final InputStream inputStream, final ChannelSftp channel) {
            m_stream = inputStream;
            m_channel = channel;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int read() throws IOException {
            return m_stream.read();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int available() throws IOException {
            return m_stream.available();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void close() throws IOException {
            m_stream.close();
            m_channel.disconnect();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public synchronized void mark(final int readlimit) {
            m_stream.mark(readlimit);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean markSupported() {
            return m_stream.markSupported();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int read(final byte[] b) throws IOException {
            return m_stream.read(b);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int read(final byte[] b, final int off, final int len) throws IOException {
            return m_stream.read(b, off, len);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public synchronized void reset() throws IOException {
            m_stream.reset();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long skip(final long n) throws IOException {
            return m_stream.skip(n);
        }

    }

    private static class SFTPOutputStream extends OutputStream {

        private final OutputStream m_stream;

        private final ChannelSftp m_channel;

        SFTPOutputStream(final OutputStream outputStream, final ChannelSftp channel) {
            m_stream = outputStream;
            m_channel = channel;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void write(final int b) throws IOException {
            m_stream.write(b);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void close() throws IOException {
            m_stream.close();
            m_channel.disconnect();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void flush() throws IOException {
            m_stream.flush();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void write(final byte[] b) throws IOException {
            m_stream.write(b);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void write(final byte[] b, final int off, final int len) throws IOException {
            m_stream.write(b, off, len);
        }

    }

}
