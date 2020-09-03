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

import static org.knime.ext.ssh.filehandling.node.SshConnectionSettingsModel.NULL_LOCATION;
import static org.knime.ext.ssh.filehandling.node.SshConnectionSettingsModel.isEmpty;

import java.util.Optional;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.context.url.URLConfiguration;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelAuthentication.AuthenticationType;
import org.knime.core.node.defaultnodesettings.SettingsModelPassword;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ButtonGroupEnumInterface;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;

/**
 * Settings for {@link SshConnectionNodeModel}.
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
public class SshAuthenticationSettingsModel extends SettingsModel implements ChangeListener {
    private static final String KEY_AUTH_TYPE = "authType";
    private static final String SECRET_KEY = "ekerjvjhmzle,ptktysq";
    private static final String KEY_KNOWN_HOSTS_FILE = "knownHostsFile";
    private static final String KEY_KEY_FILE = "keyFile";

    //authentication
    private AuthType m_authType;
    private final SettingsModelPassword m_password;
    private final String m_configName;
    private NodeCreationConfiguration m_nodeCreationConfig;

    private final SettingsModelString m_credential;
    private SettingsModelReaderFileChooser m_keyFile;
    private final SettingsModelPassword m_keyFilePassword;

    /**
     * Authentication type enumeration.
     *
     * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
     */
    public enum AuthType implements ButtonGroupEnumInterface {
        /**
         * User name and password authentication.
         */
        USER_PWD("Username & password", "Username and password based authentication"),
        /**
         * User name and password authentication based on workflow credentionals.
         */
        CREDENTIALS("Credentials", "Workflow credentials"),
        /**
         * Public key based authentication.
         */
        KEY_FILE("Key file", "Private key based authentication");

        private String m_toolTip;
        private String m_text;

        private AuthType(final String text, final String toolTip) {
            m_text = text;
            m_toolTip = toolTip;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getText() {
            return m_text;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getActionCommand() {
            return name();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getToolTip() {
            return m_toolTip;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isDefault() {
            return equals(USER_PWD);
        }

        /**
         * @param actionCommand the action command
         * @return the {@link AuthenticationType} for the action command
         */
        public static AuthType get(final String actionCommand) {
            return valueOf(actionCommand);
        }
    }

    /**
     * @param configName
     *            configuration name.
     * @param cfg
     *            node creation configuration.
     */
    public SshAuthenticationSettingsModel(final String configName, final NodeCreationConfiguration cfg) {
        m_configName = configName;
        m_nodeCreationConfig = cfg;

        //authentication
        m_authType = AuthType.USER_PWD;
        m_password = new SettingsModelPassword("password", SECRET_KEY, "");
        m_credential = new SettingsModelString("credential", "");
        m_keyFile = createFileChooserSettings(KEY_KEY_FILE, cfg);
        m_keyFilePassword = new SettingsModelPassword("keyFilePassword", SECRET_KEY, "");

        m_password.addChangeListener(this);
        m_credential.addChangeListener(this);
        m_keyFile.addChangeListener(this);
        m_keyFilePassword.addChangeListener(this);
    }

    private static SettingsModelReaderFileChooser createFileChooserSettings(
            final String name,
            final NodeCreationConfiguration cfg) {
        final SettingsModelReaderFileChooser model = new SettingsModelReaderFileChooser(
                name,
                cfg.getPortConfig().orElseThrow(() -> new IllegalStateException("port creation config is absent")),
                SshConnectionNodeFactory.FS_CONNECT_GRP_ID, FilterMode.FILE);

        // set up initial location for dialog
        final Optional<? extends URLConfiguration> urlConfig = cfg.getURLConfig();
        if (urlConfig.isPresent()) {
            model.setLocation(new FSLocation(FSCategory.CUSTOM_URL, "1000", urlConfig.get().getUrl().toString()));
        }

        return model;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);

        m_password.setEnabled(enabled);
        m_credential.setEnabled(enabled);
        m_keyFile.setEnabled(enabled);
        m_keyFilePassword.setEnabled(enabled);
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
     * @return authentication type.
     */
    public AuthType getAuthType() {
        return m_authType;
    }

    /**
     * @return password
     */
    public String getPassword() {
        String value = m_password.getStringValue();
        return isEmpty(value) ? null : value;
    }

    /**
     * @return password settings model.
     */
    public SettingsModelPassword getPasswordModel() {
        return m_password;
    }

    /**
     * @return workflow credentials.
     */
    public String getCredential() {
        String creds = m_credential.getStringValue();
        return isEmpty(creds) ? null : creds;
    }
    /**
     * @return key file location.
     */
    public FSLocation getKeyFile() {
        return m_keyFile.getLocation();
    }
    /**
     * @return key file password.
     */
    public String getKeyFilePassword() {
        return m_keyFilePassword.getStringValue();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public SshAuthenticationSettingsModel createClone() {
        // save
        NodeSettings settings = new NodeSettings("tmp");
        saveSettingsForModel(settings);

        // restore
        SshAuthenticationSettingsModel cloned = new SshAuthenticationSettingsModel(getConfigName(), m_nodeCreationConfig);
        try {
            cloned.loadSettingsForModel(settings);
            cloned.m_keyFile = m_keyFile.createClone();
        } catch (InvalidSettingsException ex) {
            throw new RuntimeException("Failed to clone SSH Node settings", ex);
        }
        return cloned;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsForModel(final NodeSettingsWO connectionSettings) {
        NodeSettingsWO settings = connectionSettings.addNodeSettings(getConfigName());

        settings.addString(KEY_AUTH_TYPE, m_authType.name());
        m_password.saveSettingsTo(settings);
        m_credential.saveSettingsTo(settings);
        m_keyFile.saveSettingsTo(settings);
        m_keyFilePassword.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForModel(final NodeSettingsRO connectionSettings) throws InvalidSettingsException {
        NodeSettingsRO settings = connectionSettings.getNodeSettings(getConfigName());

        m_authType = AuthType.valueOf(settings.getString(KEY_AUTH_TYPE));
        m_password.loadSettingsFrom(settings);
        m_credential.loadSettingsFrom(settings);
        try {
            m_keyFile.loadSettingsFrom(settings);
        } catch (InvalidSettingsException ex) {
            // possible error in case of null location
        }
        m_keyFilePassword.loadSettingsFrom(settings);
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
        return "SFTPAuthentication";
    }

    /**
     * @param authType the authType to set
     */
    public void setAuthType(final AuthType authType) {
        AuthType old = getAuthType();
        m_authType = authType;

        if (old != authType) {
            notifyChangeListeners();
        }
    }

    /**
     * @return path to key file location settings.
     */
    public String[] getKeyFileLocationPath() {
        return new String[] { m_configName, SshAuthenticationSettingsModel.KEY_KEY_FILE };
    }

    /**
     * @return path to known host location settings.
     */
    public String[] getKnownHostLocationPath() {
        return new String[] { m_configName, SshAuthenticationSettingsModel.KEY_KNOWN_HOSTS_FILE };
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
        SshAuthenticationSettingsModel tmp = new SshAuthenticationSettingsModel(getConfigName(), m_nodeCreationConfig);
        tmp.loadSettingsForModel(settings);
        tmp.validate();
    }

    /**
     * Validates the settings.
     *
     * @throws InvalidSettingsException
     */
    public void validate() throws InvalidSettingsException {
        switch (getAuthType()) {
        case CREDENTIALS:
            if (m_credential == null || isEmpty(m_credential.getStringValue())) {
                throw new InvalidSettingsException("Please select a valid credential");
            }
            break;
        case KEY_FILE:
            if (m_keyFile == null) {
                throw new InvalidSettingsException("Please select a valid credential");
            }
            break;
        case USER_PWD:
            if (m_password == null || isEmpty(m_password.getStringValue())) {
                throw new InvalidSettingsException("Please enter a valid password");
            }
            break;
        default:
            break;
        }
    }

    /**
     * @param location
     *            location to test.
     * @return true if the location is NULL location in fact.
     */
    private static boolean isNullLocation(final FSLocation location) {
        if (location == null || location == FSLocation.NULL || location == NULL_LOCATION) {
            return true;
        }
        if (location.getFileSystemCategory() != null && isEmpty(location.getPath())) {
            return true;
        }
        return false;
    }

    /**
     * @return true if has key file.
     */
    public boolean hasKeyFile() {
        FSLocation keyFile = m_keyFile.getLocation();
        return !isNullLocation(keyFile);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getAuthType().name();
    }

    /**
     * @return key file location model.
     */
    public SettingsModelReaderFileChooser getKeyFileModel() {
        return m_keyFile;
    }

    /**
     * @return credential model.
     */
    public SettingsModelString getCredentialModel() {
        return m_credential;
    }

    /**
     * @return key file password settings model.
     */
    public SettingsModelPassword getKeyFilePasswordModel() {
        return m_keyFilePassword;
    }
}
