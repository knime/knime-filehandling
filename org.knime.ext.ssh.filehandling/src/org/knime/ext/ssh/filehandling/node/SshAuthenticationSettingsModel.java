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

import static org.knime.ext.ssh.filehandling.node.SshConnectionSettingsModel.isEmpty;

import java.util.function.Consumer;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.defaultnodesettings.SettingsModelAuthentication.AuthenticationType;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelPassword;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ButtonGroupEnumInterface;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;

/**
 * Settings for {@link SshConnectionNodeModel}.
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
public class SshAuthenticationSettingsModel {

    private static final String KEY_AUTH_TYPE = "authType";

    private static final String KEY_CREDENTIAL = "credential";

    private static final String KEY_USER = "user";

    private static final String KEY_PASSWORD = "password";

    private static final String KEY_KEY_USER = "keyUser";

    private static final String KEY_USE_KEY_PASSPHRASE = "useKeyPassphrase";

    private static final String KEY_KEY_PASSPHRASE = "keyPassphrase";

    /**
     * Settings key for the key file (must be public to derive flow variable model).
     */
    public static final String KEY_KEY_FILE = "keyFile";

    private static final String SECRET_KEY = "ekerjvjhmzle,ptktysq";


    private final NodeCreationConfiguration m_nodeCreationConfig;

    private AuthType m_authType;

    private final SettingsModelString m_credential;

    private final SettingsModelString m_user;

    private final SettingsModelPassword m_password;

    private final SettingsModelString m_keyUser;

    private SettingsModelBoolean m_useKeyPassphrase;

    private final SettingsModelPassword m_keyPassphrase;

    private SettingsModelReaderFileChooser m_keyFile;



    /**
     * Authentication type enumeration.
     *
     * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
     */
    public enum AuthType implements ButtonGroupEnumInterface {
        /**
         * User name and password authentication.
         */
        USER_PWD("USER_PWD", "Username/password", "Username and password based authentication"),
        /**
         * User name and password authentication based on workflow credentials.
         */
        CREDENTIALS("CREDENTIALS", "Credentials", "Workflow credentials"),
        /**
         * Public key based authentication.
         */
        KEY_FILE("KEY_FILE", "Key file", "Private key based authentication");

        private final String m_settingsValue;
        private final String m_toolTip;
        private final String m_text;

        private AuthType(final String settingsValue, final String text, final String toolTip) {
            m_settingsValue = settingsValue;
            m_text = text;
            m_toolTip = toolTip;
        }

        /**
         * @return the settings value
         */
        public String getSettingsValue() {
            return m_settingsValue;
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

        /**
         * Maps a settings value string to an {@link AuthType} enum value.
         *
         * @param settingsValue
         *            The node settings value of a {@link AuthType} enum value.
         * @return the {@link AuthType} enum value corresponding to the given node
         *         settings value.
         * @throws IllegalArgumentException
         *             if there is no {@link AuthType} enum value corresponding to the
         *             given node settings value.
         */
        public static AuthType fromSettingsValue(final String settingsValue) {
            if (settingsValue.equals(CREDENTIALS.getSettingsValue())) {
                return CREDENTIALS;
            } else if (settingsValue.equals(USER_PWD.getSettingsValue())) {
                return USER_PWD;
            } else if (settingsValue.equals(KEY_FILE.getSettingsValue())) {
                return KEY_FILE;
            } else {
                throw new IllegalArgumentException("Invalid authentication type " + settingsValue);
            }
        }
    }

    /**
     * @param cfg
     *            node creation configuration.
     */
    public SshAuthenticationSettingsModel(final NodeCreationConfiguration cfg) {
        m_nodeCreationConfig = cfg;

        //authentication
        m_authType = AuthType.USER_PWD;
        m_credential = new SettingsModelString(KEY_CREDENTIAL, "");
        m_user = new SettingsModelString(KEY_USER, System.getProperty("user.name"));
        m_password = new SettingsModelPassword(KEY_PASSWORD, SECRET_KEY, "");
        m_keyUser = new SettingsModelString(KEY_KEY_USER, System.getProperty("user.name"));
        m_useKeyPassphrase = new SettingsModelBoolean(KEY_USE_KEY_PASSPHRASE, false);
        m_keyPassphrase = new SettingsModelPassword(KEY_KEY_PASSPHRASE, SECRET_KEY, "");
        m_keyFile = new SettingsModelReaderFileChooser(KEY_KEY_FILE,
                cfg.getPortConfig().orElseThrow(() -> new IllegalStateException("port creation config is absent")),
                SshConnectionNodeFactory.FS_CONNECT_GRP_ID, FilterMode.FILE);

        m_useKeyPassphrase.addChangeListener(e -> m_keyPassphrase
                .setEnabled(m_useKeyPassphrase.getBooleanValue() && m_useKeyPassphrase.isEnabled()));

        updateEnabledness();
    }

    /**
     * @return authentication type.
     */
    public AuthType getAuthType() {
        return m_authType;
    }

    /**
     * @param authType
     *            the authType to set
     */
    public void setAuthType(final AuthType authType) {
        m_authType = authType;
        updateEnabledness();
    }

    private void updateEnabledness() {
        m_credential.setEnabled(m_authType == AuthType.CREDENTIALS);

        m_user.setEnabled(m_authType == AuthType.USER_PWD);
        m_password.setEnabled(m_authType == AuthType.USER_PWD);

        m_keyUser.setEnabled(m_authType == AuthType.KEY_FILE);
        m_useKeyPassphrase.setEnabled(m_authType == AuthType.KEY_FILE);
        m_keyPassphrase.setEnabled(m_useKeyPassphrase.getBooleanValue() && m_authType == AuthType.KEY_FILE);
        m_keyFile.setEnabled(m_authType == AuthType.KEY_FILE);
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
     * @return a (deep) clone of this node settings object.
     */
    public SshAuthenticationSettingsModel createClone() {
        final NodeSettings tempSettings = new NodeSettings("ignored");
        saveSettingsTo(tempSettings);

        final SshAuthenticationSettingsModel toReturn = new SshAuthenticationSettingsModel(m_nodeCreationConfig);
        try {
            toReturn.loadSettingsFrom(tempSettings);
        } catch (InvalidSettingsException ex) { // NOSONAR can never happen
            // won't happen
        }
        return toReturn;
    }

    /**
     * Saves settings to the given {@link NodeSettingsWO}.
     *
     * @param settings
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString(KEY_AUTH_TYPE, m_authType.getSettingsValue());
        m_credential.saveSettingsTo(settings);
        m_user.saveSettingsTo(settings);
        m_password.saveSettingsTo(settings);
        m_keyUser.saveSettingsTo(settings);
        m_useKeyPassphrase.saveSettingsTo(settings);
        m_keyPassphrase.saveSettingsTo(settings);
        m_keyFile.saveSettingsTo(settings);
    }

    /**
     * Loads settings from the given {@link NodeSettingsRO}.
     *
     * @param settings
     * @throws InvalidSettingsException
     */
    public void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        try {
            m_authType = AuthType.fromSettingsValue(settings.getString(KEY_AUTH_TYPE));
        } catch (IllegalArgumentException e) {
            throw new InvalidSettingsException(
                    settings.getString(KEY_AUTH_TYPE) + " is not a valid authentication type", e);
        }

        m_credential.loadSettingsFrom(settings);
        m_user.loadSettingsFrom(settings);
        m_password.loadSettingsFrom(settings);
        m_keyUser.loadSettingsFrom(settings);
        m_useKeyPassphrase.loadSettingsFrom(settings);
        m_keyPassphrase.loadSettingsFrom(settings);
        m_keyFile.loadSettingsFrom(settings);

        updateEnabledness();
    }

    /**
     * Validates the settings in the given {@link NodeSettingsRO}.
     *
     * @param settings
     * @throws InvalidSettingsException
     */
    public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_credential.validateSettings(settings);
        m_user.validateSettings(settings);
        m_password.validateSettings(settings);
        m_keyUser.validateSettings(settings);
        m_useKeyPassphrase.validateSettings(settings);
        m_keyPassphrase.validateSettings(settings);
        m_keyFile.validateSettings(settings);

        SshAuthenticationSettingsModel temp = new SshAuthenticationSettingsModel(m_nodeCreationConfig);
        temp.loadSettingsFrom(settings);
        temp.validate();
    }

    /**
     * Validates the current settings.
     *
     * @throws InvalidSettingsException
     */
    public void validate() throws InvalidSettingsException {
        if (getAuthType() != AuthType.CREDENTIALS && isEmpty(m_user.getStringValue())) {
            throw new InvalidSettingsException("Please provide a valid user name");
        }

        switch (getAuthType()) {
        case CREDENTIALS:
            validateCredential();
            break;
        case USER_PWD:
            validateUserPasswordSettings();
            break;
        case KEY_FILE:
            validateKeyFileSettings();
            break;
        default:
            break;
        }
    }

    private void validateUserPasswordSettings() throws InvalidSettingsException {
        if (isEmpty(m_user.getStringValue())) {
            throw new InvalidSettingsException("Please provide a valid user name");
        }
        // password can be empty
    }

    private void validateKeyFileSettings() throws InvalidSettingsException {
        if (isEmpty(m_keyUser.getStringValue())) {
            throw new InvalidSettingsException("Please provide a valid user name");
        }

        if (m_useKeyPassphrase.getBooleanValue() && m_keyPassphrase.getStringValue().isEmpty()) {
            throw new InvalidSettingsException("Please enter a passphrase for the private key file");
        }

        if (m_keyFile.getLocation().getPath().isEmpty()) {
            throw new InvalidSettingsException("Please select a valid key file location");
        }
    }

    private void validateCredential() throws InvalidSettingsException {
        if (isEmpty(m_credential.getStringValue())) {
            throw new InvalidSettingsException("Please select a valid credential");
        }
    }

    /**
     * @param location
     *            location to test.
     * @return true if the location is NULL location in fact.
     */
    private static boolean isEmptyLocation(final FSLocation location) {
        return location == null || isEmpty(location.getPath());
    }

    /**
     * @return true if has key file.
     */
    public boolean hasKeyFile() {
        FSLocation keyFile = m_keyFile.getLocation();
        return !isEmptyLocation(keyFile);
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
     * @return the key file username model
     */
    public SettingsModelString getKeyUserModel() {
        return m_keyUser;
    }

    /**
     * @return model to determine whether to use a key passphrase or not.
     */
    public SettingsModelBoolean getUseKeyPassphraseModel() {
        return m_useKeyPassphrase;
    }

    /**
     * @return key file password settings model.
     */
    public SettingsModelPassword getKeyPassphraseModel() {
        return m_keyPassphrase;
    }

    /**
     * @return user name model.
     */
    public SettingsModelString getUserModel() {
        return m_user;
    }

    /**
     * Forwards the given {@link PortObjectSpec} and status message consumer to the
     * file chooser settings models to they can configure themselves properly.
     *
     * @param inSpecs
     *            input specifications.
     * @param statusConsumer
     *            status consumer.
     * @throws InvalidSettingsException
     */
    public void configureInModel(final PortObjectSpec[] inSpecs, final Consumer<StatusMessage> statusConsumer)
            throws InvalidSettingsException {

        if (m_authType == AuthType.KEY_FILE) {
            m_keyFile.configureInModel(inSpecs, statusConsumer);
        }
    }
}
