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
 *   Nov 5, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.remote.files;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import org.apache.commons.io.FilenameUtils;
import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformation;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.util.CheckUtils;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.Session;

/**
 * Implementation of the SCP remote file.
 *
 *
 * @author Patrick Winter, KNIME.com, Zurich, Switzerland
 * @deprecated use {@link SFTPRemoteFile} instead
 */
@Deprecated
public class SCPRemoteFile extends RemoteFile<SSHConnection> {

    private static final String EXCEPTION_FILE_NOT_FOUND = "File not found";

    /**
     * Creates a SCP remote file for the given URI.
     *
     *
     * @param uri The URI
     * @param connectionInformation Connection information to the given URI
     * @param connectionMonitor Monitor for the connection
     */
    SCPRemoteFile(final URI uri, final ConnectionInformation connectionInformation,
            final ConnectionMonitor<SSHConnection> connectionMonitor) {
        super(uri, connectionInformation, connectionMonitor);
        CheckUtils.checkArgumentNotNull(connectionInformation, "Connection information must not be null");
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
        return "scp";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exists() throws Exception {
        // Assume missing file
        boolean exists = false;
        final SCPChannel scp = new SCPChannel();
        try {
            // Open file input will throw file not found exception if file does
            // not exist
            scp.openFileInput(getURI().getPath());
            scp.close();
            // If no exception was thrown up until this point the file does
            // exist
            exists = true;
        } catch (final Exception e) {
            // Throw the exception if it was not the file not found exception
            if (!e.getMessage().equals(EXCEPTION_FILE_NOT_FOUND)) {
                throw e;
            }
        }
        return exists;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDirectory() throws Exception {
        // SCP cannot address directories
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(final RemoteFile<SSHConnection> file, final ExecutionContext exec) throws Exception {
        final byte[] buffer = new byte[1024];
        final SCPChannel scp = new SCPChannel();
        // Open direct output stream
        scp.openFileOutput(getURI().getPath(), file.getSize());
        final InputStream in = file.openInputStream();
        final OutputStream out = scp.getOutputStream();
        int length;
        // Read should deliver exactly file.getSize() bytes
        while ((length = in.read(buffer)) > 0) {
            out.write(buffer, 0, length);
        }
        in.close();
        // Close SCP connection correctly
        scp.closeFileOutput();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream openInputStream() throws Exception {
        // Use wrapper that hides the SCP communication
        return new SCPInputStream(getURI().getPath());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputStream openOutputStream() throws Exception {
        // Use wrapper that hides the SCP communication and buffers the bytes
        // into a temporary file, since SCP needs to know the expected size up
        // front
        return new SCPOutputStream(getURI().getPath());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSize() throws Exception {
        // Assume missing file
        long size = 0;
        final SCPChannel scp = new SCPChannel();
        try {
            // Opening the file input does deliver the size
            size = scp.openFileInput(getURI().getPath());
            scp.close();
        } catch (final Exception e) {
            // Throw the exception if it was not the file not found exception
            if (!e.getMessage().equals(EXCEPTION_FILE_NOT_FOUND)) {
                throw e;
            }
        }
        return size;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long lastModified() throws Exception {
        throw new UnsupportedOperationException(unsupportedMessage("lastModified"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean delete() throws Exception {
        throw new UnsupportedOperationException(unsupportedMessage("delete"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RemoteFile<SSHConnection>[] listFiles() throws Exception {
        throw new UnsupportedOperationException(unsupportedMessage("listFiles"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean mkDir() throws Exception {
        throw new UnsupportedOperationException(unsupportedMessage("mkDir"));
    }

    /**
     * SCP channel to handle the start and closing of SCP data transfer.
     *
     *
     * @author Patrick Winter, KNIME.com, Zurich, Switzerland
     */
    private class SCPChannel {

        private InputStream m_in;

        private OutputStream m_out;

        private ChannelExec m_channel;

        /**
         * Get the input stream of this channel.
         *
         *
         * @return Input stream
         */
        public InputStream getInputStream() {
            return m_in;
        }

        /**
         * Get the output stream of this channel.
         *
         *
         * @return Output stream
         */
        public OutputStream getOutputStream() {
            return m_out;
        }

        /**
         * Open a channel for file input.
         *
         *
         * @param path Path to the remote file
         * @return Size of the remote file
         * @throws Exception If the opening was unsuccessful
         */
        public long openFileInput(final String path) throws Exception {
            String skipped;
            long size = 0L;
            // Open execution channel
            final Session session = getConnection().getSession();
            m_channel = (ChannelExec)session.openChannel("exec");
            // Get communication streams
            m_in = m_channel.getInputStream();
            m_out = m_channel.getOutputStream();
            // Execute SCP
            m_channel.setCommand("scp -f " + path);
            m_channel.connect();
            sendConfirmation();
            checkForConfirmation();
            // skip over permissions
            skipped = skipTo(m_in, ' ');
            // look for error message
            if (skipped.contains("scp:")) {
                throw new IOException(EXCEPTION_FILE_NOT_FOUND);
            }
            // read file size
            skipped = skipTo(m_in, ' ');
            size = Long.parseLong(skipped);
            // skip filename
            skipTo(m_in, '\n');
            sendConfirmation();
            return size;
        }

        /**
         * Close a file input channel.
         *
         *
         * @throws IOException If the closing was unsuccessful
         */
        public void closeFileInput() throws IOException {
            checkForConfirmation();
            sendConfirmation();
            close();
        }

        /**
         * Open a channel for file output.
         *
         *
         * @param path Path where the file should be written to
         * @param size Size of the file that should be written
         * @throws Exception If the opening was unsuccessful
         */
        public void openFileOutput(final String path, final long size) throws Exception {
            // Open execution channel
            final Session session = getConnection().getSession();
            m_channel = (ChannelExec)session.openChannel("exec");
            // Get communication streams
            m_in = m_channel.getInputStream();
            m_out = m_channel.getOutputStream();
            // Execute SCP
            m_channel.setCommand("scp -t " + path);
            m_channel.connect();
            checkForConfirmation();
            // Send line with permissions (using default), file size and file
            // name
            final String command = "C0644 " + size + " " + FilenameUtils.getName(path) + "\n";
            m_out.write(command.getBytes());
            m_out.flush();
            checkForConfirmation();
        }

        /**
         * Close a file output channel.
         *
         *
         * @throws IOException If the closing was unsuccessful
         */
        public void closeFileOutput() throws IOException {
            sendConfirmation();
            checkForConfirmation();
            close();
        }

        /**
         * Close input and output streams and disconnect channel.
         *
         *
         * @throws IOException If the closing was unsuccessful
         */
        private void close() throws IOException {
            m_in.close();
            m_out.close();
            m_channel.disconnect();
        }

        /**
         * Check the input stream for the confirmation byte.
         *
         *
         * @throws IOException If communication was unsuccessful, or
         *             confirmation was false
         */
        private void checkForConfirmation() throws IOException {
            final byte[] buffer = new byte[1];
            if (m_in.read(buffer) == 0) {
                // Stream does not have valid data
                throw new IOException("SCP error");
            }
            // 0 means positive confirmation
            if (buffer[0] != 0) {
                throw new IOException("SCP error");
            }
        }

        /**
         * Send a confirmation byte.
         *
         *
         * @throws IOException If communication was unsuccessful
         */
        private void sendConfirmation() throws IOException {
            final byte[] buffer = new byte[1];
            // 0 means positive confirmation
            buffer[0] = 0;
            m_out.write(buffer);
            m_out.flush();
        }

        /**
         * Consumes all bytes from the input stream up to and including the
         * given character.
         *
         *
         * @param streamIn Stream to read from
         * @param character The character that will be searched for
         * @return All consumed characters (excluding the given character)
         * @throws IOException If the input stream does not contain the given
         *             character
         */
        private String skipTo(final InputStream streamIn, final char character) throws IOException {
            final StringBuffer result = new StringBuffer();
            final byte[] buffer = new byte[1];
            // Read single byte until character appears
            do {
                final int length = streamIn.read(buffer, 0, 1);
                if (length < 0) {
                    throw new IOException("Reached end of stream and found no '" + character + "'");
                }
                // Build string with found characters
                result.append(new String(buffer));
            } while (buffer[0] != character);
            // Remove the found character from the result
            result.deleteCharAt(result.length() - 1);
            return result.toString();
        }

    }

    /**
     * Input stream wrapper for SCP.
     *
     *
     * @author Patrick Winter, KNIME.com, Zurich, Switzerland
     */
    private class SCPInputStream extends InputStream {

        private final SCPChannel m_scp;

        private long m_bytesLeft;

        /**
         * Create SCP input stream wrapper.
         *
         *
         * @param path The path to the remote file
         * @throws Exception If the opening was unsuccessful
         */
        public SCPInputStream(final String path) throws Exception {
            m_scp = new SCPChannel();
            m_bytesLeft = m_scp.openFileInput(path);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int read() throws IOException {
            final byte[] b = new byte[1];
            // Pass to read(buffer)
            if (read(b) == 0) {
                throw new IOException("Stream has no bytes left");
            }
            return b[0];
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int read(final byte[] buffer) throws IOException {
            // Pass to read(buffer, offset, length)
            return read(buffer, 0, buffer.length);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int read(final byte[] buffer, final int offset, final int length) throws IOException {
            // Get input stream of SCP channel
            final InputStream in = m_scp.getInputStream();
            int result = -1;
            // Check if bytes are available
            if (m_bytesLeft > 0L) {
                // calculate min(length, buffer.length, m_bytesLeft)
                int readLength = Math.min(buffer.length, length);
                if (m_bytesLeft < readLength) {
                    readLength = (int)m_bytesLeft;
                }
                result = in.read(buffer, 0, readLength);
                // subtract read bytes from bytes left
                m_bytesLeft -= readLength;
            }
            return result;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long skip(final long n) throws IOException {
            // Get input stream of SCP channel
            final InputStream in = m_scp.getInputStream();
            final long skip = m_bytesLeft < n ? m_bytesLeft : n;
            // skip bytes
            final long skipped = in.skip(skip);
            // subtract skipped bytes from bytes left
            m_bytesLeft -= skipped;
            return skipped;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void close() throws IOException {
            m_scp.closeFileInput();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int available() throws IOException {
            return m_scp.getInputStream().available();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public synchronized void mark(final int readlimit) {
            m_scp.getInputStream().mark(readlimit);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean markSupported() {
            return m_scp.getInputStream().markSupported();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public synchronized void reset() throws IOException {
            m_scp.getInputStream().reset();
        }

    }

    /**
     * Output stream wrapper for SCP.
     *
     *
     * Workaround for SCPs inability to write a file without previously knowing
     * the size. Will write the bytes to a temporary file and write the file to
     * SCP on <code>close()</code>.
     *
     * @author Patrick Winter, KNIME.com, Zurich, Switzerland
     */
    private class SCPOutputStream extends OutputStream {

        private final OutputStream m_stream;

        private final File m_file;

        private final String m_path;

        /**
         * Create SCP output stream wrapper.
         *
         *
         * @param path Remote path
         * @throws Exception If the temporary file could not be created
         */
        public SCPOutputStream(final String path) throws Exception {
            m_path = path;
            // Create temporary file
            m_file = File.createTempFile(FilenameUtils.getName(m_path), ".tmp");
            // Open file stream
            m_stream = new FileOutputStream(m_file);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void close() throws IOException {
            // Open file input stream
            final InputStream in = new FileInputStream(m_file);
            try {
                final byte[] buffer = new byte[1024];
                int length;
                // Close file output stream
                m_stream.close();
                final SCPChannel scp = new SCPChannel();
                scp.openFileOutput(m_path, m_file.length());
                // Get scp channel output stream
                final OutputStream out = scp.getOutputStream();
                // Copy bytes from file input to scp output
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
                in.close();
                scp.closeFileOutput();
            } catch (final Exception e) {
                if (in != null) {
                    in.close();
                }
                // Convert exception to IOException
                throw new IOException(e);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void write(final int b) throws IOException {
            final byte[] bytes = new byte[1];
            bytes[0] = (byte)b;
            // Pass to write(buffer)
            write(bytes);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void write(final byte[] buffer) throws IOException {
            // Pass to write(buffer, offset, length)
            write(buffer, 0, buffer.length);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void write(final byte[] buffer, final int offset, final int length) throws IOException {
            // Write to file stream
            m_stream.write(buffer, offset, length);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void flush() throws IOException {
            m_stream.flush();
        }

    }

}
