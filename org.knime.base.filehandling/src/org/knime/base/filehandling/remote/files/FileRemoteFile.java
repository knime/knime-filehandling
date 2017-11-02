/*
 * ------------------------------------------------------------------------
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
 *   Nov 2, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.remote.files;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.knime.core.node.ExecutionContext;

/**
 * Implementation of the file remote file.
 *
 *
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 */
public class FileRemoteFile extends RemoteFile<Connection> {

    private Boolean m_existsCache = null;

    private Boolean m_isdirCache = null;

    private Long m_sizeCache = null;

    private Long m_modifiedCache = null;

    private void resetCache() {
        // Empty cache
        m_existsCache = null;
        m_isdirCache = null;
        m_sizeCache = null;
        m_modifiedCache = null;
    }

    /**
     * Creates a file remote file for the given URI.
     *
     *
     * @param uri The URI
     */
    FileRemoteFile(final URI uri) {
        super(uri, null, null);
    }

    private FileRemoteFile(final URI uri, final boolean isDirectory) {
        this(uri);
        m_isdirCache = isDirectory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean usesConnection() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Connection createConnection() {
        // Does not use a connection
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getType() {
        return "file";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exists() throws Exception {
        if (m_existsCache == null) {
            m_existsCache = new File(getURI()).exists();
        }
        return m_existsCache;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDirectory() throws Exception {
        if (m_isdirCache == null) {
            m_isdirCache = new File(getURI()).isDirectory();
        }
        return m_isdirCache;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void move(final RemoteFile<Connection> file, final ExecutionContext exec) throws Exception {
        try {
            if (file instanceof FileRemoteFile) {
                final FileRemoteFile source = (FileRemoteFile)file;
                Path toMove = Paths.get(source.getURI());
                Path newName = Paths.get(getURI()).resolve(source.getName());
                Files.move(toMove, newName);
            } else {
                super.move(file, exec);
            }
        } finally {
            resetCache();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream openInputStream() throws Exception {
        return new FileInputStream(new File(getURI()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputStream openOutputStream() throws Exception {
        try {
            return new FileOutputStream(new File(getURI()));
        } finally {
            resetCache();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSize() throws Exception {
        if (m_sizeCache == null) {
            m_sizeCache = new File(getURI()).length();
        }
        return m_sizeCache;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long lastModified() throws Exception {
        if (m_modifiedCache == null) {
            m_modifiedCache = new File(getURI()).lastModified() / 1000;
        }
        return m_modifiedCache;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean delete() throws Exception {
        try {
            return deleteRecursively(getURI().getPath());
        } finally {
            resetCache();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RemoteFile<Connection>[] listFiles() throws Exception {
        final List<FileRemoteFile> files = new ArrayList<FileRemoteFile>();
        if (isDirectory()) {
            final Path current = new File(getURI()).toPath();
            Set<FileVisitOption> options = new HashSet<FileVisitOption>();
            options.add(FileVisitOption.FOLLOW_LINKS);
            Files.walkFileTree(current, options, 2, new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
                    if(dir.equals(current)) {
                        return FileVisitResult.CONTINUE;
                    }
                    files.add(new FileRemoteFile(removeTripleSlash(dir.toUri()), true));
                    return FileVisitResult.SKIP_SUBTREE;
                }
                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                    if(!file.equals(current)) {
                        files.add(new FileRemoteFile(removeTripleSlash(file.toUri()), false));
                    }
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult visitFileFailed(final Path file, final IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        Collections.sort(files);
        return files.toArray(new FileRemoteFile[files.size()]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean mkDir() throws Exception {
        try {
            Path f = Paths.get(getURI());
            if (Files.isDirectory(f)) {
                return false;
            } else {
                Files.createDirectory(f);
                return true;
            }
        } finally {
            resetCache();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RemoteFile<Connection> getParent() throws Exception {
        final File parentFile = new File(getURI()).getParentFile();
        if (parentFile == null) {
            // This file has no parent
            return null;
        }
        // Build URI
        final URI uri = parentFile.toURI();
        // Create remote file and open it
        final RemoteFile<Connection> file = new FileRemoteFile(uri);
        file.open();
        return file;
    }

    /**
     * Deletes files and directories recursively.
     *
     *
     * @param path Path to the file or directory
     * @return true if deletion was successful, false otherwise
     */
    private boolean deleteRecursively(final String path) {
        final File file = new File(path);
        if (file.isDirectory()) {
            // Get files in directory
            final String[] files = file.list();
            for (final String file2 : files) {
                // Delete each file recursively
                deleteRecursively(new File(file, file2).getAbsolutePath());
            }
        }
        // Delete this file
        return file.delete();
    }

    private URI removeTripleSlash(URI uri) {
        try {
            if (uri.getSchemeSpecificPart().startsWith("///")) {
                uri = new URI(uri.getScheme(), uri.getSchemeSpecificPart().substring(2), uri.getFragment());
            }
        } catch (URISyntaxException e) {
            // return the original
        }
        return uri;
    }

}
