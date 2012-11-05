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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.io.FilenameUtils;
import org.knime.base.filehandling.remote.Connection;
import org.knime.base.filehandling.remote.RemoteFile;
import org.knime.base.filehandling.remote.SSHConnection;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * 
 * @author Patrick Winter, University of Konstanz
 */
public class SCPRemoteFile extends RemoteFile {

    private URI m_uri;

    /**
     * @param uri The URI
     */
    public SCPRemoteFile(final URI uri) {
        try {
            m_uri = new URI(uri.toString().replaceFirst("scp", "ssh"));
        } catch (URISyntaxException e) {
            // should not happen
        }
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
    protected Connection createConnection() {
        return new SSHConnection(m_uri);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws Exception {
        // Not used
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream openInputStream() throws Exception {
        String skipped;
        byte[] buffer = new byte[1];
        String path = m_uri.getPath();
        Session session = ((SSHConnection)getConnection()).getSession();
        ChannelExec channel = (ChannelExec)session.openChannel("exec");
        channel.setCommand("scp -f " + path);
        InputStream streamIn = channel.getInputStream();
        OutputStream streamOut = channel.getOutputStream();
        channel.connect();
        // write '0'
        buffer[0] = 0;
        streamOut.write(buffer);
        streamOut.flush();
        streamIn.read(buffer);
        if (buffer[0] != 0) {
            throw new IOException("SCP error");
        }
        // skip first token
        skipped = skipTo(streamIn, ' ');
        // look for error message
        if (skipped.contains("scp:")) {
            throw new IOException("File not found");
        }
        long size = 0L;
        // read filesize in single digits
        while (true) {
            int length = streamIn.read(buffer);
            if (length < 0) {
                break;
            }
            if (buffer[0] == ' ') {
                break;
            }
            size = size * 10L + (buffer[0] - '0');
        }
        // skip filename
        skipTo(streamIn, '\n');
        // write '0'
        buffer[0] = 0;
        streamOut.write(buffer);
        streamOut.flush();
        return new SCPInputStream(size, streamIn, streamOut, channel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputStream openOutputStream() throws Exception {
        Session session = ((SSHConnection)getConnection()).getSession();
        return new SCPOutputStream(session, m_uri.getPath());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSize() throws Exception {
        String skipped;
        byte[] buffer = new byte[1];
        String path = m_uri.getPath();
        Session session = ((SSHConnection)getConnection()).getSession();
        ChannelExec channel = (ChannelExec)session.openChannel("exec");
        channel.setCommand("scp -f " + path);
        InputStream streamIn = channel.getInputStream();
        OutputStream streamOut = channel.getOutputStream();
        channel.connect();
        // write '0'
        buffer[0] = 0;
        streamOut.write(buffer);
        streamOut.flush();
        streamIn.read(buffer);
        if (buffer[0] != 0) {
            throw new IOException("SCP error");
        }
        // skip first token
        skipped = skipTo(streamIn, ' ');
        // look for error message
        if (skipped.contains("scp:")) {
            throw new IOException("File not found");
        }
        long size = 0L;
        // read filesize in single digits
        while (true) {
            int length = streamIn.read(buffer);
            if (length < 0) {
                break;
            }
            if (buffer[0] == ' ') {
                break;
            }
            size = size * 10L + (buffer[0] - '0');
        }
        streamIn.close();
        streamOut.close();
        channel.disconnect();
        return size;
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
    protected boolean usesConnection() {
        return true;
    }

    /**
     * Consumes all bytes from the input stream up to and including the given
     * character.
     * 
     * 
     * @param streamIn Stream to read from
     * @param character The character that will be searched for
     * @return All consumed characters (excluding the given character)
     * @throws IOException If the input stream does not contain the given
     *             character
     */
    private String skipTo(final InputStream streamIn, final char character)
            throws IOException {
        StringBuffer result = new StringBuffer();
        byte[] buffer = new byte[1];
        do {
            int length = streamIn.read(buffer, 0, 1);
            if (length < 0) {
                throw new IOException("Reached end of stream and found no '"
                        + character + "'");
            }
            result.append(new String(buffer));
        } while (buffer[0] != character);
        result.deleteCharAt(result.length() - 1);
        return result.toString();
    }

    private class SCPInputStream extends InputStream {

        private InputStream m_stream;

        private OutputStream m_outstream;

        private ChannelExec m_channel;

        private long m_bytesLeft;

        public SCPInputStream(final long bytesToRead,
                final InputStream inStream, final OutputStream outStream,
                final ChannelExec channel) {
            m_stream = inStream;
            m_outstream = outStream;
            m_channel = channel;
            m_bytesLeft = bytesToRead;
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
            byte[] buffer = new byte[1];
            m_stream.read(buffer);
            if (buffer[0] != 0) {
                throw new IOException("SCP error");
            }
            buffer[0] = 0;
            m_outstream.write(buffer);
            m_outstream.flush();
            m_stream.close();
            m_outstream.close();
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
        public synchronized void reset() throws IOException {
            m_stream.reset();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long skip(final long n) throws IOException {
            long result = m_bytesLeft < n ? m_bytesLeft : n;
            m_bytesLeft -= result;
            return m_stream.skip(n);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int read() throws IOException {
            byte[] b = new byte[1];
            read(b);
            return b[0];
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int read(final byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int read(final byte[] b, final int off, final int len)
                throws IOException {
            int result = -1;
            if (m_bytesLeft > 0L) {
                int length = len < b.length ? len : b.length;
                if (length < m_bytesLeft) {
                    length = b.length;
                } else {
                    length = (int)m_bytesLeft;
                }
                result = m_stream.read(b, 0, length);
                m_bytesLeft -= length;
            }
            return result;
        }

    }

    private class SCPOutputStream extends OutputStream {

        private OutputStream m_stream;

        private File m_file;

        private Session m_session;

        private String m_path;

        public SCPOutputStream(final Session session, final String path)
                throws Exception {
            m_session = session;
            m_path = path;
            m_file = File.createTempFile(FilenameUtils.getName(m_path), ".tmp");
            m_stream = new FileOutputStream(m_file);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void close() throws IOException {
            try {
                int length;
                m_stream.close();
                InputStream fileIn = new FileInputStream(m_file);
                long filesize = m_file.length();
                byte[] buffer = new byte[1];
                ChannelExec channel =
                        (ChannelExec)m_session.openChannel("exec");
                channel.setCommand("scp -t " + m_path);
                InputStream streamIn = channel.getInputStream();
                OutputStream streamOut = channel.getOutputStream();
                channel.connect();
                streamIn.read(buffer);
                if (buffer[0] != 0) {
                    throw new IOException("SCP error");
                }
                String command =
                        "C0644 " + filesize + " "
                                + FilenameUtils.getName(m_path) + "\n";
                streamOut.write(command.getBytes());
                streamOut.flush();
                streamIn.read(buffer);
                if (buffer[0] != 0) {
                    throw new IOException("SCP error");
                }
                while ((length = fileIn.read(buffer)) > 0) {
                    streamOut.write(buffer, 0, length);
                }
                // write '0'
                buffer[0] = 0;
                streamOut.write(buffer);
                streamOut.flush();
                streamIn.read(buffer);
                if (buffer[0] != 0) {
                    throw new IOException("SCP error");
                }
                fileIn.close();
                streamIn.close();
                streamOut.close();
                channel.disconnect();
            } catch (JSchException e) {
                throw new IOException(e);
            }
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
        public void write(final byte[] b, final int off, final int len)
                throws IOException {
            m_stream.write(b, off, len);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void write(final byte[] b) throws IOException {
            write(b, 0, b.length);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void write(final int b) throws IOException {
            byte[] bytes = new byte[1];
            bytes[0] = (byte)b;
            write(bytes);
        }

    }

}
