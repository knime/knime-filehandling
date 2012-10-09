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
package org.knime.base.filehandling.copyfiles;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.uri.URIContent;
import org.knime.core.data.uri.URIDataCell;
import org.knime.core.data.uri.URIDataValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * This is the model implementation of URI to string.
 * 
 * 
 * @author Patrick Winter, University of Konstanz
 */
class CopyFilesNodeModel extends NodeModel {

    private SettingsModelString m_copyormove;

    private SettingsModelString m_sourcecolumn;

    private SettingsModelString m_filenamehandling;

    private SettingsModelString m_outputdirectory;

    private SettingsModelString m_pattern;

    private SettingsModelString m_targetcolumn;

    private SettingsModelString m_ifexists;

    /**
     * Constructor for the node model.
     */
    protected CopyFilesNodeModel() {
        super(1, 1);
        m_copyormove = SettingsFactory.createCopyOrMoveSettings();
        m_sourcecolumn = SettingsFactory.createSourceColumnSettings();
        m_filenamehandling = SettingsFactory.createFilenameHandlingSettings();
        m_outputdirectory =
                SettingsFactory
                        .createOutputDirectorySettings(m_filenamehandling);
        m_pattern = SettingsFactory.createPatternSettings(m_filenamehandling);
        m_targetcolumn =
                SettingsFactory.createTargetColumnSettings(m_filenamehandling);
        m_ifexists = SettingsFactory.createIfExistsSettings();
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
        boolean appenduricolumn =
                m_filenamehandling.getStringValue().equals(
                        FilenameHandling.GENERATE.getName());
        try {
            // If the columns do not get appended the create file method will
            // not be called so it has to happen manually
            if (!appenduricolumn) {
                int rows = inData[0].getRowCount();
                int i = 0;
                // Create the file for each row
                for (DataRow row : inData[0]) {
                    exec.checkCanceled();
                    exec.setProgress((double)i / rows);
                    createFile(row, i, filenames, inData[0].getDataTableSpec(),
                            exec);
                    i++;
                }
            }
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
     * Create a rearranger that adds the URI column.
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
        DataColumnSpec colSpec;
        boolean appenduricolumn =
                m_filenamehandling.getStringValue().equals(
                        FilenameHandling.GENERATE.getName());
        // Append URI column if selected. This will also call the
        // create file method
        if (appenduricolumn) {
            // Create column for the URI of the files
            String uriColName =
                    DataTableSpec.getUniqueColumnName(inSpec, "URI");
            colSpec =
                    new DataColumnSpecCreator(uriColName, URIDataCell.TYPE)
                            .createSpec();
            // Factory that creates the files and the corresponding URI cell
            CellFactory factory = new SingleCellFactory(colSpec) {
                private int m_rownr = 0;

                @Override
                public DataCell getCell(final DataRow row) {
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
        // Is the source column set?
        if (m_sourcecolumn.getStringValue().equals("")) {
            throw new InvalidSettingsException("Source column not set");
        }
        // Does the source column setting reference to an existing
        // column?
        int columnIndex =
                inSpec.findColumnIndex(m_sourcecolumn.getStringValue());
        if (columnIndex < 0) {
            throw new InvalidSettingsException("Source column not set");
        }
        // Is the source column setting referencing to a column of the
        // type URI data value?
        DataType type = inSpec.getColumnSpec(columnIndex).getType();
        if (!type.isCompatible(URIDataValue.class)) {
            throw new InvalidSettingsException("Source column not set");
        }
        // Check settings only if filename handling is from column
        if (m_filenamehandling.getStringValue().equals(
                FilenameHandling.FROMCOLUMN.getName())) {
            // Is the target column set?
            if (m_targetcolumn.getStringValue().equals("")) {
                throw new InvalidSettingsException("Target column not set");
            }
            // Does the target column setting reference to an existing column?
            columnIndex =
                    inSpec.findColumnIndex(m_targetcolumn.getStringValue());
            if (columnIndex < 0) {
                throw new InvalidSettingsException("Target column not set");
            }
            // Is the target column setting referencing to a column of the type
            // URI data value?
            type = inSpec.getColumnSpec(columnIndex).getType();
            if (!type.isCompatible(URIDataValue.class)) {
                throw new InvalidSettingsException("Target column not set");
            }
        }
        // Check settings only if filename handling is generate
        if (m_filenamehandling.getStringValue().equals(
                FilenameHandling.GENERATE.getName())) {
            // Does the output directory exist?
            File outputdirectory = new File(m_outputdirectory.getStringValue());
            if (!outputdirectory.isDirectory()) {
                throw new InvalidSettingsException("Output directory \""
                        + outputdirectory.getAbsoluteFile()
                        + "\" does not exist");
            }
            // Does the name pattern at least contain a '?'?
            String pattern = m_pattern.getStringValue();
            if (!pattern.contains("?")) {
                throw new InvalidSettingsException("Pattern has to contain"
                        + " a ?");
            }
        }
    }

    /**
     * 
     * @param row Row with the needet data
     * @param rowNr Number of the row in the table
     * @param filenames Set of files that have already been created
     * @param inSpec Specification of the input table
     * @param exec Context of this execution
     * @return Cell containing the URI to the created file
     */
    private DataCell createFile(final DataRow row, final int rowNr,
            final Set<String> filenames, final DataTableSpec inSpec,
            final ExecutionContext exec) {
        String sourceColumn = m_sourcecolumn.getStringValue();
        int sourceIndex = inSpec.findColumnIndex(sourceColumn);
        String filenameHandling = m_filenamehandling.getStringValue();
        String outputDirectory = m_outputdirectory.getStringValue();
        String fromColumn = FilenameHandling.FROMCOLUMN.getName();
        String generate = FilenameHandling.GENERATE.getName();
        String ifExists = m_ifexists.getStringValue();
        String filename = "";
        // Assume missing binary object
        DataCell uriCell = DataType.getMissingCell();
        if (!row.getCell(sourceIndex).isMissing()) {
            if (filenameHandling.equals(fromColumn)) {
                // Get filename from table
                int nameIndex =
                        inSpec.findColumnIndex(m_targetcolumn.getStringValue());
                if (row.getCell(nameIndex).isMissing()) {
                    throw new RuntimeException("Filename in row \""
                            + row.getKey() + "\" is missing");
                }
                filename =
                        ((URIDataCell)(row.getCell(nameIndex))).getURIContent()
                                .getURI().toString();
            }
            if (filenameHandling.equals(generate)) {
                // Generate filename using pattern, by replacing the ? with the
                // row number
                filename = m_pattern.getStringValue().replace("?", "" + rowNr);
            }
            try {
                File targetFile = new File(outputDirectory, filename);
                // Check if a file with the same name has already been created
                if (filenames.contains(targetFile.getAbsolutePath())) {
                    throw new RuntimeException("Duplicate entry \""
                            + targetFile.getAbsolutePath()
                            + "\" in the filenames column");
                }
                // Check if a file with the same name already exists (but was
                // not created by this execution)
                if (targetFile.exists()) {
                    // Abort if policy is abort
                    if (ifExists.equals(OverwritePolicy.ABORT.getName())) {
                        throw new RuntimeException("File \""
                                + targetFile.getAbsolutePath()
                                + "\" exists, overwrite policy: \"" + ifExists
                                + "\"");
                    }
                    // Remove if policy is overwrite
                    if (ifExists.equals(OverwritePolicy.OVERWRITE.getName())) {
                        targetFile.delete();
                    }
                }
                byte[] buffer = new byte[1024];
                targetFile.getParentFile().mkdirs();
                // Add file to created files
                filenames.add(targetFile.getAbsolutePath());
                String location =
                        ((URIDataValue)row.getCell(sourceIndex))
                                .getURIContent().getURI().getPath();
                if (m_copyormove.getStringValue().equals(
                        CopyOrMove.COPY.getName())) {
                    targetFile.createNewFile();
                    // Get input stream from the source file
                    InputStream input = new FileInputStream(location);
                    OutputStream output = new FileOutputStream(targetFile);
                    int length;
                    // Copy data from source file to target file
                    while ((length = input.read(buffer)) > 0) {
                        exec.checkCanceled();
                        output.write(buffer, 0, length);
                    }
                    input.close();
                    output.close();
                }
                if (m_copyormove.getStringValue().equals(
                        CopyOrMove.MOVE.getName())) {
                    File source = new File(location);
                    source.renameTo(targetFile);
                }
                // Create cell with the URI information
                URI uri = targetFile.getAbsoluteFile().toURI();
                String extension = FilenameUtils.getExtension(uri.getPath());
                uriCell = new URIDataCell(new URIContent(uri, extension));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return uriCell;
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
        m_copyormove.saveSettingsTo(settings);
        m_sourcecolumn.saveSettingsTo(settings);
        m_filenamehandling.saveSettingsTo(settings);
        m_outputdirectory.saveSettingsTo(settings);
        m_pattern.saveSettingsTo(settings);
        m_targetcolumn.saveSettingsTo(settings);
        m_ifexists.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_copyormove.loadSettingsFrom(settings);
        m_sourcecolumn.loadSettingsFrom(settings);
        m_filenamehandling.loadSettingsFrom(settings);
        m_outputdirectory.loadSettingsFrom(settings);
        m_pattern.loadSettingsFrom(settings);
        m_targetcolumn.loadSettingsFrom(settings);
        m_ifexists.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_copyormove.validateSettings(settings);
        m_sourcecolumn.validateSettings(settings);
        m_filenamehandling.validateSettings(settings);
        m_outputdirectory.validateSettings(settings);
        m_pattern.validateSettings(settings);
        m_targetcolumn.validateSettings(settings);
        m_ifexists.validateSettings(settings);
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
