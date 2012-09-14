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

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
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
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        long size = inspectData(inData[0], exec);
        Progress progress = new Progress(size);
        ColumnRearranger rearranger =
                createColumnRearranger(inData[0].getDataTableSpec(), progress,
                        exec);
        BufferedDataTable out =
                exec.createColumnRearrangeTable(inData[0], rearranger, exec);
        return new BufferedDataTable[]{out};
    }

    private long inspectData(final BufferedDataTable inData,
            final ExecutionContext exec) throws Exception {
        long size = 0;
        int index =
                inData.getDataTableSpec().findColumnIndex(
                        m_bocolumn.getStringValue());
        for (DataRow row : inData) {
            exec.checkCanceled();
            if (!row.getCell(index).isMissing()) {
                size += ((BinaryObjectDataValue)row.getCell(index)).length();
            }
        }
        return size;
    }

    private ColumnRearranger createColumnRearranger(final DataTableSpec inSpec,
            final Progress progress, final ExecutionContext exec)
            throws InvalidSettingsException {
        checkSettings(inSpec);
        ColumnRearranger rearranger = new ColumnRearranger(inSpec);
        DataColumnSpec[] colSpecs = new DataColumnSpec[2];
        colSpecs[0] =
                new DataColumnSpecCreator("Location", StringCell.TYPE)
                        .createSpec();
        colSpecs[1] =
                new DataColumnSpecCreator("URL", StringCell.TYPE).createSpec();
        CellFactory factory = new AbstractCellFactory(colSpecs) {
            private int m_rownr = 0;

            @Override
            public DataCell[] getCells(final DataRow row) {
                return createFile(row, m_rownr, progress, inSpec, exec);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void setProgress(final int curRowNr, final int rowCount,
                    final RowKey lastKey, final ExecutionMonitor exec2) {
                exec.setProgress(progress.getProgressInPercent());
                m_rownr = curRowNr;
            }
        };
        rearranger.append(factory);
        return rearranger;
    }

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
        // Does the output directory exist?
        File outputdirectory = new File(m_outputdirectory.getStringValue());
        if (!outputdirectory.isDirectory()) {
            throw new InvalidSettingsException(
                    "Output directory does not exist");
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

    private DataCell[] createFile(final DataRow row, final int rowNr,
            final Progress progress, final DataTableSpec inSpec,
            final ExecutionContext exec) {
        String boColumn = m_bocolumn.getStringValue();
        int boIndex = inSpec.findColumnIndex(boColumn);
        String filenameHandling = m_filenamehandling.getStringValue();
        String outputDirectory = m_outputdirectory.getStringValue();
        String ifExists = m_ifexists.getStringValue();
        String filename = "";
        DataCell location = DataType.getMissingCell();
        DataCell url = DataType.getMissingCell();
        if (!row.getCell(boIndex).isMissing()) {
            if (filenameHandling.equals(
                    FilenameHandling.FROMCOLUMN.getName())) {
                int nameIndex =
                        inSpec.findColumnIndex(m_namecolumn.getStringValue());
                filename =
                        ((StringCell)(row.getCell(nameIndex))).getStringValue();
            }
            if (filenameHandling.equals(FilenameHandling.GENERATE.getName())) {
                filename =
                        m_namepattern.getStringValue().replace("?", "" + rowNr);
            }
            try {
                File file = new File(outputDirectory, filename);
                if (file.exists()) {
                    if (ifExists.equals(OverwritePolicy.ABORT)) {
                        throw new RuntimeException("File \""
                                + file.getAbsolutePath()
                                + "\" exists, overwrite policy: \"" + ifExists
                                + "\"");
                    }
                    if (ifExists.equals(OverwritePolicy.OVERWRITE)) {
                        file.delete();
                    }
                }
                byte[] buffer = new byte[1024];
                file.createNewFile();
                BinaryObjectDataValue bocell =
                        (BinaryObjectDataValue)row.getCell(boIndex);
                InputStream input = bocell.openInputStream();
                OutputStream output = new FileOutputStream(file);
                int length;
                while ((length = input.read(buffer)) > 0) {
                    exec.checkCanceled();
                    exec.setProgress(progress.getProgressInPercent());
                    output.write(buffer, 0, length);
                    progress.advance(length);
                }
                input.close();
                output.close();
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
