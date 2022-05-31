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
package org.knime.archive.zip.filehandling.fs;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipException;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.archivers.zip.ZipSplitReadOnlySeekableByteChannel;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.lang3.StringUtils;
import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.connections.base.BaseFileSystem;
import org.knime.filehandling.core.defaultnodesettings.ExceptionUtil;

/**
 * ArchiveZip {@link FileSystem}.
 *
 * @author Dragan Keselj, KNIME GmbH
 */
public class ArchiveZipFileSystem extends BaseFileSystem<ArchiveZipPath> {

    /**
     * Character to use as path separator
     */
    public static final String SEPARATOR = "/";

    private final ArchiveZipFile m_zipFile;

    /**
     * A map of zip file entry names with their paths.
     */
    private final Map<String, ArchiveZipPath> m_entryNamesMap;

    /**
     * A map of paths for every zip file entry name.
     */
    private final Map<ArchiveZipPath, String> m_pathsMap;

    /**
     * Map of directory paths with paths of their content
     */
    private final Map<ArchiveZipPath, Set<ArchiveZipPath>> m_childrenMap;

    /**
     * Constructor.
     *
     * @param config
     *            The file system configuration
     * 
     * @throws IOException
     */
    protected ArchiveZipFileSystem(final ArchiveZipFSConnectionConfig config) throws IOException {
        super(new ArchiveZipFileSystemProvider(config), //
                0, //
                ArchiveZipFileSystem.SEPARATOR, //
                config.createFSLocationSpec());

        final String zipFilePath = config.getZipFilePath();
        SeekableByteChannel byteChannel = null;
        ArchiveZipFile zipFile = null;
        if (StringUtils.isBlank(zipFilePath)) {
            throw new IllegalArgumentException("Zip file path is empty!");
        }
        try {
            final Path filePath = Path.of(zipFilePath);
            final var byteChannels = getOrderedZipSegmentByteChannels(filePath);
            byteChannel = byteChannels.size() == 1 ? byteChannels.get(0)
                    : new ZipSplitReadOnlySeekableByteChannel(byteChannels);
            zipFile = new ArchiveZipFile(byteChannel);

            final Map<String, ArchiveZipPath> entryNamesMap = new HashMap<>();
            final Map<ArchiveZipPath, String> pathsMap = new HashMap<>();
            final Map<ArchiveZipPath, Set<ArchiveZipPath>> childrenMap = new HashMap<>();
            final Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();
            while (entries.hasMoreElements()) {
                final ZipArchiveEntry entry = entries.nextElement();
                if (entryNamesMap.containsKey(entry.getName())) {
                    throw new ZipException("Multiple zip entries with same name: " + entry.getName());
                }
                final ArchiveZipPath path = createPath(zipFile, entry);
                if (pathsMap.containsKey(path)) {
                    throw new ZipException(
                            "Same path " + path + " for zip entries " + entry + ", " + pathsMap.get(path));
                }
                entryNamesMap.put(entry.getName(), path);
                pathsMap.put(path, entry.getName());
                if (!zipFile.getTopEntries().contains(entry)) {
                    addChildPath(childrenMap, path.getParent(), path);
                }
            }
            m_entryNamesMap = Collections.unmodifiableMap(entryNamesMap);
            m_pathsMap = Collections.unmodifiableMap(pathsMap);
            m_childrenMap = Collections.unmodifiableMap(childrenMap);
            m_zipFile = zipFile;
        } catch (Exception ex) {
            closeQuietly(byteChannel);
            ZipFile.closeQuietly(zipFile);
            throw ExceptionUtil.wrapAsIOException(ex);
        }
    }

    private void addChildPath(Map<ArchiveZipPath, Set<ArchiveZipPath>> childrenMap, ArchiveZipPath parentPath,
            ArchiveZipPath childPath) {
        if (parentPath == null) {
            parentPath = (ArchiveZipPath) getRootDirectories().iterator().next();
        }
        if (childrenMap.containsKey(parentPath)) {
            childrenMap.get(parentPath).add(childPath);
        } else {
            var children = new LinkedHashSet<ArchiveZipPath>();
            children.add(childPath);
            childrenMap.put(parentPath, children);
        }
    }

    ArchiveZipFile getZipFile() {
        return m_zipFile;
    }

