/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 * History
 *   Nov 9, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.remote.connectioninformation.node;

import org.apache.commons.lang.StringUtils;
import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformation;
import org.knime.base.filehandling.remote.files.FTPRemoteFileHandler;
import org.knime.base.filehandling.remote.files.Protocol;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.node.workflow.ICredentials;
import org.knime.core.util.KnimeEncryption;

/**
 * Configuration for the node.
 *
 *
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 */
class ConnectionInformationConfiguration {

    private final Protocol m_protocol;

    private String m_user;

    private String m_host;

    private int m_port;

    private String m_authenticationmethod;

    private String m_passwordPlain;

    /** added in 3.4.1, see AP-7807. */
    private String m_passwordEncrypted;

    private String m_tokenEncrypted;

    private String m_keyfile;

    private boolean m_useknownhosts;

    private String m_knownhosts;

    private boolean m_useworkflowcredentials;

    private String m_workflowcredentials;

    private int m_timeout = 30000;

    private final FTPProxyConfiguration m_ftpProxy = new FTPProxyConfiguration();

    /**
     * Create uninitialized configuration to a certain protocol.
     *
     *
     * @param protocol The protocol of this connection information configuration
     */
    ConnectionInformationConfiguration(final Protocol protocol) {
        m_protocol = protocol;
    }

    /**
     * @return the user
     */
    String getUser() {
        return m_user;
    }

    /**
     * @param user the user to set
     */
    void setUser(final String user) {
        m_user = user;
    }

    /**
     * @return the host
     */
    String getHost() {
        return m_host;
    }

    /**
     * @param host the host to set
     */
    void setHost(final String host) {
        m_host = host;
    }

    /**
     * @return the port
     */
    int getPort() {
        return m_port;
    }

    /**
     * @param port the port to set
     */
    void setPort(final int port) {
        m_port = port;
    }

    /**
     * @return the authenticationmethod
     */
    String getAuthenticationmethod() {
        return m_authenticationmethod;
    }

    /**
     * @param authenticationmethod the authenticationmethod to set
     */
    void setAuthenticationmethod(final String authenticationmethod) {
        m_authenticationmethod = authenticationmethod;
    }

    /**
     * @return the password, i.e. the "plain" password when set or otherwise the encrypted password (but then decrypted)
     */
    String getPassword() {
        // the plain password is not set by the dialog but only set either via flow variable
        // or when loading an old workflow (< 3.4.1); then the plain password takes precedence
        return isPasswordPlainSet() ? m_passwordPlain : m_passwordEncrypted;
    }

    /**
     * @return <code>true</code> if a plain password is set, ie either by flow variable or when loading old workflows.
     */
    boolean isPasswordPlainSet() {
        return StringUtils.isNotEmpty(m_passwordPlain);
    }

    /**
     * @param passwordEncrypted the password to be encrypted
     */
    void setPasswordEncrypted(final String passwordEncrypted) {
        m_passwordEncrypted = passwordEncrypted;
    }

    /**
     * @return the encrypted token
     */
    String getToken() {
        return m_tokenEncrypted;
    }

    /**
     * @param tokenEncrypted the encrypted token
     */
    void setTokenEncrypted(final String tokenEncrypted) {
        m_tokenEncrypted = tokenEncrypted;
    }

    /**
     * @return the keyfile
     */
    String getKeyfile() {
        return m_keyfile;
    }

    /**
     * @param keyfile the keyfile to set
     */
    void setKeyfile(final String keyfile) {
        m_keyfile = keyfile;
    }

    /**
     * @return the useknownhosts
     */
    boolean getUseknownhosts() {
        return m_useknownhosts;
    }

    /**
     * Returns the timeout for the connection.
     *
     * @return the timeout in milliseconds
     */
    int getTimeout() {
        return m_timeout;
    }


    /**
     * @param useknownhosts the useknownhosts to set
     */
    void setUseknownhosts(final boolean useknownhosts) {
        m_useknownhosts = useknownhosts;
    }

    /**
     * @return the known hosts
     */
    String getKnownhosts() {
        return m_knownhosts;
    }

    /**
     * @param knownhosts the knownhosts to set
     */
    void setKnownhosts(final String knownhosts) {
        m_knownhosts = knownhosts;
    }

    /**
     * Sets the timeout for the connection.
     *
     * @param timeout the timeout in milliseconds
     */
    void setTimeout(final int timeout) {
        m_timeout = timeout;
    }

    /**
     *
     * @return the ftp-proxy configuration. Empty (never {@code null}!) if not configured.
     */
    FTPProxyConfiguration getFTPProxy() {
        return m_ftpProxy;
    }

