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
 *   2023-02-14 (Alexander Bondaletov): created
 */
package org.knime.ext.box.filehandling.fs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.util.Iterator;
import java.util.Set;

import org.knime.filehandling.core.connections.base.BaseFileSystemProvider;
import org.knime.filehandling.core.connections.base.attributes.BaseFileAttributes;

import com.box.sdk.BoxAPIException;
import com.box.sdk.BoxFile;
import com.box.sdk.BoxFolder;

/**
 * File system provider for the {@link BoxFileSystem}.
 *
 * @author Alexander Bondaletov, Redfield SE
 */
class BoxFileSystemProvider extends BaseFileSystemProvider<BoxPath, BoxFileSystem> {

    static final String[] REQUIRED_FIELDS = new String[] { "name", "modified_at", "created_at", "size" };

    @Override
    protected SeekableByteChannel newByteChannelInternal(final BoxPath path, final Set<? extends OpenOption> options,
            final FileAttribute<?>... attrs) throws IOException {
        return new BoxSeekableFileChannel(path, options);
    }

    @Override
    protected void copyInternal(final BoxPath source, final BoxPath target, final CopyOption... options) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    protected InputStream newInputStreamInternal(final BoxPath path, final OpenOption... options) throws IOException {
        return new BoxInputStream(path);
    }

    @Override
    protected OutputStream newOutputStreamInternal(final BoxPath path, final OpenOption... options) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected Iterator<BoxPath> createPathIterator(final BoxPath dir, final Filter<? super Path> filter)
            throws IOException {
        return new BoxPathIterator(dir, filter);
    }

    @Override
    protected void createDirectoryInternal(final BoxPath dir, final FileAttribute<?>... attrs) throws IOException {
        var boxFolder = getBoxFolder(dir.getParent());
        try {
            boxFolder.createFolder(dir.getFileName().toString());
        } catch (BoxAPIException ex) {
            throw BoxUtils.toIOE(ex, dir.toString());
        }
    }

    @SuppressWarnings("resource")
    @Override
    protected BaseFileAttributes fetchAttributesInternal(final BoxPath path, final Class<?> type) throws IOException {
        if (path.isRoot()) {
            return new BoxFileAttributes(path);
        }

        var parentPath = path.getParent();
        var boxFolder = getBoxFolder(parentPath);

        try {
            for (final var info : boxFolder.getChildren(REQUIRED_FIELDS)) {
                var itemPath = (BoxPath) parentPath.resolve(info.getName());
                var attrs = new BoxFileAttributes(itemPath, info);

                if (info.getName().equals(path.getFileName().toString())) {
                    // BoxItemIterator uses pagination under the hood, so it is better to break
                    // early in order to avoid making unnecessary requests
                    return attrs;
                }

                // cache attributes for items that already fetched anyway
                path.getFileSystem().addToAttributeCache(itemPath, attrs);
            }
        } catch (BoxAPIException ex) {
            throw BoxUtils.toIOE(ex, path.toString());
        }

        throw new NoSuchFileException(path.toString());
    }

    @Override
    protected void checkAccessInternal(final BoxPath path, final AccessMode... modes) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    protected void deleteInternal(final BoxPath path) throws IOException {
        // TODO Auto-generated method stub
    }

    /**
     * Returns a {@link BoxFolder} object corresponding to a given path. Throws an
     * exception if the path does not represent an existing directory.
     *
     * @return The {@link BoxFolder} object.
     * @throws IOException
     */
    @SuppressWarnings("resource")
    BoxFolder getBoxFolder(final BoxPath path) throws IOException {
        var api = getFileSystemInternal().getApi();
        if (path.isRoot()) {
            return BoxFolder.getRootFolder(api);
        } else {
            var attrs = (BoxFileAttributes) readAttributes(path, BasicFileAttributes.class);
            if (attrs.isDirectory()) {
                return new BoxFolder(api, attrs.getItemId());
            } else {
                throw new NotDirectoryException(toString());
            }
        }
    }

    /**
     * Returns a {@link BoxFile} object corresponding to a given path. Throws an
     * exception if the path does no represent an existing file.
     *
     * @return The {@link BoxFile} object.
     * @throws IOException
     */
    @SuppressWarnings("resource")
    BoxFile getBoxFile(final BoxPath path) throws IOException {
        var api = getFileSystemInternal().getApi();
        var attrs = (BoxFileAttributes) readAttributes(path, BasicFileAttributes.class);

        if (attrs.isRegularFile()) {
            return new BoxFile(api, attrs.getItemId());
        } else {
            throw new IOException(toString() + " is not a file");
        }
    }
}
