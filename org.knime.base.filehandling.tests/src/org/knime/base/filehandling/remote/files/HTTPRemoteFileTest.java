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
 *   08.12.2014 (tibuch): created
 */
package org.knime.base.filehandling.remote.files;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.time.ZonedDateTime;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformation;
import org.knime.core.util.KnimeEncryption;

/**
 *
 * @author Tim-Oliver Buchholz, KNIME AG, Zurich, Switzerland
 */
public class HTTPRemoteFileTest extends RemoteFileTest<Connection> {


    @Before
    @Override
    public void setup() {
        m_connInfo = new ConnectionInformation();
        m_connInfo.setHost("httpbin.testing.knime.com");
        m_connInfo.setProtocol("https");
        m_connInfo.setPort(443);

        m_type = "https";
        m_host = "httpbin.testing.knime.com";

        m_connectionMonitor = new ConnectionMonitor<Connection>();

        m_fileHandler = new HTTPRemoteFileHandler();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isEnabled() {
        return true; // works on both systems
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testMkDirFileExists() throws Exception {
        // not supported by http
        // test expects exception
        throw new FileAlreadyExistsException("");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testDelete() throws Exception {
        // not supported by http
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testListFiles() throws Exception {
        // not supported by http
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testListFilesSymlinks() throws Exception {
        // not supported by http
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testIsDirectory() throws Exception {
        // not supported by http
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testMove() throws Exception {
        // not supported by http
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testMkDir() throws Exception {
        // not supported by http
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testExits() throws Exception {

        RemoteFile<Connection> remoteFile =
            m_fileHandler.createRemoteFile(new URI(m_type, m_host, "/get", null), m_connInfo, m_connectionMonitor);

        boolean exists = remoteFile.exists();
        assertThat("Entry at position 1 does not exists", exists, is(true));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testGetName() throws Exception {

        RemoteFile<Connection> remoteFile =
            m_fileHandler.createRemoteFile(new URI(m_type, m_host, "/folder/README.txt", null), m_connInfo, m_connectionMonitor);

        assertThat("Incorrect path of file", remoteFile.getName(), equalTo("README.txt"));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testLastModified() throws Exception {

        RemoteFile<Connection> remoteFile =
            m_fileHandler.createRemoteFile(
                new URI(m_type, m_host, "/response-headers", "Last-Modified=Wed, 21 Oct 2015 07:28:00 GMT", null),
                m_connInfo, m_connectionMonitor);

        // if this test fails you should check the last modified entry on the test-server.
        // README.txt is expected to never getting modified.
        long expected = ZonedDateTime.parse("2015-10-21T07:28:00+00:00[GMT]").toEpochSecond();
        long actual = remoteFile.lastModified();

        assertThat("LastModified time is not equal", actual, equalTo(expected));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testGetSize() throws Exception {

        long expectedSize = 37;
        RemoteFile<Connection> remoteFile =
            m_fileHandler.createRemoteFile(new URI(m_type, m_host, "/bytes/" + expectedSize, null), m_connInfo,
                m_connectionMonitor);

        long actualSize = remoteFile.getSize();
        assertThat("Size is not equal", actualSize, equalTo(expectedSize));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String createPath(final Path p) {
        // cannot really return anything useful here since all the tests use different
        // httpbin endpoints
        return "/";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testOpenOutputStream() throws Exception {
        // not supported by http
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testGetPath() throws Exception {
        // not supported by http
    }

    /**
     * Test for {@link HTTPRemoteFile#openInputStream()} with authentication.
     *
     * @throws Exception if communication did not work
     */
    @Test
    public void testOpenInputStreamWithUserInfo() throws Exception {

        m_connInfo.setPassword(KnimeEncryption.encrypt("passwd".toCharArray()));
        URI u = new URI(m_type, "user", m_host, 443, "/basic-auth/user/passwd", null, null);
        RemoteFile<Connection> remoteFile =
                m_fileHandler.createRemoteFile(u, m_connInfo, m_connectionMonitor);

        String expected = "{\"authorized\":true,\"user\":\"user\"}";
        try (InputStream stream = remoteFile.openInputStream()) {
            String str2 = IOUtils.toString(stream).replaceAll("\\s", "");
            assertThat("Strings do not have same length", str2.length(), is(expected.length()));
            assertThat("Strings are not equal", str2, equalTo(expected));
        }

        // test for if-statement which checks port < 0
        u = new URI(m_type, "user", m_host, -1, "/basic-auth/user/passwd", null, null);
        remoteFile =
                m_fileHandler.createRemoteFile(u, m_connInfo, m_connectionMonitor);

        try (InputStream stream = remoteFile.openInputStream()) {
            String str2 = IOUtils.toString(stream).replaceAll("\\s", "");
            assertThat("Strings do not have same length", str2.length(), is(expected.length()));
            assertThat("Strings are not equal", str2, equalTo(expected));
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void testOpenInputStream() throws Exception {
        RemoteFile<Connection> remoteFile =
            m_fileHandler.createRemoteFile(new URI(m_type, m_host, "/base64/encode/teststring", null), m_connInfo, m_connectionMonitor);

        String expected = "dGVzdHN0cmluZw=="; // this is "teststring" in base64
        try (InputStream stream = remoteFile.openInputStream()) {
            String str2 = IOUtils.toString(stream);
            assertThat("Strings do not have same length", str2.length(), is(expected.length()));
            assertThat("Strings are not equal", str2, equalTo(expected));
        }
    }
}
