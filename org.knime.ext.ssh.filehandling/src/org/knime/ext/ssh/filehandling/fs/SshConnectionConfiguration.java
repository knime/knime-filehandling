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
 *   2020-08-31 (Vyacheslav Soldatov): created
 */
package org.knime.ext.ssh.filehandling.fs;

/**
 * Settings required for create the SFTP connection.
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
public class SshConnectionConfiguration {
    private ConnectionToNodeModelBridge m_bridge;

    private long m_connectionTimeout = 30000l;
    private int m_port = 22;
    private String m_host;
    private String m_userName;
    private boolean m_useKnownHosts;

    private String m_password;

    private boolean m_useKeyFile;
    private String m_keyFilePassword;

    /**
     * Default constructor.
     */
    public SshConnectionConfiguration() {
        super();
    }

    /**
     * @return the bridge to node model.
     */
    public ConnectionToNodeModelBridge getBridge() {
        return m_bridge;
    }

    /**
     * @param bridge
     *            the bridge to node model.
     */
    public void setBridge(final ConnectionToNodeModelBridge bridge) {
        m_bridge = bridge;
    }

    /**
     * @return connection time out.
     */
    public long getConnectionTimeout() {
        return m_connectionTimeout;
    }

    /**
     * @param connectionTimeout
     *            connection time out.
     */
    public void setConnectionTimeout(final long connectionTimeout) {
        m_connectionTimeout = connectionTimeout;
    }

    /**
     * @return the port.
     */
    public int getPort() {
        return m_port;
    }

    /**
     * @param port
     *            the port.
     */
    public void setPort(final int port) {
        m_port = port;
    }

    /**
     * @return the host address.
     */
    public String getHost() {
        return m_host;
    }

    /**
     * @param host
     *            host address.
     */
    public void setHost(final String host) {
        m_host = host;
    }

    /**
     * @return the user name.
     */
    public String getUserName() {
        return m_userName;
    }

    /**
     * @param userName
     *            the user name.
     */
    public void setUserName(final String userName) {
        m_userName = userName;
    }

    /**
     * @return true if known hosts file should be used for connection verification
     */
    public boolean isUseKnownHosts() {
        return m_useKnownHosts;
    }

    /**
     * @param useKnownHosts
     *            whether or not known hosts file should be used for connection
     *            verification
     */
    public void setUseKnownHosts(final boolean useKnownHosts) {
        m_useKnownHosts = useKnownHosts;
    }

    /**
     * @return password in case of username & password authentication.
     */
    public String getPassword() {
        return m_password;
    }

    /**
     * @param password
     *            password in case of username & password authentication
     */
    public void setPassword(final String password) {
        m_password = password;
    }

    /**
     * @return true if key file should be used for authentication.
     */
    public boolean isUseKeyFile() {
        return m_useKeyFile;
    }

    /**
     * @param useKeyFile
     *            whether or not key file should be used for authentication.
     */
    public void setUseKeyFile(final boolean useKeyFile) {
        m_useKeyFile = useKeyFile;
    }

    /**
     * @return key file password in case of key file authentication.
     */
    public String getKeyFilePassword() {
        return m_keyFilePassword;
    }

    /**
     * @param keyFilePassword
     *            key file password in case of key file authentication
     */
    public void setKeyFilePassword(final String keyFilePassword) {
        m_keyFilePassword = keyFilePassword;
    }
}
