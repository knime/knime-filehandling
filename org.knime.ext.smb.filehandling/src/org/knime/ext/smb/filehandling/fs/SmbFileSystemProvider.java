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
 *   2021-03-05 (Alexander Bondaletov): created
 */
package org.knime.ext.smb.filehandling.fs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.connections.base.BaseFileSystemProvider;
import org.knime.filehandling.core.connections.base.attributes.BaseFileAttributes;
import org.knime.filehandling.core.defaultnodesettings.ExceptionUtil;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msfscc.fileinformation.FileAllInformation;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2CreateOptions;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.mssmb2.SMBApiException;
import com.hierynomus.protocol.commons.buffer.Buffer.BufferException;
import com.hierynomus.smbj.share.DiskEntry;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.share.File;

/**
 * File system provider for the {@link SmbFileSystem}.
 *
 * @author Alexander Bondaletov
 */
class SmbFileSystemProvider extends BaseFileSystemProvider<SmbPath, SmbFileSystem> {

    @Override
    protected SeekableByteChannel newByteChannelInternal(final SmbPath path, final Set<? extends OpenOption> options,
            final FileAttribute<?>... attrs) throws IOException {
        return new SmbSeekableFileChannel(path, options);
    }

    @Override
    protected void copyInternal(final SmbPath source, final SmbPath target, final CopyOption... options) throws IOException {
        if (FSFiles.isDirectory(source)) {
            if (!existsCached(target)) {
                createDirectory(target);
            }
        } else {
            copyFile(source, target);
        }
    }

    @SuppressWarnings("resource")
    private static void copyFile(final SmbPath source, final SmbPath target) throws IOException {
        DiskShare client = source.getFileSystem().getClient();
        File srcFile = null;
        File dstFile = null;
        try {
            srcFile = client.openFile(source.getSmbjPath(), EnumSet.of(AccessMask.GENERIC_READ), null,
                    EnumSet.of(SMB2ShareAccess.FILE_SHARE_READ), SMB2CreateDisposition.FILE_OPEN, null);
            dstFile = client.openFile(target.getSmbjPath(), EnumSet.of(AccessMask.GENERIC_WRITE), null,
                    EnumSet.of(SMB2ShareAccess.FILE_SHARE_WRITE), SMB2CreateDisposition.FILE_OVERWRITE_IF,
                    EnumSet.noneOf(SMB2CreateOptions.class));

            if (srcFile.getDiskShare() == dstFile.getDiskShare()) {
                srcFile.remoteCopyTo(dstFile);
            } else {
                copyByDownloading(source, target);
            }

        } catch (BufferException ex) {
            throw ExceptionUtil.wrapAsIOException(ex);
        } catch (SMBApiException ex) {
            throw SmbUtils.toIOE(ex, source.toString(), target.toString());
        } finally {
            if (srcFile != null) {
                srcFile.close();
            }
            if (dstFile != null) {
                dstFile.close();
            }
        }
    }

    private static void copyByDownloading(final SmbPath source, final SmbPath target) throws IOException {
        try (InputStream in = Files.newInputStream(source)) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Override
    protected void moveInternal(final SmbPath source, final SmbPath target, final CopyOption... options)
            throws IOException {
        try (DiskEntry file = openFileForRename(source)) {
            file.rename(target.getSmbjPath(), true);
        } catch (SMBApiException ex) {
            throw SmbUtils.toIOE(ex, source.toString(), target.toString());
        }
    }

    @SuppressWarnings("resource")
    private static DiskEntry openFileForRename(final SmbPath path) throws AccessDeniedException {
        String pathString = path.getSmbjPath();
        Set<AccessMask> accessMask = EnumSet.of(AccessMask.DELETE);
        Set<SMB2ShareAccess> shareAccess = EnumSet.of(SMB2ShareAccess.FILE_SHARE_DELETE);
        SMB2CreateDisposition createDisposition = SMB2CreateDisposition.FILE_OPEN;

        DiskShare client = path.getFileSystem().getClient();

        if (FSFiles.isDirectory(path)) {
            return client.openDirectory(pathString, accessMask, null, shareAccess, createDisposition, null);
        } else {
            return client.openFile(pathString, accessMask, null, shareAccess, createDisposition, null);
        }
    }

    @Override
    protected InputStream newInputStreamInternal(final SmbPath path, final OpenOption... options) throws IOException {
        return new SmbInputStream(path);
    }

    @Override
    protected OutputStream newOutputStreamInternal(final SmbPath path, final OpenOption... options) throws IOException {
        final Set<OpenOption> opts = new HashSet<>(Arrays.asList(options));
        return new SmbOutputStream(path, opts.contains(StandardOpenOption.APPEND));
    }

    @Override
    protected Iterator<SmbPath> createPathIterator(final SmbPath dir, final Filter<? super Path> filter) throws IOException {
        return new SmbPathIterator(dir, filter);
    }

    @SuppressWarnings("resource")
    @Override
    protected void createDirectoryInternal(final SmbPath dir, final FileAttribute<?>... attrs) throws IOException {
        DiskShare client = dir.getFileSystem().getClient();
        try {
            client.mkdir(dir.getSmbjPath());
        } catch (SMBApiException ex) {
            throw SmbUtils.toIOE(ex, dir.toString());
        }
    }

    @SuppressWarnings("resource")
    @Override
    protected BaseFileAttributes fetchAttributesInternal(final SmbPath path, final Class<?> type) throws IOException {
        DiskShare client = path.getFileSystem().getClient();
        try {
            FileAllInformation info = client.getFileInformation(path.getSmbjPath());
            return createAttributes(path, info);
        } catch (SMBApiException ex) {
            throw SmbUtils.toIOE(ex, path.toString());
        }
    }

    private static BaseFileAttributes createAttributes(final SmbPath path, final FileAllInformation fileInfo) {
        boolean isDirectory = fileInfo.getStandardInformation().isDirectory();
        FileTime createdAt = FileTime.fromMillis(fileInfo.getBasicInformation().getCreationTime().toEpochMillis());
        FileTime modifiedAt = FileTime.fromMillis(fileInfo.getBasicInformation().getChangeTime().toEpochMillis());
        FileTime accessedAt = FileTime.fromMillis(fileInfo.getBasicInformation().getLastAccessTime().toEpochMillis());
        long size = fileInfo.getStandardInformation().getEndOfFile();

        return new BaseFileAttributes(!isDirectory, path, modifiedAt, accessedAt, createdAt, size, false, false, null);
    }

    @Override
    protected void checkAccessInternal(final SmbPath path, final AccessMode... modes) throws IOException {
        // nothing to do
    }

    @SuppressWarnings("resource")
    @Override
    protected void deleteInternal(final SmbPath path) throws IOException {
        DiskShare client = path.getFileSystem().getClient();
        try {
            if (Files.isDirectory(path)) {
                client.rmdir(path.getSmbjPath(), false);
            } else {
                client.rm(path.getSmbjPath());
            }
        } catch (SMBApiException ex) {
            throw SmbUtils.toIOE(ex, path.toString());
        }
    }

}
