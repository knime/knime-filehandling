/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   2023-02-17 (Alexander Bondaletov): created
 */
package org.knime.ext.box.filehandling.fs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.box.sdk.BoxAPIException;
import com.box.sdk.BoxFile;

/**
 * {@link InputStream} stream implementation to read files from Box. Reading is
 * performed in blocks.
 *
 * @author Alexander Bondaletov, Redfield SE
 */
public class BoxInputStream extends InputStream {
    private static final int RANGE_NOT_SATISFIABLE_ERROR_CODE = 416;
    private static final int BLOCK_SIZE = 1024 * 1024;

    private final BoxPath m_path;
    private final BoxFile m_boxFile;

    private final ByteArrayOutputStream m_downloadBufferStream;

    private long m_nextOffset;
    private byte[] m_buffer;
    private int m_bufferOffset;
    private boolean m_lastBlock;

    /**
     * @param path
     *            The file to read.
     * @throws IOException
     *
     */
    @SuppressWarnings("resource")
    public BoxInputStream(final BoxPath path) throws IOException {
        m_path = path;
        m_boxFile = ((BoxFileSystemProvider) path.getFileSystem().provider()).getBoxFile(path);
        m_downloadBufferStream = new ByteArrayOutputStream(BLOCK_SIZE);
        m_buffer = new byte[0];
        m_bufferOffset = 0;
        m_nextOffset = 0;
        m_lastBlock = false;
        readNextBlockIfNecessary();
    }

    private void readNextBlockIfNecessary() throws IOException {
        if (!m_lastBlock && m_bufferOffset == m_buffer.length) {
            m_buffer = fetchNextBlock();
            m_bufferOffset = 0;
            m_nextOffset += m_buffer.length;
            m_lastBlock = m_buffer.length < BLOCK_SIZE;
        }
    }

    private byte[] fetchNextBlock() throws IOException {
        try {
            m_boxFile.downloadRange(m_downloadBufferStream, m_nextOffset, m_nextOffset + BLOCK_SIZE);
            var retValue = m_downloadBufferStream.toByteArray();
            m_downloadBufferStream.reset();
            return retValue;
        } catch (BoxAPIException ex) {
            if (ex.getResponseCode() == RANGE_NOT_SATISFIABLE_ERROR_CODE) {
                // the file is empty
                return new byte[0];
            } else {
                throw BoxUtils.toIOE(ex, m_path.toString());
            }
        }
    }

    @Override
    public int read() throws IOException {
        readNextBlockIfNecessary();

        if (m_bufferOffset == m_buffer.length) {
            return -1;
        } else {
            final int indexToRead = m_bufferOffset;
            m_bufferOffset++;
            // return byte as int between 0 and 255
            return m_buffer[indexToRead] & 0xff;
        }
    }

    @Override
    public int read(final byte[] dest, final int off, final int len) throws IOException {
        readNextBlockIfNecessary();

        if (m_bufferOffset == m_buffer.length) {
            return -1;
        } else {
            final int bytesToRead = Math.min(len, m_buffer.length - m_bufferOffset);
            System.arraycopy(m_buffer, m_bufferOffset, dest, off, bytesToRead);
            m_bufferOffset += bytesToRead;
            return bytesToRead;
        }
    }

    @Override
    public void close() throws IOException {
        m_downloadBufferStream.close();
        m_buffer = null;
    }
}
