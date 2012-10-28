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

import org.apache.commons.io.FilenameUtils;
import org.knime.base.filehandling.NodeUtils;
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
 * This is the model implementation.
 * 
 * 
 * @author Patrick Winter, University of Konstanz
 */
class CopyFilesNodeModel extends NodeModel {

    private SettingsModelString m_copyormove;

    private SettingsModelString m_sourcecolumn;

    private SettingsModelString m_filenamehandling;

    private SettingsModelString m_targetcolumn;

    private SettingsModelString m_outputdirectory;

    private SettingsModelString m_ifexists;

    /**
     * Constructor for the node model.
     */
    protected CopyFilesNodeModel() {
        super(1, 1);
        m_copyormove = SettingsFactory.createCopyOrMoveSettings();
        m_sourcecolumn = SettingsFactory.createSourceColumnSettings();
        m_filenamehandling = SettingsFactory.createFilenameHandlingSettings();
        m_targetcolumn =
                SettingsFactory.createTargetColumnSettings(m_filenamehandling);
        m_outputdirectory =
                SettingsFactory
                        .createOutputDirectorySettings(m_filenamehandling);
        m_ifexists = SettingsFactory.createIfExistsSettings();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        BufferedDataTable out = null;
        // Monitor for duplicate checking and rollback
        CopyOrMoveMonitor monitor =
                new CopyOrMoveMonitor(m_copyormove.getStringValue());
        // Append only if target column is not used
        boolean appenduricolumn =
                !m_filenamehandling.getStringValue().equals(
                        FilenameHandling.FROMCOLUMN.getName());
        try {
            // If the columns do not get appended the create file method will
            // not be called so it has to happen manually
            if (!appenduricolumn) {
                int rows = inData[0].getRowCount();
                int i = 0;
                // Do the action for each row
                for (DataRow row : inData[0]) {
                    exec.checkCanceled();
                    exec.setProgress((double)i / rows);
                    doAction(row, i, monitor, inData[0].getDataTableSpec(),
                            exec);
                    i++;
                }
            }
            ColumnRearranger rearranger =
                    createColumnRearranger(inData[0].getDataTableSpec(),
                            monitor, exec);
            out = exec.createColumnRearrangeTable(inData[0], rearranger, exec);
        } catch (Exception e) {
            // In case of exception, do a rollback
            monitor.rollback();
            throw e;
        }
        return new BufferedDataTable[]{out};
    }