    /**
     * @return the useworkflowcredentials
     */
    public boolean getUseworkflowcredentials() {
        return m_useworkflowcredentials;
    }

    /**
     * @param useworkflowcredentials the useworkflowcredentials to set
     */
    public void setUseworkflowcredentials(final boolean useworkflowcredentials) {
        m_useworkflowcredentials = useworkflowcredentials;
    }

    /**
     * @return the workflowcredentials
     */
    public String getWorkflowcredentials() {
        return m_workflowcredentials;
    }

    /**
     * @param workflowcredentials the workflowcredentials to set
     */
    public void setWorkflowcredentials(final String workflowcredentials) {
        m_workflowcredentials = workflowcredentials;
    }

    /**
     * Create a connection information object from this settings.
     *
     *
     * @param credentialsProvider Provider for the credentials
     * @return The connection information object
     */
    ConnectionInformation getConnectionInformation(final CredentialsProvider credentialsProvider) {
        // Create connection information object
        final ConnectionInformation connectionInformation = new ConnectionInformation();
        // Put settings into object
        connectionInformation.setProtocol(m_protocol.getName());
        connectionInformation.setHost(getHost());
        connectionInformation.setPort(getPort());
        final String authenticationMethod = getAuthenticationmethod();
        if (authenticationMethod.equals(AuthenticationMethod.TOKEN.getName())) {
            if (m_useworkflowcredentials) {
                try {
                    final ICredentials credentials = credentialsProvider.get(m_workflowcredentials);
                    connectionInformation.setUseToken(true);
                    connectionInformation.setToken(KnimeEncryption.encrypt(credentials.getPassword().toCharArray()));
                } catch (final Exception e) {
                    // Password encryption failed
                }
            } else {
                connectionInformation.setUseToken(true);
                connectionInformation.setToken(getToken());
            }
        } else if (!authenticationMethod.equals(AuthenticationMethod.NONE.getName())) {
            if (m_useworkflowcredentials) {
                // Use credentials
                final ICredentials credentials = credentialsProvider.get(m_workflowcredentials);
                connectionInformation.setUser(credentials.getLogin());
                try {
                    connectionInformation.setPassword(KnimeEncryption.encrypt(credentials.getPassword().toCharArray()));
                } catch (final Exception e) {
                    // Set no password
                }
            } else {
                // Use direct settings
                connectionInformation.setUser(getUser());
                connectionInformation.setPassword(getPassword());
            }
        }
        if (m_protocol.hasKeyfileSupport()
                && authenticationMethod.equals(AuthenticationMethod.KEYFILE.getName())) {
            connectionInformation.setKeyfile(getKeyfile());
        }
        if (m_protocol.hasKnownhostsSupport() && getUseknownhosts()) {
            connectionInformation.setKnownHosts(getKnownhosts());
        }
        connectionInformation.setTimeout(getTimeout());
        connectionInformation.setUseKerberos(AuthenticationMethod.KERBEROS.getName().equals(authenticationMethod));
        if (FTPRemoteFileHandler.PROTOCOL.equals(m_protocol) && m_ftpProxy.isUseFTPProxy()) {
            connectionInformation.setFTPProxy(m_ftpProxy.getConnectionInformation(credentialsProvider));
        }
        return connectionInformation;
    }

    /**
     * Save this configuration into the settings.
     *
     *
     * @param settings The <code>NodeSettings</code> to write to
     */
    void save(final NodeSettingsWO settings) {
        settings.addBoolean("useworkflowcredentials", m_useworkflowcredentials);
        settings.addString("workflowcredentials", m_workflowcredentials);
        settings.addString("user", m_user);
        settings.addString("host", m_host);
        settings.addInt("port", m_port);
        settings.addString("authenticationmethod", m_authenticationmethod);
        // m_passwordPlain is usually empty, except when loaded from
        // an old workflow (<3.4.1) or when controlled via flow variable.
        settings.addString("password", m_passwordPlain);
        settings.addPassword("xpassword", ">$:g~l63t(uc1[y#[u", m_passwordEncrypted); // added in 3.4.1
        // Only save if the protocol support tokens
        if (m_protocol.hasTokenSupport()) {
            settings.addPassword("xtoken", ">$:g~l63t(uc1[y#[u", m_tokenEncrypted); // added in 4.1
        }
        // Only save if the protocol supports keyfiles
        if (m_protocol.hasKeyfileSupport()) {
            settings.addString("keyfile", m_keyfile);
        }
        // Only save if the protocol supports known hosts
        if (m_protocol.hasKnownhostsSupport()) {
            settings.addBoolean("useknownhosts", m_useknownhosts);
            settings.addString("knownhosts", m_knownhosts);
        }
        settings.addInt("timeout", m_timeout);
        if (FTPRemoteFileHandler.PROTOCOL.equals(m_protocol)) {
            m_ftpProxy.save(settings);
        }
    }

