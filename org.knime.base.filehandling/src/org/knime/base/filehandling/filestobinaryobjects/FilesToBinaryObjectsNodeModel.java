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
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;

import org.knime.base.filehandling.NodeUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.blob.BinaryObjectCellFactory;
import org.knime.core.data.blob.BinaryObjectDataCell;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.uri.URIDataValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * This is the model implementation.
 * 
 * 
 * @author Patrick Winter, University of Konstanz
 */
final class FilesToBinaryObjectsNodeModel extends NodeModel {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(FilesToBinaryObjectsNodeModel.class);

    private final SettingsModelString m_uricolumn;

    private final SettingsModelString m_bocolumnname;

    private final SettingsModelString m_replace;

    /**
     * Constructor for the node model.
     */
    protected FilesToBinaryObjectsNodeModel() {
        super(1, 1);
        m_uricolumn = SettingsFactory.createURIColumnSettings();
        m_replace = SettingsFactory.createReplacePolicySettings();
        m_bocolumnname =
                SettingsFactory.createBinaryObjectColumnNameSettings(m_replace);
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
     * @return Rearranger that will add a binary object column or replace the
     *         URI column
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
        String bocolumnname;
        if (replace) {
            bocolumnname = m_uricolumn.getStringValue();
        } else {
            bocolumnname = m_bocolumnname.getStringValue();
        }
        ColumnRearranger rearranger = new ColumnRearranger(inSpec);
        // Create column of the binary objects
        DataColumnSpec colSpec =
                new DataColumnSpecCreator(bocolumnname,
                        BinaryObjectDataCell.TYPE).createSpec();
        int inColIndex = inSpec.findColumnIndex(uricolumn);
        // Factory that creates the binary objects
        FilesToBinaryCellFactory factory =
                new FilesToBinaryCellFactory(colSpec, bocellfactory, inColIndex);
        if (replace) {
            // Replace URI column with the binary object column
            rearranger.replace(factory, inColIndex);
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
    @SuppressWarnings("unchecked")
    private void checkSettings(final DataTableSpec inSpec)
            throws InvalidSettingsException {
        String uricolumn = m_uricolumn.getStringValue();
        NodeUtils.checkColumnSelection(inSpec, "URI", uricolumn,
                URIDataValue.class);
        boolean append =
                m_replace.getStringValue().equals(
                        ReplacePolicy.APPEND.getName());
        if (append) {
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

    private final class FilesToBinaryCellFactory extends SingleCellFactory {

        private final BinaryObjectCellFactory m_bocellfactory;

        private final int m_colIndex;

        /** Error count, atomic integer because of possible async exec. */
        private final AtomicInteger m_errorCount = new AtomicInteger();

        private final AtomicInteger m_totalCount = new AtomicInteger();

        private FilesToBinaryCellFactory(final DataColumnSpec newColSpec,
                final BinaryObjectCellFactory bocellfactory, final int colIndex) {
            super(newColSpec);
            m_bocellfactory = bocellfactory;
            m_colIndex = colIndex;
        }

        /** {@inheritDoc} */
        @Override
        public DataCell getCell(final DataRow row) {
            DataCell cell = row.getCell(m_colIndex);
            if (cell.isMissing()) {
                return DataType.getMissingCell();
            }
            URIDataValue value = (URIDataValue)cell;
            URI uri = value.getURIContent().getURI();
            InputStream input = null;
            try {
                m_totalCount.incrementAndGet();
                URL url = uri.toURL();
                input = url.openStream();
                return m_bocellfactory.create(input);
            } catch (Exception e) {
                String error = "Can't read \"" + uri + "\": " + e.getMessage();
                if (m_errorCount.getAndIncrement() == 0) {
                    error = error + " (suppressing further warnings)";
                    LOGGER.error(error, e);
                } else {
                    LOGGER.debug(error, e);
                }
                return DataType.getMissingCell();
            } finally {
                if (input != null) {
                    try {
                        input.close();
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }
        }

        @Override
        public void afterProcessing() {
            int error = m_errorCount.get();
            if (error != 0) {
                int total = m_totalCount.get();
                setWarningMessage(String.format(
                        "Failed to read %d/%d files (see log for details)",
                        error, total));
            }
        }
    }

}
