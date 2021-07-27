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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFileAttributes;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.net.ftp.FTPFile;
import org.knime.filehandling.core.connections.base.BaseFileSystemProvider;
import org.knime.filehandling.core.connections.base.FilterOutputStream;
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
    public FtpFileSystemProvider(final FtpFSConnectionConfig config) throws IOException {
        m_clientPool = new ClientPool(config);
        m_clientPool.start();
    }

    @Override
    protected SeekableByteChannel newByteChannelInternal(final FtpPath path, final Set<? extends OpenOption> options,
            final FileAttribute<?>... attrs) throws IOException {
        return new FtpSeekableByteChannel(path, options);
    }

    @SuppressWarnings("resource")
    @Override
    protected void moveInternal(final FtpPath source, final FtpPath target, final CopyOption... options)
            throws IOException {
        // first of all get source metadata because it is cached yet
        final FTPFile sourceMeta = ((FtpFileAttributes) readAttributes(source, PosixFileAttributes.class))
                .getMetadata();

        if (exists(target)) {
            FTPFile targetMeta = ((FtpFileAttributes) readAttributes(target, PosixFileAttributes.class)).getMetadata();
            // should remove already existing file
            deleteInternal(target, targetMeta);
        }

        // renaming
        invokeWithResource(c -> {
            c.rename(source.toString(), target.toString());
            return null;
        });

        // if not any exceptions thrown should clear the cache deeply
        getFileSystemInternal().removeFromAttributeCacheDeep(source);

        // correct source metadata and cache it for target
        sourceMeta.setTimestamp(new GregorianCalendar());
        sourceMeta.setName(target.getFileName().toString());
        cacheAttributes(target, new FtpFileAttributes(target, sourceMeta));
    }

    @Override
    protected void copyInternal(final FtpPath source, final FtpPath target, final CopyOption... options)
            throws IOException {
        // first of all get source metadata because it is cached yet
        final FTPFile sourceMeta = ((FtpFileAttributes) readAttributes(source, PosixFileAttributes.class))
                .getMetadata();

        if (exists(target)) {
            FTPFile targetMeta = ((FtpFileAttributes) readAttributes(target, PosixFileAttributes.class)).getMetadata();
            // should remove already existing file
            deleteInternal(target, targetMeta);
        }

        if (sourceMeta.getType() == FTPFile.DIRECTORY_TYPE) {
            // just create new folder
            createDirectoryInternal(target);
        } else {
            // there is not a simple way to copy the file on server side.
            // just need to load it locally and push then to server again.
            // some FTP servers can support proprietary extension for it
            // but it is not universal approach to use
            Path tmp = Files.createTempFile("knime-ftp-", ".tmp");
            try {
                // copy file from remote to temporary local file
                try (OutputStream out = Files.newOutputStream(tmp)) {
                    copyFromRemote(source.toString(), out);
                }

                // copy temporary local file to FTP server
                try (InputStream in = Files.newInputStream(tmp)) {
                    copyToRemote(target.toString(), in);
                }
            } finally {
                Files.delete(tmp);
            }
        }
    }

    @SuppressWarnings("resource")
    @Override
    protected InputStream newInputStreamInternal(final FtpPath path, final OpenOption... options) throws IOException {
        final FtpClientResource resource = takeResource();
        final InputStream stream = resource.get().getFileContentAsStream(path.toString());

        return new FilterInputStream(stream) {
            boolean m_isOpen = true;

            @Override
            public synchronized void close() throws IOException {
                if (m_isOpen) {
                    // we have to protect the underlying stream from being closed multiple times
                    // as closing it will read a server response from the control connection
                    m_isOpen = false;

                    try {
                        super.close();
                    } finally {
                        releaseResource(resource);
                    }
                }
            }
        };
    }

    @SuppressWarnings("resource")
    @Override
    protected OutputStream newOutputStreamInternal(final FtpPath path, final OpenOption... options) throws IOException {
        final Set<OpenOption> opts = new HashSet<>(Arrays.asList(options));

        final FtpClientResource resource = takeResource();

        final OutputStream out;
        if (opts.contains(StandardOpenOption.APPEND)) {
            out = resource.get().openForAppend(path.toString());
        } else {
            out = resource.get().openForRewrite(path.toString());
        }

        return new FilterOutputStream(out) {
            boolean m_isOpen = true;

            @Override
            public synchronized void close() throws IOException {
                if (m_isOpen) {
                    // we have to protect the underlying stream from being closed multiple times
                    // as closing it will read a server response from the control connection
                    m_isOpen = false;

                    try {
                        flush();
                        super.close();
                    } finally {
                        releaseResource(resource);
                    }
                }
            }
        };
    }

    @Override
    protected Iterator<FtpPath> createPathIterator(final FtpPath dir, final Filter<? super Path> filter)
            throws IOException {
        final FTPFile[] ftpFiles = invokeWithResource(c -> c.listFiles(dir.toString()));

        final List<FtpPath> files = new LinkedList<>();
        for (FTPFile ftpFile : ftpFiles) {
            final FtpPath path = dir.resolve(ftpFile.getName());
            files.add(path);
            // cache file attributes
            cacheAttributes(path, new FtpFileAttributes(path, ftpFile));
        }
        return new FtpPathIterator(dir, files, filter);
    }

    @Override
    protected void createDirectoryInternal(final FtpPath dir, final FileAttribute<?>... attrs)
            throws IOException {
        invokeWithResource(c -> {
            c.mkdir(dir.toString());
            return null;
        });
    }

    @Override
    protected BaseFileAttributes fetchAttributesInternal(final FtpPath path, final Class<?> type) throws IOException {
        FTPFile meta = invokeWithResource(c -> c.getFileInfo(path));
        return new FtpFileAttributes(path, meta);
    }

    @Override
    protected void checkAccessInternal(final FtpPath path, final AccessMode... modes) throws IOException {
        // nothing for now
    }

    @Override
    protected void deleteInternal(final FtpPath path) throws IOException {
        FTPFile file = ((FtpFileAttributes) readAttributes(path, PosixFileAttributes.class)).getMetadata();
        deleteInternal(path, file);
    }

    /**
     * @param path
     *            path to delete.
     * @param meta
     *            metadata.
     * @throws IOException
     */
    void deleteInternal(final FtpPath path, final FTPFile meta) throws IOException {
        invokeWithResource(c -> {
            if (meta.getType() == FTPFile.DIRECTORY_TYPE) {
                c.deleteDirectory(path.toString());
            } else {
                c.deleteFile(path.toString());
            }
            return null;
        });
    }

    @Override
    public void setAttribute(final Path path, final String name, final Object value,
            final LinkOption... options)
            throws IOException {
        // ignored now
    }

    @Override
    protected boolean isHiddenInternal(final FtpPath path) throws IOException {
        return path != null && !path.isRoot() && path.getFileName().toString().startsWith(".");
    }

    /**
     * @param path
     *            source remote path.
     * @param out
     *            output stream to copy.
     * @throws IOException
     */
    void copyFromRemote(final String path, final OutputStream out) throws IOException {
        invokeWithResource(c -> {
            c.getFileContent(path, out);
            return null;
        });
    }

    /**
     * @param path
     *            file path.
     * @param in
     *            file content.
     * @throws IOException
     */
    void copyToRemote(final String path, final InputStream in) throws IOException {
        invokeWithResource(c -> {
            c.createFile(path, in);
            return null;
        });
    }

    /**
     * @param func
     *            function to invoke with resource.
     * @return invocation result.
     */
    private <R> R invokeWithResource(final WithClientInvocable<R> func)
            throws IOException {

        final FtpClientResource resource = takeResource();
        try {
            return func.invoke(resource.get());
        } finally {
            releaseResource(resource);
        }
    }

    @SuppressWarnings("resource")
    void cacheAttributes(final FtpPath path, final BaseFileAttributes attr) {
        getFileSystemInternal().addToAttributeCache(path, attr);
    }

    /**
     * @param resource
     *            resource to release.
     */
    private void releaseResource(final FtpClientResource resource) {
        m_clientPool.release(resource);
    }

    /**
     * @return resource.
     * @throws IOException
     */
    private FtpClientResource takeResource() throws IOException {
        final FtpClientResource resource;
        try {
            resource = m_clientPool.take();
        } catch (InterruptedException ex) { // NOSONAR there is better place to catch this exception.
            throw ExceptionUtil.wrapAsIOException(ex);
        }
        return resource;
    }

    /**
     * Closes provider. Stops clients pool.
     */
    void prepareClose() {
        m_clientPool.stop();
    }
}
