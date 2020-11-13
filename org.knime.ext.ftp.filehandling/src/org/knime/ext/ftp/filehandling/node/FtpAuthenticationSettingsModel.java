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
 *   2020-10-15 (Vyacheslav Soldatov): created
 */
package org.knime.ext.ftp.filehandling.node;

import static org.knime.ext.ftp.filehandling.node.FtpConnectionSettingsModel.isEmpty;

import java.util.function.Consumer;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelAuthentication.AuthenticationType;
import org.knime.core.node.defaultnodesettings.SettingsModelPassword;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ButtonGroupEnumInterface;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;

/**
 * Authentication settings for {@link FtpConnectionNodeModel}.
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
public class FtpAuthenticationSettingsModel {

    private static final String KEY_AUTH_TYPE = "authType";

    private static final String KEY_CREDENTIAL = "credential";

    private static final String KEY_USER = "user";

    private static final String KEY_PASSWORD = "password";

    private static final String SECRET_KEY = "ekerjvjhmzle,ptktysq";

    private AuthType m_authType;

    private final SettingsModelString m_credential;

    private final SettingsModelString m_user;

    private final SettingsModelPassword m_password;

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
        ANONYMOUS("ANONYMOUS", "Anonymous", "Anonymous authentication");

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
            } else if (settingsValue.equals(ANONYMOUS.getSettingsValue())) {
                return ANONYMOUS;
            } else {
                throw new IllegalArgumentException("Invalid authentication type " + settingsValue);
            }
        }
    }

    /**
     * Default constructor.
     */
    public FtpAuthenticationSettingsModel() {
        //authentication
        m_authType = AuthType.USER_PWD;
        m_credential = new SettingsModelString(KEY_CREDENTIAL, "");
        m_user = new SettingsModelString(KEY_USER, System.getProperty("user.name"));
        m_password = new SettingsModelPassword(KEY_PASSWORD, SECRET_KEY, "");

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
     * @return a (deep) clone of this node settings object.
     */
    public FtpAuthenticationSettingsModel createClone() {
        final NodeSettings tempSettings = new NodeSettings("ignored");
        saveSettingsForModel(tempSettings);

        final FtpAuthenticationSettingsModel toReturn = new FtpAuthenticationSettingsModel();
        try {
            toReturn.loadSettingsForModel(tempSettings);
        } catch (InvalidSettingsException ex) { // NOSONAR can never happen
            // won't happen
        }
        return toReturn;
    }

    /**
     * Loads settings from the given {@link NodeSettingsRO} (to be called by the dialog).
     *
     * @param settings
     * @throws InvalidSettingsException
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings) throws InvalidSettingsException {
        load(settings);
        // m_keyFile must be loaded by dialog component
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
    }

    private void load(final NodeSettingsRO settings) throws InvalidSettingsException {
        try {
            m_authType = AuthType.fromSettingsValue(settings.getString(KEY_AUTH_TYPE));
        } catch (IllegalArgumentException e) {
            throw new InvalidSettingsException(
                    settings.getString(KEY_AUTH_TYPE) + " is not a valid authentication type", e);
        }

        m_credential.loadSettingsFrom(settings);
        m_user.loadSettingsFrom(settings);
        m_password.loadSettingsFrom(settings);

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

        FtpAuthenticationSettingsModel temp = new FtpAuthenticationSettingsModel();
        temp.loadSettingsForModel(settings);
        temp.validate();
    }

    /**
     * Validates the current settings.
     *
     * @throws InvalidSettingsException
     */
    public void validate() throws InvalidSettingsException {
        switch (getAuthType()) {
        case CREDENTIALS:
            validateCredential();
            break;
        case USER_PWD:
            validateUserPasswordSettings();
            break;
        case ANONYMOUS:
            break;
        default:
            break;
        }
    }

    /**
     * Saves the settings (to be called by node dialog).
     *
     * @param settings
     */
    public void saveSettingsForDialog(final NodeSettingsWO settings) {
        save(settings);
    }

    /**
     * Saves the settings (to be called by node model).
     *
     * @param settings
     */
    public void saveSettingsForModel(final NodeSettingsWO settings) {
        save(settings);
    }

    /**
     * Saves settings to the given {@link NodeSettingsWO}.
     *
     * @param settings
     */
    private void save(final NodeSettingsWO settings) {
        settings.addString(KEY_AUTH_TYPE, m_authType.getSettingsValue());
        m_credential.saveSettingsTo(settings);
        m_user.saveSettingsTo(settings);
        m_password.saveSettingsTo(settings);
    }

    private void validateUserPasswordSettings() throws InvalidSettingsException {
        if (isEmpty(m_user.getStringValue())) {
            throw new InvalidSettingsException("Please provide a valid user name");
        }
        // password can be empty
    }

    private void validateCredential() throws InvalidSettingsException {
        if (isEmpty(m_credential.getStringValue())) {
            throw new InvalidSettingsException("Please select a valid credential");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getAuthType().name();
    }

    /**
     * @return credential model.
     */
    public SettingsModelString getCredentialModel() {
        return m_credential;
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
        // nothing for now
    }
}
