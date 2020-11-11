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
 *   2020-10-07 (Vyacheslav Soldatov): created
 */
package org.knime.ext.ftp.filehandling.fs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFileAttributes;
import java.util.Set;

import org.apache.commons.net.ftp.FTPFile;
import org.knime.filehandling.core.connections.base.TempFileSeekableByteChannel;

/**
 * FTP implementation of {@link SeekableByteChannel}
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
public class FtpSeekableByteChannel extends TempFileSeekableByteChannel<FtpPath> {

    /**
     * @param file
     *            file to streaming.
     * @param options
     *            open file options.
     * @throws IOException
     */
    public FtpSeekableByteChannel(final FtpPath file, final Set<? extends OpenOption> options)
            throws IOException {
        super(file, options);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copyFromRemote(final FtpPath path, final Path tempFile) throws IOException {
        FTPFile meta = fetchAttributes(path).getMetadata();
        if (meta.getType() != FTPFile.FILE_TYPE) {
            throw new IOException("Not a file: " + path);
        }

        // copy content from FTP to tmp file
        try (OutputStream out = Files.newOutputStream(tempFile)) {
            getProvider(path).copyFromRemote(path.toString(), out);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copyToRemote(final FtpPath path, final Path tempFile) throws IOException {
        FtpFileAttributes attr = null;
        try {
            attr = fetchAttributes(path);
        } catch (IOException ex) { // NOSONAR it is expected if not file exists.
            // nothing
        }

        final FtpFileSystemProvider provider = getProvider(path);
        if (attr != null) {
            // should remove previous file
            provider.deleteInternal(path, attr.getMetadata());
        }

        try (InputStream in = Files.newInputStream(tempFile, StandardOpenOption.READ)) {
            provider.copyToRemote(path.toString(), in);
        }
    }

    private static FtpFileAttributes fetchAttributes(final FtpPath path) throws IOException {
        FtpFileSystemProvider provider = getProvider(path);
        return (FtpFileAttributes) provider.readAttributes(path, PosixFileAttributes.class);
    }

    @SuppressWarnings("resource")
    private static FtpFileSystemProvider getProvider(final FtpPath path) {
        return path.getFileSystem().provider();
    }
}
