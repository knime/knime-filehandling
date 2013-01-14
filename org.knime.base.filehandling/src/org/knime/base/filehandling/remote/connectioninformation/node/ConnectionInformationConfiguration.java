/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 * 
 * History
 *   Nov 9, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.remote.connectioninformation.node;

import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformation;
import org.knime.core.node.InvalidSettingsException;
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

    private Protocol m_protocol;

    private String m_user;

    private String m_host;

    private int m_port;

    private String m_authenticationmethod;

    private String m_password;

    private String m_keyfile;

    private boolean m_useknownhosts;

    private String m_knownhosts;

    private boolean m_useworkflowcredentials;

    private String m_workflowcredentials;

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
     * @return the password
     */
    String getPassword() {
        return m_password;
    }

    /**
     * @param password the password to set
     */
    void setPassword(final String password) {
        m_password = password;
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
    ConnectionInformation getConnectionInformation(
            final CredentialsProvider credentialsProvider) {
        // Create connection information object
        ConnectionInformation connectionInformation =
                new ConnectionInformation();
        // Put settings into object
        connectionInformation.setProtocol(m_protocol.getName());
        connectionInformation.setHost(getHost());
        connectionInformation.setPort(getPort());
        if (!getAuthenticationmethod().equals(
                AuthenticationMethod.NONE.getName())) {
            if (m_useworkflowcredentials) {
                // Use credentials
                ICredentials credentials =
                        credentialsProvider.get(m_workflowcredentials);
                connectionInformation.setUser(credentials.getLogin());
                try {
                    connectionInformation.setPassword(KnimeEncryption
                            .encrypt(credentials.getPassword().toCharArray()));
                } catch (Exception e) {
                    // Set no password
                }
            } else {
                // Use direct settings
                connectionInformation.setUser(getUser());
                connectionInformation.setPassword(getPassword());
            }
        }
        if (m_protocol.hasKeyfileSupport()
                && getAuthenticationmethod().equals(
                        AuthenticationMethod.KEYFILE.getName())) {
            connectionInformation.setKeyfile(getKeyfile());
        }
        if (m_protocol.hasKnownhostsSupport() && getUseknownhosts()) {
            connectionInformation.setKnownHosts(getKnownhosts());
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
        settings.addString("password", m_password);
        // Only save if the protocol supports keyfiles
        if (m_protocol.hasKeyfileSupport()) {
            settings.addString("keyfile", m_keyfile);
        }
        // Only save if the protocol supports known hosts
        if (m_protocol.hasKnownhostsSupport()) {
            settings.addBoolean("useknownhosts", m_useknownhosts);
            settings.addString("knownhosts", m_knownhosts);
        }
    }

    /**
     * Load this configuration from the settings.
     * 
     * 
     * @param settings The <code>NodeSettings</code> to read from
     */
    void load(final NodeSettingsRO settings) {
        m_useworkflowcredentials =
                settings.getBoolean("useworkflowcredentials", false);
        m_workflowcredentials = settings.getString("workflowcredentials", "");
        m_user = settings.getString("user", "");
        m_host = settings.getString("host", "");
        m_port = settings.getInt("port", m_protocol.getPort());
        m_authenticationmethod =
                settings.getString("authenticationmethod",
                        AuthenticationMethod.PASSWORD.getName());
        m_password = settings.getString("password", "");
        // Only load if the protocol supports keyfiles
        if (m_protocol.hasKeyfileSupport()) {
            m_keyfile = settings.getString("keyfile", "");
        }
        // Only load if the protocol supports known hosts
        if (m_protocol.hasKnownhostsSupport()) {
            m_useknownhosts = settings.getBoolean("useknownhosts", false);
            m_knownhosts = settings.getString("knownhosts", "");
        }
    }

    /**
     * Load and validate this configuration from the settings.
     * 
     * 
     * @param settings The <code>NodeSettings</code> to read from
     * @throws InvalidSettingsException If one of the settings is not valid
     */
    void loadAndValidate(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_useworkflowcredentials =
                settings.getBoolean("useworkflowcredentials");
        m_workflowcredentials = settings.getString("workflowcredentials");
        m_user = settings.getString("user");
        m_host = settings.getString("host");
        validate(m_host, "host");
        m_port = settings.getInt("port");
        m_authenticationmethod = settings.getString("authenticationmethod");
        validate(m_authenticationmethod, "authenticationmethod");
        m_password = settings.getString("password");
        // Only validate if the authentication method is set to password
        if (m_authenticationmethod.equals(AuthenticationMethod.PASSWORD
                .getName())) {
            if (m_useworkflowcredentials) {
                validate(m_workflowcredentials, "workflowcredentials");
            } else {
                validate(m_user, "user");
                validate(m_password, "password");
            }
        }
        // Only load if the protocol supports keyfiles
        if (m_protocol.hasKeyfileSupport()) {
            m_keyfile = settings.getString("keyfile");
            // Only validate if the authentication method is set to keyfile
            if (m_authenticationmethod.equals(AuthenticationMethod.KEYFILE
                    .getName())) {
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
    }

    /**
     * Checks if the string is not null or empty.
     * 
     * 
     * @param string The string to check
     * @param settingName The name of the setting
     * @throws InvalidSettingsException If the string is null or empty
     */
    private void validate(final String string, final String settingName)
            throws InvalidSettingsException {
        if (string == null || string.length() == 0) {
            throw new InvalidSettingsException(settingName + " missing");
        }
    }

}
