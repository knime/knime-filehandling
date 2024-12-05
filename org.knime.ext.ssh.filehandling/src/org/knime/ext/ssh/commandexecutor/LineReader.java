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
 *   2024-12-05 (jloescher): created
 */
package org.knime.ext.ssh.commandexecutor;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Objects;

import org.apache.sshd.common.util.buffer.BufferUtils;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.streamable.RowOutput;

abstract class LineReader {
    protected long m_row;

    public abstract void read(String line) throws InterruptedException; // NOSONAR

    public OutputStream asOutputStream(final Charset charset) {
        return new ToLinesOutputStream(this, charset);
    }

    static final class RowLineReader extends LineReader {
        final RowOutput m_output;

        public RowLineReader(final RowOutput output) {
            m_output = output;
        }

        @Override
        public void read(final String line) throws InterruptedException {
            final var cell = StringCell.StringCellFactory.create(line);
            m_output.push(new DefaultRow(RowKey.createRowKey(m_row), cell));
            m_row++;
        }
    }

    static final class LimitedLineReader extends LineReader {
        final StringBuilder m_string = new StringBuilder();
        final long m_limit;

        public LimitedLineReader(final long limit) {
            m_limit = limit;
        }

        @Override
        public void read(final String line) {
            if (m_row < m_limit) {
                m_string.append(line);
                m_string.append('\n');
            } else if (m_row == m_limit) {
                m_string.append("[...]\n");
                m_row++;
            }
        }
    }

    private static class ToLinesOutputStream extends OutputStream {
        private static final int EXPECTED_LINE_LENGTH = 128;

        final LineReader m_reader;
        final Charset m_charset;

        byte[] m_buffer;
        int m_writePos;

        boolean m_skipLF;
        boolean m_closed;

        public ToLinesOutputStream(final LineReader reader, final Charset charset) {
            m_reader = reader;
            this.m_charset = charset;
            m_buffer = new byte[EXPECTED_LINE_LENGTH];
        }

        @Override
        public void write(final int b) throws IOException {
            ensureOpen();
            if (b == '\r') {
                m_skipLF = true;
                writeLine(false);
                return;
            }
            if (b == '\n' && !m_skipLF) {
                writeLine(false);
            } else if (b != '\n') {
                ensureCapacity(m_writePos + 1);
                m_buffer[m_writePos] = (byte) (b & 0xFF);
                m_writePos++;
            }
            m_skipLF = false;
        }
        @Override
        public void write(final byte[] b, final int off, final int len) throws IOException {
            Objects.checkFromIndexSize(off, len, b.length);
            ensureOpen();
            var start = off;
            var currLen = len;
            if (m_writePos > 0) {
                start = writeWithBuffer(b, off, currLen);
                currLen = len - (start - off);
            }
            start = writeRemainingLines(b, start, currLen);
            currLen = len - (start - off);
            // rest to buffer
            if (currLen > 0) {
                ensureCapacity(m_writePos + currLen);
                System.arraycopy(b, start, m_buffer, m_writePos, currLen);
                m_writePos += currLen;
            }
        }

        private int writeWithBuffer(final byte[] b, final int off, final int len) throws IOException {
            var start = off;
            final var end = start + len;
            // first line break
            for (var curr = off; curr < end; curr++) {
                if (b[curr] == '\r') {
                    m_skipLF = true;
                    ensureCapacity(m_writePos + curr - off);
                    System.arraycopy(b, off, m_buffer, m_writePos, curr - off);
                    m_writePos += curr - off;
                    writeLine(false);
                    start = curr + 1;
                    return start;
                }
                if (b[curr] == '\n') {
                    if (!m_skipLF) {
                        ensureCapacity(m_writePos + curr - off);
                        System.arraycopy(b, off, m_buffer, m_writePos, curr - off);
                        m_writePos += curr - off;
                        writeLine(false);
                    }
                    m_skipLF = false;
                    start = curr + 1;
                    return start;
                }
                m_skipLF = false;
            }
            return start;
        }

        private int writeRemainingLines(final byte[] b, final int off, final int len) throws IOException {
            var start = off;
            // mid line break
            final var end = start + len;
            for (var curr = start; curr < end; curr++) {
                if (b[curr] == '\r') {
                    m_skipLF = true;
                    writeLine(b, start, curr - start);
                    start = curr + 1;
                    continue;
                }
                if (b[curr] == '\n') {
                    if (!m_skipLF) {
                        writeLine(b, start, curr - start);
                    }
                    start = curr + 1;
                }
                m_skipLF = false;
            }
            return start;
        }

        private void writeLine(final boolean skipEmpty) throws IOException {
            try {
                if (!skipEmpty || m_writePos > 0) {
                    m_reader.read(new String(m_buffer, 0, m_writePos, m_charset));
                    m_writePos = 0;
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IOException(ex);
            }
        }

        private void writeLine(final byte[] b, final int off, final int len) throws IOException {
            try {
                m_reader.read(new String(b, off, len, m_charset));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IOException(ex);
            }
        }

        @Override
        public void flush() throws IOException {
            // the Apache MINA API seems to flush the stream after each write but
            // we do not want to introduce line breaks so this flush is a no-op
        }

        @Override
        public void close() throws IOException {
            writeLine(true);
            m_closed = true;
        }

        private void ensureCapacity(final int length) {
            if (length > m_buffer.length) {
                final var newBuffer = new byte[BufferUtils.getNextPowerOf2(length)];
                System.arraycopy(m_buffer, 0, newBuffer, 0, m_buffer.length);
                m_buffer = newBuffer;
            }
        }

        private void ensureOpen() throws IOException {
            if (m_closed) {
                throw new IOException("Stream closed");
            }
        }
    }
}
