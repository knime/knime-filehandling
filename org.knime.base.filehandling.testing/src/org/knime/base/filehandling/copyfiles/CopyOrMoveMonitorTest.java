/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ------------------------------------------------------------------------
 *
 * History
 *   Oct 11, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.copyfiles;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.knime.core.util.FileUtil;

/**
 * JUnit test for the copy or move monitor class. Checks if the
 * <code>isNewFile()</code> and <code>rollback()</code> methods work correctly.
 *
 *
 * @author Patrick Winter, KNIME.com, Zurich, Switzerland
 */
public class CopyOrMoveMonitorTest {
    private File m_tempDir;

    private List<File> m_files;

    /**
     * Creates a temporary directory and puts a bunch of files into it.
     *
     *
     * @throws IOException If the files or the folder could not be created
     */
    @Before
    public void setupFiles() throws IOException {
        String[] filenames = new String[]{"file1", "file2", "file3"};
        m_tempDir = FileUtil.createTempDir("MonitorTest");
        m_files = new ArrayList<File>();
        for (int i = 0; i < filenames.length; i++) {
            File file = new File(m_tempDir, filenames[i]);
            if (!file.createNewFile()) {
                throw new IOException("File " + file + " could not be created");
            }
            m_files.add(file);
        }
    }

    /**
     * Removes the temporary directory.
     *
     *
     * @throws IOException If a correct cleanup was not possible
     */
    @After
    public void cleanupFiles() throws IOException {
        FileUtils.deleteDirectory(m_tempDir);
    }

    /**
     * Make a copy of each file and then a rollback (deleting the new files).
     *
     *
     * @throws Exception If the test fails
     */
    @Test
    public void copyRollback() throws Exception {
        String action = CopyOrMove.COPY.getName();
        SimpleDirectoryComparer comparer = new SimpleDirectoryComparer(m_tempDir);
        CopyOrMoveMonitor monitor = new CopyOrMoveMonitor(action);
        doToFiles(action, monitor);
        monitor.rollback();
        if (comparer.different(m_tempDir)) {
            throw new Exception("Files have changed");
        }
    }

    /**
     * Move each file to a new location and then make a rollback (moving them
     * back to there original place).
     *
     *
     * @throws Exception If the test fails
     */
    @Test
    public void moveRollback() throws Exception {
        String action = CopyOrMove.MOVE.getName();
        SimpleDirectoryComparer comparer = new SimpleDirectoryComparer(m_tempDir);
        CopyOrMoveMonitor monitor = new CopyOrMoveMonitor(action);
        doToFiles(action, monitor);
        monitor.rollback();
        if (comparer.different(m_tempDir)) {
            throw new Exception("Files have changed");
        }
    }

    /**
     * Check if unregistered files are identified as new and registered files as
     * old.
     *
     *
     * @throws Exception If the test fails
     */
    @Test
    public void touchedOrNot() throws Exception {
        CopyOrMoveMonitor monitor = new CopyOrMoveMonitor(CopyOrMove.COPY.getName());
        if (!monitor.isNewFile("file")) {
            throw new Exception("Untouched file has been identified as touched");
        }
        monitor.registerFiles("source", "target");
        if (monitor.isNewFile("source")) {
            throw new Exception("Touched source has not been identified as touched");
        }
        if (monitor.isNewFile("target")) {
            throw new Exception("Touched target has not been identified as touched");
        }
    }

    /**
     * Copies or moves the files according to the action and registers the
     * handled files in the monitor.
     *
     *
     * @param action Whether to copy or move the files
     * @param monitor Where the files will be registered
     * @throws IOException If the file operation fails
     */
    private void doToFiles(final String action, final CopyOrMoveMonitor monitor) throws IOException {
        for (File file : m_files) {
            String source = file.getAbsolutePath();
            String target = source + ".new";
            monitor.registerFiles(source, target);
            if (action.equals(CopyOrMove.COPY.getName())) {
                FileUtils.copyFile(new File(source), new File(target));
            }
            if (action.equals(CopyOrMove.MOVE.getName())) {
                FileUtils.moveFile(new File(source), new File(target));
            }
        }
    }

    /**
     * Checks if the directory given at creation time and the compare directory
     * contain the same files. Only the filenames will be checked no attributes.
     * Does not work recursively. The list of files is read at construction
     * time, so the same directory can differ over time.
     *
     *
     * @author Patrick Winter, KNIME.com, Zurich, Switzerland
     */
    private static class SimpleDirectoryComparer {

        private File[] m_filelist;

        /**
         * Constructer reads the files names present in the directory.
         *
         *
         * @param directory Original directory
         */
        public SimpleDirectoryComparer(final File directory) {
            m_filelist = directory.listFiles();
        }

        /**
         * Compares the filenamelist of the directory to the one created at
         * construction time.
         *
         *
         * @param directory Directory to check against
         * @return true if the filelists differ, false otherwise
         */
        public boolean different(final File directory) {
            boolean result = false;
            File[] newfilelist = directory.listFiles();
            if (m_filelist.length != newfilelist.length) {
                result = true;
            }
            for (int i = 0; i < m_filelist.length; i++) {
                if (!m_filelist[i].equals(newfilelist[i])) {
                    result = true;
                }
            }
            return result;
        }

    }

}
