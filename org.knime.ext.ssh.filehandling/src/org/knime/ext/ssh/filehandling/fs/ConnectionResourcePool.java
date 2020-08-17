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
 *   2020-08-01 (Vyacheslav Soldatov): created
 */

package org.knime.ext.ssh.filehandling.fs;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.auth.password.UserAuthPasswordFactory;
import org.apache.sshd.client.config.hosts.HostConfigEntryResolver;
import org.apache.sshd.client.keyverifier.AcceptAllServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.FactoryManager;
import org.apache.sshd.common.PropertyResolverUtils;
import org.apache.sshd.common.keyprovider.KeyIdentityProvider;
import org.apache.sshd.common.session.SessionListener;
import org.knime.ext.ssh.filehandling.node.SshConnectionSettings;

/**
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 *
 */
public class ConnectionResourcePool implements SessionListener {
    private SshClient m_sshClient;
    private ClientSession m_session;

    private SshConnectionSettings m_settings;

    private static IOException convertToCreateSshConnection(final Throwable exc) {
        //handle exception
        if (exc instanceof IOException) {
            return (IOException) exc;
        } else {
            return new IOException("Failed to create SSH connection: "
                    + exc.getMessage(), exc);
        }
    }

    /**
     * Starts resource pool.
     * @throws IOException
     */
    public synchronized void start() throws IOException {
        this.m_sshClient = SshClient.setUpDefaultClient();

        m_sshClient.setServerKeyVerifier(AcceptAllServerKeyVerifier.INSTANCE);
        m_sshClient.setHostConfigEntryResolver(HostConfigEntryResolver.EMPTY);
        m_sshClient.setKeyIdentityProvider(KeyIdentityProvider.EMPTY_KEYS_PROVIDER);

        try {
            m_sshClient.start();

            m_session = m_sshClient.connect(getUser(), getHost(), getPort()).verify(getConnectionTimeOut())
                    .getSession();

            m_session.getFactoryManager().setUserAuthFactoriesNameList(UserAuthPasswordFactory.INSTANCE.getName());
            m_session.addPasswordIdentity(getPassword());

            // set idle time out one year for avoid of unexpected
            // session closing
            PropertyResolverUtils.updateProperty(m_session, FactoryManager.IDLE_TIMEOUT, TimeUnit.DAYS.toMillis(365));

            // do authorization
            try {
                m_session.auth().verify(getConnectionTimeOut());
            } catch (final Exception exc) {
                m_session.close();
                throw new IOException("Authentication failed", exc);
            }

        } catch (final Throwable exc) {
            if (m_session != null) {
                closeSession();
            }
            if (m_sshClient != null) {
                m_sshClient.stop();
            }

            throw convertToCreateSshConnection(exc);
        }
    }

    private void closeSession() {
        try {
            m_session.close();
        } catch (Throwable ex) {
        }
    }

    /**
     * Stops resource pool.
     */
    public synchronized void stop() {
        if (m_session != null) {
            closeSession();
            m_session = null;
        }
        m_sshClient.stop();
    }

    private String getHost() {
        return m_settings.getHost();
    }

    private int getPort() {
        return m_settings.getPort();
    }

    private String getUser() {
        return m_settings.getUsername();
    }

    private String getPassword() {
        return m_settings.getPassword();
    }

    private long getConnectionTimeOut() {
        return m_settings.getConnectionTimeout() * 1000l;
    }

    /**
     * @param settings connection settings.
     */
    public void setSettings(final SshConnectionSettings settings) {
        this.m_settings = settings;
    }
}
