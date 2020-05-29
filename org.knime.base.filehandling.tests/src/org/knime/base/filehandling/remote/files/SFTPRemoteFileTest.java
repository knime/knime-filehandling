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
 *   02.12.2014 (thor): created
 */
package org.knime.base.filehandling.remote.files;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformation;
import org.knime.core.util.PathUtils;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * Testcases for {@link SFTPRemoteFile}.
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 */
public class SFTPRemoteFileTest extends RemoteFileTest<SSHConnection> {

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isEnabled() {
        String hostString = System.getenv("KNIME_SSHD_ADDRESS");
        return hostString != null; // running on old jenkins
    }

    /**
     * Set up a connection to the sshd container with the jenkins user using PPK authentication.
     */
    @Before
    @Override
    public void setup() {
        if (!isEnabled()) {
            return;
        }

        String hostString = System.getenv("KNIME_SSHD_ADDRESS");

        String userString = "jenkins";
        final String[] sshdHostInfo = hostString.split(":");

        m_host = userString + "@" + sshdHostInfo[0];
        final int port = Integer.parseInt(sshdHostInfo[1]);

        m_connInfo = new ConnectionInformation();
        m_connInfo.setHost(sshdHostInfo[0]);
        m_connInfo.setPort(port);
        m_connInfo.setUser(userString);
        m_connInfo.setProtocol("ssh");

        final Path sshDir = Paths.get(System.getProperty("user.home"), ".ssh");
        m_connInfo.setKeyfile(sshDir.resolve("id_rsa").toString());

        m_type = "sftp";
        m_connectionMonitor = new ConnectionMonitor<SSHConnection>();
        m_fileHandler = new SFTPRemoteFileHandler();
    }

    /**
     * {@inheritDoc}
     *
     * @throws MalformedURLException
     */
    @Override
    public String createPath(final Path p) throws MalformedURLException {
        return p.toUri().toURL().getPath();
    }

    /**
     * @param command
     * @return
     * @throws JSchException
     * @throws IOException
     */
    private String runCommandWithOutput(final String command) throws JSchException, IOException {

        // SSH into remote machine and run the commands
        final JSch j = new JSch();
        j.addIdentity(m_connInfo.getKeyfile());

        final Session s = j.getSession(m_connInfo.getUser(), m_connInfo.getHost(), m_connInfo.getPort());
        s.setConfig("StrictHostKeyChecking", "no"); // ignore the hostkey
        s.connect();
        final ChannelExec c = (ChannelExec)s.openChannel("exec");
        c.setCommand(command);

        String result;
        c.setInputStream(null);
        try (InputStream is = c.getInputStream()) {
            c.connect();
            result = IOUtils.toString(is, StandardCharsets.UTF_8);

        }
        c.disconnect();
        s.disconnect();
        return result;
    }

