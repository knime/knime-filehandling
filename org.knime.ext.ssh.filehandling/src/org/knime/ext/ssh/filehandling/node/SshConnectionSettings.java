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

import org.knime.core.node.InvalidSettingsException;
import org.knime.ext.ssh.filehandling.fs.SshFileSystem;

/**
 * Settings for {@link SshConnectionNodeModel}.
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
public class SshConnectionSettings implements Cloneable {
    /**
     * Default connection time out.
     */
    public static final int DEFAULT_TIMEOUT = 30;

    private String m_workingDirectory;
    private int m_connectionTimeout;
    private int m_port;
    private String m_host;

    //authentication
    private String m_userName;
    private String m_password;

    /**
     * Default constructor.
     */
    public SshConnectionSettings() {
        m_host = "localhost";
        m_port = 22;
        m_connectionTimeout = DEFAULT_TIMEOUT;

        //authentication
        m_userName = System.getProperty("user.name");

        m_workingDirectory = SshFileSystem.PATH_SEPARATOR;
    }

    /**
     * @return user name.
     */
    public String getUsername() {
        return m_userName;
    }
    /**
     * @return password
     */
    public String getPassword() {
        return m_password;
    }

    /**
     * Validates settings consistency for this instance.
     *
     * @throws InvalidSettingsException
     */
    public void validate() throws InvalidSettingsException {
        if (m_connectionTimeout < 1) {
            throw new InvalidSettingsException("Invalid connection timeout "
                    + m_connectionTimeout);
        }
        if (m_port < 1) {
            throw new InvalidSettingsException("Invalid port "
                    + m_port);
        }
        if (isEmpty(m_workingDirectory)) {
            throw new InvalidSettingsException("Working directory must be specified.");
        }
        if (m_host == null) {
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
        return m_workingDirectory;
    }
    /**
     * @param dir working directory.
     */
    public void setWorkingDirectory(final String dir) {
        this.m_workingDirectory = dir;
    }
    /**
     * @param connectionTimeout the connectionTimeout to set
     */
    public void setConnectionTimeout(final int connectionTimeout) {
        this.m_connectionTimeout = connectionTimeout;
    }
    /**
     * @param password the password to set
     */
    public void setPassword(final String password) {
        this.m_password = password;
    }
    /**
     * @param userName the userName to set
     */
    public void setUserName(final String userName) {
        this.m_userName = userName;
    }
    /**
     * @return remote port.
     */
    public int getPort() {
        return m_port;
    }
    /**
     * @param port port to set.
     */
    public void setPort(final int port) {
        m_port = port;
    }
    /**
     * @return remote host.
     */
    public String getHost() {
        return m_host;
    }
    /**
     * @param host host to set.
     */
    public void setHost(final String host) {
        this.m_host = host;
    }

    /**
     * @return connection time out.
     */
    public int getConnectionTimeout() {
        return m_connectionTimeout;
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
}