    /**
     * Load this configuration from the settings.
     *
     *
     * @param settings The <code>NodeSettings</code> to read from
     */
    void load(final NodeSettingsRO settings) {
        m_useworkflowcredentials = settings.getBoolean("useworkflowcredentials", false);
        m_workflowcredentials = settings.getString("workflowcredentials", "");
        m_user = settings.getString("user", "");
        m_host = settings.getString("host", "");
        m_port = settings.getInt("port", m_protocol.getPort());
        m_authenticationmethod = settings.getString("authenticationmethod", AuthenticationMethod.PASSWORD.getName());
        m_passwordPlain = settings.getString("password", "");
        if (StringUtils.isNotEmpty(m_passwordPlain)) {
            m_passwordEncrypted = m_passwordPlain;
            m_passwordPlain = null;
        } else {
            // added in 3.4.1
            m_passwordEncrypted = settings.getPassword("xpassword", ">$:g~l63t(uc1[y#[u", null);
        }
        // Only load if the protocol supports tokens
        if (m_protocol.hasTokenSupport()) {
            m_tokenEncrypted = settings.getPassword("xtoken", ">$:g~l63t(uc1[y#[u", ""); // new option in 4.1
        }
        // Only load if the protocol supports keyfiles
        if (m_protocol.hasKeyfileSupport()) {
            m_keyfile = settings.getString("keyfile", "");
        }
        // Only load if the protocol supports known hosts
        if (m_protocol.hasKnownhostsSupport()) {
            m_useknownhosts = settings.getBoolean("useknownhosts", false);
            m_knownhosts = settings.getString("knownhosts", "");
        }
        m_timeout = settings.getInt("timeout", 30000); // new option in 2.10
        if (FTPRemoteFileHandler.PROTOCOL.equals(m_protocol)) {
            m_ftpProxy.load(settings);
        }
    }

    /**
     * Load and validate this configuration from the settings.
     *
     *
     * @param settings The <code>NodeSettings</code> to read from
     * @throws InvalidSettingsException If one of the settings is not valid
     */
    void loadAndValidate(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_useworkflowcredentials = settings.getBoolean("useworkflowcredentials");
        m_workflowcredentials = settings.getString("workflowcredentials");
        m_user = settings.getString("user");
        m_host = settings.getString("host");
        validate(m_host, "host");
        m_port = settings.getInt("port");
        m_authenticationmethod = settings.getString("authenticationmethod");
        validate(m_authenticationmethod, "authenticationmethod");
        m_passwordPlain = settings.getString("password");
        if (StringUtils.isNotEmpty(m_passwordPlain)) {
            m_passwordEncrypted = m_passwordPlain;
            m_passwordPlain = null;
        } else {
            // added in 3.4.1
            m_passwordEncrypted = settings.getPassword("xpassword", ">$:g~l63t(uc1[y#[u", null);
        }

        // Only validate if the authentication method is set to password
        if (m_authenticationmethod.equals(AuthenticationMethod.PASSWORD.getName())) {
            if (m_useworkflowcredentials) {
                validate(m_workflowcredentials, "workflowcredentials");
            } else {
                validate(m_user, "user");
                validate(getPassword(), "password");
            }
        }

        // Only validate if the authentication method is set to token
        if (m_authenticationmethod.equals(AuthenticationMethod.TOKEN.getName())) {
            m_tokenEncrypted = settings.getPassword("xtoken", ">$:g~l63t(uc1[y#[u", ""); // new option in 4.1
            if (m_useworkflowcredentials) {
                validate(m_workflowcredentials, "workflowcredentials");
            } else {
                validate(getToken(), "token");
            }
        }

        // Only load if the protocol supports keyfiles
        if (m_protocol.hasKeyfileSupport()) {
            m_keyfile = settings.getString("keyfile");
            // Only validate if the authentication method is set to keyfile
            if (m_authenticationmethod.equals(AuthenticationMethod.KEYFILE.getName())) {
                if (m_useworkflowcredentials) {
                    validate(m_workflowcredentials, "workflowcredentials");
                } else {
                    validate(m_user, "user");
                }
                validate(m_keyfile, "keyfile");
            }
        }
        // Only load if the protocol supports known hosts
        if (m_protocol.hasKnownhostsSupport()) {
            m_useknownhosts = settings.getBoolean("useknownhosts");
            m_knownhosts = settings.getString("knownhosts");
            if (m_useknownhosts) {
                validate(m_knownhosts, "knownhosts");
            }
        }
        m_timeout = settings.getInt("timeout", 30000); // new option in 2.10
        if (FTPRemoteFileHandler.PROTOCOL.equals(m_protocol)) {
            m_ftpProxy.loadAndValidate(settings);
        }
    }

