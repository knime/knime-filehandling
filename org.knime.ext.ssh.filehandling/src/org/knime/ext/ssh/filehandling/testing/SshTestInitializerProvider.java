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
import java.util.Map;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;
import org.knime.ext.ssh.filehandling.fs.SshConnection;
import org.knime.ext.ssh.filehandling.fs.SshFileSystem;
import org.knime.ext.ssh.filehandling.node.SshConnectionSettings;
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

        final String host = cfg.get("host");
        final String userName = cfg.get("username");
        final String password = cfg.get("password");
        final int port = cfg.containsKey("port") ? Integer.parseInt(cfg.get("port")) : 22;

        NodeSettings settings = new NodeSettings("tmp");
        settings.addString("workingDirectory", workingDir);
        settings.addString("host", host);
        settings.addInt("port", port);
        settings.addInt("connectionTimeout", 30000);

        NodeSettingsWO auth = settings.addNodeSettings("auth");
        auth.addString("user", userName);
        auth.addPassword("password", "ekerjvjhmzle,ptktysq", password);

        final SshConnectionSettings sshSettings = new SshConnectionSettings("unit-test");
        try {
            sshSettings.loadSettingsFrom(settings);
        } catch (InvalidSettingsException ex) {
            throw new RuntimeException("Failed to initialize connection settings", ex);
        }

        final SshConnection connection = new SshConnection(sshSettings);
        return new SshTestInitializer(connection);
    }

    private static void validateConfiguration(final Map<String, String> configuration) {
        CheckUtils.checkArgumentNotNull(configuration.get("host"), "host must be specified.");
        CheckUtils.checkArgumentNotNull(configuration.get("workingDirPrefix"), "workingDirPrefix must be specified.");
        CheckUtils.checkArgumentNotNull(configuration.get("username"), "username must be specified.");
        CheckUtils.checkArgumentNotNull(configuration.get("password"), "password must be specified.");
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
