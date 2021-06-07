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

import java.time.Duration;
import java.util.function.Consumer;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.ext.ssh.filehandling.fs.ConnectionToNodeModelBridge;
import org.knime.ext.ssh.filehandling.fs.SshFSConnectionConfig;
import org.knime.ext.ssh.filehandling.fs.SshFileSystem;
import org.knime.ext.ssh.filehandling.node.auth.KeyFileAuthProviderSettings;
import org.knime.ext.ssh.filehandling.node.auth.SshAuth;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.base.auth.AuthSettings;
import org.knime.filehandling.core.connections.base.auth.StandardAuthTypes;
import org.knime.filehandling.core.connections.base.auth.UserPasswordAuthProviderSettings;
import org.knime.filehandling.core.defaultnodesettings.EnumConfig;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;

/**
 * Settings for {@link SshConnectorNodeModel}.
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
public class SshConnectorNodeSettings {

    /**
     * Default value for the maximum number of SFTP sessions to open.
     */
    public static final int DEFAULT_MAX_SESSION_COUNT = 8;

    /**
     * Default value for connection timeout in seconds.
     */
    public static final int DEFAULT_CONNECTION_TIMEOUT_SECONDS = 30;

    private static final String KEY_WORKING_DIRECTORY = "workingDirectory";

    private static final String KEY_CONNECTION_TIMEOUT = "connectionTimeout";

    private static final String KEY_PORT = "port";

    private static final String KEY_HOST = "host";

    private static final String KEY_MAX_SESSION_COUNT = "maxSessionCount";

    private static final String KEY_USE_KNOWN_HOSTS = "useKnownHosts";

    private static final String KEY_KNOWN_HOSTS_FILE = "knownHostsFile";

    private NodeCreationConfiguration m_nodeCreationConfig;

    private final SettingsModelString m_host;
    private final SettingsModelIntegerBounded m_port;
    private final AuthSettings m_authSettings;
    private final SettingsModelString m_workingDirectory;

    private final SettingsModelIntegerBounded m_connectionTimeout;
    private final SettingsModelIntegerBounded m_maxSessionCount;
    private final SettingsModelBoolean m_useKnownHostsFile;
    private SettingsModelReaderFileChooser m_knownHostsFile;

    /**
     * @param cfg
     *            node creation configuration.
     */
    public SshConnectorNodeSettings(final NodeCreationConfiguration cfg) {
        m_nodeCreationConfig = cfg;

        m_host = new SettingsModelString(KEY_HOST, "localhost");
        m_port = new SettingsModelIntegerBounded(KEY_PORT, 22, 1, 65535);
        m_connectionTimeout = new SettingsModelIntegerBounded(KEY_CONNECTION_TIMEOUT,
                DEFAULT_CONNECTION_TIMEOUT_SECONDS,
                1,
                Integer.MAX_VALUE);
        m_maxSessionCount = new SettingsModelIntegerBounded(KEY_MAX_SESSION_COUNT, DEFAULT_MAX_SESSION_COUNT, 1, Integer.MAX_VALUE);

        m_authSettings = SshAuth.createAuthSettings(cfg);

        m_useKnownHostsFile = new SettingsModelBoolean(KEY_USE_KNOWN_HOSTS, false);

        m_knownHostsFile = new SettingsModelReaderFileChooser( //
                KEY_KNOWN_HOSTS_FILE, //
                cfg.getPortConfig().orElseThrow(() -> new IllegalStateException("port creation config is absent")), //
                SshConnectorNodeFactory.FS_CONNECT_GRP_ID, EnumConfig.create(FilterMode.FILE));

        m_workingDirectory = new SettingsModelString(KEY_WORKING_DIRECTORY, SshFileSystem.PATH_SEPARATOR);

        m_useKnownHostsFile.addChangeListener(e -> m_knownHostsFile.setEnabled(m_useKnownHostsFile.getBooleanValue()));

        m_knownHostsFile.setEnabled(false);
    }

    private void save(final NodeSettingsWO settings) {
        m_host.saveSettingsTo(settings);
        m_port.saveSettingsTo(settings);

        m_workingDirectory.saveSettingsTo(settings);

        m_connectionTimeout.saveSettingsTo(settings);
        m_maxSessionCount.saveSettingsTo(settings);
        m_useKnownHostsFile.saveSettingsTo(settings);
    }

    /**
     * Saves settings to the given {@link NodeSettingsWO} (to be called by the node
     * dialog).
     *
     * @param settings
     */
    public void saveSettingsForDialog(final NodeSettingsWO settings) {
        save(settings);
        // m_knownHostsFile must be saved by dialog component
        // m_authSettings are also saved by AuthenticationDialog
    }

    /**
     * Saves settings to the given {@link NodeSettingsWO} (to be called by the node
     * model).
     *
     * @param settings
     */
    public void saveSettingsForModel(final NodeSettingsWO settings) {
        save(settings);
        m_knownHostsFile.saveSettingsTo(settings);
        m_authSettings.saveSettingsForModel(settings.addNodeSettings(AuthSettings.KEY_AUTH));
    }

    private void load(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_host.loadSettingsFrom(settings);
        m_port.loadSettingsFrom(settings);

        m_workingDirectory.loadSettingsFrom(settings);

        m_connectionTimeout.loadSettingsFrom(settings);
        m_maxSessionCount.loadSettingsFrom(settings);
        m_useKnownHostsFile.loadSettingsFrom(settings);

        m_knownHostsFile.setEnabled(m_useKnownHostsFile.getBooleanValue());
    }

    /**
     * Loads settings from the given {@link NodeSettingsRO} (to be called by the
     * node dialog).
     *
     * @param settings
     * @throws NotConfigurableException
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings) throws NotConfigurableException {
        try {
            load(settings);
            // m_knownHostsFile and m_authSettings must be loaded by dialog
        } catch (InvalidSettingsException ex) {
            throw new NotConfigurableException(ex.getMessage(), ex);
        }
    }

    /**
     * Loads settings from the given {@link NodeSettingsRO} (to be called by the
     * node model).
     *
     * @param settings
     * @throws InvalidSettingsException
     */
    public void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        load(settings);
        m_knownHostsFile.loadSettingsFrom(settings);
        m_authSettings.loadSettingsForModel(settings.getNodeSettings(AuthSettings.KEY_AUTH));
    }

    /**
     * Forwards the given {@link PortObjectSpec} and status message consumer to the
     * file chooser settings models to they can configure themselves properly.
     *
     * @param inSpecs
     *            input specifications.
     * @param statusConsumer
     *            status consumer.
     * @param credentialsProvider
     * @throws InvalidSettingsException
     */
    public void configureInModel(final PortObjectSpec[] inSpecs, final Consumer<StatusMessage> statusConsumer,
            final CredentialsProvider credentialsProvider) throws InvalidSettingsException {

        if (m_useKnownHostsFile.getBooleanValue()) {
            m_knownHostsFile.configureInModel(inSpecs, statusConsumer);
        }

        m_authSettings.configureInModel(inSpecs, statusConsumer, credentialsProvider);
    }

    /**
     * Validates the settings in the given {@link NodeSettingsRO}.
     *
     * @param settings
     * @throws InvalidSettingsException
     */
    public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_host.validateSettings(settings);
        m_port.validateSettings(settings);
        m_authSettings.validateSettings(settings.getNodeSettings(AuthSettings.KEY_AUTH));
        m_workingDirectory.validateSettings(settings);
        m_connectionTimeout.validateSettings(settings);
        m_maxSessionCount.validateSettings(settings);
        m_useKnownHostsFile.validateSettings(settings);
        m_knownHostsFile.validateSettings(settings);

        final SshConnectorNodeSettings temp = new SshConnectorNodeSettings(m_nodeCreationConfig);
        temp.loadSettingsForModel(settings);
        temp.validate();
    }

    /**
     * Validates the current settings.
     *
     * @throws InvalidSettingsException
     */
    public void validate() throws InvalidSettingsException {
        if (isEmpty(getHost())) {
            throw new InvalidSettingsException("Host must be specified.");
        }

        if (getPort() < 1 || getPort() > 65535) {
            throw new InvalidSettingsException("Port must be between 1 and 65535.");
        }

        if (m_useKnownHostsFile.getBooleanValue() && isEmpty(m_knownHostsFile.getLocation().getPath())) {
            throw new InvalidSettingsException("Known hosts file must be specified.");
        }

        if (isEmpty(getWorkingDirectory())) {
            throw new InvalidSettingsException("Working directory must be specified.");
        }

        getAuthenticationSettings().validate();
    }

    static boolean isEmpty(final String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * @return working directory.
     */
    public String getWorkingDirectory() {
        return m_workingDirectory.getStringValue();
    }

    /**
     * @return the settings model for the working directory.
     */
    public SettingsModelString getWorkingDirectoryModel() {
        return m_workingDirectory;
    }

    /**
     * @return remote port.
     */
    public int getPort() {
        return m_port.getIntValue();
    }
    /**
     * @return remote host.
     */
    public String getHost() {
        return m_host.getStringValue();
    }

    /**
     * @return connection time out.
     */
    public Duration getConnectionTimeout() {
        return Duration.ofSeconds(m_connectionTimeout.getIntValue());
    }

    /**
     * @return known hosts file location.
     */
    public FSLocation getKnownHostsFile() {
        return m_knownHostsFile.getLocation();
    }

    /**
     * @param location
     *            location to test.
     * @return true if the location is NULL location in fact.
     */
    static boolean isEmptyLocation(final FSLocation location) {
        return location == null || isEmpty(location.getPath());
    }

    /**
     * @return host settings model.
     */
    public SettingsModelString getHostModel() {
        return m_host;
    }

    /**
     * @return port settings model.
     */
    public SettingsModelNumber getPortModel() {
        return m_port;
    }

    /**
     * @return connection time out settings model (holds seconds value)
     */
    public SettingsModelNumber getConnectionTimeoutModel() {
        return m_connectionTimeout;
    }

    /**
     * @return true, when a known hosts file should be used, false otherwise.
     */
    public boolean useKnownHostsFile() {
        return m_useKnownHostsFile.getBooleanValue();
    }

    /**
     * @return settings model for whether to use a known hosts file or not.
     */
    public SettingsModelBoolean getUseKnownHostsFileModel() {
        return m_useKnownHostsFile;
    }

    /**
     * @return known hosts location model.
     */
    public SettingsModelReaderFileChooser getKnownHostsFileModel() {
        return m_knownHostsFile;
    }

    /**
     * @return authentication settings.
     */
    public AuthSettings getAuthenticationSettings() {
        return m_authSettings;
    }

    /**
     * @return maximum number of SFTP sessions.
     */
    public int getMaxSessionCount() {
        return m_maxSessionCount.getIntValue();
    }

    /**
     * @return settings model of maximum number of SFTP sessions.
     */
    public SettingsModelIntegerBounded getMaxSessionCountModel() {
        return m_maxSessionCount;
    }

    /**
     * @return a (deep) clone of this node settings object.
     */
    public SshConnectorNodeSettings createClone() {
        final NodeSettings tempSettings = new NodeSettings("ignored");
        saveSettingsForModel(tempSettings);

        final SshConnectorNodeSettings toReturn = new SshConnectorNodeSettings(m_nodeCreationConfig);
        try {
            toReturn.loadSettingsForModel(tempSettings);
        } catch (InvalidSettingsException ex) { // NOSONAR can never happen
            // won't happen
        }
        return toReturn;
    }

    /**
     * Convert settings to a {@link SshFSConnectionConfig} instance.
     */
    SshFSConnectionConfig toFSConnectionConfig(final CredentialsProvider credentials,
            final ConnectionToNodeModelBridge bridge) throws InvalidSettingsException {

        final SshFSConnectionConfig cfg = new SshFSConnectionConfig(getWorkingDirectory());
        cfg.setHost(getHost());
        cfg.setConnectionTimeout(getConnectionTimeout());
        cfg.setPort(getPort());
        cfg.setMaxSftpSessionLimit(getMaxSessionCount());

        // auth
        final AuthSettings auth = getAuthenticationSettings();
        cfg.setUseKeyFile(auth.getAuthType() == SshAuth.KEY_FILE_AUTH_TYPE);
        cfg.setUseKnownHosts(useKnownHostsFile());

        if (auth.getAuthType() == StandardAuthTypes.USER_PASSWORD) {
            final UserPasswordAuthProviderSettings userPwdSettings = auth
                    .getSettingsForAuthType(StandardAuthTypes.USER_PASSWORD);
            cfg.setUserName(userPwdSettings.getUser(credentials::get));
            cfg.setPassword(userPwdSettings.getPassword(credentials::get));
        } else if (auth.getAuthType() == SshAuth.KEY_FILE_AUTH_TYPE) {
            final KeyFileAuthProviderSettings keyFileSettings = auth.getSettingsForAuthType(SshAuth.KEY_FILE_AUTH_TYPE);
            cfg.setUserName(keyFileSettings.getKeyUserModel().getStringValue());
            cfg.setKeyFilePassword(keyFileSettings.getKeyPassphraseModel().getStringValue());
        }

        cfg.setBridge(bridge);

        return cfg;
    }
}