    /**
     * Checks if the string is not null or empty.
     *
     *
     * @param string The string to check
     * @param settingName The name of the setting
     * @throws InvalidSettingsException If the string is null or empty
     */
    private void validate(final String string, final String settingName) throws InvalidSettingsException {
        if (StringUtils.isEmpty(string)) {
            throw new InvalidSettingsException(settingName + " missing");
        }
    }

    /**
     * Class to store a ftp-proxy configuration. If no proxy is configured (default configuration)
     * {@link #isUseFTPProxy()} returns {@code false}. <b>Do not call any getter methods if {@link #isUseFTPProxy()}
     * returns {@code false}!</b>
     *
     * @author ferry.abt
     */
    class FTPProxyConfiguration {

        private boolean m_useFTPProxy = false;

        private String m_ftpProxyHost = "";

        private int m_ftpProxyPort = 21;

        private boolean m_userAuth = false;

        private boolean m_useWorkflowCredentials = false;

        private String m_ftpProxyWorkflowCredentials = "";

        private String m_ftpProxyUser = "";

        private String m_password = "";

        /**
         * @return true if an ftp-proxy should be used
         */
        boolean isUseFTPProxy() {
            return m_useFTPProxy;
        }

        /**
         * @return an ftp-proxy configuration in the form of a {@link ConnectionInformation}-object to be passed via a
         *         {@code ConnectionInformationPort}.
         */
        ConnectionInformation getConnectionInformation(final CredentialsProvider credentialsProvider) {
            final ConnectionInformation connectionInformation = new ConnectionInformation();
            connectionInformation.setHost(m_ftpProxyHost);
            connectionInformation.setPort(m_ftpProxyPort);
            if (m_userAuth) {
                if (m_useWorkflowCredentials) {
                    // Use credentials
                    final ICredentials credentials = credentialsProvider.get(m_ftpProxyWorkflowCredentials);
                    connectionInformation.setUser(credentials.getLogin());
                    try {
                        connectionInformation
                            .setPassword(KnimeEncryption.encrypt(credentials.getPassword().toCharArray()));
                    } catch (final Exception e) {
                        // Set no password
                    }
                } else {
                    // Use direct settings
                    connectionInformation.setUser(m_ftpProxyUser);
                    connectionInformation.setPassword(m_password);
                }
            }
            return connectionInformation;
        }

        /**
         * @param useFTPProxy should an ftp-proxy be used (default: {@code false})
         */
        void setUseFTPProxy(final boolean useFTPProxy) {
            this.m_useFTPProxy = useFTPProxy;
        }

        /**
         * @return the host of the ftp-proxy
         */
        String getFtpProxyHost() {
            return m_ftpProxyHost;
        }

        /**
         * @param host of the ftp-proxy (default: {@code ""})
         */
        void setFtpProxyHost(final String host) {
            this.m_ftpProxyHost = host;
        }

        /**
         * @return the port of the ftp-proxy
         */
        int getFtpProxyPort() {
            return m_ftpProxyPort;
        }

        /**
         * @param port of the ftp-proxy (default: {@code 21})
         */
        void setFtpProxyPort(final int port) {
            this.m_ftpProxyPort = port;
        }

        /**
         * @return whether an authentication for the ftp-proxy is configured
         */
        boolean isUserAuth() {
            return m_userAuth;
        }

        /**
         * @param userAuth whether an authentication is required for the ftp-proxy (default: {@code false})
         */
        void setUserAuth(final boolean userAuth) {
            this.m_userAuth = userAuth;
        }

        /**
         * @return whether workflow credentials should be used for the ftp-proxy authentication
         */
        boolean isUseWorkflowCredentials() {
            return m_useWorkflowCredentials;
        }

        /**
         * @param useWorkflowCredentials whether workflow credentials should be used for the ftp-proxy authentication
         *            (default: {@code false})
         */
        void setUseWorkflowCredentials(final boolean useWorkflowCredentials) {
            this.m_useWorkflowCredentials = useWorkflowCredentials;
        }

        /**
         * @return the workflow credentials
         */
        String getFtpProxyWorkflowCredentials() {
            return m_ftpProxyWorkflowCredentials;
        }

