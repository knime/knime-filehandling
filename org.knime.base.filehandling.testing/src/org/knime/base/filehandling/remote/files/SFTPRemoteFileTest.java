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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 *   02.12.2014 (thor): created
 */
package org.knime.base.filehandling.remote.files;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformation;
import org.knime.core.util.PathUtils;

/**
 * Testcases for {@link SFTPRemoteFile}.
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 */
public class SFTPRemoteFileTest extends RemoteFileTest<SSHConnection> {
    /**
     * Set up a connection to localhost with the current user using PPK authentication.
     */
    @Before
    @Override
    public void setup() {
        m_connInfo = new ConnectionInformation();
        m_connInfo.setHost("localhost");
        m_connInfo.setUser(System.getProperty("user.name"));
        m_connInfo.setProtocol("ssh");
        m_connInfo.setPort(22);

        Path sshDir = Paths.get(System.getProperty("user.home"), ".ssh");
        if (Files.exists(sshDir.resolve("id_ecdsa"))) {
            m_connInfo.setKeyfile(sshDir.resolve("id_ecdsa").toString());
        } else if (Files.exists(sshDir.resolve("id_dsa"))) {
            m_connInfo.setKeyfile(sshDir.resolve("id_dsa").toString());
        } else if (Files.exists(sshDir.resolve("id_rsa"))) {
            m_connInfo.setKeyfile(sshDir.resolve("id_rsa").toString());
        }

        m_type = "sftp";
        m_host = "localhost";

        m_connectionMonitor = new ConnectionMonitor<SSHConnection>();

        m_fileHandler = new SFTPRemoteFileHandler();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String createPath(final Path p) {
        return p.toUri().getPath().replace("C:", "cygdrive/c"); // fix path for Windows;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testMove() throws Exception {
        super.testMove();

        ConnectionInformation connInfo = new ConnectionInformation();
        connInfo = new ConnectionInformation();
        connInfo.setProtocol("file");

        ConnectionMonitor<Connection> connectionMonitor = new ConnectionMonitor<Connection>();
        RemoteFileHandler<Connection> fileFileHandler = new FileRemoteFileHandler();

        Path fileTempRoot = PathUtils.createTempDir(getClass().getName());

        Path fileFile = Files.createFile(fileTempRoot.resolve("sftpFile"));

        String fileFilePath = fileFile.toUri().toURL().getPath();
        RemoteFile<Connection> fileRemoteFile =
            fileFileHandler.createRemoteFile(new URI("file", null, fileFilePath, null), connInfo, connectionMonitor);

        String str1 = "sftpRemoteFile was here";
        try (OutputStream os = fileRemoteFile.openOutputStream()) {
            IOUtils.write(str1, os);
        }

        Path tempRoot = PathUtils.createTempDir(getClass().getName());

        Path file = Files.createFile(tempRoot.resolve("file"));

        String path = createPath(file);
        RemoteFile<SSHConnection> remoteFile =
            m_fileHandler.createRemoteFile(new URI(m_type, m_host, path, null), m_connInfo, m_connectionMonitor);

        remoteFile.move((RemoteFile)fileRemoteFile, null);

        try (InputStream is = remoteFile.openInputStream()) {
            String str2 = IOUtils.toString(is, "UTF-8");
            assertThat("file content is different", str2, equalTo(str1));
        }

    }

    /**
     * Test if URI without path works.
     *
     * @throws Exception if error occurs
     */
    @Test
    public void testOpenChannelWithoutPath() throws Exception {
        Path tempRoot = PathUtils.createTempDir(getClass().getName());
        Files.createFile(tempRoot.resolve("file"));

        RemoteFile<SSHConnection> remoteFile =
                m_fileHandler.createRemoteFile(new URI(m_type, m_host, null, null), m_connInfo, m_connectionMonitor);
        assertThat("Directory does not exist", remoteFile.exists(), is(true));
        assertThat("Directory is not root directory", remoteFile.getFullName(), equalTo("/"));
    }
}
