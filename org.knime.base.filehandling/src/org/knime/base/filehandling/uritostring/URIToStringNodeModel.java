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
package org.knime.base.filehandling.uritostring;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
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
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;

/**
 * This is the model implementation of URI to string.
 * 
 * 
 * @author Patrick Winter, University of Konstanz
 */
class URIToStringNodeModel extends NodeModel {

    private SettingsModelFilterString m_columnselection;

    /**
     * Constructor for the node model.
     */
    protected URIToStringNodeModel() {
        super(1, 1);
        m_columnselection = SettingsFactory.createColumnSelectionSettings();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        ColumnRearranger rearranger =
                createColumnRearranger(inData[0].getDataTableSpec());
        BufferedDataTable out =
                exec.createColumnRearrangeTable(inData[0], rearranger, exec);
        return new BufferedDataTable[]{out};
    }

    /**
     * Create a rearranger that replaces the selected columns with there string
     * counterpart.
     * 
     * 
     * @param inSpec Specification of the input table
     * @return Rearranger that will replace the selected columns
     * @throws InvalidSettingsException If the settings are incorrect
     */
    private ColumnRearranger createColumnRearranger(final DataTableSpec inSpec)
            throws InvalidSettingsException {
        // Check settings for correctness
        checkSettings(inSpec);
        ColumnRearranger rearranger = new ColumnRearranger(inSpec);
        // Get selected columns and create arrays for there indexes and
        // replacements specs
        List<String> columns = m_columnselection.getIncludeList();
        DataColumnSpec[] colSpecs = new DataColumnSpec[columns.size()];
        int[] colIndexes = new int[columns.size()];
        // Create new specification for each column
        for (int i = 0; i < columns.size(); i++) {
            colIndexes[i] = inSpec.findColumnIndex(columns.get(i));
            colSpecs[i] =
                    new DataColumnSpecCreator(columns.get(i), StringCell.TYPE)
                            .createSpec();
        }
        // Factory that will generate the replacements
        CellFactory factory = new AbstractCellFactory(colSpecs) {
            @Override
            public DataCell[] getCells(final DataRow row) {
                return createStringCells(row, inSpec);
            }
        };
        // Replace old columns with new ones
        rearranger.replace(factory, colIndexes);
        return rearranger;
    }

    /**
     * Create the correspondent string cells to the selected URI cells.
     * 
     * 
     * @param row The row with the URI cells
     * @param spec Specification of the input table
     * @return Replacement cells
     */
    private DataCell[] createStringCells(final DataRow row,
            final DataTableSpec spec) {
        List<String> columns = m_columnselection.getIncludeList();
        DataCell[] cells = new DataCell[columns.size()];
        // Create new cell for each selected cell
        for (int i = 0; i < columns.size(); i++) {
            DataCell oldCell =
                    row.getCell(spec.findColumnIndex(columns.get(i)));
            // Is the cell missing?
            if (oldCell.isMissing()) {
                cells[i] = DataType.getMissingCell();
            } else {
                // URI to string
                String value =
                        ((URIDataValue)oldCell).getURIContent().getURI()
                                .toString();
                cells[i] = new StringCell(value);
            }
        }
        return cells;
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
        List<String> columns = m_columnselection.getIncludeList();
        // Is at least one column selected?
        if (columns.size() < 1) {
            throw new InvalidSettingsException("No column selected");
        }
        // Are all the selected columns of the right type
        for (int i = 0; i < columns.size(); i++) {
            String column = columns.get(i);
            int index = inSpec.findColumnIndex(column);
            // Does the column exist?
            if (index < 0) {
                throw new InvalidSettingsException("Columns not set");
            }
            DataType type = inSpec.getColumnSpec(index).getType();
            if (!type.isCompatible(URIDataValue.class)) {
                throw new InvalidSettingsException("Column \"" + column
                        + "\" is not of the type URI");
            }
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
        // createColumnRearranger will check the settings
        DataTableSpec outSpec = createColumnRearranger(inSpecs[0]).createSpec();
        return new DataTableSpec[]{outSpec};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_columnselection.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_columnselection.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_columnselection.validateSettings(settings);
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
