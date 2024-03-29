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
package org.knime.base.filehandling.stringstobinaryobjects;

import java.io.File;
import java.io.IOException;

import org.knime.base.filehandling.NodeUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.StringValue;
import org.knime.core.data.blob.BinaryObjectCellFactory;
import org.knime.core.data.blob.BinaryObjectDataCell;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
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
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 */
class StringsToBinaryObjectsNodeModel extends NodeModel {

    private SettingsModelString m_columnselection;

    private SettingsModelString m_encoding;

    private SettingsModelString m_columnname;

    private SettingsModelString m_replace;

    /**
     * Constructor for the node model.
     */
    protected StringsToBinaryObjectsNodeModel() {
        super(1, 1);
        m_columnselection = SettingsFactory.createColumnSelectionSettings();
        m_encoding = SettingsFactory.createEncodingSettings();
        m_replace = SettingsFactory.createReplacePolicySettings();
        m_columnname = SettingsFactory.createColumnNameSettings(m_replace);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception {
        ColumnRearranger rearranger = createColumnRearranger(inData[0].getDataTableSpec(), exec);
        BufferedDataTable out = exec.createColumnRearrangeTable(inData[0], rearranger, exec);
        return new BufferedDataTable[]{out};
    }

    /**
     * Create a rearranger that either replaces the selected column with its
     * binary object counterpart, or appends a new column.
     *
     *
     * @param inSpec Specification of the input table
     * @param exec Context of this execution
     * @return Rearranger that will append a new column or replace the selected
     *         column
     * @throws InvalidSettingsException If the settings are incorrect
     */
    private ColumnRearranger createColumnRearranger(final DataTableSpec inSpec, final ExecutionContext exec)
            throws InvalidSettingsException {
        // Check settings for correctness
        checkSettings(inSpec);
        // Create binary object factory -- only assign during execution
        final BinaryObjectCellFactory bocellfactory = exec == null ? null : new BinaryObjectCellFactory(exec);
        // Get replace setting
        boolean replace = m_replace.getStringValue().equals(ReplacePolicy.REPLACE.getName());
        // Set column name
        String columnName;
        if (replace) {
            columnName = m_columnselection.getStringValue();
        } else {
            columnName = DataTableSpec.getUniqueColumnName(inSpec, m_columnname.getStringValue());
        }
        ColumnRearranger rearranger = new ColumnRearranger(inSpec);
        DataColumnSpec colSpec = new DataColumnSpecCreator(columnName, BinaryObjectDataCell.TYPE).createSpec();
        // Factory that creates a column with binary objects
        CellFactory factory = new SingleCellFactory(colSpec) {
            @Override
            public DataCell getCell(final DataRow row) {
                return createBinaryObjectCell(row, inSpec, bocellfactory);
            }
        };
        if (replace) {
            // Replace selected column with the binary objects from the factory
            rearranger.replace(factory, m_columnselection.getStringValue());
        } else {
            // Append binary objects from the factory
            rearranger.append(factory);
        }
        return rearranger;
    }

    /**
     * Create a cell containing the binary object.
     *
     *
     * @param row Row containing the string cell
     * @param spec Specification of the input table
     * @param bocellfactory Factory for the creation of the binary objects
     * @return Cell containing the binary object
     */
    private DataCell createBinaryObjectCell(final DataRow row, final DataTableSpec spec,
            final BinaryObjectCellFactory bocellfactory) {
        String column = m_columnselection.getStringValue();
        // Assume missing cell
        DataCell cell = DataType.getMissingCell();
        DataCell oldCell = row.getCell(spec.findColumnIndex(column));
        // Is the cell missing?
        if (!oldCell.isMissing()) {
            // String to binary object
            StringValue value = (StringValue)oldCell;
            try {
                byte[] bytes = value.getStringValue().getBytes(m_encoding.getStringValue());
                cell = bocellfactory.create(bytes);
            } catch (IOException e) {
                // ignore
            }
        }
        return cell;
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
        String selectedColumn = m_columnselection.getStringValue();
        NodeUtils.checkColumnSelection(inSpec, "Binary object", selectedColumn, StringValue.class);
        boolean append = m_replace.getStringValue().equals(ReplacePolicy.APPEND.getName());
        if (append) {
            // Is column name empty?
            if (m_columnname.getStringValue().equals("")) {
                throw new InvalidSettingsException("Column name cannot be empty");
            }
            if (inSpec.findColumnIndex(m_columnname.getStringValue()) != -1) {
                throw new InvalidSettingsException("Column name already taken");
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
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        // createColumnRearranger will check the settings
        DataTableSpec outSpec = createColumnRearranger(inSpecs[0], null).createSpec();
        return new DataTableSpec[]{outSpec};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_columnselection.saveSettingsTo(settings);
        m_encoding.saveSettingsTo(settings);
        m_columnname.saveSettingsTo(settings);
        m_replace.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_columnselection.loadSettingsFrom(settings);
        m_encoding.loadSettingsFrom(settings);
        m_columnname.loadSettingsFrom(settings);
        m_replace.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_columnselection.validateSettings(settings);
        m_encoding.validateSettings(settings);
        m_columnname.validateSettings(settings);
        m_replace.validateSettings(settings);
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
