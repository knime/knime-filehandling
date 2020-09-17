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
import java.util.EnumSet;
import java.util.function.Consumer;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.node.workflow.ICredentials;
import org.knime.ext.ssh.filehandling.fs.ConnectionToNodeModelBridge;
import org.knime.ext.ssh.filehandling.fs.SshConnection;
import org.knime.ext.ssh.filehandling.fs.SshConnectionConfiguration;
import org.knime.ext.ssh.filehandling.fs.SshFileSystem;
import org.knime.ext.ssh.filehandling.node.SshAuthenticationSettingsModel.AuthType;
import org.knime.filehandling.core.connections.FSConnectionRegistry;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.ReadPathAccessor;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.status.NodeModelStatusConsumer;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage.MessageType;
import org.knime.filehandling.core.port.FileSystemPortObject;
import org.knime.filehandling.core.port.FileSystemPortObjectSpec;

/**
 * SSH Connection node.
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
public class SshConnectionNodeModel extends NodeModel {

    private static final String FILE_SYSTEM_NAME = "SSH";

    private static final Consumer<StatusMessage> NOOP_STATUS_CONSUMER = s -> {
    };

    private final SshConnectionSettingsModel m_settings;

    private final NodeModelStatusConsumer m_statusConsumer = new NodeModelStatusConsumer(
            EnumSet.of(MessageType.ERROR, MessageType.WARNING));

    private String m_fsId;

    private SshConnection m_connection;

    /**
     * Creates new instance.
     *
     * @param creationConfig
     *            node creation configuration.
     */
    protected SshConnectionNodeModel(final NodeCreationConfiguration creationConfig) {
        super(creationConfig.getPortConfig().orElseThrow(IllegalStateException::new).getInputPorts(),
                creationConfig.getPortConfig().orElseThrow(IllegalStateException::new).getOutputPorts());

        m_settings = new SshConnectionSettingsModel(creationConfig);
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        m_fsId = FSConnectionRegistry.getInstance().getKey();
        m_settings.configureInModel(inSpecs, m_statusConsumer);
        m_statusConsumer.setWarningsIfRequired(this::setWarningMessage);
        return new PortObjectSpec[] { createSpec() };
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        m_connection = createConnection(m_settings, getCredentialsProvider(), m_statusConsumer);
        m_statusConsumer.setWarningsIfRequired(this::setWarningMessage);
        FSConnectionRegistry.getInstance().register(m_fsId, m_connection);
        return new PortObject[] { new FileSystemPortObject(createSpec()) };
    }

    /**
     * @param settings
     *            SSH connection settings.
     * @param credentials
     *            credentials provider.
     * @return file system connection
     * @throws InvalidSettingsException
     * @throws IOException
     */
    public static SshConnection createConnection(final SshConnectionSettingsModel settings,
            final CredentialsProvider credentials)
            throws InvalidSettingsException, IOException {
        return createConnection(settings, credentials, NOOP_STATUS_CONSUMER);
    }

    private static SshConnection createConnection(final SshConnectionSettingsModel settings,
            final CredentialsProvider credentials,
            final Consumer<StatusMessage> statusConsumer) throws InvalidSettingsException, IOException {

        final SshConnectionConfiguration cfg = new SshConnectionConfiguration();
        cfg.setHost(settings.getHost());
        cfg.setConnectionTimeout(settings.getConnectionTimeout() * 1000l);
        cfg.setPort(settings.getPort());
        cfg.setMaxSftpSessionLimit(settings.getMaxSessionCount());

        // auth
        final SshAuthenticationSettingsModel auth = settings.getAuthenticationSettings();
        cfg.setUseKeyFile(auth.getAuthType() == AuthType.KEY_FILE);
        cfg.setUseKnownHosts(settings.useKnownHostsFile());

        if (auth.getAuthType() == AuthType.USER_PWD) {
            cfg.setUserName(auth.getUserModel().getStringValue());
            cfg.setPassword(auth.getPasswordModel().getStringValue());
        } else if (auth.getAuthType() == AuthType.KEY_FILE){
            cfg.setUserName(auth.getKeyUserModel().getStringValue());
            cfg.setKeyFilePassword(auth.getKeyPassphraseModel().getStringValue());
        } else {
            final ICredentials cred = credentials.get(auth.getCredential());
            if (cred == null) {
                throw new InvalidSettingsException("Unable to find credential flow variable: " + auth.getCredential());
            }
            cfg.setUserName(cred.getLogin());
            cfg.setPassword(cred.getPassword());
        }

        cfg.setBridge(new DefaultBridge(settings, statusConsumer));

        return new SshConnection(cfg, settings.getWorkingDirectory());
    }

    private FileSystemPortObjectSpec createSpec() {
        return new FileSystemPortObjectSpec(FILE_SYSTEM_NAME, m_fsId,
                SshFileSystem.createFSLocationSpec(m_settings.getHost()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        setWarningMessage("SSH connection no longer available. Please re-execute the node.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // nothing to save
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO output) {
        m_settings.saveSettingsTo(output);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO input) throws InvalidSettingsException {
        m_settings.validateSettings(input);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO input) throws InvalidSettingsException {
        m_settings.loadSettingsFrom(input);
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

        private final SshConnectionSettingsModel m_settings;

        private final Consumer<StatusMessage> m_statusConsumer;

        DefaultBridge(final SshConnectionSettingsModel settings, final Consumer<StatusMessage> statusConsumer) {
            m_settings = settings;
            m_statusConsumer = statusConsumer;
        }

        @Override
        public void doWithKnownHostsFile(final Consumer<Path> operation) throws IOException, InvalidSettingsException {
            doWithFileChooserModel(m_settings.getKnownHostsFileModel(), operation);
        }

        @Override
        public void doWithKeysFile(final Consumer<Path> operation) throws IOException, InvalidSettingsException {
            doWithFileChooserModel(m_settings.getAuthenticationSettings().getKeyFileModel(), operation);
        }

        private void doWithFileChooserModel(final SettingsModelReaderFileChooser chooser,
                final Consumer<Path> operation) throws IOException, InvalidSettingsException {
            try (ReadPathAccessor accessor = chooser.createReadPathAccessor()) {
                FSPath path = accessor.getRootPath(m_statusConsumer);
                operation.accept(path);
            }
        }
    }
}
