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
 * ------------------------------------------------------------------------
 *
 * History
 *   Sep 5, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.unzip;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.core.runtime.Platform;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.uri.URIContent;
import org.knime.core.data.uri.URIDataCell;
import org.knime.core.node.BufferedDataContainer;
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

/**
 * This is the model implementation.
 *
 *
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 */
@Deprecated
class UnzipNodeModel extends NodeModel {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(UnzipNodeModel.class);

    private SettingsModelString m_source;

    private SettingsModelString m_targetdirectory;

    private SettingsModelString m_output;

    private SettingsModelString m_ifexists;

    /**
     * Constructor for the node model.
     */
    protected UnzipNodeModel() {
        super(0, 1);
        m_source = SettingsFactory.createSourceSettings();
        m_targetdirectory = SettingsFactory.createTargetDirectorySettings();
        m_output = SettingsFactory.createOutputSettings();
        m_ifexists = SettingsFactory.createIfExistsSettings();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception {
        // Create output spec and container
        DataTableSpec outSpec = createOutSpec();
        BufferedDataContainer outContainer = exec.createDataContainer(outSpec);
        // Extract files from zip file
        extractZip(outContainer, exec);
        outContainer.close();
        return new BufferedDataTable[]{outContainer.getTable()};
    }

    /**
     * Extracts files from a zip file.
     *
     *
     * Extracts files from a zip file. The path or URI to the files will be put
     * into the container.
     *
     * @param outContainer Container to put the path or URI to the extracted
     *            files into
     * @param exec Execution context for <code>checkCanceled()</code> and
     *            <code>setProgress()</code>
     * @throws Exception When abort condition is met or user canceled
     */
    private void extractZip(final BufferedDataContainer outContainer, final ExecutionContext exec) throws Exception {
        final boolean isWindows = Platform.OS_WIN32.equals(Platform.getOS());

        boolean localFile = true;
        int rowID = 0;
        List<String> filenames = new LinkedList<String>();
        String rawSource = m_source.getStringValue();
        InputStream in = null;
        ZipInputStream zin = null;
        FileOutputStream out = null;
        Progress progress = null;
        try {
            try {
                URL sourceURL = new URL(rawSource);
                if (!sourceURL.getProtocol().equals("file")) {
                    in = new URL(rawSource).openStream();
                    progress = new Progress();
                    localFile = false;
                }
            } catch (MalformedURLException e) {
                // sourceStream is still null
            }
            if (in == null) {
                File sourceFile = new File(textToPath(rawSource));
                in = new FileInputStream(sourceFile);
                progress = new Progress(checkFiles(exec));
            }
            File directory = new File(m_targetdirectory.getStringValue());
            byte[] buffer = new byte[8 * 1024];
            zin = new ZipInputStream(in);
            ZipEntry entry = zin.getNextEntry();
            while (entry != null) {
                // Generate full path to file
                String pathFromZip = entry.getName();
                if (isWindows && pathFromZip.matches("^[a-zA-Z]:[/\\\\].*")) {
                    // remove driver letter because this leads to invalid paths under Windows
                    pathFromZip = pathFromZip.substring(2);
                }
                File file = new File(directory, pathFromZip);

                // If file exists and policy is abort throw an exception
                String ifExists = m_ifexists.getStringValue();
                if (file.exists() && ifExists.equals(OverwritePolicy.ABORT.getName())) {
                    throw new IOException("File \"" + file.getAbsolutePath() + "\" exists, overwrite policy: \""
                            + ifExists + "\"");
                }
                // It depends on the zip file if the contained directories get
                // listed as entries or not
                if (entry.isDirectory()) {
                    // In case of a directory entry create the directory if not
                    // present
                    if (!file.exists()) {
                        filenames.add(file.getAbsolutePath());
                        file.mkdirs();
                    }
                } else {
                    filenames.add(file.getAbsolutePath());
                    // Remove old file if it exists
                    if (file.exists()) {
                        if (!file.delete()) {
                            throw new IOException("Could not delete file " + file);
                        }
                        LOGGER.info("Replacing existing file \"" + file.getAbsolutePath() + "\"");
                    }
                    // Create directories if necessary (directories may not get
                    // listed as entries and therefore have to be created here)
                    file.getAbsoluteFile().getParentFile().mkdirs();
                    if (!file.createNewFile()) {
                        throw new IOException("Could not create file " + file);
                    }
                    out = new FileOutputStream(file);
                    int length;
                    // Copy content into new file
                    while ((length = zin.read(buffer)) > 0) {
                        exec.checkCanceled();
                        if (localFile) {
                            exec.setProgress(progress.getProgressInPercent());
                        } else {
                            exec.setMessage("Unziped " + FileUtils.byteCountToDisplaySize(progress.getProgress()));
                        }
                        out.write(buffer, 0, length);
                        progress.advance(length);
                    }
                    out.close();
                    // Create row with path or URI
                    DataCell cell = null;
                    String outputSelection = m_output.getStringValue();
                    if (outputSelection.equals(OutputSelection.LOCATION.getName())) {
                        cell = new StringCell(file.getAbsolutePath());
                    }
                    if (outputSelection.equals(OutputSelection.URI.getName())) {
                        URI uri = file.getAbsoluteFile().toURI();
                        String extension = FilenameUtils.getExtension(uri.getPath());
                        URIContent content = new URIContent(uri, extension);
                        cell = new URIDataCell(content);
                    }
                    outContainer.addRowToTable(new DefaultRow("Row" + rowID, cell));
                    rowID++;
                }
                entry = zin.getNextEntry();
            }
        } catch (Exception e) {
            // Remove created files if node got aborted
            removeFiles(filenames);
            throw e;
        } finally {
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
            if (zin != null) {
                zin.close();
            }
        }
    }

    /**
     * Checks if any of the files in the zip already exist and returns the size
     * of all files in the zip file.
     *
     *
     * @param exec Execution context for <code>checkCanceled()</code>
     * @return Uncompressed size of all files in the zip file
     * @throws Exception When abort condition is met or user canceled
     */
    private long checkFiles(final ExecutionContext exec) throws Exception {
        long size = 0;
        File source = new File(textToPath(m_source.getStringValue()));
        File directory = new File(m_targetdirectory.getStringValue());
        String ifExists = m_ifexists.getStringValue();
        ZipFile zipFile = new ZipFile(source);
        try {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            // Get the file size of each entry and check for abort condition
            while (entries.hasMoreElements()) {
                exec.checkCanceled();
                ZipEntry entry = entries.nextElement();
                size += entry.getSize();
                File file = new File(directory, entry.getName());
                // If file exists and policy is abort throw an exception
                if (file.exists() && ifExists.equals(OverwritePolicy.ABORT.getName())) {
                    throw new IOException("File \"" + file.getAbsolutePath() + "\" exists, overwrite policy: \""
                            + ifExists + "\"");
                }
            }
        } finally {
            zipFile.close();
        }
        return size;
    }

    /**
     * Removes all files in the list.
     *
     *
     * This method removes all files in the given list to clean up, when the
     * node gets aborted.
     *
     * @param filenames List of the files to remove
     */
    private void removeFiles(final List<String> filenames) {
        for (int i = filenames.size() - 1; i >= 0; i--) {
            File file = new File(filenames.get(i));
            file.delete();
        }
    }

    /**
     * Tries to create a path from the passed string.
     *
     * @param text the string to transform into a path
     * @return Path if entered value could be properly transformed
     */
    private static String textToPath(final String text) {
        String path;
        try {
            URL url = new URL(text);
            try {
                URI uri = url.toURI();
                path = uri.getPath();
            } catch (URISyntaxException e) {
                path = url.getPath();
            }
        } catch (MalformedURLException e) {
            // see if they specified a file without giving the protocol
            File tmp = new File(text);

            // if that blows off we let the exception go up the stack.
            path = tmp.getAbsolutePath();
        }
        return path;
    }

    /**
     * Factory method for the output table spec.
     *
     *
     * @return Output table spec
     */
    private DataTableSpec createOutSpec() {
        DataColumnSpec columnSpec = null;
        String output = m_output.getStringValue();
        if (output.equals(OutputSelection.LOCATION.getName())) {
            columnSpec = new DataColumnSpecCreator("Location", StringCell.TYPE).createSpec();
        }
        if (output.equals(OutputSelection.URI.getName())) {
            columnSpec = new DataColumnSpecCreator("URI", URIDataCell.TYPE).createSpec();
        }
        return new DataTableSpec(columnSpec);
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
        // Is the source set?
        if (m_source.getStringValue().equals("")) {
            throw new InvalidSettingsException("Source not set");
        }
        // Is the target set?
        if (m_targetdirectory.getStringValue().equals("")) {
            throw new InvalidSettingsException("Target directory not set");
        }
        // Does the target directory exist?
        File targetdirectory = new File(m_targetdirectory.getStringValue());
        if (!targetdirectory.isDirectory()) {
            throw new InvalidSettingsException("Target directory does not exist");
        }
        return new DataTableSpec[]{createOutSpec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_source.saveSettingsTo(settings);
        m_targetdirectory.saveSettingsTo(settings);
        m_output.saveSettingsTo(settings);
        m_ifexists.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_source.loadSettingsFrom(settings);
        m_targetdirectory.loadSettingsFrom(settings);
        m_output.loadSettingsFrom(settings);
        m_ifexists.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_source.validateSettings(settings);
        m_targetdirectory.validateSettings(settings);
        m_output.validateSettings(settings);
        m_ifexists.validateSettings(settings);
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
