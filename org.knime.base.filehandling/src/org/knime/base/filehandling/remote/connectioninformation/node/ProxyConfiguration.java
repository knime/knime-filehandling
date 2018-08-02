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
 *   Aug 2, 2018 (ferry): created
 */
package org.knime.base.filehandling.remote.connectioninformation.node;

import org.apache.commons.lang.StringUtils;
import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformation;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.node.workflow.ICredentials;
import org.knime.core.util.KnimeEncryption;

/**
 * Class to store a proxy configuration. If no proxy is configured (default configuration) {@link #isUseProxy()}
 * returns {@code false}. <b>Do not call any getter methods if {@link #isUseProxy()} returns {@code false}!</b>
 *
 * @author Ferry Abt, KNIME GmbH, Konstanz
 */
class ProxyConfiguration {

    private boolean m_useProxy = false;

    private String m_proxyHost = "";

    private int m_proxyPort;

    private boolean m_userAuth = false;

    private boolean m_useWorkflowCredentials = false;

    private String m_proxyWorkflowCredentials = "";

    private String m_proxyUser = "";

    private String m_password = "";

    ProxyConfiguration(final int defaultPort) {
        m_proxyPort = defaultPort;
    }

    ProxyConfiguration() {
        this(3128);
    }

    /**
     * @return true if a proxy should be used
     */
    boolean isUseProxy() {
        return m_useProxy;
    }

    /**
     * @param useProxy should a proxy be used (default: {@code false})
     */
    void setUseProxy(final boolean useProxy) {
        this.m_useProxy = useProxy;
    }

    /**
     * @return the host of the proxy
     */
    String getProxyHost() {
        return m_proxyHost;
    }

    /**
     * @param host of the proxy (default: {@code ""})
     */
    void setProxyHost(final String host) {
        this.m_proxyHost = host;
    }

    /**
     * @return the port of the proxy
     */
    int getProxyPort() {
        return m_proxyPort;
    }

    /**
     * @param port of the proxy
     */
    void setProxyPort(final int port) {
        this.m_proxyPort = port;
    }

    /**
     * @return whether an authentication for the proxy is configured
     */
    boolean isUserAuth() {
        return m_userAuth;
    }

    /**
     * @param userAuth whether an authentication is required for the proxy (default: {@code false})
     */
    void setUserAuth(final boolean userAuth) {
        this.m_userAuth = userAuth;
    }

    /**
     * @return whether workflow credentials should be used for the proxy authentication
     */
    boolean isUseWorkflowCredentials() {
        return m_useWorkflowCredentials;
    }

    /**
     * @param useWorkflowCredentials whether workflow credentials should be used for the proxy authentication
     *            (default: {@code false})
     */
    void setUseWorkflowCredentials(final boolean useWorkflowCredentials) {
        this.m_useWorkflowCredentials = useWorkflowCredentials;
    }

    /**
     * @return the workflow credentials
     */
    String getProxyWorkflowCredentials() {
        return m_proxyWorkflowCredentials;
    }

    /**
     * @param workflowCredentials to be used for the proxy (default: {@code ""})
     */
    void setProxyWorkflowCredentials(final String workflowCredentials) {
        this.m_proxyWorkflowCredentials = workflowCredentials;
    }

    /**
     * @return the user for the authentication for the proxy
     */
    String getProxyUser() {
        return m_proxyUser;
    }

    /**
     * @param user for the authentication for the proxy (default: {@code ""})
     */
    void setProxyUser(final String user) {
        this.m_proxyUser = user;
    }

    /**
     * @return the password for the authentication of the proxy
     */
    String getPassword() {
        return m_password;
    }

    /**
     * @param password for the authentication of the proxy (default: {@code ""})
     */
    void setPassword(final String password) {
        this.m_password = password;
    }

    /**
     * @return a proxy configuration in the form of a {@link ConnectionInformation}-object to be passed via a
     *         {@code ConnectionInformationPort}.
     */
    ConnectionInformation getConnectionInformation(final CredentialsProvider credentialsProvider) {
        final ConnectionInformation connectionInformation = new ConnectionInformation();
        connectionInformation.setHost(getProxyHost());
        connectionInformation.setPort(getProxyPort());
        if (isUserAuth()) {
            if (isUseWorkflowCredentials()) {
                // Use credentials
                final ICredentials credentials = credentialsProvider.get(getProxyWorkflowCredentials());
                connectionInformation.setUser(credentials.getLogin());
                try {
                    connectionInformation
                        .setPassword(KnimeEncryption.encrypt(credentials.getPassword().toCharArray()));
                } catch (final Exception e) {
                    // Set no password
                }
            } else {
                // Use direct settings
                connectionInformation.setUser(getProxyUser());
                connectionInformation.setPassword(getPassword());
            }
        }
        return connectionInformation;
    }

