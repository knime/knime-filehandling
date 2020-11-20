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
package org.knime.base.filehandling.download2;

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
import org.knime.base.util.WildcardMatcher;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.uri.URIContent;
import org.knime.core.data.uri.URIDataCell;
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
public class DownloadNodeModel extends NodeModel {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DownloadNodeModel.class);

    private ConnectionInformation m_connectionInformation;

    private DownloadConfiguration m_configuration;

    private boolean m_abort;

    /**
     * Constructor for the node model.
     */
    public DownloadNodeModel() {
        super(new PortType[]{ConnectionInformationPortObject.TYPE}, new PortType[]{BufferedDataTable.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        m_abort = false;
        // Create connection monitor
        final ConnectionMonitor<Connection> monitor = new ConnectionMonitor<>();
        // Create output spec and container
        final DataTableSpec outSpec = createOutSpec();
        final BufferedDataContainer outContainer = exec.createDataContainer(outSpec);
        try {
            exec.setProgress("Connecting to " + m_connectionInformation.toURI());
            // Generate URI to the source
            final URI sourceUri =
                    new URI(m_connectionInformation.toURI().toString()
                            + NodeUtils.encodePath(m_configuration.getSource()));
            // Create remote file for source selection
            final RemoteFile<Connection> file =
                    RemoteFileFactory.createRemoteFile(sourceUri, m_connectionInformation, monitor);
            // Create target folder
            final RemoteFile<Connection> folder =
                    RemoteFileFactory.createRemoteFile(new File(m_configuration.getTarget()).toURI(), null, null);
            folder.mkDirs(true);
            // Download the selected directory or file
            download(file, folder, outContainer, true, exec);
            outContainer.close();
        } finally {
            // Close connections
            monitor.closeAll();
        }
        return new PortObject[]{outContainer.getTable()};
    }

    /**
     * Downloads a file or folder.
     *
     *
     * Downloads a file or folder to the configured target directory and writes
     * the new location into the container. Folders will be downloaded
     * recursively.
     *
     * @param source The file or folder to be downloaded
     * @param folder Folder where the file goes into
     * @param outContainer Container to write the reference of the downloaded
     *            file into
     * @param root If this source is the root file / directory
     * @param exec Execution context to check if the execution has been canceled
     * @throws Exception If remote file operation did not succeed
     */
    private void download(final RemoteFile<Connection> source, final RemoteFile<Connection> folder,
            final BufferedDataContainer outContainer, final boolean root, final ExecutionContext exec)
                    throws Exception {
        try {
            boolean mkDirs = true;
            // Get filename
            final String pathHandling = m_configuration.getPathHandling();
            String name = "";
            if (pathHandling.equals(PathHandling.FULL_PATH.getName())) {
                name = source.getFullName();
            } else if (pathHandling.equals(PathHandling.ONLY_FILENAME.getName())) {
                name = source.getName();
                mkDirs = false;
            } else if (pathHandling.equals(PathHandling.TRUNCATE_PREFIX.getName())) {
                final String prefix = m_configuration.getPrefix();
                name = source.getFullName().replaceFirst(prefix, "");
            }
            if (name.startsWith("/")) {
                name = name.replaceFirst("/", "");
            }
            // Generate URI to the target
            final URI targetUri = new File(folder.getFullName() + name).toURI();
            // Create target file
            final RemoteFile<Connection> target = RemoteFileFactory.createRemoteFile(targetUri, null, null);
            // Check if the user canceled
            exec.checkCanceled();
            // If the source is a directory download inner files
            if (source.isDirectory()) {
                if (root || m_configuration.getSubfolders()) {
                    if (mkDirs) {
                        target.mkDirs(true);
                    }
                    final RemoteFile<Connection>[] files = source.listFiles();
                    for (final RemoteFile<Connection> file : files) {
                        download(file, folder, outContainer, false, exec);
                    }
                }
            } else if (fitsFilter(source.getName())) {
                target.mkDirs(false);
                boolean downloaded = false;
                boolean failure = false;
                try {
                    // Get overwrite policy
                    final String overwritePolicy = m_configuration.getOverwritePolicy();
                    if (overwritePolicy.equals(OverwritePolicy.OVERWRITE.getName())) {
                        // Policy overwrite:
                        // Just write
                        target.write(source, exec);
                        downloaded = true;
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
                                downloaded = true;
                            } else {
                                LOGGER.info(target.getURI() + " skipped, already up to date");
                            }
                        } else {
                            target.write(source, exec);
                            downloaded = true;
                        }
                    } else if (overwritePolicy.equals(OverwritePolicy.ABORT.getName())) {
                        // Policy abort:
                        // Throw exception if the target exists
                        if (target.exists()) {
                            m_abort = true;
                            throw new Exception("File " + target.getFullName() + " already exists.");
                        }
                        target.write(source, exec);
                        downloaded = true;
                    }
                } catch (final Exception e) {
                    if (m_abort || m_configuration.getAbortonfail()) {
                        throw e;
                    }
                    LOGGER.warn("Transfer from " + source.getURI() + " to " + target.getURI() + " failed: "
                            + e.getMessage());
                    failure = true;
                }
                // URI to the created file
                final String extension = FilenameUtils.getExtension(targetUri.getPath());
                final URIContent content = new URIContent(targetUri, extension);
                final URIDataCell uriCell = new URIDataCell(content);
                // Has the file been downloaded or not?
                final BooleanCell downloadedCell = BooleanCell.get(downloaded);
                if (!m_configuration.getAbortonfail()) {
                    // Has the download failed?
                    final BooleanCell failedCell = BooleanCell.get(failure);
                    // Add file information to the container
                    outContainer.addRowToTable(new DefaultRow("Row" + outContainer.size(), uriCell, downloadedCell,
                            failedCell));
                } else {
                    // Add file information to the container
                    outContainer.addRowToTable(new DefaultRow("Row" + outContainer.size(), uriCell, downloadedCell));
                }
            }
        } catch (final Exception e) {
            if (m_abort || m_configuration.getAbortonfail()) {
                throw e;
            }
        }
    }

    /**
     * Check if the file fits the filter.
     *
     *
     * @param name Name of the file
     * @return true if it fits, false if it gets filtered out
     */
    private boolean fitsFilter(final String name) {
        boolean result = false;
        final String filter = m_configuration.getFilterType();
        String pattern = m_configuration.getFilterPattern();
        if (!m_configuration.getUseFilter()) {
            result = true;
        } else {
            if (filter.equals(FilterType.WILDCARD.getName())) {
                pattern = WildcardMatcher.wildcardToRegex(pattern);
            }
            result = name.matches(pattern);
        }
        return result;
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
            columnSpecs = new DataColumnSpec[2];
        } else {
            columnSpecs = new DataColumnSpec[3];
            columnSpecs[2] = new DataColumnSpecCreator("Failed", BooleanCell.TYPE).createSpec();
        }
        columnSpecs[0] = new DataColumnSpecCreator("URI", URIDataCell.TYPE).createSpec();
        columnSpecs[1] = new DataColumnSpecCreator("Downloaded", BooleanCell.TYPE).createSpec();
        return new DataTableSpec(columnSpecs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        // Check if a port object is available
        if (inSpecs[0] == null) {
            throw new InvalidSettingsException("No connection information available");
        }
        final ConnectionInformationPortObjectSpec object = (ConnectionInformationPortObjectSpec)inSpecs[0];
        m_connectionInformation = object.getConnectionInformation();
        // Check if the port object has connection information
        if (m_connectionInformation == null) {
            throw new InvalidSettingsException("No connection information available");
        }
        // Check if configuration has been loaded
        if (m_configuration == null) {
            throw new InvalidSettingsException("No settings available");
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
        new DownloadConfiguration().loadAndValidate(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        final DownloadConfiguration config = new DownloadConfiguration();
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
