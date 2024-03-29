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

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Locale;

import org.knime.filehandling.core.connections.DefaultFSLocationSpec;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.connections.base.BaseFileSystem;
import org.knime.filehandling.core.connections.meta.FSType;
import org.knime.filehandling.core.connections.meta.FSTypeRegistry;

/**
 * SFTP implementation of the {@link FileSystem}.
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
public class SshFileSystem extends BaseFileSystem<SshPath> {
    private static final long CACHE_TTL = 6000;

    /**
     * The file system type of the SSH file system.
     */
    public static final FSType FS_TYPE = FSTypeRegistry.getOrCreateFSType("ssh", "SSH");

    /**
     * Character to use as path separator
     */
    public static final String PATH_SEPARATOR = "/";

    SshFileSystem(final SshFSConnectionConfig config) throws IOException {
        super(createProvider(config), //
                CACHE_TTL, //
                config.getWorkingDirectory(), //
                createFSLocationSpec(config));
    }

    private static SshFileSystemProvider createProvider(final SshFSConnectionConfig settings) throws IOException {
        return new SshFileSystemProvider(settings);
    }

    @Override
    public SshFileSystemProvider provider() {
        return (SshFileSystemProvider) super.provider();
    }

    /**
     * @param config
     *            connection configuration
     * @return the {@link FSLocationSpec} for a SSH file system.
     */
    public static DefaultFSLocationSpec createFSLocationSpec(final SshFSConnectionConfig config) {
        String resolvedHost = config.getHost().toLowerCase(Locale.ENGLISH);
        try {
            resolvedHost = InetAddress.getByName(config.getHost()).getCanonicalHostName();
        } catch (UnknownHostException ex) { // NOSONAR ignore if thrown
        }

        return new DefaultFSLocationSpec(FSCategory.CONNECTED, SshFileSystem.FS_TYPE + ":" + resolvedHost);
    }

    @Override
    protected void prepareClose() {
        provider().prepareClose();
    }

    @Override
    public SshPath getPath(final String first, final String... more) {
        return new SshPath(this, first, more);
    }

    @Override
    public String getSeparator() {
        return PATH_SEPARATOR;
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        return Collections.singletonList(getPath(PATH_SEPARATOR));
    }

    @Override
    public synchronized void unregisterCloseable(final Closeable closeable) {
        provider().unregisterCloseable(closeable);
        super.unregisterCloseable(closeable);
    }
}