    /**
     * Create a rearranger that adds the URI column.
     * 
     * 
     * @param inSpec Specification of the input table
     * @param monitor Monitors which files have been copied/moved
     * @param exec Context of this execution
     * @return Rearranger that will add a column for the URIs to the created
     *         files (if necessary).
     * @throws InvalidSettingsException If the settings are incorrect
     */
    private ColumnRearranger createColumnRearranger(final DataTableSpec inSpec,
            final CopyOrMoveMonitor monitor, final ExecutionContext exec)
            throws InvalidSettingsException {
        // Check settings for correctness
        checkSettings(inSpec);
        ColumnRearranger rearranger = new ColumnRearranger(inSpec);
        DataColumnSpec colSpec;
        // Append only if target column is not used
        boolean appenduricolumn =
                !m_filenamehandling.getStringValue().equals(
                        FilenameHandling.FROMCOLUMN.getName());
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
                    return doAction(row, m_rownr, monitor, inSpec, exec);
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
    @SuppressWarnings("unchecked")
    private void checkSettings(final DataTableSpec inSpec)
            throws InvalidSettingsException {
        String sourcecolumn = m_sourcecolumn.getStringValue();
        NodeUtils.checkColumnSelection(inSpec, "Source", sourcecolumn,
                URIDataValue.class);
        // Check settings only if filename handling is from column
        if (m_filenamehandling.getStringValue().equals(
                FilenameHandling.FROMCOLUMN.getName())) {
            String targetcolumn = m_targetcolumn.getStringValue();
            NodeUtils.checkColumnSelection(inSpec, "Target", targetcolumn,
                    URIDataValue.class);
            // Do the target and the source column differ
            if (sourcecolumn.equals(targetcolumn)) {
                throw new InvalidSettingsException(
                        "Source and target do not differ");
            }
        }
        // Check settings only if filename handling is generate
        if (m_filenamehandling.getStringValue().equals(
                FilenameHandling.SOURCENAME.getName())) {
            // Does the output directory exist?
            File outputdirectory = new File(m_outputdirectory.getStringValue());
            if (!outputdirectory.isDirectory()) {
                throw new InvalidSettingsException("Output directory \""
                        + outputdirectory.getAbsoluteFile()
                        + "\" does not exist");
            }
        }
    }

    /**
     * Executes the configured action (copy or move).
     * 
     * 
     * @param row Row with the needed data
     * @param rowNr Number of the row in the table
     * @param monitor Monitors which files have been copied/moved
     * @param inSpec Specification of the input table
     * @param exec Context of this execution
     * @return Cell containing the URI to the created file
     */
    private DataCell doAction(final DataRow row, final int rowNr,
            final CopyOrMoveMonitor monitor, final DataTableSpec inSpec,
            final ExecutionContext exec) {
        String sourceColumn = m_sourcecolumn.getStringValue();
        int sourceIndex = inSpec.findColumnIndex(sourceColumn);
        String filenameHandling = m_filenamehandling.getStringValue();
        String outputDirectory = m_outputdirectory.getStringValue();
        String fromColumn = FilenameHandling.FROMCOLUMN.getName();
        String generate = FilenameHandling.SOURCENAME.getName();
        String ifExists = m_ifexists.getStringValue();
        String filename = "";
        // Assume missing source URI
        DataCell uriCell = DataType.getMissingCell();
        if (!row.getCell(sourceIndex).isMissing()) {
            URI sourceUri =
                    ((URIDataValue)row.getCell(sourceIndex)).getURIContent()
                            .getURI();
            if (!sourceUri.getScheme().equals("file")) {
                throw new RuntimeException(
                        "This node only supports the protocol \"file\"");
            }
            if (filenameHandling.equals(fromColumn)) {
                // Get target URI from table
                int targetIndex =
                        inSpec.findColumnIndex(m_targetcolumn.getStringValue());
                if (row.getCell(targetIndex).isMissing()) {
                    throw new RuntimeException("Target URI in row \""
                            + row.getKey() + "\" is missing");
                }
                URI targetUri =
                        ((URIDataCell)(row.getCell(targetIndex)))
                                .getURIContent().getURI();
                if (!targetUri.getScheme().equals("file")) {
                    throw new RuntimeException(
                            "This node only supports the protocol \"file\"");
                }
                // Absolute path in filename, no preceding directory
                filename = targetUri.getPath();
                outputDirectory = "";
            }
            if (filenameHandling.equals(generate)) {
                filename = FilenameUtils.getName(sourceUri.getPath());
            }
            try {
                String sourcePath = sourceUri.getPath();
                File sourceFile = new File(sourcePath);
                // Check if the same file has already been touched
                if (!monitor.isNewFile(sourcePath)) {
                    throw new RuntimeException("Duplicate entry \""
                            + sourcePath + "\"");
                }
                // Abort if the source file does not exist
                if (!sourceFile.exists()) {
                    throw new RuntimeException("The file \""
                            + sourceFile.getAbsolutePath() + "\" is missing");
                }
                File targetFile = new File(outputDirectory, filename);
                // Check if the same file has already been touched
                if (!monitor.isNewFile(targetFile.getAbsolutePath())) {
                    throw new RuntimeException("Duplicate entry \""
                            + targetFile.getAbsolutePath() + "\"");
                }
                // Check if a file with the same name already exists
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
                targetFile.getParentFile().mkdirs();
                if (m_copyormove.getStringValue().equals(
                        CopyOrMove.COPY.getName())) {
                    byte[] buffer = new byte[1024];
                    targetFile.createNewFile();
                    // Register files as processed to enable rollback (in this
                    // case removal of the target file)
                    monitor.registerFiles(sourcePath,
                            targetFile.getAbsolutePath());
                    // Get input stream from the source file
                    InputStream input = new FileInputStream(sourcePath);
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
                    sourceFile.renameTo(targetFile);
                    // Register files as processed to enable rollback (in this
                    // case renaming the target back to the source)
                    monitor.registerFiles(sourcePath,
                            targetFile.getAbsolutePath());
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
        m_targetcolumn.saveSettingsTo(settings);
        m_outputdirectory.saveSettingsTo(settings);
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
        m_targetcolumn.loadSettingsFrom(settings);
        m_outputdirectory.loadSettingsFrom(settings);
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
        m_targetcolumn.validateSettings(settings);
        m_outputdirectory.validateSettings(settings);
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
