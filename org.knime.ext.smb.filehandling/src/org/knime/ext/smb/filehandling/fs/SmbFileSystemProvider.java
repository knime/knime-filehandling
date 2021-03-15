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
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.util.Iterator;
import java.util.Set;

import org.knime.ext.smb.filehandling.SmbUtils;
import org.knime.filehandling.core.connections.base.BaseFileSystemProvider;
import org.knime.filehandling.core.connections.base.attributes.BaseFileAttributes;

import com.hierynomus.msfscc.fileinformation.FileAllInformation;
import com.hierynomus.mssmb2.SMBApiException;
import com.hierynomus.smbj.share.DiskShare;

/**
 * File system provider for the {@link SmbFileSystem}.
 *
 * @author Alexander Bondaletov
 */
public class SmbFileSystemProvider extends BaseFileSystemProvider<SmbPath, SmbFileSystem> {
    /**
     * Samba URI scheme.
     */
    public static final String FS_TYPE = "smb";

    @Override
    protected SeekableByteChannel newByteChannelInternal(final SmbPath path, final Set<? extends OpenOption> options,
            final FileAttribute<?>... attrs) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void copyInternal(final SmbPath source, final SmbPath target, final CopyOption... options) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    protected InputStream newInputStreamInternal(final SmbPath path, final OpenOption... options) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected OutputStream newOutputStreamInternal(final SmbPath path, final OpenOption... options) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected Iterator<SmbPath> createPathIterator(final SmbPath dir, final Filter<? super Path> filter) throws IOException {
        return new SmbPathIterator(dir, filter);
    }

    @SuppressWarnings("resource")
    @Override
    protected void createDirectoryInternal(final SmbPath dir, final FileAttribute<?>... attrs) throws IOException {
        DiskShare client = dir.getFileSystem().getClient();
        client.mkdir(dir.getSmbjPath());
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
        long size = fileInfo.getStandardInformation().getAllocationSize();

        return new BaseFileAttributes(!isDirectory, path, modifiedAt, accessedAt, createdAt, size, false, false, null);
    }

    @Override
    protected void checkAccessInternal(final SmbPath path, final AccessMode... modes) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    protected void deleteInternal(final SmbPath path) throws IOException {
        // TODO Auto-generated method stub

    }

}