        /**
         * @param workflowCredentials to be used for the ftp-proxy (default: {@code ""})
         */
        void setFtpProxyWorkflowCredentials(final String workflowCredentials) {
            this.m_ftpProxyWorkflowCredentials = workflowCredentials;
        }

        /**
         * @return the user for the authentication for the ftp-proxy
         */
        String getFtpProxyUser() {
            return m_ftpProxyUser;
        }

        /**
         * @param user for the authentication for the ftp-proxy (default: {@code ""})
         */
        void setFtpProxyUser(final String user) {
            this.m_ftpProxyUser = user;
        }

        /**
         * @return the password for the authentication of the ftp-proxy
         */
        String getPassword() {
            return m_password;
        }

        /**
         * @param password for the authentication of the ftp-proxy (default: {@code ""})
         */
        void setPassword(final String password) {
            this.m_password = password;
        }

        private static final String KEY_PROXY_SETTINGS = "ftp-proxy";

        private static final String KEY_USE_PROXY = "useFTPProxy";

        private static final String KEY_HOST = "ftpProxyHost";

        private static final String KEY_PORT = "ftpProxyPort";

        private static final String KEY_USE_USER_AUTH = "ftpProxyUserAuth";

        private static final String KEY_USE_WF_CRED = "ftpProxyUseWFCred";

        private static final String KEY_WF_CRED = "ftpProxyWFCred";

        private static final String KEY_USER = "ftpProxyUser";

        private static final String KEY_PASSWORD = "ftpProxyPassword";

        /**
         *
         * @param settings -object to store the settings into. Creates sub-settings with the key "ftp-proxy".
         */
        void save(final NodeSettingsWO settings) {
            final NodeSettingsWO proxySettings = settings.addNodeSettings(KEY_PROXY_SETTINGS);
            proxySettings.addBoolean(KEY_USE_PROXY, m_useFTPProxy);
            proxySettings.addString(KEY_HOST, m_ftpProxyHost);
            proxySettings.addInt(KEY_PORT, m_ftpProxyPort);
            proxySettings.addBoolean(KEY_USE_USER_AUTH, m_userAuth);
            proxySettings.addBoolean(KEY_USE_WF_CRED, m_useWorkflowCredentials);
            proxySettings.addString(KEY_WF_CRED, m_ftpProxyWorkflowCredentials);
            proxySettings.addString(KEY_USER, m_ftpProxyUser);
            proxySettings.addPassword(KEY_PASSWORD, ">$:g~l63t(uc1[y#[u", m_password);
        }

        /**
         *
         * @param settings -object to read the settings from the contained sub-settings "ftp-proxy".
         */
        void load(final NodeSettingsRO settings) {
            NodeSettingsRO proxySettings;
            try {
                proxySettings = settings.getNodeSettings(KEY_PROXY_SETTINGS);
            } catch (InvalidSettingsException e) {
                proxySettings = new NodeSettings(KEY_PROXY_SETTINGS);
            }
            m_useFTPProxy = proxySettings.getBoolean(KEY_USE_PROXY, false);
            m_ftpProxyHost = proxySettings.getString(KEY_HOST, "");
            m_ftpProxyPort = proxySettings.getInt(KEY_PORT, 21);
            m_userAuth = proxySettings.getBoolean(KEY_USE_USER_AUTH, false);
            m_useWorkflowCredentials = proxySettings.getBoolean(KEY_USE_WF_CRED, false);
            m_ftpProxyWorkflowCredentials = proxySettings.getString(KEY_WF_CRED, "");
            m_ftpProxyUser = proxySettings.getString(KEY_USER, "");
            m_password = proxySettings.getPassword(KEY_PASSWORD, ">$:g~l63t(uc1[y#[u", "");
        }

        /**
         * @see {@link #load(NodeSettingsRO)}
         * @see {@link ConnectionInformationConfiguration#loadAndValidate(NodeSettingsRO)}
         * @param settings -object to read the settings from the contained sub-settings "ftp-proxy".
         * @throws InvalidSettingsException
         */
        void loadAndValidate(final NodeSettingsRO settings) throws InvalidSettingsException {
            load(settings);
            if (m_useFTPProxy) {
                validate(m_ftpProxyHost, "ftp-proxy host");
                if (m_ftpProxyPort < 0 || m_ftpProxyPort > 65535) {
                    throw new InvalidSettingsException("Invalid ftp-proxy port number: " + m_ftpProxyPort);
                }
                if (m_useWorkflowCredentials) {
                    validate(m_ftpProxyWorkflowCredentials, "ftp-proxy workflowcredentials");
                }
            }
        }
    }
}
