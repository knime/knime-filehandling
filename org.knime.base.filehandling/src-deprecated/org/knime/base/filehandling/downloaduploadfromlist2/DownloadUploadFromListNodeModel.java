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
package org.knime.base.filehandling.downloaduploadfromlist2;

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
public class DownloadUploadFromListNodeModel extends NodeModel {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DownloadUploadFromListNodeModel.class);

    private ConnectionInformation m_connectionInformation;

    private DownloadUploadFromListConfiguration m_configuration;

    /**
     * Constructor for the node model.
     */
    public DownloadUploadFromListNodeModel() {
        super(new PortType[]{ConnectionInformationPortObject.TYPE_OPTIONAL, BufferedDataTable.TYPE},
                new PortType[]{BufferedDataTable.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        // Create connection monitor
        final ConnectionMonitor<Connection> monitor = new ConnectionMonitor<>();
        // Create output spec and container
        final DataTableSpec outSpec = createOutSpec();
        final BufferedDataContainer outContainer = exec.createDataContainer(outSpec);
        try {
            final String source = m_configuration.getSource();
            final String target = m_configuration.getTarget();
            // Get table with source URIs
            final BufferedDataTable table = (BufferedDataTable)inObjects[1];
            final int sourceIndex = table.getDataTableSpec().findColumnIndex(source);
            final int targetIndex = table.getDataTableSpec().findColumnIndex(target);
            long i = 0L;
            final long rowCount = table.size();
            // Process each row
            for (final DataRow row : table) {
                exec.checkCanceled();
                exec.setProgress(String.format("Row %d/%d (\"%s\")", i, rowCount, row.getKey()));
                ExecutionContext subContext = exec.createSubExecutionContext(1.0 / rowCount);
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
                    // Create parent directories if not existent.
                    final URI folderUri =
                            new URI(targetUri.getScheme(), targetUri.getAuthority(), FilenameUtils.getFullPath(targetUri.getPath()), targetUri.getQuery(), targetUri.getFragment());
                    final RemoteFile<? extends Connection> folder =
                            RemoteFileFactory.createRemoteFile(folderUri, connectionInformation, monitor);
                    folder.mkDirs(true);

                    // Create target file
                    final RemoteFile<Connection> targetFile =
                            RemoteFileFactory.createRemoteFile(targetUri, connectionInformation, monitor);
                    // Copy file
                    copy(sourceUri, sourceFile, targetFile, outContainer, monitor, subContext);
                    i++;
                }
                subContext.setProgress(1.0);
                i += 1L;
            }
        } finally {
            // Close connections
            monitor.closeAll();
            outContainer.close();
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
        DataColumnSpec[] columnSpecs;
        if (m_configuration.getAbortonfail()) {
            columnSpecs = new DataColumnSpec[3];
        } else {
            columnSpecs = new DataColumnSpec[4];
            columnSpecs[3] = new DataColumnSpecCreator("Failed", BooleanCell.TYPE).createSpec();
        }
        columnSpecs[0] = new DataColumnSpecCreator("Source URI", URIDataCell.TYPE).createSpec();
        columnSpecs[1] = new DataColumnSpecCreator("URI", URIDataCell.TYPE).createSpec();
        columnSpecs[2] = new DataColumnSpecCreator("Transferred", BooleanCell.TYPE).createSpec();
        return new DataTableSpec(columnSpecs);
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
    private void copy(final URI sourceUri, final RemoteFile<Connection> source, final RemoteFile<Connection> target,
            final BufferedDataContainer outContainer, final ConnectionMonitor<Connection> monitor,
            final ExecutionContext exec) throws Exception {
        if (source.isDirectory()) {
            target.mkDir();
            final RemoteFile<Connection>[] files = source.listFiles();
            String targetUri = target.getURI().toString();
            if (!targetUri.endsWith("/")) {
                targetUri += "/";
            }
            for (final RemoteFile<Connection> file : files) {
                final URI newTargetUri = new URI(targetUri + NodeUtils.encodePath(file.getName()));
                final RemoteFile<Connection> newTarget =
                        RemoteFileFactory.createRemoteFile(newTargetUri, target.getConnectionInformation(), monitor);
                copy(sourceUri, file, newTarget, outContainer, monitor, exec);
            }
        } else {
            boolean abort = false;
            boolean transferred = false;
            boolean failure = false;
            try {
                // Get overwrite policy
                final String overwritePolicy = m_configuration.getOverwritePolicy();
                if (overwritePolicy.equals(OverwritePolicy.OVERWRITE.getName())) {
                    // Policy overwrite:
                    // Just write
                    target.write(source, exec);
                    transferred = true;
                } else if (overwritePolicy.equals(OverwritePolicy.OVERWRITEIFNEWER.getName())) {
                    // Policy overwrite if newer:
                    // Get modification time
                    final long sourceTime = source.lastModified();
                    final long targetTime = target.lastModified();
                    // Check if both times could be retrieved, else do an
                    // overwrite
                    if (sourceTime > 0 && targetTime > 0) {
                        // Check if the target is older then the source
                        if (target.lastModified() < source.lastModified()) {
                            target.write(source, exec);
                            transferred = true;
                        } else {
                            LOGGER.info(target.getURI() + " skipped, already up to date");
                        }
                    } else {
                        target.write(source, exec);
                        transferred = true;
                    }
                } else if (overwritePolicy.equals(OverwritePolicy.ABORT.getName())) {
                    // Policy abort:
                    // Throw exception if the target exists
                    if (target.exists()) {
                        abort = true;
                        throw new Exception("File " + target.getFullName() + " already exists.");
                    }
                    target.write(source, exec);
                    transferred = true;
                }
            } catch (final Exception e) {
                if (abort || m_configuration.getAbortonfail()) {
                    throw e;
                }
                LOGGER.warn("Transfer from " + source.getURI() + " to " + target.getURI() + " failed: "
                        + e.getMessage());
                failure = true;
            }
            // URI to the source file
            final String srcExtension = FilenameUtils.getExtension(sourceUri.getPath());
            final URIContent srcContent = new URIContent(sourceUri, srcExtension);
            final URIDataCell srcUriCell = new URIDataCell(srcContent);
            // URI to the created file
            final URI targetUri = target.getURI();
            final String extension = FilenameUtils.getExtension(targetUri.getPath());
            final URIContent content = new URIContent(targetUri, extension);
            final URIDataCell uriCell = new URIDataCell(content);
            // Has the file been transferred or not?
            final BooleanCell transferredCell = BooleanCell.get(transferred);
            if (!m_configuration.getAbortonfail()) {
                // Has the transfer failed?
                final BooleanCell failedCell = BooleanCell.get(failure);
                // Add file information to the container
                outContainer.addRowToTable(new DefaultRow("Row" + outContainer.size(), srcUriCell, uriCell,
                        transferredCell, failedCell));
            } else {
                // Add file information to the container
                outContainer.addRowToTable(new DefaultRow("Row" + outContainer.size(), srcUriCell, uriCell,
                        transferredCell));
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
