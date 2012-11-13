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

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

/**
 * Remote file.
 * 
 * 
 * @author Patrick Winter, University of Konstanz
 */
public abstract class RemoteFile {

    private Connection m_connection = null;

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
     */
    final void open() throws Exception {
        // Only create a connection if this remote file uses a connection
        if (usesConnection() && m_connection == null) {
            // Look for existing connection
            String identifier = getIdentifier();
            Connection connection =
                    ConnectionMonitor.findConnection(identifier);
            // If no connection available create a new one
            if (connection == null) {
                connection = createConnection();
                connection.open();
                ConnectionMonitor.registerConnection(identifier, connection);
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
    protected abstract Connection createConnection();

    /**
     * Return the current connection.
     * 
     * 
     * @return The current connection
     */
    public final Connection getConnection() {
        return m_connection;
    }

    /**
     * Internal method to create the identifier.
     * 
     * 
     * @return Identifier to this remote files connection
     */
    protected abstract String getIdentifier();

    /**
     * Build the default identifier to the given URI.
     * 
     * 
     * Consists of the scheme, the user, the host and the port. Uses the default
     * port if the URI specifies no port.
     * 
     * @param uri The URI
     * @return Identifier for the given URI
     */
    protected final String buildIdentifier(final URI uri) {
        int port = uri.getPort();
        // If no port is available use the default port
        if (port < 0) {
            port = DefaultPortMap.getMap().get(getType());
        }
        // Format: scheme://user@host:port
        return uri.getScheme() + "://" + uri.getUserInfo() + "@"
                + uri.getHost() + ":" + port;
    }

    /**
     * Creates the unsupported operation message.
     * 
     * 
     * @param operation The operation that is unsupported
     * @return Message for the unsupported operation exception
     */
    protected final String unsupportedMessage(final String operation) {
        return "The operation " + operation + " is not supported by "
                + getType();
    }

    /**
     * Returns the type (URI scheme) of the remote file.
     * 
     * 
     * @return The remote files type
     */
    public abstract String getType();

    /**
     * @return The name of this file
     * @throws Exception If the operation could not be executed
     */
    public abstract String name() throws Exception;

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
     * @return true if the file was successfully moved, false otherwise
     * @throws Exception If the operation could not be executed
     */
    public abstract boolean move(final RemoteFile file) throws Exception;

    /**
     * Write the given remote file into this files location.
     * 
     * 
     * This method will overwrite the old file if it exists.
     * 
     * @param file Source remote file
     * @throws Exception If the operation could not be executed
     */
    public abstract void write(final RemoteFile file) throws Exception;

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
     * @return Time in UNIX-Time or 0 if the file does not exist
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
    public abstract RemoteFile[] listFiles() throws Exception;

    /**
     * Create a directory.
     * 
     * 
     * @return true if the directory has been created, false otherwise
     * @throws Exception If the operation could not be executed
     */
    public abstract boolean mkDir() throws Exception;

    /**
     * Close this remote file.
     * 
     * 
     * @throws Exception If closing did not succeed
     */
    public abstract void close() throws Exception;

}
