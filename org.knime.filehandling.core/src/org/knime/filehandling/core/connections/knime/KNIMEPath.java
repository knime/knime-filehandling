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
 *   Sep 3, 2019 (Tobias Urhaug, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.core.connections.knime;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.knime.filehandling.core.connections.base.GenericPathUtil;
import org.knime.filehandling.core.connections.base.UnixStylePathUtil;

/**
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
public class KNIMEPath implements Path {

    private final KNIMEFileSystem m_fileSystem;
    private final String[] m_pathComponents;

    private Path m_path;

    private boolean m_hasRootComponent;
    private boolean m_isAbsolute;


    public KNIMEPath (final KNIMEFileSystem fileSystem, final String first, final String... more) {
        m_fileSystem = fileSystem;

        m_isAbsolute = false;

        String unixSeperatedPath = first.replaceAll("\\\\", "/");
        String[] pathComponentsArray = UnixStylePathUtil.toPathComponentsArray(unixSeperatedPath);
        m_pathComponents = ArrayUtils.addAll(pathComponentsArray, more);

        m_path = Paths.get(first, more);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileSystem getFileSystem() {
        return m_fileSystem;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAbsolute() {
        return m_isAbsolute;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path getRoot() {
        return new KNIMEPath(m_fileSystem, "");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path getFileName() {
        if (m_pathComponents.length == 0) {
            return null;
        }

        final String fileName = m_pathComponents[m_pathComponents.length - 1];
        return new KNIMEPath(m_fileSystem, fileName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path getParent() {
        if (m_pathComponents.length < 2) {
            return null;
        }

        String first = m_pathComponents[0];
        String[] more = Arrays.copyOfRange(m_pathComponents, 1, m_pathComponents.length - 1);
        return new KNIMEPath(m_fileSystem, first, more);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNameCount() {
        return m_pathComponents.length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path getName(final int index) {
        return new KNIMEPath(m_fileSystem, m_pathComponents[index]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path subpath(final int beginIndex, final int endIndex) {
        if (beginIndex < 0 || m_pathComponents.length < endIndex) {
            throw new IllegalArgumentException("Begin or end index is out of range");
        }

        if (endIndex <= beginIndex) {
            throw new IllegalArgumentException("End index must be greater than begin index");
        }

        String first = m_pathComponents[beginIndex];
        String[] more = Arrays.copyOfRange(m_pathComponents, beginIndex + 1, endIndex);

        return new KNIMEPath(m_fileSystem, first, more);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean startsWith(final Path other) {
        if (!other.getFileSystem().equals(m_fileSystem)) {
            return false;
        }

        return GenericPathUtil.startsWith(this, other);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean startsWith(final String other) {
        KNIMEPath knimePath = new KNIMEPath(m_fileSystem, other);
        return GenericPathUtil.startsWith(this, knimePath);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean endsWith(final Path other) {
        if (!other.getFileSystem().equals(m_fileSystem)) {
            return false;
        }

        return GenericPathUtil.endsWith(this, other);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean endsWith(final String other) {
        KNIMEPath knimePath = new KNIMEPath(m_fileSystem, other);
        return GenericPathUtil.endsWith(this, knimePath);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path normalize() {
        return new KNIMEPath(m_fileSystem, m_path.normalize().toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path resolve(final Path other) {
        if (other.getFileSystem() != m_fileSystem) {
            throw new IllegalArgumentException("Cannot resolve paths across different file systems");
        }

        final KNIMEPath otherPath = (KNIMEPath)other;
        if (other.isAbsolute()) { return other; }
        if (other.getNameCount() == 0) { return this; }

        return new KNIMEPath(m_fileSystem, pathAsString(), otherPath.m_pathComponents);
    }

    private String pathAsString() {
        return Arrays.stream(m_pathComponents).collect(Collectors.joining(m_fileSystem.getSeparator()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path resolve(final String other) {
        return new KNIMEPath(m_fileSystem, pathAsString(), other);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path resolveSibling(final Path other) {
        if (other.getFileSystem() != m_fileSystem) {
            throw new IllegalArgumentException("Cannot resolve paths across different file systems");
        }

        final KNIMEPath otherPath = (KNIMEPath)other;
        if (getParent() == null || other.isAbsolute()) { return other; }
        if (other.getNameCount() == 0) { return this; }

        return new KNIMEPath(m_fileSystem, getParent().toString(), otherPath.m_pathComponents);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path resolveSibling(final String other) {
        if (getParent() == null ) { return new KNIMEPath(m_fileSystem, other); }
        if (StringUtils.isEmpty(other)) { return this; }

        return new KNIMEPath(m_fileSystem, getParent().toString(), other);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path relativize(final Path other) {
        if (other.getFileSystem() != m_fileSystem) {
            throw new IllegalArgumentException("Cannot relativize paths across different file systems");
        }





        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URI toUri() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path toAbsolutePath() {
        if (!m_path.isAbsolute()) {


        }


        return m_path;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path toRealPath(final LinkOption... options) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File toFile() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WatchKey register(final WatchService watcher, final Kind<?>[] events, final Modifier... modifiers) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WatchKey register(final WatchService watcher, final Kind<?>... events) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<Path> iterator() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final Path other) {
        // TODO Auto-generated method stub
        return 0;
    }


    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final KNIMEPath other = (KNIMEPath)o;
        return m_path.equals(other.m_path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return m_path.hashCode();
    }

    @Override
    public String toString() {
        return pathAsString();
    }

}
