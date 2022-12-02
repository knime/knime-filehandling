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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.knime.archive.zip.filehandling.fs.ArchiveZipFSConnection;
import org.knime.archive.zip.filehandling.fs.ArchiveZipFSConnectionConfig;
import org.knime.archive.zip.filehandling.fs.ArchiveZipFSDescriptorProvider;
import org.knime.filehandling.core.connections.DefaultFSConnectionFactory;
import org.knime.filehandling.core.connections.DefaultFSLocationSpec;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.connections.meta.FSType;
import org.knime.filehandling.core.connections.meta.base.BaseFSConnectionConfig.BrowserRelativizationBehavior;
import org.knime.filehandling.core.defaultnodesettings.ExceptionUtil;
import org.knime.filehandling.core.testing.DefaultFSTestInitializerProvider;
import org.knime.filehandling.core.testing.FSTestInitializerProvider;

/**
 * {@link FSTestInitializerProvider} for the ArchiveZip file system.
 *
 * @author Dragan Keselj, KNIME GmbH
 */
public class ArchiveZipFSTestInitializerProvider extends DefaultFSTestInitializerProvider {
    /**
     * The full path to the external zip file will be used for testing. If not specified, the zip file will be created
     * in the temp directory automatically.
     */
    private static final String ZIP_FILE_PATH = "zipFile";

    private static final String WORKING_DIRECTORY_PREFIX = "workingDirPrefix";

    private static final String TEMP_DIRECTORY_NAME = "ziptest_1234567890";

    private static final String TEMP_SOURCE_DIRECTORY_NAME = "source";

    private static final String TEMP_ZIP_FILE_NAME = "test.zip";

    /**
     * The path to the directory where {@link TEMP_SOURCE_DIRECTORY_NAME } directory will be created with all files and
     * folders used by test classes and later compressed into {@link TEMP_ZIP_FILE_NAME } zip file.
     */
    private Path m_parentDirPath = null;

    @SuppressWarnings("resource")
    @Override
    public ArchiveZipFSTestInitializer setup(final Map<String, String> configuration) throws IOException {
        prepareDirectory();
        ArchiveZipFSConnection fsConnection = null;
        try {
            final var config = createFSConnectionConfig(configuration);
            fsConnection = new ArchiveZipFSConnection(config);
            return new ArchiveZipFSTestInitializer(fsConnection);
        } catch (Exception e) { //NOSONAR
            closeQuietly(fsConnection);
            clean();
            throw ExceptionUtil.wrapAsIOException(e);
        }
    }

    @SuppressWarnings("resource")
    private ArchiveZipFSConnectionConfig createFSConnectionConfig(final Map<String, String> configuration)
        throws IOException {
        FSConnection fsConnection = null;
        try {
            String workingDir = getParameter(configuration, WORKING_DIRECTORY_PREFIX);
            final var config = new ArchiveZipFSConnectionConfig(workingDir, BrowserRelativizationBehavior.ABSOLUTE);
            if (configuration.containsKey(ZIP_FILE_PATH)) {
                final String zipFilePathTxt = getParameter(configuration, ZIP_FILE_PATH);
                if (StringUtils.isBlank(zipFilePathTxt)) {
                    throw new IllegalArgumentException("Missing zip file path");
                }
                fsConnection = DefaultFSConnectionFactory.createLocalFSConnection();
                final FSPath zipFilePath = fsConnection.getFileSystem() //
                    .getPath(zipFilePathTxt);
                if (!FSFiles.exists(zipFilePath)) {
                    throw new NoSuchFileException(zipFilePathTxt);
                }
                if (FSFiles.isDirectory(zipFilePath)) {
                    throw new NoSuchFileException(zipFilePathTxt + " is directory!");
                }
                if (!Files.isReadable(zipFilePath)) {
                    throw new AccessDeniedException(zipFilePathTxt + " is not readable!");
                }
                config.setZipFilePath(zipFilePath);
            } else {
                var zipFilePath = createZipFile();
                fsConnection = DefaultFSConnectionFactory.createLocalFSConnection();
                final FSPath zipFileFSPath = fsConnection.getFileSystem() //
                    .getPath(zipFilePath.toAbsolutePath().normalize().toString());
                config.setZipFilePath(zipFileFSPath);
            }
            return config;
        } catch (Exception e) { //NOSONAR
            closeQuietly(fsConnection);
            throw ExceptionUtil.wrapAsIOException(e);
        }
    }

