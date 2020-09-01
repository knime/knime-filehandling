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

import java.util.function.Consumer;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.ext.ssh.filehandling.fs.SshFileSystem;
import org.knime.ext.ssh.filehandling.node.SshAuthenticationSettingsModel.AuthType;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;

/**
 * Settings for {@link SshConnectionNodeModel}.
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
public class SshConnectionSettingsModel extends SettingsModel implements ChangeListener {
    /**
     * Null location instance.
     */
    public static final FSLocation NULL_LOCATION = new FSLocation(FSCategory.LOCAL, "");

    private static final String AUTH = "auth";
    private static final String KEY_KNOWN_HOSTS_FILE = "knownHostsFile";
    private static final int DEFAULT_TIMEOUT = 30;

    private final String m_configName;
    private NodeCreationConfiguration m_nodeCreationConfig;

    private final SettingsModelString m_workingDirectory;
    private final SettingsModelIntegerBounded m_connectionTimeout;
    private final SettingsModelIntegerBounded m_port;
    private final SettingsModelString m_host;
    private final SettingsModelString m_userName;
    private final SshAuthenticationSettingsModel m_authSettings;
    private SettingsModelReaderFileChooser m_knownHostsFile;

    /**
     * @param configName
     *            configuration name.
     * @param cfg
     *            node creation configuration.
     */
    public SshConnectionSettingsModel(final String configName, final NodeCreationConfiguration cfg) {
        m_configName = configName;
        m_nodeCreationConfig = cfg;

        m_host = new SettingsModelString("host", "localhost");
        m_port = new SettingsModelIntegerBounded("port", 22, 1, Integer.MAX_VALUE);
        m_connectionTimeout = new SettingsModelIntegerBounded("connectionTimeout", DEFAULT_TIMEOUT,
                1,
                Integer.MAX_VALUE);

        m_authSettings = new SshAuthenticationSettingsModel(AUTH, cfg);

        m_userName = new SettingsModelString("user", System.getProperty("user.name"));
        m_knownHostsFile = createFileChooserSettings(KEY_KNOWN_HOSTS_FILE, cfg);

        m_workingDirectory = new SettingsModelString("workingDirectory", SshFileSystem.PATH_SEPARATOR);

        // enable change listening
        m_workingDirectory.addChangeListener(this);
        m_connectionTimeout.addChangeListener(this);
        m_port.addChangeListener(this);
        m_host.addChangeListener(this);
        m_userName.addChangeListener(this);
        m_authSettings.addChangeListener(this);
        m_knownHostsFile.addChangeListener(this);
    }

    private static SettingsModelReaderFileChooser createFileChooserSettings(
            final String name,
            final NodeCreationConfiguration cfg) {
        final SettingsModelReaderFileChooser model = new SettingsModelReaderFileChooser(
                name,
                cfg.getPortConfig().orElseThrow(() -> new IllegalStateException("port creation config is absent")),
                SshConnectionNodeFactory.FS_CONNECT_GRP_ID, FilterMode.FILE);
        return model;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);

        m_workingDirectory.setEnabled(enabled);
        m_connectionTimeout.setEnabled(enabled);
        m_port.setEnabled(enabled);
        m_host.setEnabled(enabled);
        m_userName.setEnabled(enabled);
        m_authSettings.setEnabled(enabled);
        m_knownHostsFile.setEnabled(enabled);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stateChanged(final ChangeEvent e) {
        // forward change event from listened fields to settings listeners.
        notifyChangeListeners();
    }

    /**
     * @return user name.
     */
    public String getUsername() {
        return m_userName.getStringValue();
    }

    /**
     * @return user name model.
     */
    public SettingsModelString getUsernameModel() {
        return m_userName;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public SshConnectionSettingsModel createClone() {
        // save
        NodeSettings settings = new NodeSettings("tmp");
        saveSettingsForModel(settings);

        // restore
        SshConnectionSettingsModel cloned = new SshConnectionSettingsModel(getConfigName(), m_nodeCreationConfig);
        try {
            cloned.loadSettingsForModel(settings);
            cloned.m_knownHostsFile = m_knownHostsFile.createClone();
        } catch (InvalidSettingsException ex) {
            throw new RuntimeException("Failed to clone SSH Node settings", ex);
        }
        return cloned;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsForModel(final NodeSettingsWO settings) {
        m_workingDirectory.saveSettingsTo(settings);
        m_connectionTimeout.saveSettingsTo(settings);
        m_port.saveSettingsTo(settings);
        m_host.saveSettingsTo(settings);
        m_userName.saveSettingsTo(settings);
        m_knownHostsFile.saveSettingsTo(settings);
        m_authSettings.saveSettingsForModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_workingDirectory.loadSettingsFrom(settings);
        m_connectionTimeout.loadSettingsFrom(settings);
        m_port.loadSettingsFrom(settings);
        m_host.loadSettingsFrom(settings);
        m_userName.loadSettingsFrom(settings);
        m_knownHostsFile.loadSettingsFrom(settings);

        // authentication
        m_authSettings.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getConfigName() {
        return m_configName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getModelTypeID() {
        return "SSHConnection";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
            throws NotConfigurableException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsForDialog(final NodeSettingsWO settings) throws InvalidSettingsException {
    }

    /**
     * Validates settings consistency for this instance. {@inheritDoc}
     */
    @Override
    protected void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        SshConnectionSettingsModel tmp = new SshConnectionSettingsModel(getConfigName(), m_nodeCreationConfig);
        tmp.loadSettingsForModel(settings);
        tmp.validate();
    }

    /**
     * @param inSpecs
     *            input specifications.
     * @param statusConsumer
     *            status consumer.
     * @throws InvalidSettingsException
     */
    public void configure(final PortObjectSpec[] inSpecs, final Consumer<StatusMessage> statusConsumer)
            throws InvalidSettingsException {
        SshAuthenticationSettingsModel auth = getAuthenticationSettings();
        if (auth.getAuthType() == AuthType.KEY_FILE) {
            auth.getKeyFileModel().configureInModel(inSpecs, statusConsumer);
        }
        if (hasKnownHostsFile()) {
            getKnownHostsFileModel().configureInModel(inSpecs, statusConsumer);
        }
    }

    /**
     * Validates the settings.
     *
     * @throws InvalidSettingsException
     */
    public void validate() throws InvalidSettingsException {
        int connectionTimeOut = m_connectionTimeout.getIntValue();
        if (connectionTimeOut < 1) {
            throw new InvalidSettingsException("Invalid connection timeout " + connectionTimeOut);
        }

        int port = m_port.getIntValue();
        if (port < 1) {
            throw new InvalidSettingsException("Invalid port " + port);
        }

        if (isEmpty(m_workingDirectory.getStringValue())) {
            throw new InvalidSettingsException("Working directory must be specified.");
        }
        if (isEmpty(m_host.getStringValue())) {
            throw new InvalidSettingsException("Host must be specified.");
        }

        if (m_authSettings.getAuthType() != AuthType.CREDENTIALS) {
            if (isEmpty(m_userName.getStringValue())) {
                throw new InvalidSettingsException("Please enter a valid user name");
            }
        }
    }

    static boolean isEmpty(final String str) {
        return str == null || str.length() == 0;
    }

    /**
     * @return working directory.
     */
    public String getWorkingDirectory() {
        return m_workingDirectory.getStringValue();
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
    public int getConnectionTimeout() {
        return m_connectionTimeout.getIntValue();
    }

    /**
     * @return known hosts file location.
     */
    public FSLocation getKnownHostsFile() {
        return m_knownHostsFile.getLocation();
    }

    /**
     * @return true if has key file.
     */
    public boolean hasKnownHostsFile() {
        FSLocation knownHosts = m_knownHostsFile.getLocation();
        return !isNullLocation(knownHosts);
    }

    /**
     * @param location
     *            location to test.
     * @return true if the location is NULL location in fact.
     */
    static boolean isNullLocation(final FSLocation location) {
        if (location == null || location == FSLocation.NULL || location == NULL_LOCATION) {
            return true;
        }
        if (location.getFileSystemCategory() != null && isEmpty(location.getPath())) {
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getUsername() + "@" + getHost() + ":" + getPort();
    }

    /**
     * @return path to known host location settings.
     */
    public static String[] getKnownHostLocationPath() {
        return new String[] { AUTH, KEY_KNOWN_HOSTS_FILE };
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
     * @return connection time out settings model.
     */
    public SettingsModelNumber getConnectionTimeoutModel() {
        return m_connectionTimeout;
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
    public SshAuthenticationSettingsModel getAuthenticationSettings() {
        return m_authSettings;
    }
}
