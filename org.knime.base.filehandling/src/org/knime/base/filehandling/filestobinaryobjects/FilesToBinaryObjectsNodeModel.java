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
package org.knime.base.filehandling.filestobinaryobjects;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.blob.BinaryObjectCellFactory;
import org.knime.core.data.blob.BinaryObjectDataCell;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
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
 * This is the model implementation of Files to Binary Objects.
 * 
 * 
 * @author Patrick Winter, University of Konstanz
 */
class FilesToBinaryObjectsNodeModel extends NodeModel {

    private SettingsModelString m_uricolumn;

    private SettingsModelString m_bocolumnname;

    private SettingsModelString m_replace;

    /**
     * Constructor for the node model.
     */
    protected FilesToBinaryObjectsNodeModel() {
        super(1, 1);
        m_uricolumn = SettingsFactory.createURIColumnSettings();
        m_bocolumnname = SettingsFactory.createBinaryObjectColumnNameSettings();
        m_replace = SettingsFactory.createReplacePolicySettings();
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
        boolean replace =
                m_replace.getStringValue().equals(
                        ReplacePolicy.REPLACE.getName());
        // Check settings for correctness
        checkSettings(inSpec);
        // Create binary object factory -- only assign during execution
        final BinaryObjectCellFactory bocellfactory =
                exec == null ? null : new BinaryObjectCellFactory(exec);
        String uricolumn = m_uricolumn.getStringValue();
        String bocolumnname = m_bocolumnname.getStringValue();
        ColumnRearranger rearranger = new ColumnRearranger(inSpec);
        // Create column of the binary objects
        DataColumnSpec colSpec =
                new DataColumnSpecCreator(bocolumnname,
                        BinaryObjectDataCell.TYPE).createSpec();
        // Factory that creates the binary objects
        CellFactory factory = new SingleCellFactory(colSpec) {
            @Override
            public DataCell getCell(final DataRow row) {
                return createBinaryObjectCell(row, inSpec, bocellfactory);
            }
        };
        if (replace) {
            // Replace URI column with the binary object column
            int index = inSpec.findColumnIndex(uricolumn);
            rearranger.replace(factory, index);
        } else {
            // Append the binary object column
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
        // Is the URI column set?
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
        // Is the binary object column name empty?
        if (m_bocolumnname.getStringValue().equals("")) {
            throw new InvalidSettingsException(
                    "Binary object column name can not be empty");
        }
        if (inSpec.findColumnIndex(m_bocolumnname.getStringValue()) != -1) {
            throw new InvalidSettingsException(
                    "Binary object column name already taken");
        }
    }

    /**
     * Create a cell containing the binary object.
     * 
     * 
     * Create a cell containing the binary object to the file referenced by the
     * URI cell of the row.
     * 
     * @param row
     * @param inSpec
     * @param bocellfactory
     * @return Cell containing the binary object
     */
    private DataCell createBinaryObjectCell(final DataRow row,
            final DataTableSpec inSpec,
            final BinaryObjectCellFactory bocellfactory) {
        // Assume missing cell
        DataCell result = DataType.getMissingCell();
        int uriIndex = inSpec.findColumnIndex(m_uricolumn.getStringValue());
        if (!row.getCell(uriIndex).isMissing()) {
            // Get location
            URIDataValue value = (URIDataValue)row.getCell(uriIndex);
            if (!value.getURIContent().getURI().getScheme().equals("file")) {
                throw new RuntimeException(
                        "This node only supports the protocol \"file\"");
            }
            String location = value.getURIContent().getURI().getPath();
            try {
                // Create input stream and give it to the factory
                InputStream input = new FileInputStream(location);
                result = bocellfactory.create(input);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        }
        return result;
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
                createColumnRearranger(inSpecs[0], null).createSpec();
        return new DataTableSpec[]{outSpec};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_uricolumn.saveSettingsTo(settings);
        m_bocolumnname.saveSettingsTo(settings);
        m_replace.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_uricolumn.loadSettingsFrom(settings);
        m_bocolumnname.loadSettingsFrom(settings);
        m_replace.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_uricolumn.validateSettings(settings);
        m_bocolumnname.validateSettings(settings);
        m_replace.validateSettings(settings);
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
