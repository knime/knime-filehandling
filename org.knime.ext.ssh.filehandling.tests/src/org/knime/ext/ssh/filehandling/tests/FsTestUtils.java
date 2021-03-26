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
 *   2020-08-04 (Vyacheslav Soldatov): created
 */

package org.knime.ext.ssh.filehandling.tests;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

import org.knime.core.node.InvalidSettingsException;
import org.knime.ext.ssh.filehandling.fs.ConnectionToNodeModelBridge;
import org.knime.ext.ssh.filehandling.fs.SshFSConnection;
import org.knime.ext.ssh.filehandling.fs.SshConnectionConfiguration;

/**
 * Utilities for SSH file system tests.
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 *
 */
public class FsTestUtils {
    /**
     * Default constructor.
     */
    private FsTestUtils() {
        super();
    }

    /**
     * @return file system connection.
     * @throws IOException
     */
    public static SshFSConnection createConnection() throws IOException {
        // working directory
        final String workingDirectory = "/tmp";
        final SshConnectionConfiguration cfg = new SshConnectionConfiguration();

        // This connects to the SSH server specified by the KNIME_SSHD_ADDRESS
        // environment variable.
        // The environment variable is set by Jenkins when running the unit tests at
        // build time (see Jenkinsfile).
        final String sshdAddress = System.getenv("KNIME_SSHD_ADDRESS");
        if (sshdAddress == null || sshdAddress.isEmpty()) {
            throw new IllegalArgumentException(
                    "Environment variable KNIME_SSHD_ADDRESS must be set to host:port of the SSH server");
        }

        final String[] sshdAddressSplits = sshdAddress.split(":");

        cfg.setHost(sshdAddressSplits[0]);
        cfg.setPort(Integer.parseInt(sshdAddressSplits[1]));
        cfg.setUserName("jenkins");
        cfg.setUseKeyFile(true);
        cfg.setKeyFilePassword("");
        cfg.setUseKnownHosts(false);

        cfg.setBridge(new ConnectionToNodeModelBridge() {
            @Override
            public void doWithKnownHostsFile(final Consumer<Path> consumer) throws IOException, InvalidSettingsException {
                throw new IOException("Not known hosts user");
            }

            @Override
            public void doWithKeysFile(final Consumer<Path> consumer) throws IOException, InvalidSettingsException {
                final Path privKey = Paths.get(System.getProperty("user.home"), ".ssh", "id_rsa");
                consumer.accept(privKey);
            }
        });

        return new SshFSConnection(cfg, workingDirectory);
    }
}
