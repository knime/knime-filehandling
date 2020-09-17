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

import java.io.IOException;
import java.nio.channels.UnresolvedAddressException;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.auth.UserAuthFactory;
import org.apache.sshd.client.auth.password.UserAuthPasswordFactory;
import org.apache.sshd.client.auth.pubkey.UserAuthPublicKeyFactory;
import org.apache.sshd.client.config.hosts.HostConfigEntryResolver;
import org.apache.sshd.client.keyverifier.AcceptAllServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.FactoryManager;
import org.apache.sshd.common.PropertyResolverUtils;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.common.keyprovider.KeyIdentityProvider;
import org.apache.sshd.common.signature.BuiltinSignatures;
import org.knime.core.node.InvalidSettingsException;
import org.knime.filehandling.core.defaultnodesettings.ExceptionUtil;

/**
 * Creates authorized SFTP sessions.
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
public class SftpSessionFactory {
    private SshClient m_sshClient;

    private final SshConnectionConfiguration m_settings;

    /**
     * @param settings
     */
    public SftpSessionFactory(final SshConnectionConfiguration settings) {
        super();
        m_settings = settings;
    }

    /**
     * Starts resource pool.
     *
     * @throws IOException
     */
    public void init() throws IOException {
        m_sshClient = SshClient.setUpDefaultClient();

        m_sshClient.setServerKeyVerifier(AcceptAllServerKeyVerifier.INSTANCE);
        m_sshClient.setHostConfigEntryResolver(HostConfigEntryResolver.EMPTY);
        m_sshClient.setKeyIdentityProvider(KeyIdentityProvider.EMPTY_KEYS_PROVIDER);

        if (m_settings.isUseKeyFile()) {
            final UserAuthPublicKeyFactory authFactory = new UserAuthPublicKeyFactory(
                    new LinkedList<>(BuiltinSignatures.VALUES));

            final List<UserAuthFactory> fs = new LinkedList<>();
            fs.add(authFactory);
            m_sshClient.setUserAuthFactories(fs);
        }

        try {
            m_sshClient.start();
        } catch (final Throwable exc) {
            if (m_sshClient != null) {
                m_sshClient.stop();
            }

            throw convertToCreateSshConnection(exc);
        }
    }

    private static IOException convertToCreateSshConnection(final Throwable exc) {
        // handle exception
        if (exc instanceof IOException) {
            return (IOException) exc;
        } else {
            return new IOException("Failed to create SSH connection: " + exc.getMessage(), exc);
        }
    }

    /**
     * @return SFTP session.
     * @throws IOException
     */
    public ClientSession createSession() throws IOException {
        long connectionTimeOut = m_settings.getConnectionTimeout();

        ClientSession session = null;
        try {
            session = m_sshClient
                    .connect(m_settings.getUserName(), m_settings.getHost(), m_settings.getPort())
                    .verify(connectionTimeOut).getSession();

            if (m_settings.isUseKnownHosts()) {
                session.setServerKeyVerifier(new FileChooserServerKeyVerifier(m_settings.getBridge()));
            } else {
                session.setServerKeyVerifier(AcceptAllServerKeyVerifier.INSTANCE);
            }

            if (m_settings.isUseKeyFile()) {
                session.getFactoryManager().setUserAuthFactoriesNameList(UserAuthPublicKeyFactory.INSTANCE.getName());
                addPublicKeys(session);
            } else {
                final String password = m_settings.getPassword();
                if (password != null && !password.isEmpty()) {
                    session.getFactoryManager()
                            .setUserAuthFactoriesNameList(UserAuthPasswordFactory.INSTANCE.getName());
                    session.addPasswordIdentity(password);
                }
            }

            // set idle time out one year for avoid of unexpected
            // session closing
            PropertyResolverUtils.updateProperty(session, FactoryManager.IDLE_TIMEOUT, TimeUnit.DAYS.toMillis(365));
            PropertyResolverUtils.updateProperty(session, FactoryManager.AUTH_TIMEOUT,
                    connectionTimeOut);

            // do authorization
            session.auth().verify(connectionTimeOut);
        } catch (final Exception exc) {
            if (session != null) {
                closeSessionSafely(session);
            }

            if (ExceptionUtil.getDeepestError(exc) instanceof UnresolvedAddressException) {
                // UnresolvedAddressException unfortunately has a very ugly message
                throw new IOException("Unknown host " + m_settings.getHost(), exc.getCause());
            } else {
                final String msg = Optional.ofNullable(ExceptionUtil.getDeepestErrorMessage(exc, true))
                        .orElse("Failed to connect for unknown reason. Please see KNIME log for more details.");
                throw new IOException(msg, exc);
            }
        }

        return session;
    }

    private static void closeSessionSafely(final ClientSession session) {
        try {
            session.close();
        } catch (IOException ioe) {
            // do nothing
        }
    }

    /**
     * @param session
     * @throws IOException
     * @throws InvalidSettingsException
     */
    private void addPublicKeys(final ClientSession session) throws IOException, InvalidSettingsException {
        m_settings.getBridge().doWithKeysFile(path -> addPublicKeys(path, session));
    }

    private void addPublicKeys(final Path path, final ClientSession session) {
        // create temporary key pair provider instance
        final FileKeyPairProvider keyPairProvider = new FileKeyPairProvider(path);
        String keyFilePassword = m_settings.getKeyFilePassword();
        if (keyFilePassword != null) {
            keyPairProvider.setPasswordFinder(FilePasswordProvider.of(keyFilePassword));
        }

        // add public keys to session using key pair provider
        for (final KeyPair key : keyPairProvider.loadKeys(session)) {
            session.addPublicKeyIdentity(key);
        }
    }

    /**
     * Destroys session factory.
     */
    public void destroy() {
        SshClient client = m_sshClient;
        if (client != null) {
            m_sshClient = null;
            client.stop();
        }
    }
}
