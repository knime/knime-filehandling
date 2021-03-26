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
package org.knime.ext.ssh.filehandling.testing;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.CheckUtils;
import org.knime.ext.ssh.filehandling.fs.ConnectionToNodeModelBridge;
import org.knime.ext.ssh.filehandling.fs.SshFSConnection;
import org.knime.ext.ssh.filehandling.fs.SshConnectionConfiguration;
import org.knime.ext.ssh.filehandling.fs.SshFileSystem;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.testing.DefaultFSTestInitializerProvider;

/**
 * Initializer provider for SSH.
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
public class SshTestInitializerProvider extends DefaultFSTestInitializerProvider {

    @SuppressWarnings("resource")
    @Override
    public SshTestInitializer setup(final Map<String, String> cfg) throws IOException {

        validateConfiguration(cfg);

        final String workingDir = generateRandomizedWorkingDir(cfg.get("workingDirPrefix"),
                SshFileSystem.PATH_SEPARATOR);

        final SshConnectionConfiguration sshCfg = new SshConnectionConfiguration();
        sshCfg.setHost(cfg.get("host"));
        sshCfg.setUserName(cfg.get("username"));
        if (cfg.get("password") != null) {
            sshCfg.setPassword(cfg.get("password"));
        } else {
            sshCfg.setUseKeyFile(true);
            sshCfg.setBridge(new ConnectionToNodeModelBridge() {

                @Override
                public void doWithKnownHostsFile(final Consumer<Path> consumer)
                        throws IOException, InvalidSettingsException {
                    // not supported
                }

                @Override
                public void doWithKeysFile(final Consumer<Path> consumer) throws IOException, InvalidSettingsException {
                    consumer.accept(Paths.get(cfg.get("keyFile"))); // NOSONAR filename intentionally comes from a
                                                                    // user-defined parameter
                }
            });
        }
        sshCfg.setPort(cfg.containsKey("port") ? Integer.parseInt(cfg.get("port")) : 22);
        sshCfg.setConnectionTimeout(Duration.ofSeconds(300)); // set a big time out for easier debugging

        // create connection
        final SshFSConnection connection = new SshFSConnection(sshCfg, workingDir);
        return new SshTestInitializer(connection);
    }

    private static void validateConfiguration(final Map<String, String> configuration) {
        CheckUtils.checkArgumentNotNull(configuration.get("host"), "host must be specified.");
        CheckUtils.checkArgumentNotNull(configuration.get("workingDirPrefix"), "workingDirPrefix must be specified.");
        CheckUtils.checkArgumentNotNull(configuration.get("username"), "username must be specified.");
        CheckUtils.checkArgumentNotNull(
                Optional.ofNullable(configuration.get("password")).orElse(configuration.get("keyFile")),
                "Either password or keyFile must be specified.");
    }

    @Override
    public String getFSType() {
        return SshFileSystem.FS_TYPE;
    }

    @Override
    public FSLocationSpec createFSLocationSpec(final Map<String, String> configuration) {
        validateConfiguration(configuration);
        return SshFileSystem.createFSLocationSpec(configuration.get("host"));
    }
}
