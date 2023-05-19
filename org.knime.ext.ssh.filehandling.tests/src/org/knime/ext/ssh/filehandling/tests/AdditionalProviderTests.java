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
 *   2020-08-04 (Vyacheslav Soldatov): created
 */

package org.knime.ext.ssh.filehandling.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSFileSystem;
import org.knime.filehandling.core.connections.FSFileSystemProvider;
import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.connections.FSPath;

/**
 * Additional to integration tests for SSH file system provider.
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 *
 */
public class AdditionalProviderTests {
    private static FSConnection m_connection;
    private FSFileSystem<FSPath> m_fileSystem;
    private FSFileSystemProvider<FSPath, FSFileSystem<FSPath>> m_provider;
    private FSPath m_tmpDir;

    /**
     * Default constructor.
     */
    public AdditionalProviderTests() {
        super();
    }

    /**
     * Tests file attributes really copied if StandardCopyOption.COPY_ATTRIBUTES
     * presented.
     *
     * @throws IOException
     */
    @Test
    public void test_when_move_file_copies_attributes() throws IOException {
        final FSPath f = m_provider.createTempFile(m_tmpDir, "tmp-", ".tmp");

        //set last modified time.
        final FileTime time = FileTime.fromMillis(System.currentTimeMillis() - 10000000l);
        Files.setAttribute(f, "lastModifiedTime", time);

        final FSPath out = m_provider.createTempDirectory(m_tmpDir, "tmp-", ".tmp");

        final FSPath copy = m_fileSystem.getPath(out.toString()
                + m_fileSystem.getSeparator() + "copy");
        Files.move(f, copy, StandardCopyOption.COPY_ATTRIBUTES);

        final FileTime lastModified = Files.getLastModifiedTime(copy);

        // time is stored in Epoch seconds, therefore should be compared
        // with quantity of 1 second (2 because possible approximations)
        assertEquals(time.toMillis(), lastModified.toMillis(), 2000);
    }

    // file attribute view tests.
    /**
     * Tests of using file attributes view, sets last modified time and check it is
     * really set.
     *
     * @throws IOException
     */
    @Test
    public void test_when_set_lastModified_it_should_be_changed() throws IOException {
        final FSPath f = m_provider.createTempFile(m_tmpDir, "tmp-", ".tmp");

        //set last modified time.
        final FileTime time = FileTime.fromMillis(System.currentTimeMillis() - 10000000l);
        Files.setLastModifiedTime(f, time);

        final FileTime lastModified = Files.getLastModifiedTime(f);

        // time is stored in Epoch seconds, therefore should be compared
        // with quantity of 1 second (2 because possible approximations)
        assertEquals(time.toMillis(), lastModified.toMillis(), 2000);
    }

    /**
     * Changes file permission (owner execute) and checks is it really changed.
     *
     * @throws IOException
     */
    @Test
    public void test_when_set_file_permissions_it_should_be_changed() throws IOException {
        final FSPath f = m_provider.createTempFile(m_tmpDir, "tmp-", ".tmp");

        //set last modified time.
        final Set<PosixFilePermission> perms = new HashSet<>();
        perms.add(PosixFilePermission.OWNER_EXECUTE);
        Files.setPosixFilePermissions(f, perms);

        final Set<PosixFilePermission> resultPerms = Files.getPosixFilePermissions(f);
        assertTrue(resultPerms.contains(PosixFilePermission.OWNER_EXECUTE));
    }

    /**
     * Tests the file starting with '.' is determined as hidden.
     *
     * @throws IOException
     */
    @Test
    public void test_hidden_files() throws IOException {
        final FSPath notHidden = m_provider.createTempFile(m_tmpDir, "not-", ".hidden");
        final FSPath hidden = m_provider.createTempFile(m_tmpDir, ".hidden-", ".tmp");

        // set last modified time.
        assertFalse(Files.isHidden(notHidden));
        assertTrue(Files.isHidden(hidden));
    }

    /**
     * Test access denied exception is thrown when attempts to create file inside of
     * root user home directory
     *
     * @throws IOException
     */
    @Test
    public void test_throws_accessDeniedException() throws IOException {
        FSPath path = m_fileSystem.getPath("/root/1");
        assertThrows(AccessDeniedException.class, () -> {
            m_provider.createDirectory(path);
        });
    }

    /**
     * Tests access denied exception while attempted to list of root user's home
     * folder
     *
     * @throws IOException
     */
    @Test
    public void test_list_root_folder() throws IOException {
        FSPath path = m_fileSystem.getPath("/root");
        assertThrows(AccessDeniedException.class, () -> {
            Files.list(path);
        });
    }

    /**
     * Finishes of current test, clear temporary directory.
     *
     * @throws IOException
     */
    @AfterEach
    public void afterTestCase() throws IOException {
        if (m_tmpDir != null) {
            FSFiles.deleteRecursively(m_tmpDir);
        }
    }

    /**
     * Initialized current test
     *
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    @BeforeEach
    public void beforeTestCase() throws IOException {
        this.m_fileSystem = (FSFileSystem<FSPath>) m_connection.getFileSystem();
        this.m_provider = (FSFileSystemProvider<FSPath, FSFileSystem<FSPath>>) m_fileSystem.provider();

        //create working directory
        final FSPath workDir = m_fileSystem.getWorkingDirectory();
        this.m_tmpDir = m_provider.createTempDirectory(workDir, "junit_", "_tmp");
    }

    /**
     * Creates connection.
     *
     * @throws IOException
     */
    @BeforeAll
    public static void beforeAll() throws IOException {
        m_connection = FsTestUtils.createConnection();
    }

    /**
     * Drops connection.
     *
     * @throws IOException
     */
    @AfterAll
    public static void afterAll() throws IOException {
        if (m_connection != null) {
            m_connection.close();
            m_connection = null;
        }
    }
}
