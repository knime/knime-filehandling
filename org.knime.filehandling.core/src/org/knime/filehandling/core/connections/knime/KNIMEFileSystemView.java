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
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.filechooser.FileSystemView;

/**
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
public class KNIMEFileSystemView extends FileSystemView {

    private final Path m_base;
    private final FileSystem m_fileSystem;
    private final Set<Path> m_rootDirectories;

    public KNIMEFileSystemView(final Path base, final KNIMEFileSystem fileSystem) {
        m_base = new KNIMEPath(fileSystem, base);
        m_fileSystem = fileSystem;
        m_rootDirectories = new LinkedHashSet<>();
        m_fileSystem.getRootDirectories().forEach(m_rootDirectories::add);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File getDefaultDirectory() {
        return new NioFile(m_base.toString());
    }

    @Override
    public File getHomeDirectory() {
        return new NioFile(m_base.toString());
    }

    @Override
    public File createFileObject(final String path) {
        return new NioFile(path);
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
    public File createNewFolder(final File containingDir) throws IOException {
        final Path newFolder = m_fileSystem.getPath(containingDir.getPath(), "newFolder/");
        Files.createDirectory(newFolder);
        return newFolder.toFile();
    }

    private final class NioFile extends File {

        private static final long serialVersionUID = -5343680976176201255L;

        private final Path m_path;

        private NioFile(final Path p) {
            super(p.toString());
            m_path = p;
        }

        NioFile(final String pathname) {
            super(pathname);
            m_path = m_fileSystem.getPath(pathname);
        }

        public NioFile(final File parent, final String child) {
            super(parent, child);
            m_path = null;
        }

        @Override
        public boolean exists() {
            return Files.exists(m_path);
        }

        @Override
        public boolean isDirectory() {
            return Files.isDirectory(m_path);
        }

        @Override
        public File getCanonicalFile() throws IOException {
            return new NioFile(m_path.toString());
        }

        @Override
        public File[] listFiles() {

            final List<File> files = new ArrayList<>();
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(m_path)) {
                directoryStream.iterator().forEachRemaining(p -> files.add(new NioFile(p.toString())));
            } catch (final Exception ex) {
                // Log ...
            }
            return files.toArray(new NioFile[files.size()]);
        }

        @Override
        public String getPath() {
            return m_path.toString();
        }

        @Override
        public boolean canWrite() {
            return Files.isWritable(m_path);
        }

        @Override
        public long length() {
            try {
                return Files.size(m_path);
            } catch (final IOException ex) {
                return 0L;
            }
        }

        @Override
        public long lastModified() {
            try {
                return Files.getLastModifiedTime(m_path).toMillis();
            } catch (final IOException ex) {
                return 0L;
            }
        }

        @Override
        public String getAbsolutePath() {
            if (m_path.isAbsolute()) {
                return m_path.toString();
            } else {
                return m_path.resolve(m_base).toString();
            }
        }

        @Override
        public String getParent() {
            return m_path.getParent() == null ? null : m_path.getParent().toString();
        }

        @Override
        public File getParentFile() {
            return getParent() == null ? null : new NioFile(getParent());
        }

        @Override
        public String toString() {
            return "Wrapping path: " + m_path.toString();
        }

    }
}
