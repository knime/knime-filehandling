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
 *   2020-11-18 (Bjoern Lohrmann): created
 */
package org.knime.ext.http.filehandling.fs;

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
import java.util.Iterator;
import java.util.Set;

import org.knime.ext.http.filehandling.node.HttpConnectorNodeSettings;
import org.knime.filehandling.core.connections.base.BaseFileSystemProvider;
import org.knime.filehandling.core.connections.base.attributes.BaseFileAttributes;

/**
 * File system provider for {@link HttpFileSystem}.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public class HttpFileSystemProvider extends BaseFileSystemProvider<HttpPath, HttpFileSystem> {

    /**
     * @param config
     *            HTTP connection configuration.
     * @throws IOException
     */
    public HttpFileSystemProvider(final HttpConnectorNodeSettings config) throws IOException {
    }

    @Override
    protected SeekableByteChannel newByteChannelInternal(final HttpPath path, final Set<? extends OpenOption> options,
            final FileAttribute<?>... attrs) throws IOException {
        return new HttpSeekableByteChannel(path, options);
    }

    @Override
    protected void moveInternal(final HttpPath source, final HttpPath target, final CopyOption... options)
            throws IOException {
        throw new UnsupportedOperationException("Cannot move files with HTTP.");
    }

    @Override
    protected void copyInternal(final HttpPath source, final HttpPath target, final CopyOption... options)
            throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected InputStream newInputStreamInternal(final HttpPath path, final OpenOption... options) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected OutputStream newOutputStreamInternal(final HttpPath path, final OpenOption... options)
            throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Iterator<HttpPath> createPathIterator(final HttpPath dir, final Filter<? super Path> filter)
            throws IOException {
        throw new UnsupportedOperationException("Cannot list files/folders with HTTP.");
    }

    @Override
    protected void createDirectoryInternal(final HttpPath dir, final FileAttribute<?>... attrs) throws IOException {
        throw new UnsupportedOperationException("Cannot create a folder with HTTP.");
    }

    @Override
    protected BaseFileAttributes fetchAttributesInternal(final HttpPath path, final Class<?> type) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void checkAccessInternal(final HttpPath path, final AccessMode... modes) throws IOException {
        // nothing for now
    }

    @Override
    protected void deleteInternal(final HttpPath path) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getScheme() {
        return HttpFileSystem.FS_TYPE;
    }
}
