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
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.ClosedFileSystemException;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.knime.filehandling.core.connections.FSFileSystemProvider;
import org.knime.filehandling.core.connections.FSInputStream;
import org.knime.filehandling.core.connections.base.attributes.BaseFileAttributeView;
import org.knime.filehandling.core.connections.base.attributes.BasicFileAttributesUtil;

/**
 * File system provider for the HTTP(S) file system.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
final class HttpFileSystemProvider extends FSFileSystemProvider<HttpPath, HttpFileSystem> {

    private HttpFileSystem m_fileSystem = null;

    void setFileSystem(final HttpFileSystem fs) {
        m_fileSystem = fs;
    }

    @Override
    public String getScheme() {
        return m_fileSystem.getFileSystemBaseURI().getScheme();
    }

    @Override
    public HttpFileSystem newFileSystem(final URI uri, final Map<String, ?> env) throws IOException {
        return m_fileSystem;
    }

    @Override
    public HttpFileSystem getFileSystem(final URI uri) {
        return m_fileSystem;
    }

    @Override
    public HttpPath getPath(final URI uri) {
        final StringBuilder pathString = new StringBuilder(uri.getPath());
        if (StringUtils.isBlank(uri.getQuery())) {
            pathString.append('?');
            pathString.append(uri.getQuery());
        }

        if (StringUtils.isBlank(uri.getFragment())) {
            pathString.append('#');
            pathString.append(uri.getFragment());
        }

        return m_fileSystem.getPath(pathString.toString());
    }

    @Override
    public SeekableByteChannel newByteChannel(final Path path, final Set<? extends OpenOption> options,
            final FileAttribute<?>... attrs) throws IOException {

        checkFileSystemOpen();
        if (options.contains(StandardOpenOption.WRITE) // NOSONAR need to check all of these
                || options.contains(StandardOpenOption.APPEND) //
                || options.contains(StandardOpenOption.TRUNCATE_EXISTING) //
                || options.contains(StandardOpenOption.CREATE) //
                || options.contains(StandardOpenOption.CREATE_NEW)) {

            throw new UnsupportedOperationException("The HTTP(S) connector does not support writing files.");
        }

        final HttpPath checkedPath = checkCastAndAbsolutizePath(path);
        return new HttpSeekableByteChannel(checkedPath, options);
    }

    @SuppressWarnings("resource")
    @Override
    public InputStream newInputStream(final Path path, final OpenOption... options) throws IOException {
        checkFileSystemOpen();
        final HttpPath checkedPath = checkCastAndAbsolutizePath(path);
        checkOpenOptionsForReading(options);

        final HttpClient client = m_fileSystem.getClient();
        return new FSInputStream(client.getAsInputStream(checkedPath), m_fileSystem);
    }

    /**
     * Checks whether the open options are valid for reading.
     *
     * @param options
     *            the options to check
     */
    protected static void checkOpenOptionsForReading(final OpenOption[] options) {
        for (final OpenOption option : options) {
            if (option == StandardOpenOption.APPEND || option == StandardOpenOption.WRITE) {
                throw new UnsupportedOperationException("'" + option + "' not allowed");
            }
        }
    }

    @Override
    public OutputStream newOutputStream(final Path path, final OpenOption... options) throws IOException {
        throw new UnsupportedOperationException("The HTTP(S) connector does not support writing files.");
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(final Path dir, final Filter<? super Path> filter)
            throws IOException {
        throw new UnsupportedOperationException("The HTTP(S) Connector does not support listing folder contents.");
    }

    @Override
    public void createDirectory(final Path dir, final FileAttribute<?>... attrs) throws IOException {
        throw new UnsupportedOperationException("The HTTP(S) Connector does not support creating folders.");
    }

    @Override
    public void delete(final Path path) throws IOException {
        throw new UnsupportedOperationException("The HTTP(S) Connector does not support deleting files.");
    }

    @Override
    public void copy(final Path source, final Path target, final CopyOption... options) throws IOException {
        throw new UnsupportedOperationException("The HTTP(S) Connector does not support copying files.");
    }

    @Override
    public void move(final Path source, final Path target, final CopyOption... options) throws IOException {
        throw new UnsupportedOperationException("The HTTP(S) connector does not support moving files/folders.");
    }

    @Override
    public boolean isSameFile(final Path path, final Path path2) throws IOException {
        final HttpPath checkedPath = checkCastAndAbsolutizePath(path);
        final HttpPath checkedPath2 = checkCastAndAbsolutizePath(path2);

        return checkedPath.equals(checkedPath2);
    }

    @Override
    public boolean isHidden(final Path path) throws IOException {
        return false;
    }

    @Override
    public FileStore getFileStore(final Path path) throws IOException {
        return m_fileSystem.getFileStores().iterator().next();
    }

    @Override
    public void checkAccess(final Path path, final AccessMode... modes) throws IOException {
        checkFileSystemOpen();

        // throws various exceptions if file does not exist or is not accessible
        readAttributes(path, BasicFileAttributes.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V extends FileAttributeView> V getFileAttributeView(final Path path, final Class<V> type,
            final LinkOption... options) {
        checkFileSystemOpen();

        checkPathProvider(path);
        if (type == BasicFileAttributeView.class || type == PosixFileAttributeView.class) {
            return (V) new BaseFileAttributeView(path, type);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <A extends BasicFileAttributes> A readAttributes(final Path path, final Class<A> type,
            final LinkOption... options) throws IOException {
        // allowed during closing (part of recursive temp dir deletion)
        checkFileSystemOpen();

        final HttpPath checkedPath = checkCastAndAbsolutizePath(path);

        if (type == BasicFileAttributes.class) {
            return (A) m_fileSystem.getClient().headAsFileAttributes(checkedPath);
        }

        throw new UnsupportedOperationException(String.format("only %s and %s supported",
                BasicFileAttributes.class.getName(), PosixFileAttributes.class.getName()));
    }

    @Override
    public Map<String, Object> readAttributes(final Path path, final String attributes, final LinkOption... options)
            throws IOException {
        // allowed during closing (part of recursive temp dir deletion)
        checkFileSystemOpen();
        checkPathProvider(path);
        return BasicFileAttributesUtil.attributesToMap(readAttributes(path, BasicFileAttributes.class, options),
                attributes);
    }

    @Override
    public void setAttribute(final Path path, final String attribute, final Object value, final LinkOption... options)
            throws IOException {
        throw new UnsupportedOperationException("The HTTP(S) connector does not support setting file attributes.");
    }

    /**
     * Checks whether the underlying file system is still open and not in the
     * process of closing. Throws a {@link ClosedFileSystemException} if not.
     *
     * @throws ClosedFileSystemException
     *             when the file system has already been closed or is closing right
     *             now.
     */
    protected void checkFileSystemOpen() {
        if (!m_fileSystem.isOpen()) {
            throw new ClosedFileSystemException();
        }
    }

    private HttpPath checkCastAndAbsolutizePath(final Path path) {
        checkPathProvider(path);
        return (HttpPath) path.toAbsolutePath().normalize();
    }

    @SuppressWarnings("resource")
    private void checkPathProvider(final Path path) {
        if (path.getFileSystem().provider() != this) {
            throw new IllegalArgumentException("Path is from a different provider");
        }
    }
}
