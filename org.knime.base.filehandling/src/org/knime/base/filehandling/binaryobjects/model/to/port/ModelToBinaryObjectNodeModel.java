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
package org.knime.base.filehandling.binaryobjects.model.to.port;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.blob.BinaryObjectCellFactory;
import org.knime.core.data.blob.BinaryObjectDataCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortUtil;

/**
 * This is the model implementation.
 *
 *
 * @author Eric Axt, KNIME GmbH, Konstanz, Germany
 */
final class ModelToBinaryObjectNodeModel extends NodeModel {

    private final SettingsModelString m_columnname;

    /**
     * Constructor for the node model.
     */
    protected ModelToBinaryObjectNodeModel() {
        super(new PortType[]{PortObject.TYPE}, new PortType[]{BufferedDataTable.TYPE});
        m_columnname = SettingsFactory.createColumnNameSettings();
    }

    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        var dataContainer = exec.createDataContainer(createSpec());
        var dataCell = createDataCell(inData[0], exec);
        dataContainer.addRowToTable(new DefaultRow(RowKey.createRowKey(0l), dataCell));
        dataContainer.close();
        return new PortObject[]{dataContainer.getTable()};
    }

    private DataTableSpec createSpec() {
        return new DataTableSpec(
            new DataColumnSpecCreator(m_columnname.getStringValue(), BinaryObjectDataCell.TYPE).createSpec());
    }

    private static DataCell createDataCell(final PortObject portObject, final ExecutionContext exec)
        throws IOException, CanceledExecutionException {
        var out = new ByteArrayOutputStream();
        var factory = new BinaryObjectCellFactory(exec);
        PortUtil.writeObjectToStream(portObject, out, exec);
        return factory.create(out.toByteArray());
    }

    private void checkSettings() throws InvalidSettingsException {
        // Is column name empty?
        if (m_columnname.getStringValue().equals("")) {
            throw new InvalidSettingsException("Column name cannot be empty.");
        }
    }

    @Override
    protected void reset() {
        // Not used
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        // createColumnRearranger will check the settings
        checkSettings();
        return new PortObjectSpec[]{createSpec()};
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_columnname.saveSettingsTo(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_columnname.loadSettingsFrom(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_columnname.validateSettings(settings);
    }

    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // Not used
    }

    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // Not used
    }
}
