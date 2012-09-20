/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
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
import java.util.HashSet;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.blob.BinaryObjectDataValue;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.StringCell;
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

/**
 * This is the model implementation of Files to Binary Objects.
 * 
 * 
 * @author Patrick Winter, University of Konstanz
 */
class BinaryObjectsToFilesNodeModel extends NodeModel {

    private SettingsModelString m_bocolumn;

    private SettingsModelString m_outputdirectory;

    private SettingsModelString m_filenamehandling;

    private SettingsModelString m_namecolumn;

    private SettingsModelString m_namepattern;

    private SettingsModelString m_ifexists;

    private SettingsModelBoolean m_removebocolumn;

    private SettingsModelBoolean m_appendlocationcolumns;

    /**
     * Constructor for the node model.
     */
    protected BinaryObjectsToFilesNodeModel() {
        super(1, 1);
        m_bocolumn = SettingsFactory.createBinaryObjectColumnSettings();
        m_outputdirectory = SettingsFactory.createOutputDirectorySettings();
        m_filenamehandling = SettingsFactory.createFilenameHandlingSettings();
        m_namecolumn =
                SettingsFactory.createNameColumnSettings(m_filenamehandling);
        m_namepattern =
                SettingsFactory.createNamePatternSettings(m_filenamehandling);
        m_ifexists = SettingsFactory.createIfExistsSettings();
        m_removebocolumn =
                SettingsFactory.createRemoveBinaryObjectColumnSettings();
        m_appendlocationcolumns =
                SettingsFactory.createAppendLocationColumnsSettings();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        BufferedDataTable out = null;
        // HashSet for duplicate checking and cleanup
        Set<String> filenames = new HashSet<String>();
        try {
            ColumnRearranger rearranger =
                    createColumnRearranger(inData[0].getDataTableSpec(),
                            filenames, exec);
            out = exec.createColumnRearrangeTable(inData[0], rearranger, exec);
        } catch (Exception e) {
            // In case of exception, delete all created files
            cleanUp(filenames.toArray(new String[filenames.size()]));
            throw e;
        }
        return new BufferedDataTable[]{out};
    }

    /**
     * Delete all the given files.
     * 
     * 
     * This method should be called, in case the execution got aborted. It will
     * delete all files referenced by the array.
     * 
     * @param filenames Files that should be deleted.
     */
    private void cleanUp(final String[] filenames) {
        for (int i = 0; i < filenames.length; i++) {
            try {
                File file = new File(filenames[0]);
                file.delete();
            } catch (Exception e) {
                // If one file fails, the others should still be deleted
            }
        }
    }

