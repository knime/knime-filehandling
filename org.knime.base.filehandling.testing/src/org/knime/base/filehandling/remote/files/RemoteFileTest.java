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
 *   05.12.2014 (tibuch): created
 */
package org.knime.base.filehandling.remote.files;

import static org.hamcrest.core.AnyOf.anyOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.runtime.Platform;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformation;
import org.knime.core.util.PathUtils;

/**
 *
 * @author Tim-Oliver Buchholz, KNIME.com, Zurich, Switzerland
 * @param <C> connection type
 */
public abstract class RemoteFileTest<C extends Connection> {


    /**
     * The host location.
     */
    protected String m_host;

    /**
     * The connection information.
     */
    protected ConnectionInformation m_connInfo;

    /**
     * The connection type.
     */
    protected String m_type;

    /**
     * The connection monitor.
     */
    protected ConnectionMonitor<C> m_connectionMonitor;

    /**
     * The corresponding file handler.
     */
    protected RemoteFileHandler<C> m_fileHandler;

    /**
     * Initialize the connection.
     * Initialize m_host, m_connInfo, m_type, m_connectionMonitor, m_fileHandler
     */
    @Before
    public abstract void setup();


    /**
     * @param p Path to a location.
     * @return return correct path for test environment
     * @throws MalformedURLException thrown if p is invalid
     */
    protected abstract String createPath(Path p) throws MalformedURLException;


    /**
     * Closes all connection.
     */
    @After
    public void tearDown() {
        m_connectionMonitor.closeAll();
    }

    /**
     * Test for {@link SFTPRemoteFile#getType()}.
     *
     * @throws Exception
     */
    @Test
    public void testGetType() throws Exception {
        Path tempRoot = PathUtils.createTempDir(getClass().getName());

        String tempRootPath = createPath(tempRoot);
        RemoteFile<C> remoteFile =
            m_fileHandler.createRemoteFile(new URI(m_type, m_host, tempRootPath, null), m_connInfo, m_connectionMonitor);

        assertThat("Incorrect type of tempRoot", remoteFile.getType(), equalTo(m_type));

    }

    /**
     * Test for {@link SFTPRemoteFile#getName()}.
     *
     * @throws Exception
     */
    @Test
    public void testGetName() throws Exception {
        Path tempRoot = PathUtils.createTempDir(getClass().getName());
        Files.createFile(tempRoot.resolve("file"));

        String tempRootPath = createPath(tempRoot);
        RemoteFile<C> remoteFile =
            m_fileHandler.createRemoteFile(new URI(m_type, m_host, tempRootPath, null), m_connInfo, m_connectionMonitor);

        assertThat("Incorrect path of tempRoot directory", remoteFile.getName(), equalTo(tempRoot.getFileName().toString()));
        assertThat("Incorrect path of file", remoteFile.listFiles()[0].getName(), equalTo("file"));

    }

    /**
     * Test for {@link SFTPRemoteFile#getPath()}.
     *
     * @throws Exception
     */
    @Test
    public void testGetPath() throws Exception {
        Path tempRoot = PathUtils.createTempDir(getClass().getName());
        Files.createFile(tempRoot.resolve("file"));

        String tempRootPath = createPath(tempRoot);
        RemoteFile<C> remoteFile =
            m_fileHandler.createRemoteFile(new URI(m_type, m_host, tempRootPath, null), m_connInfo, m_connectionMonitor);

        assertThat("Incorrect path of tempRoot directory", remoteFile.getPath(), equalTo(tempRootPath));
        // SFTPRemoteFile.getPath(file) returns path to directory !without! file-name (as described in method documentation)
        assertThat("Incorrect path of file", remoteFile.listFiles()[0].getPath(), equalTo(tempRootPath));
    }

    /**
     * Test for {@link SFTPRemoteFile#exists()}.
     *
     * @throws Exception
     */
    @Test
    public void testExits() throws Exception {
        Path tempRoot = PathUtils.createTempDir(getClass().getName());
        Files.createFile(tempRoot.resolve("file"));

        String path = createPath(tempRoot);
        RemoteFile<C> remoteFile =
            m_fileHandler.createRemoteFile(new URI(m_type, m_host, path, null), m_connInfo, m_connectionMonitor);

        boolean exists = remoteFile.exists();
        assertThat("tempRoot does not exists", exists, is(true));

        RemoteFile<C>[] dirContents = remoteFile.listFiles();
        exists = dirContents[0].exists();
        assertThat("Entry at position 1 does not exists", exists, is(true));

        dirContents[0].delete();
        exists = dirContents[0].exists();
        assertThat("Entry at position 1 exists", exists, is(false));
    }

    /**
     * Test for {@link SFTPRemoteFile#isDirectory()}.
     *
     * @throws Exception
     */
    @Test
    public void testIsDirectory() throws Exception {
        Path tempRoot = PathUtils.createTempDir(getClass().getName());
        Files.createFile(tempRoot.resolve("file"));

        String path = createPath(tempRoot);
        RemoteFile<C> remoteFile =
            m_fileHandler.createRemoteFile(new URI(m_type, m_host, path, null), m_connInfo, m_connectionMonitor);

        boolean isDirectory = remoteFile.isDirectory();
        assertThat("empRoot is not a directory", isDirectory, is(true));

        isDirectory = remoteFile.listFiles()[0].isDirectory();
        assertThat("Entry at position 1 is a directory", isDirectory, is(false));
    }