    /**
     * Tries to find all segments for a given split zip file archive and creates a
     * {@link SeekableByteChannel} object for each. The returned list must be in a
     * specific order (z01, z02, ..., zip).
     * 
     * @param filePath
     *            The path points to a zip archive segment. Segment's extensions
     *            must be either .zip or any of .z01, z02...
     * @return Ordered {@code List<SeekableByteChannel>}
     * @throws IOException
     */
    private List<SeekableByteChannel> getOrderedZipSegmentByteChannels(Path filePath) throws IOException {
        final List<Path> files = Files.list(filePath.getParent()).collect(Collectors.toList());
        final Pattern extensionPattern = Pattern.compile("([z][0-9]+)|(zip)$", Pattern.CASE_INSENSITIVE);
        final List<Path> zipSegments = new ArrayList<>();
        for (final Path file : files) {
            if (!FSFiles.isDirectory(filePath) && getFileBaseName(file).equalsIgnoreCase(getFileBaseName(filePath))
                    && extensionPattern.matcher(getFileExtension(file)).matches()) {
                zipSegments.add(file);
            }
        }
        zipSegments.sort(new ZipSegmentComparator());

        final var seekableByteChannels = new ArrayList<SeekableByteChannel>();
        try {
            for (final Path zipSegment : zipSegments) {
                seekableByteChannels.add(Files.newByteChannel(zipSegment));
            }
        } catch (Exception ex) {
            seekableByteChannels.stream().forEach(this::closeQuietly);
            throw ExceptionUtil.wrapAsIOException(ex);
        }
        return seekableByteChannels;
    }

    private void closeQuietly(final Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Throwable e) {
            // ignore
        }
    }

    /**
     * Finds the zip entry by a given path inside the zip file.
     * 
     * @param path
     *            (of file or directory) inside the zip file.
     * @return {@link ZipArchiveEntry} corresponds to the given path.
     * @throws IOException
     * @throws IOException
     *             if I/O error occurs while getting the zip entry.
     */
    ZipArchiveEntry getEntry(final ArchiveZipPath path) throws IOException {
        if (path.isRoot()) {
            return null;
        }
        final String entryName = m_pathsMap.get(path);
        if (entryName == null) {
            throw new NoSuchFileException(path.toString());
        }
        return m_zipFile.getEntry(entryName);
    }

    /**
     * Finds the path inside the zip file from a given zip entry.
     * 
     * @param entry
     * @return the path (of file or directory) inside the zip file of a given zip
     *         entry.
     * @throws IOException
     */
    ArchiveZipPath getPath(final ZipArchiveEntry entry) {
        final var path = m_entryNamesMap.get(entry.getName());
        if (path == null) {
            throw new IllegalArgumentException("No path found for entry " + entry);
        }
        return path;
    }

    /**
     * Finds children inside a directory at a given path inside the zip file.
     * 
     * @param directoryPath
     *            path of a directory inside the zip file.
     * @return {@code Set<ArchiveZipPath>} object represents files and folders
     *         inside the directory or {@code null} if empty.
     * @throws IOException
     *             if I/O error occurs while finding children entries.
     */
    Set<ArchiveZipPath> getChildrenEntries(final ArchiveZipPath directoryPath) throws IOException {
        return m_childrenMap.get(directoryPath);
    }

    /**
     * Creates the appropriate path for a given zip entry.
     * 
     * @param zipFile
     * @param entry
     * @return the path (of file or directory) inside the zip file of a given zip
     *         entry.
     */
    ArchiveZipPath createPath(final ArchiveZipFile zipFile, final ZipArchiveEntry entry) {
        if (!zipFile.hasTopEntries()) {
            return getPath(getSeparator(), entry.getName());
        } else {
            final ZipArchiveEntry fullTopPathEntry = zipFile.getTopEntries().get(zipFile.getTopEntries().size() - 1);
            final ArchiveZipPath fullTopPath = getPath(getSeparator(), fullTopPathEntry.getName());
            final ArchiveZipPath entryPath = getPath(getSeparator(), entry.getName());
            final ArchiveZipPath relativePath = getPath(getSeparator(), fullTopPath.relativize(entryPath).toString());
            return relativePath;
        }
    }

    private String getFileBaseName(Path path) {
        return FileNameUtils.getBaseName(path.getFileName().toString());
    }

    private String getFileExtension(Path path) {
        return FileNameUtils.getExtension(path.getFileName().toString());
    }

    @Override
    public ArchiveZipFileSystemProvider provider() {
        return (ArchiveZipFileSystemProvider) super.provider();
    }

    @Override
    protected void prepareClose() throws IOException {
        ZipFile.closeQuietly(m_zipFile);
    }

    @Override
    public ArchiveZipPath getPath(final String first, final String... more) {
        return new ArchiveZipPath(this, first, more);
    }

    @Override
    public String getSeparator() {
        return ArchiveZipFileSystem.SEPARATOR;
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        return Collections.singletonList(getPath(ArchiveZipFileSystem.SEPARATOR));
    }

    private static class ZipSegmentComparator implements Comparator<Path> {

        @Override
        public int compare(final Path path1, final Path path2) {
            final String extension1 = FileNameUtils.getExtension(path1.getFileName().toString()).toLowerCase();
            final String extension2 = FileNameUtils.getExtension(path2.getFileName().toString()).toLowerCase();

            if (extension1.equals("zip")) {
                return 1;
            }

            if (!extension1.startsWith("z")) {
                return -1;
            }

            if (!extension2.startsWith("z")) {
                return 1;
            }

            final Integer splitSegmentNumber1 = Integer.parseInt(extension1.substring(1));
            final Integer splitSegmentNumber2 = Integer.parseInt(extension2.substring(1));

            return splitSegmentNumber1.compareTo(splitSegmentNumber2);
        }
    }
}
