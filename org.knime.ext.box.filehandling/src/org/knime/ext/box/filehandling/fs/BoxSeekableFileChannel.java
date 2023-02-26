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
 *   2023-02-17 (Alexander Bondaletov, Redfield SE): created
 */
package org.knime.ext.box.filehandling.fs;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Set;

import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.connections.base.TempFileSeekableByteChannel;

import com.box.sdk.BoxAPIException;
import com.box.sdk.BoxFile;
import com.box.sdk.BoxFolder;

/**
 * {@link TempFileSeekableByteChannel} implementation for the Box file system.
 *
 * @author Alexander Bondaletov, Redfield SE
 */
public class BoxSeekableFileChannel extends TempFileSeekableByteChannel<BoxPath> {

    // Maximum allowed file size for a simple upload is 50MB and minimum allowed
    // file size for a multipart upload is 20MB
    private static final long SIMPLE_UPLOAD_SIZE_THRESHOLD = 30L * 1024 * 1024; // 30MB

    private static final Set<OpenOption> WRITE_OPTIONS = Set.of(StandardOpenOption.CREATE,
            StandardOpenOption.CREATE_NEW, StandardOpenOption.APPEND, StandardOpenOption.WRITE);

    private static final String PERMISSIONS_FIELD = "permissions";

    /**
     * @param file
     *            The file for the channel.
     * @param options
     *            Open options.
     * @throws IOException
     */
    protected BoxSeekableFileChannel(final BoxPath file, final Set<? extends OpenOption> options) throws IOException {
        super(file, options);

        if (!Collections.disjoint(options, WRITE_OPTIONS)) {
            checkWritePermissions(file);
        }
    }

    private static void checkWritePermissions(final BoxPath file) throws IOException {
        try {
            var canUpload = FSFiles.exists(file) ? canUploadNewVersion(file) : canUploadToFolder(file.getParent());

            if (!canUpload) {
                throw new AccessDeniedException(file.toString(), null, "Insufficient permissions");
            }
        } catch (BoxAPIException ex) {
            throw BoxUtils.toIOE(ex, file.toString());
        }

    }

    @SuppressWarnings("resource")
    private static boolean canUploadToFolder(final BoxPath dir) throws IOException {
        var boxFolder = ((BoxFileSystemProvider) dir.getFileSystem().provider()).getBoxFolder(dir);
        var info = boxFolder.getInfo(PERMISSIONS_FIELD);
        return info.getPermissions().contains(BoxFolder.Permission.CAN_UPLOAD);
    }

    @SuppressWarnings("resource")
    private static boolean canUploadNewVersion(final BoxPath file) throws IOException {
        var boxFolder = ((BoxFileSystemProvider) file.getFileSystem().provider()).getBoxFile(file);
        var info = boxFolder.getInfo(PERMISSIONS_FIELD);
        return info.getPermissions().contains(BoxFile.Permission.CAN_UPLOAD);
    }

    @Override
    public void copyFromRemote(final BoxPath remoteFile, final Path tempFile) throws IOException {
        Files.copy(remoteFile, tempFile);
    }

    @Override
    public void copyToRemote(final BoxPath remoteFile, final Path tempFile) throws IOException {
        var size = Files.size(tempFile);
        try (var in = Files.newInputStream(tempFile)) {
            if (Files.exists(remoteFile)) {
                uploadNewVersion(remoteFile, in, size);
            } else {
                uploadNewFile(remoteFile, in, size);
            }
        } catch (BoxAPIException ex) {
            throw BoxUtils.toIOE(ex, remoteFile.toString());
        }
    }

    @SuppressWarnings("resource")
    private static void uploadNewVersion(final BoxPath remoteFile, final InputStream in, final long size)
            throws IOException {
        var boxFile = ((BoxFileSystemProvider) remoteFile.getFileSystem().provider()).getBoxFile(remoteFile);

        if (size < SIMPLE_UPLOAD_SIZE_THRESHOLD) {
            boxFile.uploadNewVersion(in);
        } else {
            try {
                boxFile.uploadLargeFile(in, size);
            } catch (InterruptedException ex) {// NOSONAR rethrown as InterruptedIOException
                throw (IOException) new InterruptedIOException().initCause(ex);
            }
        }
    }

    @SuppressWarnings("resource")
    private static void uploadNewFile(final BoxPath remoteFile, final InputStream in, final long size)
            throws IOException {
        var name = remoteFile.getFileName().toString();
        var boxFolder = ((BoxFileSystemProvider) remoteFile.getFileSystem().provider())
                .getBoxFolder(remoteFile.getParent());

        if (size < SIMPLE_UPLOAD_SIZE_THRESHOLD) {
            boxFolder.uploadFile(in, name);
        } else {
            try {
                boxFolder.uploadLargeFile(in, name, size);
            } catch (InterruptedException ex) {// NOSONAR rethrown as InterruptedIOException
                throw (IOException) new InterruptedIOException().initCause(ex);
            }
        }
    }
}
