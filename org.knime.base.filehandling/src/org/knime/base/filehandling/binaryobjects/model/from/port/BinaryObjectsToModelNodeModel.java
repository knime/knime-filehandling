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
package org.knime.base.filehandling.binaryobjects.model.from.port;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.blob.BinaryObjectDataCell;
import org.knime.core.data.blob.BinaryObjectDataValue;
import org.knime.core.data.container.CloseableRowIterator;
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
import org.knime.core.node.util.CheckUtils;

/**
 * This is the model implementation.
 *
 *
 * @author Eric Axt, KNIME GmbH, Konstanz, Germany
 */
final class BinaryObjectsToModelNodeModel extends NodeModel {

    private final SettingsModelString m_columnselection;

    /**
     * Constructor for the node model.
     */
    protected BinaryObjectsToModelNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE}, new PortType[]{PortObject.TYPE});
        m_columnselection = SettingsFactory.createColumnSelectionSettings();
    }

    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        var table = (BufferedDataTable)inData[0];
        CheckUtils.checkArgument(table.size() > 0, "The input table is empty.");
        var portObject = extractPortObjectInFirstRow(table, exec);
        return new PortObject[]{portObject};
    }

    private PortObject extractPortObjectInFirstRow(final BufferedDataTable table, final ExecutionContext exec)
        throws CanceledExecutionException, IOException {
        var selectedColumn = m_columnselection.getStringValue();
        int index = table.getSpec().findColumnIndex(selectedColumn);
        try (CloseableRowIterator iterator = table.iterator()) {
            DataRow row = iterator.next();
            if (table.size() > 1) {
                setWarningMessage("Table has more than one row; " + "taking first row \"" + row.getKey() + "\".");
            }
            exec.checkCanceled();
            final DataCell dc = row.getCell(index);
            CheckUtils.checkArgument(!dc.isMissing(), "The first cell in column '%s' is missing.", selectedColumn);
            final BinaryObjectDataValue model = (BinaryObjectDataValue)dc;
            @SuppressWarnings("resource") // inputStream is closed by PortUtil.
            var portObject = PortUtil.readObjectFromStream(model.openInputStream(), exec);
            return portObject;
        }

    }

    /**
     * Check if the settings are all valid.
     *
     * @param inSpecs
     *
     *
     * @param inSpec Specification of the input table
     * @throws InvalidSettingsException If the settings are incorrect
     */
    private void checkSettings(final DataTableSpec tableSpec) throws InvalidSettingsException {

        var columnName = m_columnselection.getStringValue();
        var column = tableSpec.getColumnSpec(columnName);
        CheckUtils.checkSettingNotNull(column, "The selected column '%s' is not contained in the input table.",
            columnName);
        var type = column.getType();
        CheckUtils.checkSetting(type.isCompatible(BinaryObjectDataValue.class),
            "The selected column of type '%s' is not compatible with binary objects.", BinaryObjectDataCell.TYPE, type);
    }

    @Override
    protected void reset() {
        // Not used
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        // createColumnRearranger will check the settings
        checkSettings((DataTableSpec)inSpecs[0]);
        return null; //NOSONAR
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_columnselection.saveSettingsTo(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_columnselection.loadSettingsFrom(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_columnselection.validateSettings(settings);
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
