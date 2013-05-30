/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
package org.knime.base.filehandling.upload2;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import org.apache.commons.io.FilenameUtils;
import org.knime.base.filehandling.NodeUtils;
import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformation;
import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformationPortObject;
import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformationPortObjectSpec;
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
 * @author Patrick Winter, KNIME.com, Zurich, Switzerland
 */
public class UploadNodeModel extends NodeModel {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(UploadNodeModel.class);

    private ConnectionInformation m_connectionInformation;

    private UploadConfiguration m_configuration;

    /**
     * Constructor for the node model.
     */
    public UploadNodeModel() {
        super(new PortType[]{ConnectionInformationPortObject.TYPE, BufferedDataTable.TYPE},
                new PortType[]{BufferedDataTable.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        // Create connection monitor
        ConnectionMonitor monitor = new ConnectionMonitor();
        // Create output spec and container
        DataTableSpec outSpec = createOutSpec();
        BufferedDataContainer outContainer = exec.createDataContainer(outSpec);
        try {
            exec.setProgress("Connecting to " + m_connectionInformation.toURI());
            String source = m_configuration.getSource();
            // Get table with source URIs
            BufferedDataTable table = (BufferedDataTable)inObjects[1];
            int index = table.getDataTableSpec().findColumnIndex(source);
            int i = 0;
            int rows = table.getRowCount();
            // Create target folder
            String targetFolder = m_configuration.getTarget();
            if (!targetFolder.endsWith("/")) {
                targetFolder += "/";
            }
            URI folderUri = new URI(m_connectionInformation.toURI().toString() + NodeUtils.encodePath(targetFolder));
            RemoteFile folder = RemoteFileFactory.createRemoteFile(folderUri, m_connectionInformation, monitor);
            folder.mkDirs(true);
            // Process each row
            for (DataRow row : table) {
                // Skip missing values
                if (!row.getCell(index).isMissing()) {
                    exec.checkCanceled();
                    exec.setProgress((double)i / rows);
                    // Get source URI
                    URI uri = ((URIDataValue)row.getCell(index)).getURIContent().getURI();
                    // Create source file (no connection information supported)
                    RemoteFile sourceFile = RemoteFileFactory.createRemoteFile(uri, null, null);
                    // Upload file
                    upload(uri, sourceFile, folder, outContainer, monitor, exec);
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
     * Uploads a single file to the configured remote location.
     * 
     * 
     * @param source The source file
     * @param folder Folder where the file goes into
     * @param monitor The connection monitor
     * @throws Exception If the operation could not be processed
     */
    private void upload(final URI sourceUri, final RemoteFile source, final RemoteFile folder,
            final BufferedDataContainer outContainer, final ConnectionMonitor monitor, final ExecutionContext exec)
            throws Exception {
        boolean mkDirs = true;
        // Get overwrite policy
        String overwritePolicy = m_configuration.getOverwritePolicy();
        // Get filename
        String pathHandling = m_configuration.getPathHandling();
        String name = "";
        if (pathHandling.equals(PathHandling.FULL_PATH.getName())) {
            name = source.getFullName();
        } else if (pathHandling.equals(PathHandling.ONLY_FILENAME.getName())) {
            name = source.getName();
            mkDirs = false;
        } else if (pathHandling.equals(PathHandling.TRUNCATE_PREFIX.getName())) {
            String prefix = m_configuration.getPrefix();
            prefix = new File(prefix).toURI().toString().replaceFirst("file:/", "");
            name = source.getFullName().replaceFirst(prefix, "");
        }
        if (name.startsWith("/")) {
            name = name.replaceFirst("/", "");
        }

        // Generate URI to the target
        URI targetUri = new URI(folder.getURI() + NodeUtils.encodePath(name));
        // Create target file
        RemoteFile target = RemoteFileFactory.createRemoteFile(targetUri, m_connectionInformation, monitor);
        // If the source is a directory upload inner files
        if (source.isDirectory()) {
            if (mkDirs) {
                target.mkDirs(true);
            }
            RemoteFile[] files = source.listFiles();
            for (int i = 0; i < files.length; i++) {
                upload(sourceUri, files[i], folder, outContainer, monitor, exec);
            }
        } else {
            boolean abort = false;
            boolean uploaded = false;
            boolean failure = false;
            try {
                if (mkDirs) {
                    target.mkDirs(false);
                }
                if (overwritePolicy.equals(OverwritePolicy.OVERWRITE.getName())) {
                    // Policy overwrite:
                    // Just write
                    target.write(source, exec);
                    uploaded = true;
                } else if (overwritePolicy.equals(OverwritePolicy.OVERWRITEIFNEWER.getName())) {
                    // Policy overwrite if newer:
                    // Get modification time
                    long sourceTime = source.lastModified();
                    long targetTime = target.lastModified();
                    // Check if both times could be retrieved, else do an
                    // overwrite
                    if (sourceTime > 0 && targetTime > 0) {
                        // Check if the target is older then the source
                        if (target.lastModified() < source.lastModified()) {
                            target.write(source, exec);
                            uploaded = true;
                        } else {
                            LOGGER.info(target.getURI() + " skipped, already up to date");
                        }
                    } else {
                        target.write(source, exec);
                        uploaded = true;
                    }
                } else if (overwritePolicy.equals(OverwritePolicy.ABORT.getName())) {
                    // Policy abort:
                    // Throw exception if the target exists
                    if (target.exists()) {
                        abort = true;
                        throw new Exception("File " + target.getFullName() + " already exists.");
                    }
                    target.write(source, exec);
                    uploaded = true;
                }
            } catch (Exception e) {
                if (abort || m_configuration.getAbortonfail()) {
                    throw e;
                }
                LOGGER.warn("Transfer from " + source.getURI() + " to " + target.getURI() + " failed: "
                        + e.getMessage());
                failure = true;
            }
            // URI to the source file
            String srcExtension = FilenameUtils.getExtension(sourceUri.getPath());
            URIContent srcContent = new URIContent(sourceUri, srcExtension);
            URIDataCell srcUriCell = new URIDataCell(srcContent);
            // URI to the created file
            String extension = FilenameUtils.getExtension(targetUri.getPath());
            URIContent content = new URIContent(targetUri, extension);
            URIDataCell uriCell = new URIDataCell(content);
            // Has the file been uploaded or not?
            BooleanCell uploadedCell = BooleanCell.get(uploaded);
            if (!m_configuration.getAbortonfail()) {
                // Has the download failed?
                BooleanCell failedCell = BooleanCell.get(failure);
                // Add file information to the container
                outContainer.addRowToTable(new DefaultRow("Row" + outContainer.size(), srcUriCell, uriCell,
                        uploadedCell, failedCell));
            } else {
                // Add file information to the container
                outContainer.addRowToTable(new DefaultRow("Row" + outContainer.size(), srcUriCell, uriCell,
                        uploadedCell));
            }
        }
    }

    /**
     * Factory method for the output table spec.
     * 
     * 
     * @return Output table spec
     */
    private DataTableSpec createOutSpec() {
        DataColumnSpec[] columnSpecs;
        if (m_configuration.getAbortonfail()) {
            columnSpecs = new DataColumnSpec[3];
        } else {
            columnSpecs = new DataColumnSpec[4];
            columnSpecs[3] = new DataColumnSpecCreator("Failed", BooleanCell.TYPE).createSpec();
        }
        columnSpecs[0] = new DataColumnSpecCreator("Source URI", URIDataCell.TYPE).createSpec();
        columnSpecs[1] = new DataColumnSpecCreator("URI", URIDataCell.TYPE).createSpec();
        columnSpecs[2] = new DataColumnSpecCreator("Uploaded", BooleanCell.TYPE).createSpec();
        return new DataTableSpec(columnSpecs);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        // Check if a port object is available
        if (inSpecs[0] == null) {
            throw new InvalidSettingsException("No connection information available");
        }
        ConnectionInformationPortObjectSpec object = (ConnectionInformationPortObjectSpec)inSpecs[0];
        m_connectionInformation = object.getConnectionInformation();
        // Check if the port object has connection information
        if (m_connectionInformation == null) {
            throw new InvalidSettingsException("No connection information available");
        }
        // Check if configuration has been loaded
        if (m_configuration == null) {
            throw new InvalidSettingsException("No settings available");
        }
        // Check that source configuration is correct
        String source = m_configuration.getSource();
        NodeUtils.checkColumnSelection((DataTableSpec)inSpecs[1], "Source", source, URIDataValue.class);
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
        new UploadConfiguration().loadAndValidate(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
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