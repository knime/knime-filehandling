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
package org.knime.base.filehandling.upload;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import org.knime.base.filehandling.NodeUtils;
import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformation;
import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformationPortObject;
import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformationPortObjectSpec;
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
 * @author Patrick Winter, KNIME.com, Zurich, Switzerland
 */
public class UploadNodeModel extends NodeModel {

    private ConnectionInformation m_connectionInformation;

    private UploadConfiguration m_configuration;

    /**
     * Constructor for the node model.
     */
    public UploadNodeModel() {
        super(new PortType[]{ConnectionInformationPortObject.TYPE,
                BufferedDataTable.TYPE}, new PortType[]{});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects,
            final ExecutionContext exec) throws Exception {
        ConnectionMonitor monitor = new ConnectionMonitor();
        String source = m_configuration.getSource();
        // Get table with source URIs
        BufferedDataTable table = (BufferedDataTable)inObjects[1];
        int index = table.getDataTableSpec().findColumnIndex(source);
        int i = 0;
        int rows = table.getRowCount();
        // Process each row
        for (DataRow row : table) {
            // Skip missing values
            if (!row.getCell(index).isMissing()) {
                exec.checkCanceled();
                exec.setProgress((double)i / rows);
                // Get source URI
                URI uri =
                        ((URIDataValue)row.getCell(index)).getURIContent()
                                .getURI();
                // Create source file (no connection information supported)
                RemoteFile sourceFile =
                        RemoteFileFactory.createRemoteFile(uri, null, null);
                // Upload file
                upload(sourceFile, monitor);
                i++;
            }
        }
        // Close connections
        monitor.closeAll();
        return new PortObject[]{};
    }

    /**
     * Uploads a single file to the configured remote location.
     * 
     * 
     * @param source The source file
     * @param monitor The connection monitor
     * @throws Exception If the operation could not be processed
     */
    private void upload(final RemoteFile source, final ConnectionMonitor monitor)
            throws Exception {
        // Get overwrite policy
        String overwritePolicy = m_configuration.getOverwritePolicy();
        // If the source is a directory upload inner files
        if (source.isDirectory()) {
            RemoteFile[] files = source.listFiles();
            for (int i = 0; i < files.length; i++) {
                upload(files[i], monitor);
            }
        } else {
            // Get filename
            String pathHandling = m_configuration.getPathHandling();
            String name = "";
            if (pathHandling.equals(PathHandling.FULL_PATH.getName())) {
                name = source.getFullName();
            } else if (pathHandling
                    .equals(PathHandling.ONLY_FILENAME.getName())) {
                name = source.getName();
            } else if (pathHandling.equals(PathHandling.TRUNCATE_PREFIX
                    .getName())) {
                String prefix = m_configuration.getPrefix();
                name = source.getFullName().replaceFirst(prefix, "");
            }
            if (name.startsWith("/")) {
                name = name.replaceFirst("/", "");
            }
            // Generate URI to the target
            URI targetUri =
                    new URI(m_connectionInformation.toURI().toString()
                            + m_configuration.getTarget() + name);
            // Create target file
            RemoteFile target =
                    RemoteFileFactory.createRemoteFile(targetUri,
                            m_connectionInformation, monitor);
            target.mkDirs(false);
            if (overwritePolicy.equals(OverwritePolicy.OVERWRITE.getName())) {
                // Policy overwrite:
                // Just write
                target.write(source);
            } else if (overwritePolicy.equals(OverwritePolicy.OVERWRITEIFNEWER
                    .getName())) {
                // Policy overwrite if newer:
                // Get modification time
                long sourceTime = source.lastModified();
                long targetTime = target.lastModified();
                // Check if both times could be retrieved, else do an overwrite
                if (sourceTime > 0 && targetTime > 0) {
                    // Check if the target is older then the source
                    if (target.lastModified() < source.lastModified()) {
                        target.write(source);
                    }
                } else {
                    target.write(source);
                }
            } else if (overwritePolicy.equals(OverwritePolicy.ABORT.getName())) {
                // Policy abort:
                // Throw exception if the target exists
                if (target.exists()) {
                    throw new Exception("File " + target.getFullName()
                            + " already exists.");
                }
                target.write(source);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        // Check if a port object is available
        if (inSpecs[0] == null) {
            throw new InvalidSettingsException(
                    "No connection information available");
        }
        ConnectionInformationPortObjectSpec object =
                (ConnectionInformationPortObjectSpec)inSpecs[0];
        m_connectionInformation = object.getConnectionInformation();
        // Check if the port object has connection information
        if (m_connectionInformation == null) {
            throw new InvalidSettingsException(
                    "No connection information available");
        }
        // Check if configuration has been loaded
        if (m_configuration == null) {
            throw new InvalidSettingsException("No settings available");
        }
        // Check that source configuration is correct
        String source = m_configuration.getSource();
        NodeUtils.checkColumnSelection((DataTableSpec)inSpecs[1], "Source",
                source, URIDataValue.class);
        return new PortObjectSpec[]{};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // not used
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
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
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        new UploadConfiguration().loadAndValidate(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        UploadConfiguration config = new UploadConfiguration();
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