    /**
     * Test for {@link SFTPRemoteFile#move(RemoteFile, org.knime.core.node.ExecutionContext)}.
     *
     * @throws Exception
     */
    @Test
    public void testMove() throws Exception {
        Path tempRoot = PathUtils.createTempDir(getClass().getName());

        Files.createDirectories(tempRoot.resolve("dir1"));
        Files.createFile(tempRoot.resolve("file"));

        String path = createPath(tempRoot);
        RemoteFile<C> remoteFile =
            m_fileHandler.createRemoteFile(new URI(m_type, m_host, path, null), m_connInfo, m_connectionMonitor);

        RemoteFile<C>[] dirContents = remoteFile.listFiles();
        assertThat("Unexpected number of directory entries returned", dirContents.length, is(2));

        dirContents[0].move(dirContents[1], null);

        dirContents = null;
        dirContents = remoteFile.listFiles();
        assertThat("Unexpected number of directory entries returned", dirContents.length, is(1));
        assertThat("Entry at position 1 is not a directory", dirContents[0].isDirectory(), is(true));

        dirContents = dirContents[0].listFiles();
        assertThat("Unexpected number of directory entries returned", dirContents.length, is(1));
        assertThat("Entry at position 1 is a directory", dirContents[0].isDirectory(), is(false));
    }

    /**
     * Test for {@link SFTPRemoteFile#openInputStream()}.
     *
     * @throws Exception
     */
    @Test
    public void testOpenInputStream() throws Exception {

        Path tempRoot = PathUtils.createTempDir(getClass().getName());

        Path file = Files.createFile(tempRoot.resolve("file"));

        String str1 = "JUnit test for openInputStream method in SFTPRemoteFile";
        try (Writer w = Files.newBufferedWriter(file, Charset.forName("UTF-8"))) {
            w.write(str1);
        }

        String path = createPath(file);
        RemoteFile<C> remoteFile =
            m_fileHandler.createRemoteFile(new URI(m_type, m_host, path, null), m_connInfo, m_connectionMonitor);

        try (InputStream stream = remoteFile.openInputStream()) {
            String str2 = IOUtils.toString(stream, "UTF-8");

            assertThat("Strings do not have same length", str2.length(), is(str1.length()));
            assertThat("Strings are not equal", str2, equalTo(str1));
        }

    }

    /**
     * Test for {@link SFTPRemoteFile#openOutputStream()}.
     *
     * @throws Exception
     */
    @Test
    public void testOpenOutputStream() throws Exception {
        Path tempRoot = PathUtils.createTempDir(getClass().getName());

        Path file = Files.createFile(tempRoot.resolve("file"));


        String path = createPath(file);
        RemoteFile<C> remoteFile =
            m_fileHandler.createRemoteFile(new URI(m_type, m_host, path, null), m_connInfo, m_connectionMonitor);

        String str1 = "JUnit test for openInputStream method in SFTPRemoteFile";
        try (OutputStream stream = remoteFile.openOutputStream()) {
            IOUtils.write(str1, stream);
        }

        try (Reader r = Files.newBufferedReader(file, Charset.forName("UTF-8"))) {
            String str2 = IOUtils.toString(r);
            assertThat("Strings do not have same length", str2.length(), is(str1.length()));
            assertThat("Strings are not equal", str2, equalTo(str1));
        }
    }

    /**
     * Test for {@link SFTPRemoteFile#getSize()}.
     *
     * @throws Exception
     */
    @Test
    public void testGetSize() throws Exception {
        Path tempRoot = PathUtils.createTempDir(getClass().getName());

        Path file = Files.createFile(tempRoot.resolve("file"));
        try (Writer w = Files.newBufferedWriter(file, Charset.forName("UTF-8"))) {
            w.write("Add some characters");
        }

        String path = createPath(file);
        RemoteFile<C> remoteFile =
            m_fileHandler.createRemoteFile(new URI(m_type, m_host, path, null), m_connInfo, m_connectionMonitor);

        long s1 = Files.size(file);
        long s2 = remoteFile.getSize();

        assertThat("Size is not equal", s2, equalTo(s1));
    }

    /**
     * Test for {@link SFTPRemoteFile#lastModified()}.
     *
     * @throws Exception
     */
    @Test
    public void testLastModified() throws Exception {
        Path tempRoot = PathUtils.createTempDir(getClass().getName());

        Path file = Files.createFile(tempRoot.resolve("file"));

        String path = createPath(file);
        RemoteFile<C> remoteFile =
            m_fileHandler.createRemoteFile(new URI(m_type, m_host, path, null), m_connInfo, m_connectionMonitor);

        long t1 = Files.getLastModifiedTime(file, LinkOption.NOFOLLOW_LINKS).to(TimeUnit.SECONDS);
        long t2 = remoteFile.lastModified();

        assertThat("LastModified time is not equal", t2, equalTo(t1));
    }