    /**
     * @throws IOException
     *
     */
    private void prepareDirectory() throws IOException {
        try {
            final var defaultBaseDirPath = Path.of(System.getProperty("java.io.tmpdir"));
            m_parentDirPath = defaultBaseDirPath.resolve(TEMP_DIRECTORY_NAME);
            if (Files.exists(m_parentDirPath)) {
                clean();
            }
            m_parentDirPath = Files.createDirectory(m_parentDirPath);
            FileUtils.forceDeleteOnExit(m_parentDirPath.toFile());
        } catch (Exception e) { //NOSONAR
            clean();
            throw ExceptionUtil.wrapAsIOException(e);
        }
    }

    /**
     * Creates the zip file used for testing Zip Archive Connector. Its structure must match the list of directories and
     * files used in test methods. The file will be deleted after tests are completed.
     *
     * @return the zip file used in Zip Archive Connector tests.
     * @throws IOException
     */
    private Path createZipFile() throws IOException {
        try {
            final var sourceDirPath = createDir(m_parentDirPath, TEMP_SOURCE_DIRECTORY_NAME);

            final Path dir = createDir(sourceDirPath, "dir");
            createFile(dir, "emptyFile", "");
            createFile(dir, "file", getRandomTxt());
            createFile(dir, "file%20with%20percent%2520encodings", getRandomTxt());
            createFile(dir, "file.txt", getRandomTxt());
            createFile(dir, "fileName", "This is read by an input stream!!");
            createFile(dir, "fileName1", "Some content to test this byte channel");
            createFile(dir, "fileName2", "!!!This starts at byte number 3!");
            createFile(dir, "fileName3", "This has size 16");
            createFile(dir, "some file.txt", getRandomTxt());
            createFile(dir, "some+file.txt", getRandomTxt());

            final Path dir1 = createDir(sourceDirPath, "dir1");
            createFile(dir1, "fileA", "contentA");
            createFile(dir1, "fileB", "contentB");
            createFile(dir1, "fileC", "contentC");

            final Path dirWithSpaces = createDir(sourceDirPath, "dir with spaces");
            createFile(dirWithSpaces, "file with spaces", "This is read by an input stream!!");

            final Path dir1WithSpaces = createDir(sourceDirPath, "dir1 with spaces");
            createFile(dir1WithSpaces, "file with spacesA", "contentA");
            createFile(dir1WithSpaces, "file with spacesB", "contentB");
            createFile(dir1WithSpaces, "file with spacesC", "contentC");

            final Path dirWithPercent = createDir(sourceDirPath, "dir%20with%20percent%2520encodings");
            createFile(dirWithPercent, "file%20with%20percent%2520encodingsA", "This is read by an input stream!!");
            createFile(dirWithPercent, "file+with+plusesA", "contentA");

            final Path dir1WithPercent = createDir(sourceDirPath, "dir1%20with%20percent%2520encodings");
            createFile(dir1WithPercent, "file%20with%20percent%2520encodingsA", "contentA");
            createFile(dir1WithPercent, "file%20with%20percent%2520encodingsB", "contentB");
            createFile(dir1WithPercent, "file%20with%20percent%2520encodingsC", "contentC");

            final Path dirWithPluses = createDir(sourceDirPath, "dir+with+pluses");
            createFile(dirWithPluses, "file+with+pluses", "This is read by an input stream!!");

            final Path dir1WithPluses = createDir(sourceDirPath, "dir1+with+pluses");
            createFile(dir1WithPluses, "file+with+plusesA", "ContentA");
            createFile(dir1WithPluses, "file+with+plusesB", "ContentB");
            createFile(dir1WithPluses, "file+with+plusesC", "ContentC");

            createDir(sourceDirPath, "empty-directory");

            final Path dirFolder = createDir(sourceDirPath, "folder");
            createFile(dirFolder, "file", getRandomTxt());

            createDir(sourceDirPath, "myfolder");

            final Path dirSome = createDir(sourceDirPath, "some-dir");
            createFile(dirSome, "some-file", getRandomTxt());

            createFile(sourceDirPath, "file", "some content");
            createFile(sourceDirPath, "some-file", getRandomTxt());

            return createZipFile(sourceDirPath);
        } catch (Exception ex) { //NOSONAR
            clean();
            throw ExceptionUtil.wrapAsIOException(ex);
        }
    }

