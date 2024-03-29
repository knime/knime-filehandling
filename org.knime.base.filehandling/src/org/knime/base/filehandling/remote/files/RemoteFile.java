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
 *   Nov 2, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.remote.files;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformation;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.util.CheckUtils;

/**
 * Remote file.
 *
 *
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 * @param <C> the {@link Connection}
 */
public abstract class RemoteFile<C extends Connection> implements Comparable<RemoteFile<C>> {

    private C m_connection = null;

    private URI m_uri = null;

    private ConnectionInformation m_connectionInformation = null;

    private ConnectionMonitor<C> m_connectionMonitor = null;

    /**
     * Encapsulates connection specific access exceptions.
     * @since 3.3
     */
    public static class AccessControlException extends RuntimeException {
        public AccessControlException(final Throwable e) {
            super(e.getMessage(), e);
        }
    }

    /**
     * Create a remote file.
     *
     *
     * @param uri The uri pointing to the file
     * @param connectionInformation Connection information to the file
     * @param connectionMonitor Monitor for the connection
     */
    protected RemoteFile(final URI uri, final ConnectionInformation connectionInformation,
            final ConnectionMonitor<C> connectionMonitor) {
        m_uri = CheckUtils.checkArgumentNotNull(uri, "URI must not be null");
        m_connectionInformation = connectionInformation;
        m_connectionMonitor = connectionMonitor;
    }

    /**
     * If this remote file uses a connection.
     *
     *
     * @return true if this remote file uses a connection, false otherwise
     */
    protected abstract boolean usesConnection();

    /**
     * Create and open the connection for this remote file.
     *
     *
     * @throws Exception If opening failed
     * @since 2.11
     */
    public final void open() throws Exception {
        // Only create a connection if this remote file uses a connection
        if (usesConnection() && m_connection == null) {
            // Look for existing connection
            final String identifier = getIdentifier();
            C connection = m_connectionMonitor.findConnection(identifier);
            // If no connection is available create a new one
            if (connection == null) {
                connection = createConnection();
                connection.open();
                m_connectionMonitor.registerConnection(identifier, connection);
            }
            m_connection = connection;
        }
    }

    /**
     * Internal method to create a new connection.
     *
     *
     * @return New connection for this remote file
     */
    protected abstract C createConnection();

    /**
     * Return the current connection.
     *
     *
     * @return The current connection
     */
    public final C getConnection() {
        return m_connection;
    }

    /**
     * Internal method to create the identifier.
     *
     *
     * @return Identifier to this remote files connection
     * @since 2.11
     */
    public String getIdentifier() {
        final URI uri = getURI();
        int port = uri.getPort();
        // If no port is available use the default port
        if (port < 0) {
            port = RemoteFileHandlerRegistry.getDefaultPort(getType());
        }
        // Format: scheme://user@host:port
        return uri.getScheme() + "://" + uri.getUserInfo() + "@" + uri.getHost() + ":" + port;
    }

    /**
     * Creates the unsupported operation message.
     *
     *
     * @param operation The operation that is unsupported
     * @return Message for the unsupported operation exception
     */
    protected final String unsupportedMessage(final String operation) {
        return "The operation " + operation + " is not supported by " + getType();
    }

    /**
     * Get the URI to this file.
     *
     *
     * @return The URI to this file
     */
    public final URI getURI() {
        return m_uri;
    }

    /**
     * Get the connection information to this file.
     *
     *
     * @return The connection information to this file (might be null)
     */
    public final ConnectionInformation getConnectionInformation() {
        return m_connectionInformation;
    }

    /**
     * Get the monitor handling the connection.
     *
     *
     * @return the connectionMonitor (might be null)
     */
    public final ConnectionMonitor<C> getConnectionMonitor() {
        return m_connectionMonitor;
    }

    /**
     * Returns the type (URI scheme) of the remote file.
     *
     *
     * @return The remote files type
     */
    public abstract String getType();

    /**
     * Returns the name of this file without path information.
     *
     *
     * File: /usr/bin/ssh -> ssh
     *
     * Directory: /usr/bin/ -> bin
     *
     * @return The name of this file
     * @throws Exception If the operation could not be executed
     */
    public String getName() throws Exception {
        // Default implementation using just the URI
        final URI uri = getURI();
        String name = FilenameUtils.getName(uri.getPath());
        if (name != null && name.length() == 0) {
            // If name is empty it might be a directory
            name = FilenameUtils.getName(FilenameUtils.getFullPathNoEndSeparator(uri.getPath()));
        }
        return name;
    }

    /**
     * Returns the name of this file with path information.
     *
     *
     * File: /usr/bin/ssh -> /usr/bin/ssh
     *
     * Directory: /usr/bin/ -> /usr/bin/
     *
     * @return The full name with path.
     * @throws Exception If the operation could not be executed
     */
    public final String getFullName() throws Exception {
        String fullname = getPath();
        if (exists() && !isDirectory()) {
            // Append name to path
            fullname += getName();
        }
        return fullname;
    }

    /**
     * Returns the path of this file.
     *
     *
     * File: /usr/bin/ssh -> /usr/bin/
     *
     * Directory: /usr/bin/ -> /usr/bin/
     *
     * @return The path without the filename
     * @throws Exception If the operation could not be executed
     */
    public String getPath() throws Exception {
        // Default implementation using just the URI
        String path = getURI().getPath();
        if (path != null) {
            if (exists()) {
                if (!isDirectory()) {
                    path = FilenameUtils.getFullPath(path);
                }
            } else if (!path.endsWith("/")) {
                path = FilenameUtils.getFullPath(path);
            }

            if (!path.endsWith("/")) {
                path += "/";
            }
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
        }
        return path;
    }