    /**
     * Test for {@link SFTPRemoteFile#delete()}.
     *
     * @throws Exception
     */
    @Test
    public void testDelete() throws Exception {
        Path tempRoot = PathUtils.createTempDir(getClass().getName());

        Path dir = Files.createDirectories(tempRoot.resolve("dir"));
        Files.createFile(dir.resolve("file1"));
        Files.createFile(tempRoot.resolve("file2"));

        String path = createPath(tempRoot);
        RemoteFile<C> remoteFile =
            m_fileHandler.createRemoteFile(new URI(m_type, m_host, path, null), m_connInfo, m_connectionMonitor);

        RemoteFile<C>[] dirContents = remoteFile.listFiles();

        // delete directory
        boolean deleted = dirContents[0].delete();
        dirContents = remoteFile.listFiles();
        assertThat("Directory could not be deleted", deleted, is(true));
        assertThat("Unexpected number of directory entries returned", dirContents.length, is(1));

        assertThat("Unexpected directory entry at position 1", dirContents[0].getName(), is("file2"));
        assertThat("Entry at position 1 is a directory", dirContents[0].isDirectory(), is(false));

        // delete file
        RemoteFile<C> temp = dirContents[0];
        deleted = dirContents[0].delete();
        dirContents = remoteFile.listFiles();
        assertThat("File could not be deleted", deleted, is(true));
        assertThat("Unexpected number of directory entries returned", dirContents.length, is(0));

        // delete same file as above (should return false)
        deleted = temp.delete();
        assertThat("File does exist", deleted, is(false));
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

        String path = createPath(tempRoot);
        RemoteFile<C> remoteFile =
            m_fileHandler.createRemoteFile(new URI(m_type, m_host, path, null), m_connInfo, m_connectionMonitor);

        RemoteFile<C>[] dirContents = remoteFile.listFiles();
        assertThat("Unexpected number of directory entries returned", dirContents.length, is(2));

        assertThat("Unexpected directory entry at position 0", dirContents[0].getName(), is("dir"));
        assertThat("Entry at position 0 is not a directory", dirContents[0].isDirectory(), is(true));

        assertThat("Unexpected directory entry at position 1", dirContents[1].getName(), is("file"));
        assertThat("Entry at position 1 is a directory", dirContents[1].isDirectory(), is(false));

        // list on file, should not return any entries
        path = file.toUri().getPath().replace("C:", "cygdrive/c"); // fix path for Windows
        remoteFile =
            m_fileHandler.createRemoteFile(new URI(m_type, m_host, path, null), m_connInfo, m_connectionMonitor);

        dirContents = remoteFile.listFiles();
        assertThat("Unexpected number of entries for file returned", dirContents.length, is(0));
    }

    /**
     * Test for {@link SFTPRemoteFile#mkDir()}.
     *
     * @throws Exception
     */
    @Test
    public void testMkDir() throws Exception {
        Path tempRoot = PathUtils.createTempDir(getClass().getName());

        String path = createPath(tempRoot.resolve("dir"));
        RemoteFile<C> remoteFile =
            m_fileHandler.createRemoteFile(new URI(m_type, m_host, path, null), m_connInfo, m_connectionMonitor);

        remoteFile.mkDir();
        RemoteFile<C>[] dirContents = remoteFile.getParent().listFiles();
        assertThat("Unexpected number of directory entries returned", dirContents.length, is(1));

        assertThat("Unexpected directory entry at position 0", dirContents[0].getName(), is("dir"));
        assertThat("Entry at position 0 is a directory", dirContents[0].isDirectory(), is(true));


        boolean b = remoteFile.mkDir();
        assertThat("Was not a directory", b, is(false));
    }

    /**
     * Test for {@link SFTPRemoteFile#mkDir()}.
     *
     * @throws Exception
     */
    @Test(expected = FileAlreadyExistsException.class)
    public void testMkDirFileExists() throws Exception {
        Path tempRoot = PathUtils.createTempDir(getClass().getName());

        Path file = Files.createFile(tempRoot.resolve("dir"));

        String path = createPath(file);
        RemoteFile<C> remoteFile =
            m_fileHandler.createRemoteFile(new URI(m_type, m_host, path, null), m_connInfo, m_connectionMonitor);
        remoteFile.mkDir();
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

        RemoteFile<C> remoteFile =
            m_fileHandler.createRemoteFile(new URI(m_type, m_host, tempRoot.toString(), null), m_connInfo,
                m_connectionMonitor);

        RemoteFile<C>[] dirContents = remoteFile.listFiles();
        assertThat("Unexpected number of directory entries returned", dirContents.length, is(4));

        assertThat("Unexpected directory entry at position 0", dirContents[0].getName(), is("AAAdir"));
        assertThat("Entry at position 0 is not a directory", dirContents[0].isDirectory(), is(true));

        // entry at position 1 is "dir", because directories are sorted before files

        assertThat("Unexpected directory entry at position 2", dirContents[2].getName(), is("AAAfile"));
        assertThat("Entry at position 2 is a directory", dirContents[2].isDirectory(), is(false));
    }
}