/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   Sep 5, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.listdirectory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;

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
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.util.MutableInteger;

/**
 * This is the model implementation.
 *
 *
 * @author Patrick Winter, KNIME.com, Zurich, Switzerland
 */
public class ListDirectoryNodeModel extends NodeModel {

    private ConnectionInformation m_connectionInformation;

    private ListDirectoryConfiguration m_configuration;

    /**
     * Constructor for the node model.
     */
    public ListDirectoryNodeModel() {
        super(new PortType[]{new PortType(ConnectionInformationPortObject.class, true)},
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
            URI directoryUri;
            if (m_connectionInformation != null) {
                exec.setProgress("Connecting to " + m_connectionInformation.toURI());
                // Generate URI to the directory
                directoryUri =
                        new URI(m_connectionInformation.toURI().toString()
                                + NodeUtils.encodePath(m_configuration.getDirectory()));
            } else {
                // Create local URI
                directoryUri = new File(m_configuration.getDirectory()).toURI();
            }
            // Create remote file for directory selection
            final RemoteFile<? extends Connection> file =
                    RemoteFileFactory.createRemoteFile(directoryUri, m_connectionInformation, monitor);
            // List the selected directory
            exec.setProgress("Retrieving list of files");
            listDirectory(file, outContainer, true, exec, new MutableInteger(0), new MutableInteger(0));
            outContainer.close();
        } finally {
            // Close connections
            monitor.closeAll();
        }
        return new PortObject[]{outContainer.getTable()};
    }

    /**
     * List a directory.
     *
     *
     * Writes the location of all files in a directory into the container. Files
     * will be listed recursively if the option is selected.
     *
     * @param file The file or directory to be listed
     * @param outContainer Container to write the reference of the files into
     * @param root If this directory is the root directory
     * @param exec Execution context to check if the execution has been canceled
     * @throws Exception If remote file operation did not succeed
     */
    private void listDirectory(final RemoteFile<? extends Connection> file,
            final BufferedDataContainer outContainer, final boolean root,
            final ExecutionContext exec, final MutableInteger processedEntries, final MutableInteger maxEntries) throws Exception {
        // Check if the user canceled
        exec.checkCanceled();
        if (!root) {
            final URI fileUri = file.getURI();
            // URI to the file
            final String extension = FilenameUtils.getExtension(fileUri.getPath());
            final URIContent content = new URIContent(fileUri, extension);
            final URIDataCell uriCell = new URIDataCell(content);
            final BooleanCell boolCell = BooleanCell.get(file.isDirectory());
            // Add file information to the container
            outContainer.addRowToTable(new DefaultRow("Row" + outContainer.size(), uriCell, boolCell));
        }
        // If the source is a directory list inner files
        if (file.isDirectory()) {
            if (root || m_configuration.getRecursive()) {
                final RemoteFile<? extends Connection>[] files = file.listFiles();
                Arrays.sort(files);
                maxEntries.setValue(maxEntries.intValue() + files.length);
                exec.setMessage("Scanning " + file.getFullName());
                for (final RemoteFile<? extends Connection> file2 : files) {
                    listDirectory(file2, outContainer, false, exec, processedEntries, maxEntries);
                    processedEntries.inc();
                    exec.setProgress(processedEntries.intValue() / maxEntries.doubleValue());
                }
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
        final DataColumnSpec[] columnSpecs = new DataColumnSpec[2];
        columnSpecs[0] = new DataColumnSpecCreator("URI", URIDataCell.TYPE).createSpec();
        columnSpecs[1] = new DataColumnSpecCreator("Directory", BooleanCell.TYPE).createSpec();
        return new DataTableSpec(columnSpecs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        // Check if a port object is available
        if (inSpecs[0] != null) {
            final ConnectionInformationPortObjectSpec object = (ConnectionInformationPortObjectSpec)inSpecs[0];
            m_connectionInformation = object.getConnectionInformation();
            // Check if the port object has connection information
            if (m_connectionInformation == null) {
                throw new InvalidSettingsException("No connection information available");
            }
        } else {
            m_connectionInformation = null;
        }
        // Check if configuration has been loaded
        if (m_configuration == null) {
            throw new InvalidSettingsException("No settings available");
        }
        m_configuration.validate("Directory", m_configuration.getDirectory());
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
        new ListDirectoryConfiguration().loadAndValidate(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        final ListDirectoryConfiguration config = new ListDirectoryConfiguration();
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
