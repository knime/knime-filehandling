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

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Locale;

import org.knime.filehandling.core.connections.DefaultFSLocationSpec;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.connections.base.BaseFileSystem;

/**
 * FTP implementation of the {@link FileSystem}.
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
public class FtpFileSystem extends BaseFileSystem<FtpPath> {
    private static final long CACHE_TTL = 6000;

    /**
     * FTP URI scheme.
     */
    public static final String FS_TYPE = "ftp";

    /**
     * Character to use as path separator
     */
    public static final String PATH_SEPARATOR = "/";

    /**
     * @param settings
     *            FTP connection settings.
     * @throws IOException
     */
    protected FtpFileSystem(final FtpConnectionConfiguration settings)
            throws IOException {
        super(createProvider(settings), createUri(settings), CACHE_TTL,
                settings.getWorkingDirectory(),
                createFSLocationSpec(settings.getHost()));
    }

    private static FtpFileSystemProvider createProvider(final FtpConnectionConfiguration settings) throws IOException {
        return new FtpFileSystemProvider(settings);
    }

    @Override
    public FtpFileSystemProvider provider() {
        return (FtpFileSystemProvider) super.provider();
    }

    /**
     * @param cfg
     *            connection configuration.
     * @return URI from configuration.
     * @throws URISyntaxException
     */
    private static URI createUri(final FtpConnectionConfiguration cfg) throws IOException {
        try {
            return new URI(FS_TYPE, null, cfg.getHost(), cfg.getPort(), null, null, null);
        } catch (final URISyntaxException e) {
            throw new IOException("Failed to create URI", e);
        }
    }

    /**
     * @param host
     *            host name from configuration.
     * @return the {@link FSLocationSpec} for a FTP file system.
     */
    public static DefaultFSLocationSpec createFSLocationSpec(final String host) {
        String resolvedHost = host.toLowerCase(Locale.ENGLISH);
        try {
            resolvedHost = InetAddress.getByName(host).getCanonicalHostName();
        } catch (UnknownHostException ex) { // NOSONAR is possible if host can't be resolved
        }

        return new DefaultFSLocationSpec(FSCategory.CONNECTED, FtpFileSystem.FS_TYPE + ":" + resolvedHost);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void prepareClose() {
        provider().prepareClose();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FtpPath getPath(final String first, final String... more) {
        return new FtpPath(this, first, more);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSeparator() {
        return PATH_SEPARATOR;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterable<Path> getRootDirectories() {
        return Collections.singletonList(getPath(PATH_SEPARATOR));
    }
}
