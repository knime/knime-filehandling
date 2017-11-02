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
 *   Sep 5, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.extracturiinfo;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.knime.base.filehandling.NodeUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.IntCell;
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
 * This is the model implementation.
 * 
 * 
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 */
class ExtractURIInfoNodeModel extends NodeModel {

    private SettingsModelString m_columnselection;

    private SettingsModelBoolean m_authority;

    private SettingsModelBoolean m_fragment;

    private SettingsModelBoolean m_host;

    private SettingsModelBoolean m_path;

    private SettingsModelBoolean m_port;

    private SettingsModelBoolean m_query;

    private SettingsModelBoolean m_scheme;

    private SettingsModelBoolean m_userinfo;

    /**
     * Constructor for the node model.
     */
    protected ExtractURIInfoNodeModel() {
        super(1, 1);
        m_columnselection = SettingsFactory.createColumnSelectionSettings();
        m_authority = SettingsFactory.createAuthoritySettings();
        m_fragment = SettingsFactory.createFragmentSettings();
        m_host = SettingsFactory.createHostSettings();
        m_path = SettingsFactory.createPathSettings();
        m_port = SettingsFactory.createPortSettings();
        m_query = SettingsFactory.createQuerySettings();
        m_scheme = SettingsFactory.createSchemeSettings();
        m_userinfo = SettingsFactory.createUserInfoSettings();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception {
        ColumnRearranger rearranger = createColumnRearranger(inData[0].getDataTableSpec());
        BufferedDataTable out = exec.createColumnRearrangeTable(inData[0], rearranger, exec);
        return new BufferedDataTable[]{out};
    }

    /**
     * Create a rearranger that appends the selected information.
     * 
     * 
     * @param inSpec Specification of the input table
     * @return Rearranger that will append the selected columns
     * @throws InvalidSettingsException If the settings are incorrect
     */
    private ColumnRearranger createColumnRearranger(final DataTableSpec inSpec) throws InvalidSettingsException {
        // Check settings for correctness
        checkSettings(inSpec);
        String columnName;
        ColumnRearranger rearranger = new ColumnRearranger(inSpec);
        List<DataColumnSpec> colSpecs = new LinkedList<DataColumnSpec>();
        // Add columns depending on configuration
        if (m_authority.getBooleanValue()) {
            columnName = DataTableSpec.getUniqueColumnName(inSpec, "Authority");
            colSpecs.add(new DataColumnSpecCreator(columnName, StringCell.TYPE).createSpec());
        }
        if (m_fragment.getBooleanValue()) {
            columnName = DataTableSpec.getUniqueColumnName(inSpec, "Fragment");
            colSpecs.add(new DataColumnSpecCreator(columnName, StringCell.TYPE).createSpec());
        }
        if (m_host.getBooleanValue()) {
            columnName = DataTableSpec.getUniqueColumnName(inSpec, "Host");
            colSpecs.add(new DataColumnSpecCreator(columnName, StringCell.TYPE).createSpec());
        }
        if (m_path.getBooleanValue()) {
            columnName = DataTableSpec.getUniqueColumnName(inSpec, "Path");
            colSpecs.add(new DataColumnSpecCreator(columnName, StringCell.TYPE).createSpec());
        }
        if (m_port.getBooleanValue()) {
            columnName = DataTableSpec.getUniqueColumnName(inSpec, "Port");
            colSpecs.add(new DataColumnSpecCreator(columnName, IntCell.TYPE).createSpec());
        }
        if (m_query.getBooleanValue()) {
            columnName = DataTableSpec.getUniqueColumnName(inSpec, "Query");
            colSpecs.add(new DataColumnSpecCreator(columnName, StringCell.TYPE).createSpec());
        }
        if (m_scheme.getBooleanValue()) {
            columnName = DataTableSpec.getUniqueColumnName(inSpec, "Scheme");
            colSpecs.add(new DataColumnSpecCreator(columnName, StringCell.TYPE).createSpec());
        }
        if (m_userinfo.getBooleanValue()) {
            columnName = DataTableSpec.getUniqueColumnName(inSpec, "User info");
            colSpecs.add(new DataColumnSpecCreator(columnName, StringCell.TYPE).createSpec());
        }
        // Number of added columns
        final int newCols = colSpecs.size();
        // Factory that creates the cells for the configured attributes
        CellFactory factory = new AbstractCellFactory(colSpecs.toArray(new DataColumnSpec[colSpecs.size()])) {
            @Override
            public DataCell[] getCells(final DataRow row) {
                return createStringCells(row, inSpec, newCols);
            }
        };
        // Append new columns
        rearranger.append(factory);
        return rearranger;
    }

    /**
     * Create the correspondent string cell to the selected URI cell.
     * 
     * 
     * @param row The row with the URI cell
     * @param spec Specification of the input table
     * @param newCols Number of new columns
     * @return String cell
     */
    private DataCell[] createStringCells(final DataRow row, final DataTableSpec spec, final int newCols) {
        List<DataCell> cells = new ArrayList<DataCell>(newCols);
        // Get URI cell
        String column = m_columnselection.getStringValue();
        DataCell oldCell = row.getCell(spec.findColumnIndex(column));
        // Is the cell missing?
        if (oldCell.isMissing()) {
            // Create missing cells
            for (int i = 0; i < newCols; i++) {
                cells.add(DataType.getMissingCell());
            }
        } else {
            // Get URI
            URI uri = ((URIDataValue)oldCell).getURIContent().getURI();
            // Extract configured attributes
            if (m_authority.getBooleanValue()) {
                String value = uri.getAuthority();
                if (value == null) {
                    cells.add(DataType.getMissingCell());
                } else {
                    cells.add(new StringCell(value));
                }
            }
            if (m_fragment.getBooleanValue()) {
                String value = uri.getFragment();
                if (value == null) {
                    cells.add(DataType.getMissingCell());
                } else {
                    cells.add(new StringCell(value));
                }
            }
            if (m_host.getBooleanValue()) {
                String value = uri.getHost();
                if (value == null) {
                    cells.add(DataType.getMissingCell());
                } else {
                    cells.add(new StringCell(value));
                }
            }
            if (m_path.getBooleanValue()) {
                String value = uri.getPath();
                if (value == null) {
                    cells.add(DataType.getMissingCell());
                } else {
                    cells.add(new StringCell(value));
                }
            }
            if (m_port.getBooleanValue()) {
                int value = uri.getPort();
                if (value < 0) {
                    cells.add(DataType.getMissingCell());
                } else {
                    cells.add(new IntCell(value));
                }
            }
            if (m_query.getBooleanValue()) {
                String value = uri.getQuery();
                if (value == null) {
                    cells.add(DataType.getMissingCell());
                } else {
                    cells.add(new StringCell(value));
                }
            }
            if (m_scheme.getBooleanValue()) {
                String value = uri.getScheme();
                if (value == null) {
                    cells.add(DataType.getMissingCell());
                } else {
                    cells.add(new StringCell(value));
                }
            }
            if (m_userinfo.getBooleanValue()) {
                String value = uri.getUserInfo();
                if (value == null) {
                    cells.add(DataType.getMissingCell());
                } else {
                    cells.add(new StringCell(value));
                }
            }
        }
        return cells.toArray(new DataCell[cells.size()]);
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
        NodeUtils.checkColumnSelection(inSpec, "URI", selectedColumn, URIDataValue.class);
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
        DataTableSpec outSpec = createColumnRearranger(inSpecs[0]).createSpec();
        return new DataTableSpec[]{outSpec};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_columnselection.saveSettingsTo(settings);
        m_authority.saveSettingsTo(settings);
        m_fragment.saveSettingsTo(settings);
        m_host.saveSettingsTo(settings);
        m_path.saveSettingsTo(settings);
        m_port.saveSettingsTo(settings);
        m_query.saveSettingsTo(settings);
        m_scheme.saveSettingsTo(settings);
        m_userinfo.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_columnselection.loadSettingsFrom(settings);
        m_authority.loadSettingsFrom(settings);
        m_fragment.loadSettingsFrom(settings);
        m_host.loadSettingsFrom(settings);
        m_path.loadSettingsFrom(settings);
        m_port.loadSettingsFrom(settings);
        m_query.loadSettingsFrom(settings);
        m_scheme.loadSettingsFrom(settings);
        m_userinfo.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_columnselection.validateSettings(settings);
        m_authority.validateSettings(settings);
        m_fragment.validateSettings(settings);
        m_host.validateSettings(settings);
        m_path.validateSettings(settings);
        m_port.validateSettings(settings);
        m_query.validateSettings(settings);
        m_scheme.validateSettings(settings);
        m_userinfo.validateSettings(settings);
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