    /**
     * Check if the file does exist.
     *
     *
     * @return true if the file exists, false otherwise
     * @throws Exception If the operation could not be executed
     */
    public abstract boolean exists() throws Exception;

    /**
     * Check if the file is a directory.
     *
     *
     * @return true if the file is a directory, false otherwise
     * @throws Exception If the operation could not be executed
     */
    public abstract boolean isDirectory() throws Exception;

    /**
     * Move the given file to this files location.
     *
     *
     * @param file The file to be moved
     * @param exec Execution context for <code>checkCanceled()</code> and
     *            <code>setProgress()</code>
     * @throws Exception If the operation could not be executed
     */
    public void move(final RemoteFile<C> file, final ExecutionContext exec) throws Exception {
        // Default implementation using just remote file methods
        write(file, exec);
        file.delete();
    }

    /**
     * Write the given remote file into this files location.
     *
     *
     * This method will overwrite the old file if it exists.
     *
     * @param file Source remote file
     * @param exec Execution context for <code>checkCanceled()</code> and
     *            <code>setProgress()</code>
     * @throws Exception If the operation could not be executed
     */
    public void write(final RemoteFile<C> file, final ExecutionContext exec) throws Exception {
        // Default implementation using just remote file methods
        final byte[] buffer = new byte[1024 * 1024]; // 1MB
        final String uri = getURI().toString();
        final InputStream in = file.openInputStream();
        final OutputStream out = openOutputStream();
        long progress = 0;
        int length;
        while ((length = in.read(buffer)) > 0) {
            out.write(buffer, 0, length);
            progress += length;
            if (exec != null) {
                exec.checkCanceled();
                exec.setProgress("Written: " + FileUtils.byteCountToDisplaySize(progress) + " to file " + uri);
            }
        }
        in.close();
        out.close();
    }

    /**
     * Opens an input stream.
     *
     *
     * @return The input stream
     * @throws Exception If the input stream could not be opened
     */
    public abstract InputStream openInputStream() throws Exception;

    /**
     * Opens an output stream.
     *
     *
     * @return The output stream
     * @throws Exception If the output stream could not be opened
     */
    public abstract OutputStream openOutputStream() throws Exception;

    /**
     * Get the size of the file.
     *
     *
     * @return The size of the file or 0 if the file does not exist
     * @throws Exception If the size could not be retrieved
     */
    public abstract long getSize() throws Exception;

    /**
     * Check for the last time the file was modified.
     *
     *
     * @return Time in UNIX-Time (seconds since 01.01.1970) or 0 if the file does not exist
     * @throws Exception If the operation could not be executed
     */
    public abstract long lastModified() throws Exception;

    /**
     * Deletes the file.
     *
     *
     * @return true if the file could be deleted, false otherwise
     * @throws Exception If an error occurs during deletion
     */
    public abstract boolean delete() throws Exception;

    /**
     * Get the files in this directory.
     *
     *
     * @return Array of files contained in this directory, or empty array if
     *         this is not a directory
     * @throws Exception If the operation could not be executed
     */
    public abstract RemoteFile<C>[] listFiles() throws Exception;

    /**
     * Create a directory.
     *
     *
     * @return true if the directory has been created, false otherwise
     * @throws Exception If the operation could not be executed
     */
    public abstract boolean mkDir() throws Exception;

    /**
     * Create all not existing directories of this files path.
     *
     *
     * @param includeThis If this file should also be a directory, otherwise only the parent is considered
     * @return true if all directories exist, false otherwise
     * @throws Exception If the operation could not be executed
     */
    public final boolean mkDirs(final boolean includeThis) throws Exception {
        if (exists()) {
            // We don't have to create anything or care about the parent, this stops the recursion
            if (includeThis) {
                // If this file exists and is a directory
                return isDirectory();
            } else {
                // If this file exists then the parent path does also
                return true;
            }
        } else {
            final RemoteFile<C> parent = getParent();
            boolean parentIsDirectory;
            if (parent != null) {
                // We try to make the parent path (if necessary) and retrieve if it exists now
                parentIsDirectory = parent.mkDirs(true);
            } else {
                // There is no parent so it can not be a directory
                parentIsDirectory = false;
            }
            if (includeThis) {
                // Try to make this directory
                mkDir();
            }
            return includeThis ? isDirectory() : parentIsDirectory;
        }
    }

    /**
     * Get the parent of this file.
     *
     *
     * @return The parent file or null if there is no parent
     * @throws Exception If the operation could not be executed
     */
    public RemoteFile<C> getParent() throws Exception {
        String path = getFullName();
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        if (path.isEmpty()) {
            // There is no directory above this one
            return null;
        }
        path = FilenameUtils.getFullPath(path);
        // Build URI
        final URI uri = new URI(m_uri.getScheme(), m_uri.getUserInfo(), m_uri.getHost(), m_uri.getPort(),
            path, m_uri.getQuery(), m_uri.getFragment());
        // Create remote file and open it
        final RemoteFile<C> file =
                RemoteFileFactory.createRemoteFile(uri, m_connectionInformation, m_connectionMonitor);
        file.open();
        return file;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        String string = "unknown_file";
        try {
            string = getName();
        } catch (final Exception e) {
            // File name is unknown
        }
        return string;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final RemoteFile<C> o) {
        int result = 1;
        try {
            if (isDirectory() == o.isDirectory()) {
                result = getName().compareTo(o.getName());
            } else if (isDirectory()) {
                result = -1;
            }
        } catch (final Exception e) {
            // put this after o
        }
        return result;
    }

    @Override
    public boolean equals(final Object rf) {
        return 0 == this.compareTo((RemoteFile<C>)rf);
    }
}
