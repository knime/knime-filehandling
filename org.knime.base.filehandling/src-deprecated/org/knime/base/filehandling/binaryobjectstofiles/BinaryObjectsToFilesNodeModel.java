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
package org.knime.base.filehandling.binaryobjectstofiles;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import org.knime.base.filehandling.NodeUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.blob.BinaryObjectDataValue;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.uri.URIDataCell;
import org.knime.core.data.uri.URIDataValue;
import org.knime.core.data.uri.UriCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.util.FileUtil;

/**
 * This is the model implementation.
 *
 *
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 */
@Deprecated
class BinaryObjectsToFilesNodeModel extends NodeModel {

    private final SettingsModelString m_bocolumn;

    private final SettingsModelString m_filenamehandling;

    private final SettingsModelString m_targetcolumn;

    private final SettingsModelString m_outputdirectory;

    private final SettingsModelString m_namepattern;

    private final SettingsModelBoolean m_removebocolumn;

    private final SettingsModelString m_ifexists;

    /**
     * Constructor for the node model.
     */
    protected BinaryObjectsToFilesNodeModel() {
        super(1, 1);
        m_bocolumn = SettingsFactory.createBinaryObjectColumnSettings();
        m_filenamehandling = SettingsFactory.createFilenameHandlingSettings();
        m_targetcolumn = SettingsFactory.createTargetColumnSettings(m_filenamehandling);
        m_outputdirectory = SettingsFactory.createOutputDirectorySettings(m_filenamehandling);
        m_namepattern = SettingsFactory.createNamePatternSettings(m_filenamehandling);
        m_removebocolumn = SettingsFactory.createRemoveBinaryObjectColumnSettings();
        m_ifexists = SettingsFactory.createIfExistsSettings();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {

        // Check settings only if filename handling is generate
        if (m_filenamehandling.getStringValue().equals(FilenameHandling.GENERATE.getName())) {
            getOutputDirectory(getOutputDirectoryURL());
        }

        // HashSet for duplicate checking and cleanup
        final Set<String> filenames = new HashSet<>();
        // Only append if no target column is used (target column would be
        // identical)
        final boolean appenduricolumn =
            !m_filenamehandling.getStringValue().equals(FilenameHandling.FROMCOLUMN.getName());
        try {
            // If the columns do not get appended the create file method will
            // not be called by the rearranger so it has to happen manually
            if (!appenduricolumn) {
                final long rows = inData[0].size();
                int i = 0;
                // Create the file for each row
                for (final DataRow row : inData[0]) {
                    exec.checkCanceled();
                    exec.setProgress((double)i / rows);
                    createFile(row, i, filenames, inData[0].getDataTableSpec(), exec);
                    i++;
                }
            }

            final ColumnRearranger rearranger = createColumnRearranger(inData[0].getDataTableSpec(), filenames, exec);
            final BufferedDataTable out = exec.createColumnRearrangeTable(inData[0], rearranger, exec);
            return new BufferedDataTable[]{out};
        } catch (final Exception e) {
            // In case of exception, delete all created files
            cleanUp(filenames.toArray(new String[filenames.size()]));
            throw e;
        }
    }

    /**
     * Delete all the given files.
     *
     *
     * This method should be called, in case the execution got aborted. It will delete all files referenced by the
     * array.
     *
     * @param filenames Files that should be deleted.
     */
    private static void cleanUp(final String[] filenames) {
        for (int i = 0; i < filenames.length; i++) {
            try {
                Files.delete(Paths.get(filenames[i]));
            } catch (final Exception e) {
                // If one file fails, the others should still be deleted
            }
        }
    }

    /**
     * Create a rearranger that adds the URI column.
     *
     *
     * @param inSpec Specification of the input table
     * @param filenames Set of files that have already been created
     * @param exec Context of this execution
     * @return Rearranger that will add a column for the URIs to the created files (if necessary).
     * @throws InvalidSettingsException If the settings are incorrect
     */
    private ColumnRearranger createColumnRearranger(final DataTableSpec inSpec, final Set<String> filenames,
        final ExecutionContext exec) throws InvalidSettingsException {
        // Check settings for correctness
        checkSettings(inSpec);
        final ColumnRearranger rearranger = new ColumnRearranger(inSpec);
        DataColumnSpec colSpec;
        // Only append if no target column is used (target column would be
        // identical)
        final boolean appenduricolumn =
            !m_filenamehandling.getStringValue().equals(FilenameHandling.FROMCOLUMN.getName());
        // Append URI column if filehandling is not from column. This will also
        // call the create file method
        if (appenduricolumn) {
            // Create column for the URI of the files
            final String uriColName = DataTableSpec.getUniqueColumnName(inSpec, "URI");
            colSpec = new DataColumnSpecCreator(uriColName, URIDataCell.TYPE).createSpec();
            // Factory that creates the files and the corresponding URI cell
            final CellFactory factory = new SingleCellFactory(colSpec) {
                private long m_rownr = 0;

                @Override
                public DataCell getCell(final DataRow row) {
                    try {
                        return createFile(row, m_rownr, filenames, inSpec, exec);
                    } catch (final IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public void setProgress(final long curRowNr, final long rowCount, final RowKey lastKey,
                    final ExecutionMonitor execMon) {
                    super.setProgress(curRowNr, rowCount, lastKey, execMon);
                    m_rownr = curRowNr;
                }
            };
            rearranger.append(factory);
        }
        // Remove binary object column if selected
        if (m_removebocolumn.getBooleanValue()) {
            rearranger.remove(m_bocolumn.getStringValue());
        }
        return rearranger;
    }

    /**
     * Check if the settings are all valid.
     *
     *
     * @param inSpec Specification of the input table
     * @throws InvalidSettingsException If the settings are incorrect
     */
    @SuppressWarnings("unchecked")
    private void checkSettings(final DataTableSpec inSpec) throws InvalidSettingsException {
        final String bocolumn = m_bocolumn.getStringValue();
        NodeUtils.checkColumnSelection(inSpec, "Binary object", bocolumn, BinaryObjectDataValue.class);
        // Check settings only if filename handling is from column
        if (m_filenamehandling.getStringValue().equals(FilenameHandling.FROMCOLUMN.getName())) {
            final String targetcolumn = m_targetcolumn.getStringValue();
            NodeUtils.checkColumnSelection(inSpec, "Target", targetcolumn, URIDataValue.class);
        }
        // Check settings only if filename handling is generate
        if (m_filenamehandling.getStringValue().equals(FilenameHandling.GENERATE.getName())) {
            // Does the output directory exist?
            try {
                getOutputDirectory(getOutputDirectoryURL());
            } catch (final IOException e) {
                throw new InvalidSettingsException("Could not create output directory: " + e.getMessage());
            }

            // Does the name pattern at least contain a '?'?
            final String pattern = m_namepattern.getStringValue();
            if (!pattern.contains("?")) {
                throw new InvalidSettingsException("Pattern has to contain" + " a ?");
            }
        }
    }

    /**
     * @return the URL of the output directory
     * @throws IOException
     */
    private URL getOutputDirectoryURL() throws IOException {
        final URL url = FileUtil.toURL(m_outputdirectory.getStringValue());
        if (url.toString().endsWith("/")) {
            return url;
        } else {
            return new URL(url.toString() + "/");
        }
    }

    /**
     * @return the outputdirectory
     * @throws InvalidSettingsException
     */
    private static File getOutputDirectory(final URL url) throws IOException {
        final File outputdirectory = FileUtil.getFileFromURL(url);

        if (!outputdirectory.exists()) {
            throw new IOException("Output directory \"" + outputdirectory.getAbsoluteFile() + "\" does not exist");
        }
        if (!outputdirectory.isDirectory()) {
            throw new IOException(
                "Specified path doesn't point to a directory: \"" + outputdirectory.getAbsoluteFile());
        }
        return outputdirectory;
    }

    /**
     * Creates a file from the binary object, contained in the row.
     *
     *
     * This method creates a file out of the binary object, that is contained in the row. The filename is either also
     * extracted from the row or generated by using the set pattern and the rows number.
     *
     * @param row Row with the needed data
     * @param rowNr Number of the row in the table
     * @param filenames Set of files that have already been created
     * @param inSpec Specification of the input table
     * @param exec Context of this execution
     * @return Cell containing the URI to the created file
     */
    private DataCell createFile(final DataRow row, final long rowNr, final Set<String> filenames,
        final DataTableSpec inSpec, final ExecutionContext exec) throws IOException {
        final String boColumn = m_bocolumn.getStringValue();
        final int boIndex = inSpec.findColumnIndex(boColumn);

        final String filenameHandling = m_filenamehandling.getStringValue();
        final String fromColumn = FilenameHandling.FROMCOLUMN.getName();
        final String generate = FilenameHandling.GENERATE.getName();
        final String ifExists = m_ifexists.getStringValue();

        // check for missing binary object
        if (row.getCell(boIndex).isMissing()) {
            return DataType.getMissingCell();
        }

        // create filename and output folder
        final File file;

        // internal output directory, we may not report it to the user!
        if (filenameHandling.equals(fromColumn)) {
            // Get filename from table
            final int targetIndex = inSpec.findColumnIndex(m_targetcolumn.getStringValue());
            if (row.getCell(targetIndex).isMissing()) {
                throw new IOException("Filename in row \"" + row.getKey() + "\" is missing");
            }
            final URI targetUri = ((URIDataCell)(row.getCell(targetIndex))).getURIContent().getURI();

            // Absolute path in filename, no preceding directory
            file = FileUtil.getFileFromURL(targetUri.toURL());
        } else if (filenameHandling.equals(generate)) {
            // Generate filename using pattern, by replacing the ? with the
            // row number
            final String filename = m_namepattern.getStringValue().replace("?", "" + rowNr);
            final File outputDirectory = getOutputDirectory(getOutputDirectoryURL());
            file = new File(outputDirectory, filename);
        } else {
            throw new IllegalArgumentException("Invalid filename handling setting!");
        }

        // create the output file
        try {
            // Check if a file with the same name has already been created
            // (only possible with use of the target column)
            if (filenames.contains(file.getAbsolutePath())) {
                throw new IOException("Duplicate entry \"" + file.getAbsolutePath() + "\" in the target column");
            }
            // Check if a file with the same name already exists (but was
            // not created by this execution)
            if (file.exists()) {
                // Abort if policy is abort
                if (ifExists.equals(OverwritePolicy.ABORT.getName())) {
                    throw new IOException(
                        "File \"" + file.toString() + "\" exists, but overwrite policy: \"" + ifExists + "\"");
                }
                // Remove if policy is overwrite
                if (ifExists.equals(OverwritePolicy.OVERWRITE.getName())) {
                    Files.delete(file.toPath());
                }
            }
            final byte[] buffer = new byte[1024];
            file.getParentFile().mkdirs();
            if (!file.createNewFile()) {
                throw new IOException("File " + file + " could not be created");
            }
            // Add file to created files
            filenames.add(file.getAbsolutePath());
            // Get input stream from the binary object
            final BinaryObjectDataValue bocell = (BinaryObjectDataValue)row.getCell(boIndex);
            try (InputStream input = bocell.openInputStream(); final OutputStream output = new FileOutputStream(file)) {
                int length;
                // Copy data from binary object to file
                while ((length = input.read(buffer)) > 0) {
                    exec.checkCanceled();
                    output.write(buffer, 0, length);
                }
            }

            // Create cell with the URI information
            return UriCellFactory.create(file.toURI().toString());
        } catch (final Exception e) {
            throw new IOException(e);
        }
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
        // createColumnRearranger will check the settings
        final DataTableSpec outSpec = createColumnRearranger(inSpecs[0], null, null).createSpec();
        return new DataTableSpec[]{outSpec};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_bocolumn.saveSettingsTo(settings);
        m_filenamehandling.saveSettingsTo(settings);
        m_targetcolumn.saveSettingsTo(settings);
        m_outputdirectory.saveSettingsTo(settings);
        m_namepattern.saveSettingsTo(settings);
        m_removebocolumn.saveSettingsTo(settings);
        m_ifexists.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_bocolumn.loadSettingsFrom(settings);
        m_filenamehandling.loadSettingsFrom(settings);
        m_targetcolumn.loadSettingsFrom(settings);
        m_outputdirectory.loadSettingsFrom(settings);
        m_namepattern.loadSettingsFrom(settings);
        m_removebocolumn.loadSettingsFrom(settings);
        m_ifexists.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_bocolumn.validateSettings(settings);
        m_filenamehandling.validateSettings(settings);
        m_targetcolumn.validateSettings(settings);
        m_outputdirectory.validateSettings(settings);
        m_namepattern.validateSettings(settings);
        m_removebocolumn.validateSettings(settings);
        m_ifexists.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // Not used
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // Not used
    }

}