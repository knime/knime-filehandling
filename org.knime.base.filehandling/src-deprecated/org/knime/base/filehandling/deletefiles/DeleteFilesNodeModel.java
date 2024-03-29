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
package org.knime.base.filehandling.deletefiles;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import org.apache.commons.io.FilenameUtils;
import org.knime.base.filehandling.NodeUtils;
import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformation;
import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformationPortObject;
import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformationPortObjectSpec;
import org.knime.base.filehandling.remote.files.Connection;
import org.knime.base.filehandling.remote.files.ConnectionMonitor;
import org.knime.base.filehandling.remote.files.RemoteFile;
import org.knime.base.filehandling.remote.files.RemoteFileFactory;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.uri.URIContent;
import org.knime.core.data.uri.URIDataCell;
import org.knime.core.data.uri.URIDataValue;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

/**
 * This is the model implementation.
 *
 *
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 */
@Deprecated
public class DeleteFilesNodeModel extends NodeModel {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DeleteFilesNodeModel.class);

    private ConnectionInformation m_connectionInformation;

    private DeleteFilesConfiguration m_configuration;

    /**
     * Constructor for the node model.
     */
    public DeleteFilesNodeModel() {
        super(new PortType[]{ConnectionInformationPortObject.TYPE_OPTIONAL, BufferedDataTable.TYPE},
                new PortType[]{BufferedDataTable.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        // Create connection monitor
        final ConnectionMonitor<? extends Connection> monitor = new ConnectionMonitor<>();
        // Create output spec and container
        final DataTableSpec outSpec = createOutSpec();
        final BufferedDataContainer outContainer = exec.createDataContainer(outSpec);
        try {
            final String target = m_configuration.getTarget();
            // Get table with source URIs
            final BufferedDataTable table = (BufferedDataTable)inObjects[1];
            final int targetIndex = table.getDataTableSpec().findColumnIndex(target);
            int i = 0;
            final int rows = table.getRowCount();
            exec.setMessage("Deleting files...");
            // Process each row
            for (final DataRow row : table) {
                exec.checkCanceled();
                exec.setProgress((double)i / rows, i + " of " + rows + " files deleted");
                // Skip missing values
                if (!row.getCell(targetIndex).isMissing()) {
                    ConnectionInformation connectionInformation;
                    // Get target URI
                    URI targetUri = ((URIDataValue)row.getCell(targetIndex)).getURIContent().getURI();
                    try {
                        m_connectionInformation.fitsToURI(targetUri);
                        connectionInformation = m_connectionInformation;
                    } catch (final Exception e) {
                        connectionInformation = null;
                    }
                    // Create target file
                    final RemoteFile<? extends Connection> targetFile =
                            RemoteFileFactory.createRemoteFile(targetUri, connectionInformation, monitor);
                    // Delete file
                    final boolean success = targetFile.delete();
                    if (!success) {
                        final String message = "File " + targetFile.getURI() + " could not be deleted";
                        if (m_configuration.getAbortonfail()) {
                            throw new Exception(message);
                        }
                        LOGGER.warn(message);
                    }
                    targetUri = targetFile.getURI();
                    // URI to the created file
                    final String extension = FilenameUtils.getExtension(targetUri.getPath());
                    final URIContent content = new URIContent(targetUri, extension);
                    final URIDataCell uriCell = new URIDataCell(content);
                    if (m_configuration.getAbortonfail()) {
                        // Add file information to the container
                        outContainer.addRowToTable(new DefaultRow("Row" + outContainer.size(), uriCell));
                    } else {
                        // Has the file been deleted or not?
                        final BooleanCell deletedCell = BooleanCell.get(success);
                        // Add file information to the container
                        outContainer.addRowToTable(new DefaultRow("Row" + outContainer.size(), uriCell, deletedCell));
                    }
                    i++;
                }
            }
            outContainer.close();
        } finally {
            // Close connections
            monitor.closeAll();
        }
        return new PortObject[]{outContainer.getTable()};
    }

    /**
     * Factory method for the output table spec.
     *
     *
     * @return Output table spec
     */
    private DataTableSpec createOutSpec() {
        DataColumnSpec[] columnSpecs = new DataColumnSpec[2];
        if (m_configuration.getAbortonfail()) {
            columnSpecs = new DataColumnSpec[1];
        } else {
            columnSpecs = new DataColumnSpec[2];
            columnSpecs[1] = new DataColumnSpecCreator("Deleted", BooleanCell.TYPE).createSpec();
        }
        columnSpecs[0] = new DataColumnSpecCreator("URI", URIDataCell.TYPE).createSpec();
        return new DataTableSpec(columnSpecs);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        // Check if a port object is available
        if (inSpecs[0] != null) {
            final ConnectionInformationPortObjectSpec object = (ConnectionInformationPortObjectSpec)inSpecs[0];
            m_connectionInformation = object.getConnectionInformation();
        } else {
            m_connectionInformation = null;
        }
        // Check if configuration has been loaded
        if (m_configuration == null) {
            throw new InvalidSettingsException("No settings available");
        }
        // Check that target configuration is correct
        final String target = m_configuration.getTarget();
        NodeUtils.checkColumnSelection((DataTableSpec)inSpecs[1], "Target", target, URIDataValue.class);
        return new PortObjectSpec[]{createOutSpec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // not used
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // not used
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_configuration != null) {
            m_configuration.save(settings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        new DeleteFilesConfiguration().loadAndValidate(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        final DeleteFilesConfiguration config = new DeleteFilesConfiguration();
        config.loadAndValidate(settings);
        m_configuration = config;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // not used
    }

}
