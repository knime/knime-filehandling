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
package org.knime.base.filehandling.remotecopy;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import org.knime.base.filehandling.NodeUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
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
 * This is the model implementation of the node.
 * 
 * 
 * @author Patrick Winter, University of Konstanz
 */
class RemoteCopyNodeModel extends NodeModel {

    private SettingsModelString m_sourcecolumn;

    private SettingsModelString m_targetcolumn;

    /**
     * Constructor for the node model.
     */
    protected RemoteCopyNodeModel() {
        super(1, 0);
        m_sourcecolumn = SettingsFactory.createSourceColumnSettings();
        m_targetcolumn = SettingsFactory.createTargetColumnSettings();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        int number = 0;
        ConnectionMonitor monitor = new ConnectionMonitor();
        try {
            // Get indexes for source and target
            int sourceIndex =
                    inData[0].getDataTableSpec().findColumnIndex(
                            m_sourcecolumn.getStringValue());
            int targetIndex =
                    inData[0].getDataTableSpec().findColumnIndex(
                            m_targetcolumn.getStringValue());
            for (DataRow row : inData[0]) {
                exec.setProgress(number++ / (double)inData[0].getRowCount());
                // Get cells
                DataCell sourceCell = row.getCell(sourceIndex);
                DataCell targetCell = row.getCell(targetIndex);
                // Ignore rows with one or more missing cells
                if (!sourceCell.isMissing() && !targetCell.isMissing()) {
                    // Get URIs
                    URI sourceURI =
                            ((URIDataValue)row.getCell(sourceIndex))
                                    .getURIContent().getURI();
                    URI targetURI =
                            ((URIDataValue)row.getCell(targetIndex))
                                    .getURIContent().getURI();
                    // Copy from sourceURI to targetURI
                    Copier.copy(sourceURI, targetURI, monitor, exec);
                }
            }
        } finally {
            monitor.closeConnections();
        }
        return new BufferedDataTable[]{};
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
        String source = m_sourcecolumn.getStringValue();
        String target = m_targetcolumn.getStringValue();
        // Is the source setting correct?
        NodeUtils.checkColumnSelection(inSpec, "Source", source,
                URIDataValue.class);
        // Is the target setting correct?
        NodeUtils.checkColumnSelection(inSpec, "Target", target,
                URIDataValue.class);
        // Do source and target differ?
        if (source.equals(target)) {
            throw new InvalidSettingsException(
                    "Source and target do not differ");
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
        checkSettings(inSpecs[0]);
        return new DataTableSpec[]{};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_sourcecolumn.saveSettingsTo(settings);
        m_targetcolumn.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_sourcecolumn.loadSettingsFrom(settings);
        m_targetcolumn.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_sourcecolumn.validateSettings(settings);
        m_targetcolumn.validateSettings(settings);
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
