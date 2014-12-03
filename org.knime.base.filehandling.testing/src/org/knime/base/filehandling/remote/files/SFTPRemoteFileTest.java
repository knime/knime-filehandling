/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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

import static org.hamcrest.core.AnyOf.anyOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.core.runtime.Platform;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformation;
import org.knime.core.util.PathUtils;

/**
 * Testcases for {@link SFTPRemoteFile}.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 */
public class SFTPRemoteFileTest {
    private ConnectionInformation m_connInfo;

    private ConnectionMonitor<SSHConnection> m_connectionMonitor;

    private RemoteFileHandler<SSHConnection> m_fileHandler;

    /**
     * Set up a connection to localhost with the current user using PPK authentication.
     */
    @Before
    public void setup() {
        m_connInfo = new ConnectionInformation();
        m_connInfo.setHost("localhost");
        m_connInfo.setUser(System.getProperty("user.name"));

        Path sshDir = Paths.get(System.getProperty("user.home"), ".ssh");
        if (Files.exists(sshDir.resolve("id_ecdsa"))) {
            m_connInfo.setKeyfile(sshDir.resolve("id_ecdsa").toString());
        } else if (Files.exists(sshDir.resolve("id_dsa"))) {
            m_connInfo.setKeyfile(sshDir.resolve("id_dsa").toString());
        } else if (Files.exists(sshDir.resolve("id_rsa"))) {
            m_connInfo.setKeyfile(sshDir.resolve("id_rsa").toString());
        }

        m_connectionMonitor = new ConnectionMonitor<>();
        m_fileHandler = new SFTPRemoteFileHandler();
    }

    /**
     * Closes all connection.
     */
    @After
    public void tearDown() {
        m_connectionMonitor.closeAll();
    }

    /**
     * Test for {@link SFTPRemoteFile#listFiles()}.
     *
     * @throws Exception
     */
    @Test
    public void testListFiles() throws Exception {
        Path tempRoot = PathUtils.createTempDir(getClass().getName());

        Path file = Files.createFile(tempRoot.resolve("file"));
        Files.createDirectory(tempRoot.resolve("dir"));

        String path = tempRoot.toUri().getPath().replace("C:", "cygdrive/c"); // fix path for Windows
        RemoteFile<SSHConnection> remoteFile =
            m_fileHandler.createRemoteFile(new URI("sftp", "localhost", path, null), m_connInfo, m_connectionMonitor);

        RemoteFile<SSHConnection>[] dirContents = remoteFile.listFiles();
        assertThat("Unexpected number of directory entries returned", dirContents.length, is(2));

        assertThat("Unexpected directory entry at position 0", dirContents[0].getName(), is("dir"));
        assertThat("Entry at position 0 is not a directory", dirContents[0].isDirectory(), is(true));

        assertThat("Unexpected directory entry at position 1", dirContents[1].getName(), is("file"));
        assertThat("Entry at position 1 is a directory", dirContents[1].isDirectory(), is(false));

        // list on file, should not return any entries
        remoteFile =
            m_fileHandler.createRemoteFile(new URI("sftp", "localhost", file.toString(), null), m_connInfo,
                m_connectionMonitor);

        dirContents = remoteFile.listFiles();
        assertThat("Unexpected number of entries for file returned", dirContents.length, is(0));
    }

    /**
     * Test for {@link SFTPRemoteFile#listFiles()} with symlinks.
     *
     * @throws Exception
     */
    @Test
    public void testListFilesSymlinks() throws Exception {
        assumeThat(Platform.getOS(), anyOf(is(Platform.OS_LINUX), is(Platform.OS_MACOSX)));

        Path tempRoot = PathUtils.createTempDir(getClass().getName());

        Path file = Files.createFile(tempRoot.resolve("file"));
        Files.createSymbolicLink(tempRoot.resolve("AAAfile"), file);

        Path dir = Files.createDirectory(tempRoot.resolve("dir"));
        Files.createSymbolicLink(tempRoot.resolve("AAAdir"), dir);

        RemoteFile<SSHConnection> remoteFile =
            m_fileHandler.createRemoteFile(new URI("sftp", "localhost", tempRoot.toString(), null), m_connInfo,
                m_connectionMonitor);

        RemoteFile<SSHConnection>[] dirContents = remoteFile.listFiles();
        assertThat("Unexpected number of directory entries returned", dirContents.length, is(4));

        assertThat("Unexpected directory entry at position 0", dirContents[0].getName(), is("AAAdir"));
        assertThat("Entry at position 0 is not a directory", dirContents[0].isDirectory(), is(true));

        // entry at position 1 is "dir", because directories are sorted before files

        assertThat("Unexpected directory entry at position 2", dirContents[2].getName(), is("AAAfile"));
        assertThat("Entry at position 2 is a directory", dirContents[2].isDirectory(), is(false));
    }

}
