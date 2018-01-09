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
package org.knime.base.filehandling.downloaduploadfromlist;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import org.knime.base.filehandling.NodeUtils;
import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformation;
import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformationPortObject;
import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformationPortObjectSpec;
import org.knime.base.filehandling.remote.files.Connection;
import org.knime.base.filehandling.remote.files.ConnectionMonitor;
import org.knime.base.filehandling.remote.files.RemoteFile;
import org.knime.base.filehandling.remote.files.RemoteFileFactory;
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
public class DownloadUploadFromListNodeModel extends NodeModel {

    private ConnectionInformation m_connectionInformation;

    private DownloadUploadFromListConfiguration m_configuration;

    /**
     * Constructor for the node model.
     */
    public DownloadUploadFromListNodeModel() {
        super(new PortType[]{ConnectionInformationPortObject.TYPE_OPTIONAL, BufferedDataTable.TYPE},
                new PortType[]{});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        final ConnectionMonitor<Connection> monitor = new ConnectionMonitor<>();
        try {
            final String source = m_configuration.getSource();
            final String target = m_configuration.getTarget();
            // Get table with source URIs
            final BufferedDataTable table = (BufferedDataTable)inObjects[1];
            final int sourceIndex = table.getDataTableSpec().findColumnIndex(source);
            final int targetIndex = table.getDataTableSpec().findColumnIndex(target);
            int i = 0;
            final int rows = table.getRowCount();
            // Process each row
            for (final DataRow row : table) {
                exec.checkCanceled();
                exec.setProgress((double)i / rows);
                // Skip missing values
                if (!row.getCell(sourceIndex).isMissing() && !row.getCell(targetIndex).isMissing()) {
                    ConnectionInformation connectionInformation;
                    // Get source URI
                    final URI sourceUri = ((URIDataValue)row.getCell(sourceIndex)).getURIContent().getURI();
                    try {
                        m_connectionInformation.fitsToURI(sourceUri);
                        connectionInformation = m_connectionInformation;
                    } catch (final Exception e) {
                        connectionInformation = null;
                    }
                    // Create source file
                    final RemoteFile<Connection> sourceFile =
                            RemoteFileFactory.createRemoteFile(sourceUri, connectionInformation, monitor);
                    // Get target URI
                    final URI targetUri = ((URIDataValue)row.getCell(targetIndex)).getURIContent().getURI();
                    try {
                        m_connectionInformation.fitsToURI(targetUri);
                        connectionInformation = m_connectionInformation;
                    } catch (final Exception e) {
                        connectionInformation = null;
                    }
                    // Create target file
                    final RemoteFile<Connection> targetFile =
                            RemoteFileFactory.createRemoteFile(targetUri, connectionInformation, monitor);
                    targetFile.mkDirs(false);
                    // Copy file
                    copy(sourceFile, targetFile, monitor, exec);
                    i++;
                }
            }
        } finally {
            // Close connections
            monitor.closeAll();
        }
        return new PortObject[]{};
    }

    /**
     * Copies the source file to the target file location.
     *
     *
     * @param source The source file
     * @param target The target location
     * @param monitor The connection monitor
     * @throws Exception If the operation could not be processed
     */
    private void copy(final RemoteFile<Connection> source, final RemoteFile<Connection> target,
            final ConnectionMonitor<Connection> monitor,
            final ExecutionContext exec) throws Exception {
        if (source.isDirectory()) {
            target.mkDir();
            final RemoteFile<Connection>[] files = source.listFiles();
            String targetUri = target.getURI().toString();
            if (!targetUri.endsWith("/")) {
                targetUri += "/";
            }
            for (final RemoteFile<Connection> file : files) {
                final URI newTargetUri = new URI(targetUri + file.getName());
                final RemoteFile<Connection> newTarget =
                        RemoteFileFactory.createRemoteFile(newTargetUri, target.getConnectionInformation(), monitor);
                copy(file, newTarget, monitor, exec);
            }
        } else {
            // Get overwrite policy
            final String overwritePolicy = m_configuration.getOverwritePolicy();
            if (overwritePolicy.equals(OverwritePolicy.OVERWRITE.getName())) {
                // Policy overwrite:
                // Just write
                target.write(source, exec);
            } else if (overwritePolicy.equals(OverwritePolicy.OVERWRITEIFNEWER.getName())) {
                // Policy overwrite if newer:
                // Get modification time
                final long sourceTime = source.lastModified();
                final long targetTime = target.lastModified();
                // Check if both times could be retrieved, else do an overwrite
                if (sourceTime > 0 && targetTime > 0) {
                    // Check if the target is older then the source
                    if (target.lastModified() < source.lastModified()) {
                        target.write(source, exec);
                    }
                } else {
                    target.write(source, exec);
                }
            } else if (overwritePolicy.equals(OverwritePolicy.ABORT.getName())) {
                // Policy abort:
                // Throw exception if the target exists
                if (target.exists()) {
                    throw new Exception("File " + target.getFullName() + " already exists.");
                }
                target.write(source, exec);
            }
        }
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
        // Check that source configuration is correct
        final String source = m_configuration.getSource();
        NodeUtils.checkColumnSelection((DataTableSpec)inSpecs[1], "Source", source, URIDataValue.class);
        // Check that target configuration is correct
        final String target = m_configuration.getTarget();
        NodeUtils.checkColumnSelection((DataTableSpec)inSpecs[1], "Target", target, URIDataValue.class);
        if (m_configuration.getSource().equals(m_configuration.getTarget())) {
            throw new InvalidSettingsException("Source and target are the same");
        }
        return new PortObjectSpec[]{};
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
        new DownloadUploadFromListConfiguration().loadAndValidate(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        final DownloadUploadFromListConfiguration config = new DownloadUploadFromListConfiguration();
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
