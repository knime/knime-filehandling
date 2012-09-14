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
 * This is the model implementation of Files to Binary Objects.
 * 
 * 
 * @author Patrick Winter, University of Konstanz
 */
class FilesToBinaryObjectsNodeModel extends NodeModel {

    private SettingsModelString m_locationcolumn;

    private SettingsModelString m_bocolumnname;

    private SettingsModelString m_replacepolicy;

    /**
     * Constructor for the node model.
     */
    protected FilesToBinaryObjectsNodeModel() {
        super(1, 1);
        m_locationcolumn = SettingsFactory.createLocationColumnSettings();
        m_bocolumnname = SettingsFactory.createBinaryObjectColumnNameSettings();
        m_replacepolicy = SettingsFactory.createReplacePolicySettings();
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

    private ColumnRearranger createColumnRearranger(final DataTableSpec inSpec,
            final ExecutionContext exec) throws InvalidSettingsException {
        checkSettings(inSpec);
        final BinaryObjectCellFactory bocellfactory =
                new BinaryObjectCellFactory(exec);
        String locationcolumn = m_locationcolumn.getStringValue();
        String bocolumnname = m_bocolumnname.getStringValue();
        String replacepolicy = m_replacepolicy.getStringValue();
        ColumnRearranger rearranger = new ColumnRearranger(inSpec);
        DataColumnSpec colSpec =
                new DataColumnSpecCreator(bocolumnname,
                        BinaryObjectDataCell.TYPE).createSpec();
        CellFactory factory = new SingleCellFactory(colSpec) {
            @Override
            public DataCell getCell(final DataRow row) {
                return createBinaryObjectCell(row, inSpec, bocellfactory);
            }
        };
        if (replacepolicy.equals(ReplacePolicy.APPEND.getName())) {
            rearranger.append(factory);
        }
        if (replacepolicy.equals(ReplacePolicy.REPLACE.getName())) {
            int index = inSpec.findColumnIndex(locationcolumn);
            rearranger.replace(factory, index);
        }
        return rearranger;
    }

    private void checkSettings(final DataTableSpec inSpec)
            throws InvalidSettingsException {
        // Is the location column set?
        if (m_locationcolumn.getStringValue().equals("")) {
            throw new InvalidSettingsException("Location column not set");
        }
        // Does the location column setting reference to an existing column?
        int columnIndex =
                inSpec.findColumnIndex(m_locationcolumn.getStringValue());
        if (columnIndex < 0) {
            throw new InvalidSettingsException("Location column not set");
        }
        // Is the binary object column name empty?
        if (m_bocolumnname.getStringValue().equals("")) {
            throw new InvalidSettingsException(
                    "Binary object column name can not be empty");
        }
    }

    private DataCell createBinaryObjectCell(final DataRow row,
            final DataTableSpec inSpec,
            final BinaryObjectCellFactory bocellfactory) {
        DataCell result = null;
        int locationIndex =
                inSpec.findColumnIndex(m_locationcolumn.getStringValue());
        if (!row.getCell(locationIndex).isMissing()) {
            String location =
                    ((StringValue)row.getCell(locationIndex)).getStringValue();
            try {
                InputStream input = new FileInputStream(location);
                result = bocellfactory.create(input);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        } else {
            result = DataType.getMissingCell();
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
        DataTableSpec outSpec =
                createColumnRearranger(inSpecs[0], null).createSpec();
        return new DataTableSpec[]{outSpec};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_locationcolumn.saveSettingsTo(settings);
        m_bocolumnname.saveSettingsTo(settings);
        m_replacepolicy.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_locationcolumn.loadSettingsFrom(settings);
        m_bocolumnname.loadSettingsFrom(settings);
        m_replacepolicy.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_locationcolumn.validateSettings(settings);
        m_bocolumnname.validateSettings(settings);
        m_replacepolicy.validateSettings(settings);
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
