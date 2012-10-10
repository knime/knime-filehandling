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
package org.knime.base.filehandling.filemetainfo;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Calendar;

import org.apache.commons.io.FileUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.date.DateAndTimeCell;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.uri.URIDataValue;
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
class FileMetaInfoNodeModel extends NodeModel {

    private SettingsModelString m_uricolumn;

    private SettingsModelBoolean m_abortifnotlocal;

    /**
     * Constructor for the node model.
     */
    protected FileMetaInfoNodeModel() {
        super(1, 1);
        m_uricolumn = SettingsFactory.createURIColumnSettings();
        m_abortifnotlocal = SettingsFactory.createAbortIfNotLocalSettings();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        ColumnRearranger rearranger =
                createColumnRearranger(inData[0].getDataTableSpec(), exec);
        BufferedDataTable out =
                exec.createColumnRearrangeTable(inData[0], rearranger, exec);
        return new BufferedDataTable[]{out};
    }

    /**
     * Create a rearranger that adds the binary objects to the table.
     * 
     * 
     * @param inSpec Specification of the input table
     * @param exec Context of this execution
     * @return Rearranger that will add a binary object column
     * @throws InvalidSettingsException If the settings are incorrect
     */
    private ColumnRearranger createColumnRearranger(final DataTableSpec inSpec,
            final ExecutionContext exec) throws InvalidSettingsException {
        // Check settings for correctness
        checkSettings(inSpec);
        final int uriIndex =
                inSpec.findColumnIndex(m_uricolumn.getStringValue());
        ColumnRearranger rearranger = new ColumnRearranger(inSpec);
        // Create columns for the meta information using the attributes array
        Attributes[] attributes = Attributes.getAllAttributes();
        DataColumnSpec[] colSpecs = new DataColumnSpec[attributes.length];
        // Add each attribute with there name and type at there position
        for (int i = 0; i < attributes.length; i++) {
            int position = attributes[i].getPosition();
            String name =
                    DataTableSpec.getUniqueColumnName(inSpec,
                            attributes[i].getName());
            DataType type = attributes[i].getType();
            colSpecs[position] =
                    new DataColumnSpecCreator(name, type).createSpec();
        }
        // Factory that checks the files for there meta information
        CellFactory factory = new AbstractCellFactory(colSpecs) {
            @Override
            public DataCell[] getCells(final DataRow row) {
                return inspectFile(row, uriIndex);
            }
        };
        rearranger.append(factory);
        return rearranger;
    }

    /**
     * Find the attributes of a file.
     * 
     * 
     * Find out the attributes of a file. The files URI has to be contained in
     * one of the rows cells. Will return missing cells if the file does not
     * exist.
     * 
     * @param row Row with the location cell in it
     * @param uriIndex Index of the URI cell
     * @return Data cells with the attributes of the file
     */
    private DataCell[] inspectFile(final DataRow row, final int uriIndex) {
        boolean abort = m_abortifnotlocal.getBooleanValue();
        DataCell[] cells = new DataCell[Attributes.getAllAttributes().length];
        // Assume missing cells
        for (int i = 0; i < cells.length; i++) {
            cells[i] = DataType.getMissingCell();
        }
        if (!row.getCell(uriIndex).isMissing()) {
            // Get location
            URIDataValue value = (URIDataValue)row.getCell(uriIndex);
            URI uri = value.getURIContent().getURI();
            String scheme = uri.getScheme();
            if (abort && (scheme == null || !scheme.equals("file"))) {
                throw new RuntimeException("The URI \"" + uri.toString()
                        + "\" does have the scheme \"" + scheme
                        + "\", expected \"file\"");
            }
            File file = new File(uri.getPath());
            if (file.exists()) {
                try {
                    // Directory
                    cells[Attributes.DIRECTORY.getPosition()] =
                            BooleanCell.get(file.isDirectory());
                    // Hidden
                    cells[Attributes.HIDDEN.getPosition()] =
                            BooleanCell.get(file.isHidden());
                    // Size
                    long size = getFileSize(file);
                    cells[Attributes.SIZE.getPosition()] = new LongCell(size);
                    // Size (human readable)
                    String humansize = FileUtils.byteCountToDisplaySize(size);
                    cells[Attributes.HUMANSIZE.getPosition()] =
                            new StringCell(humansize);
                    // Last modified
                    long modifyDate = file.lastModified();
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(modifyDate);
                    cells[Attributes.MODIFIED.getPosition()] =
                            new DateAndTimeCell(calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH),
                                    calendar.get(Calendar.HOUR_OF_DAY),
                                    calendar.get(Calendar.MINUTE),
                                    calendar.get(Calendar.SECOND),
                                    calendar.get(Calendar.MILLISECOND));
                    // Permissions
                    String permissions = "";
                    permissions += file.canRead() ? "r" : "";
                    permissions += file.canWrite() ? "w" : "";
                    permissions += file.canExecute() ? "x" : "";
                    cells[Attributes.PERMISSIONS.getPosition()] =
                            new StringCell(permissions);
                } catch (Exception e) {
                    // If one file does not work, go on
                }
            }
        }
        return cells;
    }

    /**
     * Get the size of a file.
     * 
     * 
     * This method will return the size of the given file. If the file is a
     * directory, it will return the summarized size of the contained files.
     * 
     * @param file The file to check
     * @return The size of the file
     */
    private long getFileSize(final File file) {
        long size = 0;
        if (!file.isDirectory()) {
            size = file.length();
        } else {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                size += getFileSize(files[i]);
            }
        }
        return size;
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
        // Is the uri column set?
        if (m_uricolumn.getStringValue().equals("")) {
            throw new InvalidSettingsException("URI column not set");
        }
        // Does the URI column setting reference to an existing column?
        int columnIndex = inSpec.findColumnIndex(m_uricolumn.getStringValue());
        if (columnIndex < 0) {
            throw new InvalidSettingsException("URI column not set");
        }
        // Is the URI column setting referencing to a column of the type
        // string value?
        DataType type = inSpec.getColumnSpec(columnIndex).getType();
        if (!type.isCompatible(URIDataValue.class)) {
            throw new InvalidSettingsException("URI column not set");
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
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        // createColumnRearranger() will check the settings
        DataTableSpec outSpec =
                createColumnRearranger(inSpecs[0], null).createSpec();
        return new DataTableSpec[]{outSpec};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_uricolumn.saveSettingsTo(settings);
        m_abortifnotlocal.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_uricolumn.loadSettingsFrom(settings);
        m_abortifnotlocal.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_uricolumn.validateSettings(settings);
        m_abortifnotlocal.validateSettings(settings);
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
