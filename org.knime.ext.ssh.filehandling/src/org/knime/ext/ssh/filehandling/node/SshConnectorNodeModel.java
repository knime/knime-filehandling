/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   2020-07-28 (Vyacheslav Soldatov): created
 */
package org.knime.ext.ssh.filehandling.node;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSelection;
import org.knime.core.webui.node.impl.WebUINodeModel;
import org.knime.ext.ssh.filehandling.fs.ConnectionToNodeModelBridge;
import org.knime.ext.ssh.filehandling.fs.SshFSConnection;
import org.knime.ext.ssh.filehandling.fs.SshFSConnectionConfig;
import org.knime.ext.ssh.filehandling.fs.SshFileSystem;
import org.knime.filehandling.core.connections.FSConnectionRegistry;
import org.knime.filehandling.core.defaultnodesettings.FileSystemHelper;
import org.knime.filehandling.core.port.FileSystemPortObject;
import org.knime.filehandling.core.port.FileSystemPortObjectSpec;

/**
 * SSH Connection node.
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings({ "deprecation", "restriction" })
public class SshConnectorNodeModel extends WebUINodeModel<SshConnectorNodeParameters> {

    /**
     * The default timeout
     */
    public static final int DEFAULT_CONNECTION_TIMEOUT_SECONDS = 30;

    /**
     * The default maximum number of SFTP sessions
     */
    public static final int DEFAULT_MAX_SESSION_COUNT = 8;

    /**
     * The default maximum number of concurrent shell sessions
     */
    public static final int DEFAULT_MAX_EXEC_CHANNEL_COUNT = 1;

    private String m_fsId;

    private SshFSConnection m_connection;

    /**
     * Creates new instance.
     *
     * @param creationConfig
     *            node creation configuration.
     */
    protected SshConnectorNodeModel(final NodeCreationConfiguration creationConfig) {
        super(creationConfig.getPortConfig().orElseThrow(IllegalStateException::new).getInputPorts(),
                creationConfig.getPortConfig().orElseThrow(IllegalStateException::new).getOutputPorts(),
                SshConnectorNodeParameters.class);
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs, final SshConnectorNodeParameters params)
            throws InvalidSettingsException {
        m_fsId = FSConnectionRegistry.getInstance().getKey();
        params.validateOnConfigure(getCredentialsProvider());
        final var optFsCon = Optional.ofNullable(inSpecs.length > 0 ? inSpecs[0] : null);
        final SshFSConnectionConfig config = createConnectionConfig(params, optFsCon, getCredentialsProvider());
        return new PortObjectSpec[] { createSpec(config) };
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec,
            final SshConnectorNodeParameters params) throws Exception {
        final var optFsCon = Optional.ofNullable(inObjects.length > 0 ? inObjects[0].getSpec() : null);
        final SshFSConnectionConfig config = createConnectionConfig(params, optFsCon, getCredentialsProvider());
        m_connection = new SshFSConnection(config);
        FSConnectionRegistry.getInstance().register(m_fsId, m_connection);
        return new PortObject[] { new FileSystemPortObject(createSpec(config)) };
    }

    static SshFSConnectionConfig createConnectionConfig(final SshConnectorNodeParameters params,
            final Optional<PortObjectSpec> optFsConPort, final CredentialsProvider credentials)
            throws InvalidSettingsException {

        return params.toFSConnectionConfig(credentials, new DefaultBridge(params, optFsConPort));
    }

    private FileSystemPortObjectSpec createSpec(final SshFSConnectionConfig config) {
        return new FileSystemPortObjectSpec(SshFileSystem.FS_TYPE.getTypeId(), m_fsId,
                SshFileSystem.createFSLocationSpec(config));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        setWarningMessage("SSH connection no longer available. Please re-execute the node.");
    }

    @Override
    protected void onDispose() {
        // close the file system also when the workflow is closed
        reset();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        if (m_connection != null) {
            m_connection.closeInBackground();
            m_connection = null;
        }
        m_fsId = null;
    }

    private static class DefaultBridge implements ConnectionToNodeModelBridge {

        private final SshConnectorNodeParameters m_params;

        private final NodeContext m_nodeContext;

        private Optional<PortObjectSpec> m_optFsConPort;

        DefaultBridge(final SshConnectorNodeParameters params, final Optional<PortObjectSpec> optFsConPort) {
            m_params = params;
            m_optFsConPort = optFsConPort;
            m_nodeContext = CheckUtils.checkArgumentNotNull(NodeContext.getContext(), "Node context required");
        }

        @Override
        public void doWithKnownHostsFile(final Consumer<Path> operation) throws IOException, InvalidSettingsException {
            doWithFileChooserModel(m_params.m_knownHostsFile, operation);
        }

        @Override
        public void doWithKeysFile(final Consumer<Path> operation) throws IOException, InvalidSettingsException {
            doWithFileChooserModel(m_params.m_authentication.m_keyFileAuth.m_keyFile,
                    operation);
        }

        private void doWithFileChooserModel(final FileSelection selection,
                final Consumer<Path> operation) throws IOException, InvalidSettingsException {

            NodeContext.pushContext(m_nodeContext);

            final var optFsCon = m_optFsConPort.map(FileSystemPortObjectSpec.class::cast)
                    .flatMap(FileSystemPortObjectSpec::getFileSystemConnection);

            try (final var fscon = FileSystemHelper.retrieveFSConnection(optFsCon, selection.getFSLocation())
                    .orElseThrow(() -> new InvalidSettingsException("Please connect the file system connector node."));
                    final var fs = fscon.getFileSystem()) {
                final var path = fs.getPath(selection.getFSLocation());
                operation.accept(path);
            } finally {
                NodeContext.removeLastContext();
            }
        }
    }
}