    /**
     * Create a rearranger that adds the location and URL columns.
     * 
     * 
     * @param inSpec Specification of the input table
     * @param filenames Set of files that have already been created
     * @param exec Context of this execution
     * @return Rearranger that will add columns for the location and URL of the
     *         created files.
     * @throws InvalidSettingsException If the settings are incorrect
     */
    private ColumnRearranger createColumnRearranger(final DataTableSpec inSpec,
            final Set<String> filenames, final ExecutionContext exec)
            throws InvalidSettingsException {
        // Check settings for correctness
        checkSettings(inSpec);
        ColumnRearranger rearranger = new ColumnRearranger(inSpec);
        // Append location and URL column if selected
        if (m_appendlocationcolumns.getBooleanValue()) {
            DataColumnSpec[] colSpecs = new DataColumnSpec[2];
            // Create column for the location of the files
            String locationColName =
                    DataTableSpec.getUniqueColumnName(inSpec, "Location");
            colSpecs[0] =
                    new DataColumnSpecCreator(locationColName, StringCell.TYPE)
                            .createSpec();
            // Create column for the URL of the files
            String urlColName =
                    DataTableSpec.getUniqueColumnName(inSpec, "URL");
            colSpecs[1] =
                    new DataColumnSpecCreator(urlColName, StringCell.TYPE)
                            .createSpec();
            // Factory that creates the files and the corresponding location and
            // URL
            // cells
            CellFactory factory = new AbstractCellFactory(colSpecs) {
                private int m_rownr = 0;

                @Override
                public DataCell[] getCells(final DataRow row) {
                    return createFile(row, m_rownr, filenames, inSpec, exec);
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public void setProgress(final int curRowNr, final int rowCount,
                        final RowKey lastKey, final ExecutionMonitor exec2) {
                    super.setProgress(curRowNr, rowCount, lastKey, exec2);
                    // Save the row for pattern creation
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
    private void checkSettings(final DataTableSpec inSpec)
            throws InvalidSettingsException {
        // Is the binary object column set?
        if (m_bocolumn.getStringValue().equals("")) {
            throw new InvalidSettingsException("Binary object column not set");
        }
        // Does the binary object column setting reference to an existing
        // column?
        int columnIndex = inSpec.findColumnIndex(m_bocolumn.getStringValue());
        if (columnIndex < 0) {
            throw new InvalidSettingsException("Binary object column not set");
        }
        // Is the binary object column setting referencing to a column of the
        // type binary object data value?
        DataType type = inSpec.getColumnSpec(columnIndex).getType();
        if (!type.isCompatible(BinaryObjectDataValue.class)) {
            throw new InvalidSettingsException("Binary object column not set");
        }
        // Does the output directory exist?
        File outputdirectory = new File(m_outputdirectory.getStringValue());
        if (!outputdirectory.isDirectory()) {
            throw new InvalidSettingsException("Output directory \""
                    + outputdirectory.getAbsoluteFile() + "\" does not exist");
        }
        // Check settings only if filename handling is from column
        if (m_filenamehandling.getStringValue().equals(
                FilenameHandling.FROMCOLUMN.getName())) {
            // Is the name column set?
            if (m_namecolumn.getStringValue().equals("")) {
                throw new InvalidSettingsException("Name column not set");
            }
            // Does the name column setting reference to an existing column?
            columnIndex = inSpec.findColumnIndex(m_namecolumn.getStringValue());
            if (columnIndex < 0) {
                throw new InvalidSettingsException("Name column not set");
            }
            // Is the name column setting referencing to a column of the type
            // string data value?
            type = inSpec.getColumnSpec(columnIndex).getType();
            if (!type.isCompatible(StringValue.class)) {
                throw new InvalidSettingsException(
                        "Binary object column not set");
            }
        }
        // Check settings only if filename handling is generate
        if (m_filenamehandling.getStringValue().equals(
                FilenameHandling.GENERATE.getName())) {
            // Does the name pattern at least contain a '?'?
            String pattern = m_namepattern.getStringValue();
            if (!pattern.contains("?")) {
                throw new InvalidSettingsException("Pattern has to contain"
                        + " a ?");
            }
        }
    }

    /**
     * Creates a file from the binary object, contained in the row.
     * 
     * 
     * This method creates a file out of the binary object, that is contained in
     * the row. The filename is either also extracted from the row or generated
     * by using the set pattern and the rows number.
     * 
     * @param row Row with the needet data
     * @param rowNr Number of the row in the table
     * @param filenames Set of files that have already been created
     * @param inSpec Specification of the input table
     * @param exec Context of this execution
     * @return Cells containing the location and URL to the created file
     */
    private DataCell[] createFile(final DataRow row, final int rowNr,
            final Set<String> filenames, final DataTableSpec inSpec,
            final ExecutionContext exec) {
        String boColumn = m_bocolumn.getStringValue();
        int boIndex = inSpec.findColumnIndex(boColumn);
        String filenameHandling = m_filenamehandling.getStringValue();
        String outputDirectory = m_outputdirectory.getStringValue();
        String fromColumn = FilenameHandling.FROMCOLUMN.getName();
        String generate = FilenameHandling.GENERATE.getName();
        String ifExists = m_ifexists.getStringValue();
        String filename = "";
        // Assume missing binary object
        DataCell location = DataType.getMissingCell();
        DataCell url = DataType.getMissingCell();
        if (!row.getCell(boIndex).isMissing()) {
            if (filenameHandling.equals(fromColumn)) {
                // Get filename from table
                int nameIndex =
                        inSpec.findColumnIndex(m_namecolumn.getStringValue());
                if (row.getCell(nameIndex).isMissing()) {
                    throw new RuntimeException("Filename in row \""
                            + row.getKey() + "\" is missing");
                }
                filename =
                        ((StringCell)(row.getCell(nameIndex))).getStringValue();
            }
            if (filenameHandling.equals(generate)) {
                // Generate filename using pattern, by replacing the ? with the
                // row number
                filename =
                        m_namepattern.getStringValue().replace("?", "" + rowNr);
            }
            try {
                File file = new File(outputDirectory, filename);
                // Check if a file with the same name has already been created
                if (filenames.contains(file.getAbsolutePath())) {
                    throw new RuntimeException("Duplicate entry \""
                            + file.getAbsolutePath()
                            + "\" in the filenames column");
                }
                // Check if a file with the same name already exists (but was
                // not created by this execution)
                if (file.exists()) {
                    // Abort if policy is abort
                    if (ifExists.equals(OverwritePolicy.ABORT)) {
                        throw new RuntimeException("File \""
                                + file.getAbsolutePath()
                                + "\" exists, overwrite policy: \"" + ifExists
                                + "\"");
                    }
                    // Remove if policy is overwrite
                    if (ifExists.equals(OverwritePolicy.OVERWRITE)) {
                        file.delete();
                    }
                }
                byte[] buffer = new byte[1024];
                file.getParentFile().mkdirs();
                file.createNewFile();
                // Add file to created files
                filenames.add(file.getAbsolutePath());
                // Get input stream from the binary object
                BinaryObjectDataValue bocell =
                        (BinaryObjectDataValue)row.getCell(boIndex);
                InputStream input = bocell.openInputStream();
                OutputStream output = new FileOutputStream(file);
                int length;
                // Copy data from binary object to file
                while ((length = input.read(buffer)) > 0) {
                    exec.checkCanceled();
                    output.write(buffer, 0, length);
                }
                input.close();
                output.close();
                // Create cells with the location and URL information
                location = new StringCell(file.getAbsolutePath());
                url =
                        new StringCell(file.getAbsoluteFile().toURI().toURL()
                                .toString());
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        }
        return new DataCell[]{location, url};
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
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        // createColumnRearranger will check the settings
        DataTableSpec outSpec =
                createColumnRearranger(inSpecs[0], null, null).createSpec();
        return new DataTableSpec[]{outSpec};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_bocolumn.saveSettingsTo(settings);
        m_outputdirectory.saveSettingsTo(settings);
        m_filenamehandling.saveSettingsTo(settings);
        m_namecolumn.saveSettingsTo(settings);
        m_namepattern.saveSettingsTo(settings);
        m_ifexists.saveSettingsTo(settings);
        m_removebocolumn.saveSettingsTo(settings);
        m_appendlocationcolumns.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_bocolumn.loadSettingsFrom(settings);
        m_outputdirectory.loadSettingsFrom(settings);
        m_filenamehandling.loadSettingsFrom(settings);
        m_namecolumn.loadSettingsFrom(settings);
        m_namepattern.loadSettingsFrom(settings);
        m_ifexists.loadSettingsFrom(settings);
        m_removebocolumn.loadSettingsFrom(settings);
        m_appendlocationcolumns.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_bocolumn.validateSettings(settings);
        m_outputdirectory.validateSettings(settings);
        m_filenamehandling.validateSettings(settings);
        m_namecolumn.validateSettings(settings);
        m_namepattern.validateSettings(settings);
        m_ifexists.validateSettings(settings);
        m_removebocolumn.validateSettings(settings);
        m_appendlocationcolumns.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // Not used
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // Not used
    }

}
