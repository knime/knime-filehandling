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
 *   Sep 3, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.zip2;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.core.runtime.Platform;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.StringValue;
import org.knime.core.data.uri.URIDataValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.util.FileUtil;

/**
 * This is the model implementation.
 *
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 */
class ZipNodeModel extends NodeModel {
    private static final boolean IS_WINDOWS = Platform.OS_WIN32.equals(Platform.getOS());

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ZipNodeModel.class);

    private final SettingsModelString m_locationcolumn;

    private final SettingsModelString m_target;

    private final SettingsModelString m_pathhandling;

    private final SettingsModelString m_prefix;

    private final SettingsModelString m_ifexists;

    private final SettingsModelString m_format;

    /**
     * Constructor for the node model.
     */
    protected ZipNodeModel() {
        super(1, 0);
        m_locationcolumn = SettingsFactory.createLocationColumnSettings();
        m_target = SettingsFactory.createTargetSettings();
        m_pathhandling = SettingsFactory.createPathHandlingSettings();
        m_prefix = SettingsFactory.createPrefixSettings(m_pathhandling);
        m_ifexists = SettingsFactory.createIfExistsSettings();
        m_format = SettingsFactory.createFormatSettings();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        boolean abort = m_ifexists.getStringValue().equals(OverwritePolicy.ABORT.getName());
        // Create target url
        URL targetUrl = FileUtil.toURL(getTargetName());
        File targetFile = fileFromUrl(targetUrl);
        // Abort if setting is abort and final target file exists
        if (abort && targetFile != null && targetFile.exists()) {
            throw new Exception("File \"" + targetFile.getAbsolutePath() + "\" exists, overwrite policy: \""
                + m_ifexists.getStringValue() + "\"");
        }
        // Get filelist from the table
        URL[] urls = getUrls(inData[0]);
        // Create archive from filelist
        archive(targetUrl, urls, exec);
        return new BufferedDataTable[0];
    }

    /**
     * Create an archive file containing the listed files.
     *
     * @param target The archive file that will be created
     * @param files The list of files that will be added to the archive
     * @param exec The execution context to check for cancellation
     * @throws Exception If an error occurs
     */
    private void archive(final URL target, final URL[] files, final ExecutionContext exec) throws Exception {
        boolean append =
            m_ifexists.getStringValue().equals(OverwritePolicy.APPEND_OVERWRITE.getName())
                || m_ifexists.getStringValue().equals(OverwritePolicy.APPEND_ABORT.getName());
        // Detect archive type
        boolean zip = m_format.getStringValue().equals(Format.ZIP.getName());
        boolean tar =
            m_format.getStringValue().equals(Format.TAR_GZ.getName())
                || m_format.getStringValue().equals(Format.TAR_BZ2.getName());
        String type = "";
        if (zip) {
            type = ArchiveStreamFactory.ZIP;
        } else if (tar) {
            type = ArchiveStreamFactory.TAR;
        }
        // Create temporary target file
        File tmpFile = FileUtil.createTempFile("zip-" + name(target), "tmp");
        // Create archive output stream
        final OutputStream out = openCompressStream(tmpFile);
        ArchiveOutputStream os = new ArchiveStreamFactory().createArchiveOutputStream(type, out);
        // Append files from existing archive (if selected)
        File targetFile = fileFromUrl(target);
        boolean exists = targetFile != null && (targetFile.exists() || existsHTTPConnection(target));
        if (append && exists) {
            addOldFiles(target, os, files, type, exec);
        }
        // Add new files to archive
        for (int i = 0; i < files.length; i++) {
            exec.checkCanceled();
            // Add entry
            os.putArchiveEntry(getArchiveEntry(type, files[i]));
            // Add content if not a directory
            if (!isDirectory(files[i])) {
                try (InputStream in = FileUtil.openStreamWithTimeout(files[i])) {
                    IOUtils.copy(in, os);
                }
            }
            os.closeArchiveEntry();
        }
        os.close();
        // Replace old archive with new one
        if (targetFile != null) {
            targetFile.delete();
            FileUtils.moveFile(tmpFile, targetFile);
        } else {
            //try remote delete if exists
            if (exists) {
                try {
                    FileUtil.openOutputConnection(target, "DELETE");
                } catch (IOException e) {
                    LOGGER.warn("Failed to delete: " + target, e);
                }
            }
            //PUT to the ooutputstream
            URLConnection connection = FileUtil.openOutputConnection(target, "PUT");
            try (OutputStream outStream = connection.getOutputStream()) {
                FileUtils.copyFile(tmpFile, outStream);
                //cleanup
                FileUtils.deleteQuietly(tmpFile);
            }
        }
    }

    /**
     * Heuristic to decide whether the {@code url} is a folder/directory.
     * @param url A {@link URL}.
     * @return denotes a folder/directory or not.
     */
    private static boolean isDirectory(final URL url) {
        File file = fileFromUrl(url);
        if (file != null) {
            return file.isDirectory();
        }
        String path = url.getPath();
        return path == null || path.endsWith("/");
    }

    /**
     * @param target A {@link URL}
     * @return Checks whether there is something to read from the selected url.
     */
    private static boolean existsHTTPConnection(final URL target) {
        try {
            FileUtil.openStreamWithTimeout(target);
            //            //"Cheap" check
            //            FileUtil.openOutputConnection(target, "HEAD");
            return true;
        } catch (IOException e) {
            //            try {
            //                //More expensive check
            //                FileUtil.openOutputConnection(target, "GET");
            //                return true;
            //            } catch (IOException e1) {
            return false;
            //            }
        }
    }

    /**
     * @param url A {@link URL}.
     * @return The name part from the path of {@code code}.
     */
    private static String name(final URL url) {
        String path = url.getPath();
        if (path == null) {
            return "";
        }
        return path.substring(path.lastIndexOf('/') + 1);
    }

    /**
     * Adds files from the old archive to the new one.
     *
     * Old files that will be overwritten by one of the new files will be left out.
     *
     * @param source The old archive
     * @param target Output stream to the new archive
     * @param newFiles Array of files that will be added later
     * @param type Type of the archive
     * @param exec Execution context to check for cancellation
     * @throws Exception If an error occurs
     */
    private void addOldFiles(final URL source, final ArchiveOutputStream target, final URL[] newFiles,
        final String type, final ExecutionContext exec) throws Exception {
        boolean abort = m_ifexists.getStringValue().equals(OverwritePolicy.APPEND_ABORT.getName());
        // Create set of files that will be added later
        Set<String> newFilesSet = new HashSet<String>();
        for (int i = 0; i < newFiles.length; i++) {
            newFilesSet.add(getName(newFiles[i]));
        }
        // Open archive input stream for old archive
        InputStream in = openUncompressStream(source);
        ArchiveInputStream ais = new ArchiveStreamFactory().createArchiveInputStream(type, in);
        ArchiveEntry entry;
        // Add old entries to new archive
        while ((entry = ais.getNextEntry()) != null) {
            exec.checkCanceled();
            if (!newFilesSet.contains(entry.getName())) {
                // Put entry that will not be replaced with a new file
                target.putArchiveEntry(entry);
                IOUtils.copy(ais, target);
                target.closeArchiveEntry();
            } else if (abort) {
                // Abort if old files may not be replaced
                throw new Exception("File \"" + entry.getName() + "\" exists in old file, overwrite" + " policy: \""
                    + m_ifexists.getStringValue() + "\"");
            } else {
                // Inform user that old file will be replaced
                LOGGER.info("Replacing file " + entry.getName());
            }
        }
        ais.close();
    }

    /**
     * Compress the file (if selected).
     *
     * @param file The uncompressed file
     * @return The compressed file
     * @throws Exception If an error occurred
     */
    private OutputStream openCompressStream(final File file) throws Exception {
        OutputStream compressStream;
        // Skip compression if not selected
        boolean gzip = m_format.getStringValue().equals(Format.TAR_GZ.getName());
        boolean bzip = m_format.getStringValue().equals(Format.TAR_BZ2.getName());
        if (gzip || bzip) {
            String type = "";
            if (gzip) {
                type = CompressorStreamFactory.GZIP;
            } else if (bzip) {
                type = CompressorStreamFactory.BZIP2;
            }
            final OutputStream out = new FileOutputStream(file);
            compressStream = new CompressorStreamFactory().createCompressorOutputStream(type, out);
        } else {
            compressStream = new FileOutputStream(file);
        }
        return compressStream;
    }

    /**
     * Uncompresses the given file.
     *
     * @param source The potentially compressed source file
     * @return Uncompressed version of the source, or source if it was not compressed
     * @throws Exception If IOException occurred
     */
    private InputStream openUncompressStream(final URL source) throws Exception {
        InputStream uncompressStream;
        // Buffered stream needed for autodetection of type
        InputStream in = new BufferedInputStream(FileUtil.openStreamWithTimeout(source));
        try {
            // Try to create a compressor for the source, throws exception if
            // source is not compressed
            uncompressStream = new CompressorStreamFactory().createCompressorInputStream(in);
        } catch (CompressorException e) {
            // Source is not compressed
            uncompressStream = in;
        }
        return uncompressStream;
    }

    /**
     * Get an array of the files referenced by the table.
     *
     * The files contained in directories will already be resolved.
     *
     * @param table Table containing the references to the files.
     * @return Array of files
     * @throws IOException When file cannot be properly converted to URL
     */
    private URL[] getUrls(final BufferedDataTable table) throws IOException {
        List<URL> entries = new LinkedList<>();
        // Read filenames from table
        String column = m_locationcolumn.getStringValue();
        int index = table.getDataTableSpec().findColumnIndex(column);
        DataType type = table.getDataTableSpec().getColumnSpec(index).getType();
        if (type.isCompatible(URIDataValue.class)) {
            // Add filenames from URI column
            for (DataRow row : table) {
                if (!row.getCell(index).isMissing()) {
                    URIDataValue value = (URIDataValue)row.getCell(index);
                    entries.add(value.getURIContent().getURI().toURL());
                }
            }
        } else if (type.isCompatible(StringValue.class)) {
            // Add filenames from string column
            for (DataRow row : table) {
                if (!row.getCell(index).isMissing()) {
                    StringValue value = (StringValue)row.getCell(index);
                    entries.add(FileUtil.toURL(value.getStringValue()));
                }
            }
        }
        URL[] filenames = entries.toArray(new URL[entries.size()]);
        // Create files for each filename
        URL[] files = new URL[filenames.length];
        for (int i = 0; i < files.length; i++) {
            files[i] = filenames[i];
            if (files[i] == null) {
                throw new IllegalArgumentException(filenames[i] + " is not a valid path to a local file");
            }
        }
        // Resolve directories
        files = resolveDirectories(files);
        return files;
    }

    /**
     * Replaces directories in the given file array by all contained files.
     *
     *
     * @param files Array of files that potentially contains directories
     * @return List of all files with directories resolved
     * @throws IOException When file path cannot be converted to URL
     */
    private URL[] resolveDirectories(final URL[] files) throws IOException {
        List<URL> allFiles = new LinkedList<>();
        for (int i = 0; i < files.length; i++) {
            // Add both files and directories
            allFiles.add(files[i]);
            File file = fileFromUrl(files[i]);
            if (isDirectory(files[i])) {
                // Get inner files through recursive call and add them
                URL[] innerFiles = resolveDirectories(FileUtils.toURLs(file.listFiles()));
                for (int j = 0; j < innerFiles.length; j++) {
                    allFiles.add(innerFiles[j]);
                }
            }
        }
        return allFiles.toArray(new URL[allFiles.size()]);
    }

    /**
     * Returns the name of the archive that will be created.
     *
     * @return Name of the archive (without compression extension)
     */
    private String getTargetName() {
        boolean zip = m_format.getStringValue().equals(Format.ZIP.getName());
        boolean gzip = m_format.getStringValue().equals(Format.TAR_GZ.getName());
        boolean bzip = m_format.getStringValue().equals(Format.TAR_BZ2.getName());
        String name = m_target.getStringValue();
        if (zip) {
            // Does the name end with .zip?
            if (!name.matches(".*[.]zip")) {
                // Append .zip
                name = name + ".zip";
            }
        } else if (gzip) {
            if (!name.matches(".*[.]tar[.]gz")) {
                name = name + ".tar.gz";
            }
        } else if (bzip) {
            if (!name.matches(".*[.]tar[.]bz2")) {
                name = name + ".tar.bz2";
            }
        }
        return name;
    }

    /**
     * @param url A {@link URL} which might point to a file.
     * @return The name from the {@code url}, special cased for files.
     */
    private String getName(final URL url) {
        File file = fileFromUrl(url);
        if (file != null) {
            return getName(file);
        }
        return FilenameUtils.getName(url.getPath());
    }

    /**
     * A wrapper around {@link FileUtil#getFileFromURL(URL)} to handle non-local files similarly (ie. return {@code null}).
     *
     * @param url A {@link URL}.
     * @return The local file, or {@code null}.
     */
    private static File fileFromUrl(final URL url) {
        try {
            return FileUtil.getFileFromURL(url);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Returns the correct file name according to the path handling policy.
     *
     *
     * @param file File for the name
     * @return Name of the given file with cut path
     */
    private String getName(final File file) {
        String name = file.getAbsolutePath();
        // Get path handling setting
        String pathhandling = m_pathhandling.getStringValue();
        String prefix = m_prefix.getStringValue();
        if (pathhandling.equals(PathHandling.ONLY_FILENAME.getName())) {
            // Only use name
            name = file.getName();
        }
        if (pathhandling.equals(PathHandling.TRUNCATE_PREFIX.getName())) {
            // Remove prefix
            name = name.startsWith(prefix) ? name.substring(prefix.length()) : name;
        }
        if (IS_WINDOWS) {
            // Remove windows drive letter
            String filePrefix = FilenameUtils.getPrefix(name);
            name = name.startsWith(filePrefix) ? name.substring(filePrefix.length()) : name;
            // Always use UNIX separators
            name = FilenameUtils.separatorsToUnix(name);
        }
        // Entry name never starts with separator
        if (name.startsWith("/")) {
            name = name.substring(1);
        }
        return name;
    }

    /**
     * Creates an archive entry based on the given type.
     *
     * @param type Type of the archive
     * @param url Content of the entry
     * @return Entry based on the given type
     */
    private ArchiveEntry getArchiveEntry(final String type, final URL url) {
        String name = getName(url);
        final ArchiveEntry entry;
        File file = fileFromUrl(url);
        // Create entry for given type
        if (type.equals(ArchiveStreamFactory.ZIP)) {
            if (file != null) {
                entry = new ZipArchiveEntry(file, name);
            } else {
                ZipArchiveEntry zentry;
                String nameAdjusted = nameAdjusted(url, name);
                zentry = new ZipArchiveEntry(nameAdjusted);
                zentry.setTime(new Date().getTime());
                entry = zentry;
            }
        } else if (type.equals(ArchiveStreamFactory.TAR)) {
            if (file != null) {
                entry = new TarArchiveEntry(file, name);
            } else {
                TarArchiveEntry tentry;
                tentry = new TarArchiveEntry(nameAdjusted(url, name));
                tentry.setModTime(new Date());
                entry = tentry;
            }
        } else {
            throw new UnsupportedOperationException("Not supported type: " + type);
        }
        return entry;
    }

    /**
     * @param url A {@link URL}.
     * @param name The name part of {@code url}.
     * @return {@code name} or {@code name/} when {@code url} denotes a folder.
     */
    private static String nameAdjusted(final URL url, final String name) {
        boolean dir = isDirectory(url);
        String nameAdjusted = dir && !name.endsWith("/") ? name + "/" : name;
        return nameAdjusted;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // Not used
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        // Is the location column set?
        if (m_locationcolumn.getStringValue().equals("")) {
            throw new InvalidSettingsException("Location column not set");
        }
        // Does the location column setting reference to an existing column?
        int columnIndex = inSpecs[0].findColumnIndex(m_locationcolumn.getStringValue());
        if (columnIndex < 0) {
            throw new InvalidSettingsException("Location column not set");
        }
        // Is the location column setting referencing to a column of the
        // type string value or URI data value?
        DataType type = inSpecs[0].getColumnSpec(columnIndex).getType();
        boolean isString = type.isCompatible(StringValue.class);
        boolean isURI = type.isCompatible(URIDataValue.class);
        if (!(isString || isURI)) {
            throw new InvalidSettingsException("Location column not set");
        }
        // Is the target set?
        if (m_target.getStringValue().equals("")) {
            throw new InvalidSettingsException("Target not set");
        }
        // Does the prefix directory exist? (If it is needet)
        if (m_pathhandling.getStringValue().equals(PathHandling.TRUNCATE_PREFIX.getName())
            && !new File(m_prefix.getStringValue()).exists()) {
            throw new InvalidSettingsException("Prefix directory does not exist");
        }
        return new DataTableSpec[]{};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_locationcolumn.saveSettingsTo(settings);
        m_target.saveSettingsTo(settings);
        m_pathhandling.saveSettingsTo(settings);
        m_prefix.saveSettingsTo(settings);
        m_ifexists.saveSettingsTo(settings);
        m_format.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_locationcolumn.loadSettingsFrom(settings);
        m_target.loadSettingsFrom(settings);
        m_pathhandling.loadSettingsFrom(settings);
        m_prefix.loadSettingsFrom(settings);
        m_ifexists.loadSettingsFrom(settings);
        m_format.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        SettingsModelString cloneLocation = m_locationcolumn.createCloneWithValidatedValue(settings);
        String location = cloneLocation.getStringValue();
        if (location == null || location.length() == 0) {
            throw new InvalidSettingsException("No location specified");
        }
        m_locationcolumn.validateSettings(settings);
        m_target.validateSettings(settings);
        m_pathhandling.validateSettings(settings);
        m_prefix.validateSettings(settings);
        m_ifexists.validateSettings(settings);
        m_format.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // Not used
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // Not used
    }

}
