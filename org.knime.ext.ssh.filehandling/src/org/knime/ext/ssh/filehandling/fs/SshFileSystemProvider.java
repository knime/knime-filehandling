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
 *   2020-07-28 (Vyacheslav Soldatov): created
 */
package org.knime.ext.ssh.filehandling.fs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Iterator;
import java.util.Set;

import org.apache.sshd.client.subsystem.sftp.SftpClient;
import org.knime.ext.ssh.filehandling.node.SshConnectionSettings;
import org.knime.filehandling.core.connections.base.BaseFileSystemProvider;
import org.knime.filehandling.core.connections.base.attributes.BaseFileAttributes;

/**
 * File system provider for {@link SshFileSystem}.
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
public class SshFileSystemProvider extends BaseFileSystemProvider<SshPath, SshFileSystem> {
    private final SshFileSystem m_fileSystem;
    private final ConnectionResourcePool m_resources = new ConnectionResourcePool();

    /**
     * @param settings
     *            settings.
     * @param cacheTtl
     * @throws IOException
     */
    public SshFileSystemProvider(final SshConnectionSettings settings,
            final long cacheTtl)
            throws IOException {
        m_resources.setSettings(settings);
        m_resources.start();
        m_fileSystem = new SshFileSystem(this, cacheTtl, settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected SeekableByteChannel newByteChannelInternal(final SshPath path, final Set<? extends OpenOption> options,
            final FileAttribute<?>... attrs) throws IOException {
        throw new RuntimeException("TODO implement");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void moveInternal(final SshPath source, final SshPath target, final CopyOption... options)
            throws IOException {
        throw new RuntimeException("TODO implement");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void copyInternal(final SshPath source, final SshPath target, final CopyOption... options)
            throws IOException {
        throw new RuntimeException("TODO implement");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected InputStream newInputStreamInternal(final SshPath path, final OpenOption... options) throws IOException {
        throw new RuntimeException("TODO implement");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected OutputStream newOutputStreamInternal(final SshPath path, final OpenOption... options) throws IOException {
        throw new RuntimeException("TODO implement");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Iterator<SshPath> createPathIterator(final SshPath dir, final Filter<? super Path> filter)
            throws IOException {
        throw new RuntimeException("TODO implement");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createDirectoryInternal(final SshPath dir, final FileAttribute<?>... attrs)
            throws IOException {
        throw new RuntimeException("TODO implement");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exists(final SshPath path) throws IOException {
        throw new RuntimeException("TODO implement");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BaseFileAttributes fetchAttributesInternal(final SshPath path, final Class<?> type) throws IOException {
        throw new RuntimeException("TODO implement");
    }

    /**
     * @param sftpClient
     *            file system.
     * @param path
     *            path to file.
     * @param type
     *            attributes type.
     * @return file attributes.
     * @throws IOException
     */
    protected BaseFileAttributes fetchAttributesInternal(
            final SftpClient sftpClient,
            final SshPath path, final Class<?> type) throws IOException {
        throw new RuntimeException("TODO implement");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void checkAccessInternal(final SshPath path, final AccessMode... modes) throws IOException {
        throw new RuntimeException("TODO implement");
    }

    @Override
    protected void deleteInternal(final SshPath path) throws IOException {
        throw new RuntimeException("TODO implement");
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isHidden(final Path path) throws IOException {
        // MINE STFP File system provider just returns true
        // TODO search and possible correct implement it.
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAttribute(final Path path, final String name, final Object value,
            final LinkOption... options)
            throws IOException {
        throw new RuntimeException("TODO implement");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getScheme() {
        return SshFileSystem.FS_TYPE;
    }

    /**
     * @return the SSH file system.
     */
    public SshFileSystem getFileSystem() {
        return m_fileSystem;
    }

    /**
     * @param first first segment.
     * @param more other segments.
     * @return SSH path.
     */
    @SuppressWarnings("resource")
    public SshPath toSsh(final String first, final String... more) {
        return new SshPath(getFileSystem(), first, more);
    }
}
