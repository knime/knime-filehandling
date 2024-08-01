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
 *   2022-04-27 (Dragan Keselj): created
 */
package org.knime.archive.zip.filehandling.fs;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.io.IOUtils;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.connections.base.BaseFileSystemProvider;
import org.knime.filehandling.core.connections.base.attributes.BaseFileAttributes;
import org.knime.filehandling.core.connections.workflowaware.Entity;
import org.knime.filehandling.core.connections.workflowaware.WorkflowAware;
import org.knime.filehandling.core.util.TempPathCloseable;

/**
 * File system provider for the {@link ArchiveZipFileSystem}.
 *
 * @author Dragan Keselj, KNIME GmbH
 */
class ArchiveZipFileSystemProvider extends BaseFileSystemProvider<ArchiveZipPath, ArchiveZipFileSystem> implements WorkflowAware {

    ArchiveZipFileSystemProvider() throws IOException {

    }

    @Override
    protected SeekableByteChannel newByteChannelInternal(final ArchiveZipPath path,
        final Set<? extends OpenOption> options, final FileAttribute<?>... attrs) throws IOException {
        return new ArchiveZipSeekableFileChannel(path, options);
    }

    @Override
    protected void copyInternal(final ArchiveZipPath source, final ArchiveZipPath target, final CopyOption... options)
        throws IOException {
        throw new AccessDeniedException("Copying files is not supported");
    }

    @Override
    protected void moveInternal(final ArchiveZipPath source, final ArchiveZipPath target, final CopyOption... options)
        throws IOException {
        throw new AccessDeniedException("Moving files is not supported");
    }

    @SuppressWarnings("resource")
    @Override
    protected InputStream newInputStreamInternal(final ArchiveZipPath path, final OpenOption... options)
        throws IOException {
        final ZipArchiveEntry entry = getEntry(path);
        return getFileSystemInternal().getZipFile().getInputStream(entry);
    }

    @Override
    protected OutputStream newOutputStreamInternal(final ArchiveZipPath path, final OpenOption... options)
        throws IOException {
        throw new AccessDeniedException("Writing into the zip file is not supported");
    }

    @Override
    protected Iterator<ArchiveZipPath> createPathIterator(final ArchiveZipPath dir, final Filter<? super Path> filter)
        throws IOException {
        return new ArchiveZipPathIterator(dir, filter);
    }

    @Override
    protected void createDirectoryInternal(final ArchiveZipPath dir, final FileAttribute<?>... attrs)
        throws IOException {
        throw new AccessDeniedException("Creating directories is not supported");
    }

    @SuppressWarnings("resource")
    @Override
    protected BaseFileAttributes fetchAttributesInternal(final ArchiveZipPath path, final Class<?> type)
        throws IOException {
        if (path.isRoot()) {
            return new BaseFileAttributes(false, path, FileTime.fromMillis(0), FileTime.fromMillis(0),
                FileTime.fromMillis(0), getFileSystemInternal().getZipFile().getLength(), false, false, null);
        } else {
            final ZipArchiveEntry entry = getEntry(path);
            FileTime lastModifiedTime = safeTime(entry.getLastModifiedTime(), FileTime.fromMillis(0));
            FileTime lastAccessTime = safeTime(entry.getLastAccessTime(), lastModifiedTime);
            FileTime creationTime = safeTime(entry.getCreationTime(), lastModifiedTime);
            return new BaseFileAttributes(!entry.isDirectory(), path, lastModifiedTime, lastAccessTime, creationTime,
                entry.getSize(), false, false, null);
        }
    }

    @SuppressWarnings("resource")
    private ZipArchiveEntry getEntry(final ArchiveZipPath path) throws IOException {
        var entry = getFileSystemInternal().getEntry(path);
        if (entry != null && !getFileSystemInternal().getZipFile().canReadEntryData(entry)) {
            throw new AccessDeniedException(path.toString());
        }
        if (entry == null) {
            // entry can be null for .knwf archives
            final var entryName = getFileSystemInternal().getEntryName(path);
            entry = new ZipArchiveEntry(entryName);
        }
        return entry;
    }

    private static FileTime safeTime(final FileTime time, final FileTime defaultTime) {
        if (time != null) {
            return time;
        }
        return defaultTime != null ? defaultTime : FileTime.fromMillis(0);
    }

    @Override
    protected void checkAccessInternal(final ArchiveZipPath path, final AccessMode... modes) throws IOException {
        // FIXME: check if you can perform the requested type of access on the file.  //NOSONAR
        // In most cases however there is nothing useful you can do here.
    }

    @Override
    protected void deleteInternal(final ArchiveZipPath path) throws IOException {
        throw new AccessDeniedException("Deleting files is not supported");
    }

    @Override
    protected boolean exists(final ArchiveZipPath path) throws IOException { //NOSONAR
        return super.exists(path);
    }

    @Override
    public Entity getEntityOf(final FSPath path) throws IOException {
        checkFileSystemOpenAndNotClosing();
        var checkedPath = checkCastAndAbsolutizePath(path);
        return getEntity(checkedPath).orElseThrow(() -> new NoSuchFileException(path.toString()));
    }

    @SuppressWarnings("resource")// the file system has to stay open for further use
    private Optional<Entity> getEntity(final ArchiveZipPath path) throws IOException {
        return getFileSystemInternal().getEntity(path);
    }

    @Override
    public void deployWorkflow(final Path source, final FSPath dest, final boolean overwrite, final boolean attemptOpen) throws IOException {
        throw new AccessDeniedException("Deploying workflows is not supported");
    }

    @Override
    public TempPathCloseable toLocalWorkflowDir(final FSPath workflowToRead) throws IOException {
        final var tempDir = Files.createTempDirectory("zipped-KNIME-workflow");
        final var path = workflowToRead.toAbsolutePath();

        try(var fs = getFileSystemInternal(); var zipFile = fs.getZipFile()){

            final var entries = zipFile.getEntries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                // skip entries that use features that are unsupported by commons.io
                if (!zipFile.canReadEntryData(entry)) {
                    continue;
                }
                // only extract subdirectory that contains the workflow
                final var entryPath = Path.of(entry.getName());
                if (path.startsWith(entryPath)) {
                    continue;
                }
                var targetPath = tempDir.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(targetPath);
                } else {
                    Files.createDirectories(targetPath.getParent());
                    try (var in = new BufferedInputStream(zipFile.getInputStream(entry));
                            var out = new FileOutputStream(targetPath.toFile())) {
                        // FIXME ChannelClosedException: null
                        IOUtils.copy(in, out);
                    }

                }
            }
        }
        return new TempPathCloseable(tempDir);
    }

    @Override
    public Optional<String> getMountID() {
        return Optional.empty();
    }
}
