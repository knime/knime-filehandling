/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 * @author Patrick Winter, KNIME.com, Zurich, Switzerland
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

    private String m_keyfile;

    private boolean m_useknownhosts;

    private String m_knownhosts;

    private boolean m_useworkflowcredentials;

    private String m_workflowcredentials;

    private int m_timeout = 30000;

    private FTPProxyConfiguration m_ftpProxy = new FTPProxyConfiguration();

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

    FTPProxyConfiguration getFTPProxy(){
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
        if (!authenticationMethod.equals(AuthenticationMethod.NONE.getName())) {
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
        if(FTPRemoteFileHandler.PROTOCOL.equals(m_protocol) && m_ftpProxy.isUseFTPProxy()){
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
        if(FTPRemoteFileHandler.PROTOCOL.equals(m_protocol)){
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
        if(FTPRemoteFileHandler.PROTOCOL.equals(m_protocol)){
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
        if(FTPRemoteFileHandler.PROTOCOL.equals(m_protocol)){
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

    class FTPProxyConfiguration{

        private boolean m_useFTPProxy;
        private String m_ftpProxyHost;
        private int m_ftpProxyPort;
        private boolean m_userAuth;
        private boolean m_useWorkflowCredentials;
        private String m_ftpProxyWorkflowCredentials;
        private String m_ftpProxyUser;
        private String m_password;

        /**
         * @return the m_useFTPProxy
         */
        boolean isUseFTPProxy() {
            return m_useFTPProxy;
        }

        /**
         * @return
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
                        connectionInformation.setPassword(KnimeEncryption.encrypt(credentials.getPassword().toCharArray()));
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
         * @param m_useFTPProxy the m_useFTPProxy to set
         */
        void setUseFTPProxy(final boolean useFTPProxy) {
            this.m_useFTPProxy = useFTPProxy;
        }

        /**
         * @return the m_ftpProxyHost
         */
        String getFtpProxyHost() {
            return m_ftpProxyHost;
        }

        /**
         * @param m_ftpProxyHost the m_ftpProxyHost to set
         */
        void setFtpProxyHost(final String ftpProxyHost) {
            this.m_ftpProxyHost = ftpProxyHost;
        }

        /**
         * @return the m_ftpProxyPort
         */
        int getFtpProxyPort() {
            return m_ftpProxyPort;
        }

        /**
         * @param m_ftpProxyPort the m_ftpProxyPort to set
         */
        void setFtpProxyPort(final int ftpProxyPort) {
            this.m_ftpProxyPort = ftpProxyPort;
        }

        /**
         * @return the m_userAuth
         */
        boolean isUserAuth() {
            return m_userAuth;
        }

        /**
         * @param m_userAuth the m_userAuth to set
         */
        void setUserAuth(final boolean userAuth) {
            this.m_userAuth = userAuth;
        }

        /**
         * @return the m_useWorkflowCredentials
         */
        boolean isUseWorkflowCredentials() {
            return m_useWorkflowCredentials;
        }

        /**
         * @param m_useWorkflowCredentials the m_useWorkflowCredentials to set
         */
        void setUseWorkflowCredentials(final boolean useWorkflowCredentials) {
            this.m_useWorkflowCredentials = useWorkflowCredentials;
        }

        /**
         * @return the m_ftpProxyWorkflowCredentials
         */
        String getFtpProxyWorkflowCredentials() {
            return m_ftpProxyWorkflowCredentials;
        }

        /**
         * @param m_ftpProxyWorkflowCredentials the m_ftpProxyWorkflowCredentials to set
         */
        void setFtpProxyWorkflowCredentials(final String ftpProxyWorkflowCredentials) {
            this.m_ftpProxyWorkflowCredentials = ftpProxyWorkflowCredentials;
        }

        /**
         * @return the m_ftpProxyUser
         */
        String getFtpProxyUser() {
            return m_ftpProxyUser;
        }

        /**
         * @param m_ftpProxyUser the m_ftpProxyUser to set
         */
        void setFtpProxyUser(final String ftpProxyUser) {
            this.m_ftpProxyUser = ftpProxyUser;
        }

        /**
         * @return the m_password
         */
        String getPassword() {
            return m_password;
        }

        /**
         * @param m_password the m_password to set
         */
        void setPassword(final String password) {
            this.m_password = password;
        }

        void save(final NodeSettingsWO settings) {
            final NodeSettingsWO proxySettings = settings.addNodeSettings("proxy");
            proxySettings.addBoolean("useFTPProxy", m_useFTPProxy);
            proxySettings.addString("ftpHost", m_ftpProxyHost);
            proxySettings.addInt("ftpPort", m_ftpProxyPort);
            proxySettings.addBoolean("ftpUserAuth", m_userAuth);
            proxySettings.addBoolean("ftpUseWFCred", m_useWorkflowCredentials);
            proxySettings.addString("ftpWorkflowCredentials", m_ftpProxyWorkflowCredentials);
            proxySettings.addString("ftpUser", m_ftpProxyUser);
            proxySettings.addPassword("ftpPassword", ">$:g~l63t(uc1[y#[u", m_password);
        }

        void load(final NodeSettingsRO settings) {
            NodeSettingsRO proxySettings;
            try {
                proxySettings = settings.getNodeSettings("proxy");
            } catch (InvalidSettingsException e) {
                proxySettings = new NodeSettings("proxy");
            }
            m_useFTPProxy = proxySettings.getBoolean("useFTPProxy", false);
            m_ftpProxyHost = proxySettings.getString("ftpHost", "");
            m_ftpProxyPort = proxySettings.getInt("ftpPort", 21);
            m_userAuth = proxySettings.getBoolean("ftpUserAuth", false);
            m_useWorkflowCredentials = proxySettings.getBoolean("ftpUseWFCred", false);
            m_ftpProxyUser = proxySettings.getString("ftpUser", "");
            m_password = proxySettings.getPassword("ftpPassword", ">$:g~l63t(uc1[y#[u", "");
        }

        void loadAndValidate(final NodeSettingsRO settings){
            //TODO different than load?
            load(settings);
        }
    }
}