    private static Path createDir(final Path parentPath, final String dirName) throws IOException {
        final var dirPath = parentPath.resolve(dirName);
        return FSFiles.createDirectories(dirPath);
    }

    private static void createFile(final Path dir, final String fileName, final String fileContent) throws IOException {
        if (!FSFiles.exists(dir)) {
            throw new NoSuchFileException(dir.toString());
        }
        if (!FSFiles.isDirectory(dir)) {
            throw new NotDirectoryException(dir.toString());
        }
        final Path file = Files.createFile(dir.resolve(fileName));
        Files.writeString(file, fileContent);
    }

    /**
     * Creates a zip file from the source directory content.
     *
     * @param source directory to zip
     * @return path of the newly created zip file
     * @throws IOException
     */
    private static Path createZipFile(final Path dir) throws IOException {
        var zipFilePath = dir.resolveSibling(TEMP_ZIP_FILE_NAME);
        try (var outputStream = new FileOutputStream(zipFilePath.toFile());
                var bufferedOutputStream = new BufferedOutputStream(outputStream);
                var zipArchiveOutputStream = new ZipArchiveOutputStream(bufferedOutputStream)) {
            try (final var listStream = Files.list(dir)) {
                List<Path> files = listStream.collect(Collectors.toList());
                for (Path f : files) {
                    addFileToZip(zipArchiveOutputStream, f.toFile(), "");
                }
            }
            return zipFilePath;
        } catch (Exception ex) {
            throw ExceptionUtil.wrapAsIOException(ex);
        }
    }

    /**
     * Adds given file to the zip file.
     *
     * @param zipArchiveOutputStream output stream used to create the zip file
     * @param fileToZip file to zip
     * @param base full path to the parent folder
     * @throws IOException
     */
    private static void addFileToZip(final ZipArchiveOutputStream zipArchiveOutputStream, final File fileToZip,
        final String base) throws IOException {
        final String entryName = base + fileToZip.getName();
        final var zipArchiveEntry = new ZipArchiveEntry(fileToZip, entryName);
        zipArchiveOutputStream.putArchiveEntry(zipArchiveEntry);
        if (fileToZip.isFile()) {
            try (var fileInputStream = new FileInputStream(fileToZip)) {
                IOUtils.copy(fileInputStream, zipArchiveOutputStream);
                zipArchiveOutputStream.closeArchiveEntry();
            }
        } else {
            zipArchiveOutputStream.closeArchiveEntry();
            final File[] files = fileToZip.listFiles();
            if (files != null && files.length > 0) {
                for (File file : files) {
                    addFileToZip(zipArchiveOutputStream, file, entryName + "/");
                }
            }
        }
    }

    /**
     * Deletes the content created by this class. It includes the directory tree used to create a zip file and the zip
     * file itself.
     *
     * @throws IOException
     */
    private void clean() throws IOException {
        if (m_parentDirPath != null) {
            FileUtils.forceDelete(m_parentDirPath.toFile());
        }
    }

    private static void closeQuietly(final AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception e) { //NOSONAR
            // ignore
        }
    }

    private static String getRandomTxt() {
        return RandomStringUtils.random(100, true, true);
    }

    @Override
    public FSType getFSType() {
        return ArchiveZipFSDescriptorProvider.FS_TYPE;
    }

    @Override
    public FSLocationSpec createFSLocationSpec(final Map<String, String> configuration) {
        return new DefaultFSLocationSpec(FSCategory.CONNECTED, ArchiveZipFSDescriptorProvider.FS_TYPE.getTypeId());
    }

}