    @Override
    @Test
    public void testMove() throws Exception {

        final List<Path> paths = createTempFiles(
            /* Setup for the first part of the test*/
            "testMove/", "testMove/dir1/", "testMove/file",
            /* Setup second part of test*/
            "testMove2/", "testMove2/file");

        final String testMove1 = createPath(paths.get(1));
        final SFTPRemoteFile remoteFile1 = (SFTPRemoteFile)m_fileHandler
            .createRemoteFile(new URI(m_type, m_host, testMove1, null), m_connInfo, m_connectionMonitor);

        SFTPRemoteFile[] dirContents = remoteFile1.listFiles();
        assertThat("Unexpected number of directory entries returned: " + dirContents.length, dirContents.length, is(2));

        dirContents[0].move(dirContents[1], null);

        dirContents = null;
        dirContents = remoteFile1.listFiles();
        assertThat("Unexpected number of directory entries returned", dirContents.length, is(1));
        assertThat("Entry at position 1 is not a directory", dirContents[0].isDirectory(), is(true));

        dirContents = dirContents[0].listFiles();
        assertThat("Unexpected number of directory entries returned", dirContents.length, is(1));
        assertThat("Entry at position 1 is a directory", dirContents[0].isDirectory(), is(false));

        // create a local file for uploading in the next test step
        ConnectionInformation connInfo = new ConnectionInformation();
        connInfo = new ConnectionInformation();
        connInfo.setProtocol("file");

        final ConnectionMonitor<Connection> connectionMonitor = new ConnectionMonitor<Connection>();
        final RemoteFileHandler<Connection> fileFileHandler = new FileRemoteFileHandler();

        final Path localTempRoot = PathUtils.createTempDir(getClass().getName());
        final Path localFile = Files.createFile(localTempRoot.resolve("sftpFile"));

        final String localFilePath = localFile.toUri().toURL().getPath();
        final RemoteFile<Connection> fileRemoteFile =
            fileFileHandler.createRemoteFile(new URI("file", null, localFilePath, null), connInfo, connectionMonitor);

        final String str1 = "sftpRemoteFile was here";
        try (OutputStream os = fileRemoteFile.openOutputStream()) {
            IOUtils.write(str1, os, StandardCharsets.UTF_8);
        }

        // Testing upload of the created local file using the move operation
        final String path = createPath(paths.get(5)); // /testMove2/file

        final RemoteFile<SSHConnection> remoteFile2 =
            m_fileHandler.createRemoteFile(new URI(m_type, m_host, path, null), m_connInfo, m_connectionMonitor);

        remoteFile2.move((RemoteFile)fileRemoteFile, null);

        try (InputStream is = remoteFile2.openInputStream()) {
            final String str2 = IOUtils.toString(is, StandardCharsets.UTF_8);
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
        final RemoteFile<SSHConnection> remoteFile =
            m_fileHandler.createRemoteFile(new URI(m_type, m_host, null, null), m_connInfo, m_connectionMonitor);
        assertThat("Directory does not exist", remoteFile.exists(), is(true));
        assertThat("Expected root Directory, got: " + remoteFile.getFullName(), remoteFile.getFullName(), equalTo("/"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Test
    public void testLastModified() throws Exception {
        final Path result = createTempFiles("file").get(1);
        final String path = result.toUri().getPath();

        final RemoteFile<SSHConnection> remoteFile =
            m_fileHandler.createRemoteFile(new URI(m_type, m_host, path, null), m_connInfo, m_connectionMonitor);

        final long actual = remoteFile.lastModified();
        final String output = runCommandWithOutput("date +%s -r " + path).replaceAll("\\s", "");
        final long expected = Long.parseLong(output);

        assertThat("LastModified time is not equal", actual, equalTo(expected));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Test
    public void testGetSize() throws Exception {

        final List<Path> paths = createTempFiles("file");
        final Path file = paths.get(1);

        final String filePath = createPath(file);
        final String result =
            runCommandWithOutput("echo \"Add some character\" >>" + filePath + " ;stat -c %s " + filePath);

        final SFTPRemoteFile remoteFile = (SFTPRemoteFile)m_fileHandler
            .createRemoteFile(new URI(m_type, m_host, filePath, null), m_connInfo, m_connectionMonitor);

        final long expected = Long.parseLong(result.replaceAll("\\s", ""));
        final long actual = remoteFile.getSize();

        assertThat("Size is not equal", actual, equalTo(expected));
    }

    @Override
    @Test
    public void testOpenInputStream() throws Exception {

        final List<Path> paths = createTempFiles("file");
        final Path file = paths.get(1);
        final String path = createPath(file);

        final String str1 = "JUnit test for openInputStream method in RemoteFile";
        runCommandWithOutput("echo '" + str1 + "' > " + path);

        final RemoteFile<SSHConnection> remoteFile =
            m_fileHandler.createRemoteFile(new URI(m_type, m_host, path, null), m_connInfo, m_connectionMonitor);

        try (InputStream stream = remoteFile.openInputStream()) {
            final String str2 = IOUtils.toString(stream, StandardCharsets.UTF_8).replaceAll("\n", "");
            assertThat("Strings are not equal", str2, equalTo(str1));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testListFilesSymlinks() throws Exception {

        final List<Path> paths = createTempFiles("file", "dir/");
        final Path tempRoot = paths.get(0);

        final Path file = paths.get(1);
        final Path dir = paths.get(2);

        final String filePath = createPath(file);
        final String dirPath = createPath(dir);

        final String symbolicFile = "ln -s " + filePath + " " + filePath + "AAAA";
        final String symbolicDir = "ln -s " + dirPath + " " + dirPath + "AAAA";

        runCommandWithOutput(symbolicFile + "; " + symbolicDir);

        final RemoteFile<SSHConnection> remoteFile = m_fileHandler
            .createRemoteFile(new URI(m_type, m_host, tempRoot.toString(), null), m_connInfo, m_connectionMonitor);

        final RemoteFile<SSHConnection>[] dirContents = remoteFile.listFiles();
        assertThat("Unexpected number of directory entries returned", dirContents.length, is(4));

        assertThat("Unexpected directory entry at position 0", dirContents[1].getName(), is("dirAAAA"));
        assertThat("Entry at position 0 is not a directory", dirContents[1].isDirectory(), is(true));

        // entry at position 1 is "dir", because directories are sorted before files

        assertThat("Unexpected directory entry at position 2", dirContents[3].getName(), is("fileAAAA"));
        assertThat("Entry at position 2 is a directory", dirContents[3].isDirectory(), is(false));
    }

    /**
     * Test for {@link SFTPRemoteFile#openOutputStream()}.
     *
     * @throws Exception
     */
    @Test
    @Override
    public void testOpenOutputStream() throws Exception {
        final List<Path> paths = createTempFiles("file");
        final Path file = paths.get(1);

        final String path = createPath(file);
        final RemoteFile<SSHConnection> remoteFile =
            m_fileHandler.createRemoteFile(new URI(m_type, m_host, path, null), m_connInfo, m_connectionMonitor);

        final String str1 = "JUnit test for openInputStream method in RemoteFile";
        try (OutputStream stream = remoteFile.openOutputStream()) {
            IOUtils.write(str1, stream, StandardCharsets.UTF_8);
        }

        final String output = runCommandWithOutput("cat " + path);
        final String str2 = output.replaceAll("\n", "");

        assertThat("Strings do not have same length", str2.length(), is(str1.length()));
        assertThat("Strings are not equal", str2, equalTo(str1));
    }

    /**
     * @return the basefolder and all created subfiles
     *
     * @throws InterruptedException
     * @throws JSchException
     */
    @Override
    protected List<Path> createTempFiles(final String... paths) throws Exception {

        final String basepath = "/tmp" + "/testRoot" + System.currentTimeMillis() + "/";

        final List<Path> results = new ArrayList<>();
        final List<String> createCommands = new ArrayList<>();
        createCommands.add("mkdir " + basepath);
        results.add(Paths.get(basepath)); // to ensure a "/" at the end
        for (final String s : paths) {
            if (s.endsWith("/")) {
                createCommands.add("mkdir -p " + basepath + s);
            } else {
                createCommands.add("touch " + basepath + s);
            }
            results.add(Paths.get(basepath, s));
        }

        // create command line
        final StringBuilder b = new StringBuilder();
        for (final String s1 : createCommands.toArray(new String[createCommands.size()])) {
            b.append(s1);
            b.append("; ");
        }
        final String command = b.toString();
        runCommandWithOutput(command);

        return results;
    }
}
