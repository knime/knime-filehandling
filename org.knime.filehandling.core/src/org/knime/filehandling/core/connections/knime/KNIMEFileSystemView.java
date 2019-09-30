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
 *   Sep 16, 2019 (Tobias Urhaug, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.core.connections.knime;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.filechooser.FileSystemView;

import org.knime.filehandling.core.filechooser.NioFile;

/**
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
public class KNIMEFileSystemView extends FileSystemView {

    /**
     * The base of this file system view, can either be a workflow, node, or a mount point location.
     */
    private final Path m_basePath;
    /**
     * The type of KNIME URL that is used for this view (Workflow relative, Node relative or Mount Point relative).
     */
    private final String m_knimeURLType;
    private final String m_baseString;
    private final FileSystem m_fileSystem;
    private final Set<Path> m_rootDirectories;

    public KNIMEFileSystemView(final KNIMEFileSystem fileSystem) {
        m_baseString = fileSystem.getBase();
        m_basePath = Paths.get(fileSystem.getBase());
        m_knimeURLType = fileSystem.getKNIMEURLType();
//        Path emptyRelativePath = Paths.get("");
//        m_base = new KNIMEPath(fileSystem, emptyRelativePath);
        m_fileSystem = fileSystem;
        m_rootDirectories = new LinkedHashSet<>();
        m_fileSystem.getRootDirectories().forEach(m_rootDirectories::add);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File getDefaultDirectory() {
        return new NioFile(m_baseString, m_fileSystem);
    }

    @Override
    public File getHomeDirectory() {
        return new NioFile(m_baseString, m_fileSystem);
    }

    @Override
    public File createFileObject(final String path) {

        // Resolve the path here!!
        Path input = Paths.get(path);

        if (!input.isAbsolute()) {
            Path resolved = m_basePath.resolve(input);
            Path normalized = resolved.normalize();
            return new NioFile(normalized);
        }

        return new NioFile(path, m_fileSystem);
    }

    @Override
    public Boolean isTraversable(final File f) {
        return Boolean.valueOf(f.isDirectory());
    }

    @Override
    public boolean isRoot(final File f) {
        if (f != null) {
            return m_rootDirectories.stream().anyMatch(p -> p.equals(f.toPath()));
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File getParentDirectory(final File dir) {
        if (dir != null) {
            String path = dir.getPath();
        }

        return super.getParentDirectory(dir);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File createNewFolder(final File containingDir) throws IOException {
        final Path newFolder = m_fileSystem.getPath(containingDir.getPath(), "newFolder/");
        Files.createDirectory(newFolder);
        return newFolder.toFile();
    }
}
