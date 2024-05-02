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
 *   2020-11-18 (Bjoern Lohrmann): created
 */
package org.knime.ext.http.filehandling.node;

import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelAuthentication;
import org.knime.core.node.defaultnodesettings.SettingsModelAuthentication.AuthenticationType;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelPassword;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ButtonGroupEnumInterface;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;

/**
 * Authentication settings for the HTTP(S) Connector node.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
class HttpAuthenticationSettings {

    private static final String KEY_AUTH_TYPE = "authType";

    private static final String KEY_USE_CREDENTIALS = "useCredentials";

    private static final String KEY_CREDENTIALS_NAME = "credentialsName";

    private static final String KEY_USER = "user";

    private static final String KEY_PASSWORD = "password";

    private static final String SECRET_KEY = "ekerjvjhmzle,ptktysq";

    private AuthType m_authType;

    private final SettingsModelString m_basicUser;

    private final SettingsModelPassword m_basicPassword;

    private final SettingsModelBoolean m_basicUseCredentials;

    private final SettingsModelString m_basicCredentialsName;

    /**
     * Authentication type enumeration.
     *
     * @author Bjoern Lohrmann, KNIME GmbHDEFAULT_TIMEOUT
     */
    enum AuthType implements ButtonGroupEnumInterface {
        /**
         * No authentication.
         */
        NONE("none", "None", "No authentication"),
        /**
         * HTTP Basic authentication.
         */
        BASIC("basic", "Basic", "HTTP Basic authentication (Username/password)");

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

        @Override
        public String getText() {
            return m_text;
        }

        @Override
        public String getActionCommand() {
            return name();
        }

        @Override
        public String getToolTip() {
            return m_toolTip;
        }

        @Override
        public boolean isDefault() {
            return equals(BASIC);
        }

        /**
         * @param actionCommand
         *            the action command
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
            for (AuthType type : values()) {
                if (type.getSettingsValue().equals(settingsValue)) {
                    return type;
                }
            }

            throw new IllegalArgumentException("Invalid authentication type " + settingsValue);
        }
    }

    /**
     * Default constructor.
     */
    HttpAuthenticationSettings() {
        // authentication
        m_authType = AuthType.NONE;
        m_basicUser = new SettingsModelString(KEY_USER, "");
        m_basicPassword = new SettingsModelPassword(KEY_PASSWORD, SECRET_KEY, "");
        m_basicUseCredentials = new SettingsModelBoolean(KEY_USE_CREDENTIALS, false);
        m_basicCredentialsName = new SettingsModelString(KEY_CREDENTIALS_NAME, "");

        m_basicUseCredentials.addChangeListener(e -> updateEnabledness());

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
        m_basicUser.setEnabled(m_authType == AuthType.BASIC);
        m_basicPassword.setEnabled(m_authType == AuthType.BASIC);
        m_basicUseCredentials.setEnabled(m_authType == AuthType.BASIC);
        m_basicCredentialsName.setEnabled(m_authType == AuthType.BASIC && useBasicCredentials());
    }

    /**
     * @return a (deep) clone of this node settings object.
     */
    public HttpAuthenticationSettings createClone() {
        final NodeSettings tempSettings = new NodeSettings("ignored");
        saveSettingsForModel(tempSettings);

        final HttpAuthenticationSettings toReturn = new HttpAuthenticationSettings();
        try {
            toReturn.loadSettingsForModel(tempSettings);
        } catch (InvalidSettingsException ex) { // NOSONAR can never happen
            // won't happen
        }
        return toReturn;
    }

    /**
     * Loads settings from the given {@link NodeSettingsRO} (to be called by the
     * dialog).
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

        m_basicUseCredentials.loadSettingsFrom(settings);
        m_basicCredentialsName.loadSettingsFrom(settings);
        m_basicUser.loadSettingsFrom(settings);
        m_basicPassword.loadSettingsFrom(settings);

        updateEnabledness();
    }

    /**
     * Validates the settings in the given {@link NodeSettingsRO}.
     *
     * @param settings
     * @throws InvalidSettingsException
     */
    public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_basicUseCredentials.validateSettings(settings);
        m_basicCredentialsName.validateSettings(settings);
        m_basicUser.validateSettings(settings);
        m_basicPassword.validateSettings(settings);

        HttpAuthenticationSettings temp = new HttpAuthenticationSettings();
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
        case BASIC:
            validateUserPasswordSettings();
            break;
        case NONE:
            break;
        default:
            break;
        }
        clearDeselectedAuthTypes();
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
        m_basicUseCredentials.saveSettingsTo(settings);
        m_basicCredentialsName.saveSettingsTo(settings);
        m_basicUser.saveSettingsTo(settings);
        m_basicPassword.saveSettingsTo(settings);
    }

    private void validateUserPasswordSettings() throws InvalidSettingsException {
        if (useBasicCredentials()) {
            if (StringUtils.isBlank(getBasicCredentialsName())) {
                throw new InvalidSettingsException(
                        "Please choose a credentials flow variable for Basic authentication.");
            }
            m_basicUser.setStringValue("");
            m_basicPassword.setStringValue("");
        } else {
            if (StringUtils.isBlank(m_basicUser.getStringValue())
                    || StringUtils.isBlank(m_basicPassword.getStringValue())) {
                throw new InvalidSettingsException(
                        "Please provide a valid username and password for Basic authentication.");
            }
            m_basicCredentialsName.setStringValue("");
        }
    }

    @Override
    public String toString() {
        return getAuthType().name();
    }

    /**
     * @return settings model for the name of the credentials flow variable to use
     *         for HTTP Basic authentication.
     */
    public SettingsModelString getBasicCredentialsNameModel() {
        return m_basicCredentialsName;
    }

    /**
     * @return the name of the credentials flow variable to use for HTTP Basic
     *         authentication.
     */
    public String getBasicCredentialsName() {
        return m_basicCredentialsName.getStringValue();
    }

    /**
     * @return settings model for whether to use a credentials flow variable for
     *         HTTP Basic authentication.
     */
    public SettingsModelBoolean getBasicUseCredentialsModel() {
        return m_basicUseCredentials;
    }

    /**
     * @return whether to use a credentials flow variable for HTTP Basic
     *         authentication.
     */
    public boolean useBasicCredentials() {
        return m_basicUseCredentials.getBooleanValue();
    }

    /**
     * @return settings model for the user name to use for HTTP Basic
     *         authentication.
     */
    public SettingsModelString getBasicUserModel() {
        return m_basicUser;
    }

    /**
     * @return the user name to use for HTTP Basic authentication.
     */
    public String getBasicUser() {
        return m_basicUser.getStringValue();
    }

    /**
     * @return password settings model.
     */
    public SettingsModelPassword getBasicPasswordModel() {
        return m_basicPassword;
    }

    /**
     * @return the password to use for HTTP Basic authentication.
     */
    public String getBasicPassword() {
        return m_basicPassword.getStringValue();
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

    /**
     * Clears the data of the 'other' authentication types. Added as part of
     * AP-21749 to only store authentication data when needed. To specifically clear
     * the settings model, call {@link SettingsModelAuthentication#clear()}.
     */
    private void clearDeselectedAuthTypes() {
        final AuthType selectedType = getAuthType();
        for (AuthType otherType : AuthType.values()) {
            if (otherType == selectedType) {
                continue;
            }
            switch (otherType) {
            case NONE:
                    // nothing to clear
                    break;
            case BASIC:
                    m_basicUser.setStringValue("");
                    m_basicPassword.setStringValue("");
                    m_basicCredentialsName.setStringValue("");
                    break;
            default:
                    break;
            }
        }
    }
}