    private static final String KEY_PROXY_SETTINGS = "proxy";

    private static final String KEY_USE_PROXY = "useProxy";

    private static final String KEY_HOST = "proxyHost";

    private static final String KEY_PORT = "proxyPort";

    private static final String KEY_USE_USER_AUTH = "proxyUserAuth";

    private static final String KEY_USE_WF_CRED = "proxyUseWFCred";

    private static final String KEY_WF_CRED = "proxyWFCred";

    private static final String KEY_USER = "proxyUser";

    private static final String KEY_PASSWORD = "proxyPassword";

    /**
     *
     * @param proxySettings sub-settings-object to store the settings into
     */
    void saveProxySettings(final NodeSettingsWO proxySettings) {
        proxySettings.addBoolean(KEY_USE_PROXY, isUseProxy());
        proxySettings.addString(KEY_HOST, getProxyHost());
        proxySettings.addInt(KEY_PORT, getProxyPort());
        proxySettings.addBoolean(KEY_USE_USER_AUTH, isUserAuth());
        proxySettings.addBoolean(KEY_USE_WF_CRED, isUseWorkflowCredentials());
        proxySettings.addString(KEY_WF_CRED, getProxyWorkflowCredentials());
        proxySettings.addString(KEY_USER, getProxyUser());
        proxySettings.addPassword(KEY_PASSWORD, ">$:g~l63t(uc1[y#[u", getPassword());
    }

    /**
     *
     * @param settings -object to store the settings into. Creates sub-settings with the key "proxy".
     */
    void save(final NodeSettingsWO settings) {
        saveProxySettings(settings.addNodeSettings(KEY_PROXY_SETTINGS));
    }

    /**
     *
     * @param proxySettings sub-settings-object to read the settings from
     */
    void loadProxySettings(final NodeSettingsRO proxySettings) {
        setUseProxy(proxySettings.getBoolean(KEY_USE_PROXY, false));
        setProxyHost(proxySettings.getString(KEY_HOST, ""));
        setProxyPort(proxySettings.getInt(KEY_PORT, m_proxyPort));
        setUserAuth(proxySettings.getBoolean(KEY_USE_USER_AUTH, false));
        setUseWorkflowCredentials(proxySettings.getBoolean(KEY_USE_WF_CRED, false));
        setProxyWorkflowCredentials(proxySettings.getString(KEY_WF_CRED, ""));
        setProxyUser(proxySettings.getString(KEY_USER, ""));
        setPassword(proxySettings.getPassword(KEY_PASSWORD, ">$:g~l63t(uc1[y#[u", ""));
    }

    /**
     *
     * @param settings -object to read the settings from the contained sub-settings "proxy".
     */
    void load(final NodeSettingsRO settings) {
        NodeSettingsRO proxySettings;
        try {
            proxySettings = settings.getNodeSettings(KEY_PROXY_SETTINGS);
        } catch (InvalidSettingsException e) {
            proxySettings = new NodeSettings(KEY_PROXY_SETTINGS);
        }
        loadProxySettings(proxySettings);
    }

    /**
     * @see {@link #load(NodeSettingsRO)}
     * @see {@link ConnectionInformationConfiguration#loadAndValidate(NodeSettingsRO)}
     * @param settings -object to read the settings from the contained proxy-sub-settings.
     * @throws InvalidSettingsException
     */
    void loadAndValidate(final NodeSettingsRO settings) throws InvalidSettingsException {
        load(settings);
        if (isUseProxy()) {
            if (StringUtils.isEmpty(getProxyHost())) {
                throw new InvalidSettingsException("proxy host missing");
            }
            int proxyPort = getProxyPort();
            if (proxyPort < 0 || proxyPort > 65535) {
                throw new InvalidSettingsException("Invalid proxy port number: " + proxyPort);
            }
            if (isUseWorkflowCredentials()) {
                if (StringUtils.isEmpty(getProxyWorkflowCredentials())) {
                    throw new InvalidSettingsException("proxy workflowcredentials missing");
                }
            }
        }
    }

}
