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
 *   2023-02-15 (Alexander Bondaletov): created
 */
package org.knime.ext.box.filehandling.node;

import java.time.Duration;
import java.util.function.Consumer;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.ext.box.filehandling.fs.BoxFSConnectionConfig;
import org.knime.ext.box.filehandling.fs.BoxFileSystem;
import org.knime.filehandling.core.connections.base.auth.AuthSettings;
import org.knime.filehandling.core.connections.base.auth.AuthType;
import org.knime.filehandling.core.connections.base.auth.SingleSecretAuthProviderSettings;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;

/**
 * Node settings for the Box Connector
 *
 * @author Alexander Bondaletov, Redfield SE
 */
public class BoxConnectorSettings {
    private static final String KEY_WORKING_DIRECTORY = "workingDirectory";
    private static final String KEY_CONNECTION_TIMEOUT = "connectionTimeout";
    private static final String KEY_READ_TIMEOUT = "readTimeout";

    private static final int DEFAULT_TIMEOUT = 60;
    /**
     * Developer token authentication
     */
    public static final AuthType DEVELOPER_TOKEN_AUTH = new AuthType("developerToken", "Developer Token",
            "Authenticate with the developer token");

    private final AuthSettings m_authSettings;
    private final SettingsModelString m_workingDirectory;
    private final SettingsModelIntegerBounded m_connectionTimeout;
    private final SettingsModelIntegerBounded m_readTimeout;

    /**
     * Creates new instance
     */
    public BoxConnectorSettings() {
        m_authSettings = new AuthSettings.Builder() //
                .add(new SingleSecretAuthProviderSettings(DEVELOPER_TOKEN_AUTH)) //
                .defaultType(DEVELOPER_TOKEN_AUTH) //
                .build();

        m_workingDirectory = new SettingsModelString(KEY_WORKING_DIRECTORY, BoxFileSystem.SEPARATOR);
        m_connectionTimeout = new SettingsModelIntegerBounded(KEY_CONNECTION_TIMEOUT, DEFAULT_TIMEOUT, 0,
                Integer.MAX_VALUE);
        m_readTimeout = new SettingsModelIntegerBounded(KEY_READ_TIMEOUT, DEFAULT_TIMEOUT, 0, Integer.MAX_VALUE);
    }

    /**
     * @return the authSettings
     */
    public AuthSettings getAuthSettings() {
        return m_authSettings;
    }

    /**
     * @return the workingDirectory model
     */
    public SettingsModelString getWorkingDirectoryModel() {
        return m_workingDirectory;
    }

    /**
     * @return the workingDirectory
     */
    public String getWorkingDirectory() {
        return m_workingDirectory.getStringValue();
    }

    /**
     * @return the connectionTimeout model
     */
    public SettingsModelIntegerBounded getConnectionTimeoutModel() {
        return m_connectionTimeout;
    }

    /**
     * @return the connectionTimeout
     */
    public Duration getConnectionTimeout() {
        return Duration.ofSeconds(m_connectionTimeout.getIntValue());
    }

    /**
     * @return the readTimeout model
     */
    public SettingsModelIntegerBounded getReadTimeoutModel() {
        return m_readTimeout;
    }

    /**
     * @return the readTimeout
     */
    public Duration getReadTimeout() {
        return Duration.ofSeconds(m_readTimeout.getIntValue());
    }

    private void save(final NodeSettingsWO settings) {
        m_workingDirectory.saveSettingsTo(settings);
        m_connectionTimeout.saveSettingsTo(settings);
        m_readTimeout.saveSettingsTo(settings);
    }

    /**
     * Saves settings to the given {@link NodeSettingsWO} (to be called by the node
     * model).
     *
     * @param settings
     *            The settings.
     */
    public void saveForModel(final NodeSettingsWO settings) {
        save(settings);
        m_authSettings.saveSettingsForModel(settings.addNodeSettings(AuthSettings.KEY_AUTH));
    }

    /**
     * Saves settings to the given {@link NodeSettingsWO} (to be called by the node
     * dialog).
     *
     * @param settings
     *            The settings.
     */
    public void saveForDialog(final NodeSettingsWO settings) {
        save(settings);
        // m_authSettings are also saved by AuthenticationDialog
    }

    private void load(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_workingDirectory.loadSettingsFrom(settings);
        m_connectionTimeout.loadSettingsFrom(settings);
        m_readTimeout.loadSettingsFrom(settings);
    }

    /**
     * Loads settings from the given {@link NodeSettingsRO} (to be called by the
     * node model).
     *
     * @param settings
     *            The settings.
     * @throws InvalidSettingsException
     */
    public void loadForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        load(settings);
        m_authSettings.loadSettingsForModel(settings.getNodeSettings(AuthSettings.KEY_AUTH));
    }

    /**
     * Loads settings from the given {@link NodeSettingsRO} (to be called by the
     * node dialog).
     *
     * @param settings
     *            The settings.
     * @throws InvalidSettingsException
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings) throws InvalidSettingsException {
        load(settings);
        // m_authSettings are loaded by AuthenticationDialog
    }

    void configureInModel(final PortObjectSpec[] inSpecs, final Consumer<StatusMessage> statusConsumer,
            final CredentialsProvider credentialsProvider) throws InvalidSettingsException {
        m_authSettings.configureInModel(inSpecs, statusConsumer, credentialsProvider);
    }

    /**
     * Validates (shallow) the settings stored in the given {@link NodeSettingsRO}
     * without loading them.
     *
     * @param settings
     *            The settings.
     * @throws InvalidSettingsException
     */
    public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_authSettings.validateSettings(settings.getNodeSettings(AuthSettings.KEY_AUTH));
        m_workingDirectory.validateSettings(settings);
        m_connectionTimeout.validateSettings(settings);
        m_readTimeout.validateSettings(settings);
    }

    /**
     * Validates the current settings.
     *
     * @throws InvalidSettingsException
     */
    public void validate() throws InvalidSettingsException {
        m_authSettings.validate();

        var workDir = m_workingDirectory.getStringValue();
        if (workDir.isEmpty() || !workDir.startsWith(BoxFileSystem.SEPARATOR)) {
            throw new InvalidSettingsException("Working directory must be set to an absolute path.");
        }
    }

    BoxFSConnectionConfig createFSConnectionConfig(final CredentialsProvider credentialsProvider) {
        var config = new BoxFSConnectionConfig(getWorkingDirectory());

        config.setConnectionTimeout(getConnectionTimeout());
        config.setReadTimeout(getReadTimeout());

        var tokenSettings = (SingleSecretAuthProviderSettings) getAuthSettings()
                .getSettingsForAuthType(DEVELOPER_TOKEN_AUTH);
        config.setDeveloperToken(tokenSettings.getSecret(credentialsProvider));

        return config;
    }
}
