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
 *   2023-05-18 (Zkriya Rakhimberdiyev): created
 */
package org.knime.ext.ssh.filehandling.tests;


import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.apache.sshd.common.config.keys.AuthorizedKeyEntry;
import org.apache.sshd.common.config.keys.KeyUtils;
import org.apache.sshd.common.config.keys.PublicKeyEntryResolver;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.knime.core.node.InvalidSettingsException;
import org.knime.ext.ssh.filehandling.fs.ConnectionToNodeModelBridge;
import org.knime.ext.ssh.filehandling.fs.SftpSessionFactory;
import org.knime.ext.ssh.filehandling.fs.SshFSConnectionConfig;

/**
 * Tests private keys loading
 *
 * @author Zkriya Rakhimberdiyev
 */
public class PrivateKeyLoadingTests {

    private static final String USER_NAME = "root";
    private static int m_port;
    private static SshServer m_sshServer;

    private SftpSessionFactory m_sessionFactory;

    @BeforeAll
    static void setupServer() throws IOException, GeneralSecurityException {
        m_sshServer = SshServer.setUpDefaultServer();
        m_sshServer.setPort(0); // 0 means use any available port

        final var publicKeysFile = FsTestUtils.findInPlugin("authorized_keys");

        Map<String, Collection<PublicKey>> userToKeys = new HashMap<>();
        Collection<PublicKey> publicKeys = new HashSet<>();
        for (final var keyEntry : AuthorizedKeyEntry.readAuthorizedKeys(publicKeysFile.toPath())) {
            final var publicKey = keyEntry.resolvePublicKey(null, PublicKeyEntryResolver.IGNORING);
            if (publicKey != null) {
                publicKeys.add(publicKey);
            }
        }
        userToKeys.put(USER_NAME, publicKeys);
        m_sshServer.setPublickeyAuthenticator(new UserKeySetPublickeyAuthenticator(userToKeys));
        m_sshServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
        m_sshServer.start();
        m_port = m_sshServer.getPort();
    }

    @AfterAll
    static void teardownServer() throws IOException {
        if (m_sshServer != null) {
            m_sshServer.stop();
        }
    }

    @AfterEach
    void afterTestCase() throws IOException {
        if (m_sessionFactory != null) {
            m_sessionFactory.destroy();
        }
    }

    @Test
    void test_open_ssh_ecdsa_with_password() throws IOException {
        testConnection("id_ecdsa_pass", "knime");
    }

    @Test
    void test_open_ssh_eddsa() throws IOException {
        testConnection("id_eddsa", null);
    }

    @Test
    void test_putty_ecdsa() throws IOException {
        testConnection("putty_ecdsa.ppk", null);
    }

    @Test
    void test_putty_eddsa() throws IOException {
        testConnection("putty_eddsa.ppk", null);
    }

    @Test
    void test_putty_rsa_with_password() throws IOException {
        testConnection("putty_rsa_pass.ppk", "knime");
    }

    @Test
    void test_invalid_private_key() {
        final var thrown = assertThrows(IOException.class, () -> {
            testConnection("invalid", null);
        });
        assertTrue(thrown.getMessage().contains("Authentication failed"));
    }

    private void testConnection(final String privKeyPath, final String keyFilePassword) throws IOException {
        final var privKey = FsTestUtils.findInPlugin(privKeyPath);
        final var connectionConfig = createConnectionConfig(privKey.toPath(), keyFilePassword);
        m_sessionFactory = new SftpSessionFactory(connectionConfig);
        m_sessionFactory.init();

        try (final var session = m_sessionFactory.createSession()) {
            assertNotNull(session);
        }
    }

    private static SshFSConnectionConfig createConnectionConfig(final Path privKey, final String keyFilePassword) {
        // working directory
        final var workingDirectory = "/tmp";
        final var cfg = new SshFSConnectionConfig(workingDirectory);

        cfg.setHost("localhost");
        cfg.setPort(m_port);
        cfg.setUserName(USER_NAME);
        cfg.setUseKeyFile(true);
        cfg.setKeyFilePassword(StringUtils.trimToEmpty(keyFilePassword));
        cfg.setUseKnownHosts(false);

        cfg.setBridge(new ConnectionToNodeModelBridge() {
            @Override
            public void doWithKnownHostsFile(final Consumer<Path> consumer)
                    throws IOException, InvalidSettingsException {
                throw new IOException("Not known hosts user");
            }

            @Override
            public void doWithKeysFile(final Consumer<Path> consumer) throws IOException, InvalidSettingsException {
                consumer.accept(privKey);
            }
        });
        return cfg;
    }

    private static class UserKeySetPublickeyAuthenticator implements PublickeyAuthenticator {
        private final Map<String, Collection<PublicKey>> m_userToKeys;

        public UserKeySetPublickeyAuthenticator(final Map<String, Collection<PublicKey>> userToKeys) {
            m_userToKeys = userToKeys;
        }

        @Override
        public boolean authenticate(final String username, final PublicKey key, final ServerSession session) {
            return KeyUtils.findMatchingKey(key,
                    m_userToKeys.getOrDefault(username, Collections.emptyList())) != null;
        }
    }
}
