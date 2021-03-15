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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Collections;

import org.knime.filehandling.core.connections.DefaultFSLocationSpec;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.connections.base.BaseFileSystem;

import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;

/**
 * Samba implementation of the {@link FileSystem} interface.
 *
 * @author Alexander Bondaletov
 */
public class SmbFileSystem extends BaseFileSystem<SmbPath> {

    /**
     * Character to use as path separator
     */
    public static final String PATH_SEPARATOR = "\\";

    private final SMBClient m_client;
    private final DiskShare m_share;


    /**
     * @param cacheTTL
     *            The time to live for cached elements in milliseconds.
     * @param workingDirectory
     *            The working directory.
     * @param host
     *            The Samba host.
     * @param share
     *            The Samba share name.
     * @param username
     *            The user name.
     * @param password
     *            The user password.
     * @throws IOException
     */
    @SuppressWarnings("resource")
    protected SmbFileSystem(
            final long cacheTTL, final String workingDirectory, final String host, final String share,
            final String username, final String password) throws IOException {
        super(new SmbFileSystemProvider(), createUri(host, share), cacheTTL, workingDirectory,
                createFSLocationSpec(host, share));

        m_client = new SMBClient();

        Connection connection = m_client.connect(host);
        Session session = connection.authenticate(new AuthenticationContext(username, password.toCharArray(), ""));
        m_share = (DiskShare) session.connectShare(share);
    }

    /**
     * @return The share client
     */
    public DiskShare getClient() {
        return m_share;
    }

    private static URI createUri(final String host, final String share) {
        try {
            return new URI(SmbFileSystemProvider.FS_TYPE, host, "/" + share, null);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * @param host
     *            The Samba host.
     * @param share
     *            The Samba share name.
     * @return the {@link FSLocationSpec} for a ADLS file system.
     */
    public static DefaultFSLocationSpec createFSLocationSpec(final String host, final String share) {
        return new DefaultFSLocationSpec(FSCategory.CONNECTED, //
                String.format("%s:\\\\%s\\", SmbFileSystemProvider.FS_TYPE, host, share));
    }

    @Override
    protected void prepareClose() throws IOException {
        m_client.close();
    }

    @Override
    public SmbPath getPath(final String first, final String... more) {
        return new SmbPath(this, first, more);
    }

    @Override
    public String getSeparator() {
        return PATH_SEPARATOR;
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        return Collections.singletonList(getPath(PATH_SEPARATOR));
    }

}
