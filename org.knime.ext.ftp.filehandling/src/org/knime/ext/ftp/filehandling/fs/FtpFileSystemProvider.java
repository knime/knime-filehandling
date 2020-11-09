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
 *   2020-09-30 (Vyacheslav Soldatov): created
 */
package org.knime.ext.ftp.filehandling.fs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFileAttributes;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.net.ftp.FTPFile;
import org.knime.filehandling.core.connections.base.BaseFileSystemProvider;
import org.knime.filehandling.core.connections.base.attributes.BaseFileAttributes;
import org.knime.filehandling.core.defaultnodesettings.ExceptionUtil;

/**
 * File system provider for {@link FtpFileSystem}.
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
public class FtpFileSystemProvider extends BaseFileSystemProvider<FtpPath, FtpFileSystem> {
    private final ClientPool m_clientPool;

    /**
     * @param config
     *            FTP connection configuration.
     * @throws IOException
     */
    public FtpFileSystemProvider(final FtpConnectionConfiguration config) throws IOException {
        m_clientPool = new ClientPool(config);
        m_clientPool.start();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected SeekableByteChannel newByteChannelInternal(final FtpPath path, final Set<? extends OpenOption> options,
            final FileAttribute<?>... attrs) throws IOException {
        throw new IOException("TODO");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void copyInternal(final FtpPath source, final FtpPath target, final CopyOption... options)
            throws IOException {
        throw new IOException("TODO");
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("resource")
    @Override
    protected InputStream newInputStreamInternal(final FtpPath path, final OpenOption... options) throws IOException {
        final Set<OpenOption> opts = new HashSet<>(Arrays.asList(options));
        return Channels.newInputStream(newByteChannelInternal(path, opts));
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("resource")
    @Override
    protected OutputStream newOutputStreamInternal(final FtpPath path, final OpenOption... options) throws IOException {
        final Set<OpenOption> opts = new HashSet<>(Arrays.asList(options));
        return Channels.newOutputStream(newByteChannelInternal(path, opts));
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("resource")
    @Override
    protected Iterator<FtpPath> createPathIterator(final FtpPath dir, final Filter<? super Path> filter)
            throws IOException {
        final FTPFile[] ftpFiles = invokeWithResource(c -> c.listFiles(dir.toString()));

        final List<FtpPath> files = new LinkedList<>();
        for (FTPFile ftpFile : ftpFiles) {
            final FtpPath path = dir.resolve(ftpFile.getName());
            files.add(path);
            // cache file attributes
            getFileSystemInternal().addToAttributeCache(path, new FtpFileAttributes(path, ftpFile));
        }
        return new FtpPathIterator(dir, files, filter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createDirectoryInternal(final FtpPath dir, final FileAttribute<?>... attrs)
            throws IOException {
        invokeWithResource(c -> {
            c.mkdir(dir.toString());
            return null;
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BaseFileAttributes fetchAttributesInternal(final FtpPath path, final Class<?> type) throws IOException {
        FTPFile meta = invokeWithResource(c -> c.getFileInfo(path));
        return new FtpFileAttributes(path, meta);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void checkAccessInternal(final FtpPath path, final AccessMode... modes) throws IOException {
        // nothing for now
    }

    @Override
    protected void deleteInternal(final FtpPath path) throws IOException {
        FTPFile file = ((FtpFileAttributes) readAttributes(path, PosixFileAttributes.class)).getMetadata();
        final boolean isDirectory = file.getType() == FTPFile.DIRECTORY_TYPE;
        if (isDirectory && !isEmptyFolder(path)) {
            throw new DirectoryNotEmptyException(path.toString());
        }

        invokeWithResource(c -> {
            if (isDirectory) {
                c.deleteDirectory(path.toString());
            } else {
                c.deleteFile(path.toString());
            }
            return null;
        });
    }

    private boolean isEmptyFolder(final FtpPath path) throws IOException {
        FTPFile[] files = invokeWithResource(c -> c.listFiles(path.toString()));
        return files.length == 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAttribute(final Path path, final String name, final Object value,
            final LinkOption... options)
            throws IOException {
        throw new IOException("TODO");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getScheme() {
        return FtpFileSystem.FS_TYPE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isHiddenInternal(final FtpPath path) throws IOException {
        return path != null && path.getFileName().toString().startsWith(".");
    }

    /**
     * @param func
     *            function to invoke with resource.
     * @return invocation result.
     */
    private <R> R invokeWithResource(final WithClientInvocable<R> func)
            throws IOException {

        FtpClientResource resource;
        try {
            resource = m_clientPool.take();
        } catch (InterruptedException ex) { // NOSONAR there is better place to catch this exception.
            throw ExceptionUtil.wrapAsIOException(ex);
        }

        try {
            return func.invoke(resource.get());
        } finally {
            m_clientPool.release(resource);
        }
    }

    /**
     * Closes provider. Stops clients pool.
     */
    void prepareClose() {
        m_clientPool.stop();
    }
}
