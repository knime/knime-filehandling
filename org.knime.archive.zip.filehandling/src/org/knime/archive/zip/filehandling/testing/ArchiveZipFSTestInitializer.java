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
 *   2022-04-27 (Dragan Keselj): created
 */
package org.knime.archive.zip.filehandling.testing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;

import org.knime.archive.zip.filehandling.fs.ArchiveZipFileSystem;
import org.knime.archive.zip.filehandling.fs.ArchiveZipPath;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.testing.DefaultFSTestInitializer;
import org.knime.filehandling.core.testing.FSTestInitializer;

/**
 * {@link FSTestInitializer} for the ArchiveZip file system.
 *
 * @author Dragan Keselj, KNIME GmbH
 */
class ArchiveZipFSTestInitializer extends DefaultFSTestInitializer<ArchiveZipPath, ArchiveZipFileSystem> {

    /**
     * @param fsConnection
     *            FS connection.
     */
    protected ArchiveZipFSTestInitializer(final FSConnection fsConnection) {
        super(fsConnection);
    }

    @Override
    public ArchiveZipPath createFileWithContent(final String content, final String... pathComponents)
            throws IOException {
        ArchiveZipPath path = makePath(pathComponents);
        // It is not possible to create files inside the zip file.
        // The archive must already contains pre-defined folders and files used in test
        // methods.
        if (!Files.exists(path)) {
            throw new NoSuchFileException(path.toString());
        }
        return path;
    }

    @Override
    public ArchiveZipPath makePath(final String... pathComponents) {
        return getFileSystem().getPath(getFileSystem().getSeparator(), pathComponents);
    }

    @Override
    public ArchiveZipPath getTestCaseScratchDir() {
        return (ArchiveZipPath) getFileSystem().getRootDirectories().iterator().next();
    }

    @Override
    protected void beforeTestCaseInternal() throws IOException {
        //
    }

    @Override
    protected void afterTestCaseInternal() throws IOException {
        // FIXME: cleanup, e.g. delete scratch dir (see getTestCaseScratchDir())  //NOSONAR
    }

    @Override
    public void afterClass() throws IOException {
        // FIXME: any final cleanup after the tests  //NOSONAR
    }
}
