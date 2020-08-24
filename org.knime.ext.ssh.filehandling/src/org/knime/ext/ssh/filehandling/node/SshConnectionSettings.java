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

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelPassword;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.ext.ssh.filehandling.fs.SshFileSystem;

/**
 * Settings for {@link SshConnectionNodeModel}.
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
public class SshConnectionSettings extends SettingsModel implements ChangeListener {
    private static final String AUTH = "auth";
    private static final String SECRET_KEY = "ekerjvjhmzle,ptktysq";

    /**
     * Default connection time out.
     */
    public static final int DEFAULT_TIMEOUT = 30;

    private final SettingsModelString m_workingDirectory;
    private final SettingsModelIntegerBounded m_connectionTimeout;
    private final SettingsModelIntegerBounded m_port;
    private final SettingsModelString m_host;

    //authentication
    private final SettingsModelString m_userName;
    private final SettingsModelPassword m_password;
    private final String m_configName;

    private static final String KNOWN_HOSTS_FILE = "knownHostsFile";
    private static final String KEY_FILE = "keyFile";

    /**
     * @param configName
     *            configuration name.
     */
    public SshConnectionSettings(final String configName) {
        m_configName = configName;

        m_host = new SettingsModelString("host", "localhost");
        m_port = new SettingsModelIntegerBounded("port", 22, 1, Integer.MAX_VALUE);
        m_connectionTimeout = new SettingsModelIntegerBounded("connectionTimeout", DEFAULT_TIMEOUT,
                1,
                Integer.MAX_VALUE);

        //authentication
        m_userName = new SettingsModelString("user", System.getProperty("user.name"));
        m_password = new SettingsModelPassword("password", SECRET_KEY, "");

        m_workingDirectory = new SettingsModelString("workingDirectory", SshFileSystem.PATH_SEPARATOR);

        // enable change listening
        m_workingDirectory.addChangeListener(this);
        m_connectionTimeout.addChangeListener(this);
        m_port.addChangeListener(this);
        m_host.addChangeListener(this);
        m_userName.addChangeListener(this);
        m_password.addChangeListener(this);
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
     * @return password
     */
    public String getPassword() {
        String value = m_password.getStringValue();
        return isEmpty(value) ? null : value;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public SshConnectionSettings createClone() {
        // save
        NodeSettings settings = new NodeSettings("tmp");
        saveSettingsForModel(settings);

        // restore
        SshConnectionSettings cloned = new SshConnectionSettings(getConfigName());
        try {
            cloned.loadSettingsForModel(settings);
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

        // authentication
        NodeSettingsWO auth = settings.addNodeSettings(AUTH);
        m_userName.saveSettingsTo(auth);
        m_password.saveSettingsTo(auth);
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

        // authentication
        NodeSettingsRO auth = settings.getNodeSettings(AUTH);
        m_userName.loadSettingsFrom(auth);
        m_password.loadSettingsFrom(auth);
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
        m_connectionTimeout.validateSettings(settings);
        m_port.validateSettings(settings);

        if (isEmpty(m_workingDirectory.getStringValue())) {
            throw new InvalidSettingsException("Working directory must be specified.");
        }
        if (isEmpty(m_host.getStringValue())) {
            throw new InvalidSettingsException("Host must be specified.");
        }
    }

    private static boolean isEmpty(final String str) {
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

    @Override
    public SshConnectionSettings clone() {
        SshConnectionSettings clone;
        try {
            clone = (SshConnectionSettings) super.clone();
        } catch (final CloneNotSupportedException e) {
            throw new InternalError(e);
        }

        return clone;
    }

    /**
     * @return path to 'keyFile' location settings.
     */
    public static String[] getKeyFileLocationPath() {
        return new String[] { AUTH, KEY_FILE };
    }

    /**
     * @return path to 'knowhHostsFile' location settings.
     */
    public static String[] getKnownHostLocationPath() {
        return new String[] { AUTH, KNOWN_HOSTS_FILE };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getUsername() + "@" + getHost() + ":" + getPort();
    }
}
