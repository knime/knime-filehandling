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
 *   2021-03-07 (Alexander Bondaletov): created
 */
package org.knime.ext.smb.filehandling.testing;

import java.io.IOException;
import java.util.Map;

import org.knime.ext.smb.filehandling.fs.SmbFSConnection;
import org.knime.ext.smb.filehandling.fs.SmbFSConnectionConfig;
import org.knime.ext.smb.filehandling.node.SmbConnectorSettings.ConnectionMode;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.connections.base.auth.StandardAuthTypes;
import org.knime.filehandling.core.testing.DefaultFSTestInitializerProvider;

/**
 * FS test initializer provider for Samba
 *
 * @author Alexander Bondaletov
 */
public class SmbFSTestInitializerProvider extends DefaultFSTestInitializerProvider {
    private static final String HOST = "host";
    private static final String SHARE = "share";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";

    @SuppressWarnings("resource")
    @Override
    public SmbFSTestInitializer setup(final Map<String, String> configuration) throws IOException {
        final SmbFSConnectionConfig config = createFSConnectionConfig(configuration);
        final SmbFSConnection fsConnection = new SmbFSConnection(config);
        return new SmbFSTestInitializer(fsConnection);
    }

    private SmbFSConnectionConfig createFSConnectionConfig(final Map<String, String> configuration) {
        final String workDir = generateRandomizedWorkingDir(getParameter(configuration, "workingDirPrefix"),
                SmbFSConnection.PATH_SEPARATOR);

        final SmbFSConnectionConfig config = new SmbFSConnectionConfig();
        config.setConnectionMode(ConnectionMode.FILESERVER);
        config.setFileserverHost(getParameter(configuration, HOST));
        config.setFileserverPort(445);
        config.setFileserverShare(getParameter(configuration, SHARE));
        config.setAuthType(StandardAuthTypes.USER_PASSWORD);
        config.setUser(getParameter(configuration, USERNAME));
        config.setPassword(getParameter(configuration, PASSWORD));
        config.setWorkingDirectory(workDir);
        return config;
    }


    @Override
    public String getFSType() {
        return SmbFSConnection.FS_TYPE;
    }

    @Override
    public FSLocationSpec createFSLocationSpec(final Map<String, String> configuration) {
        return createFSConnectionConfig(configuration).createFSLocationSpec();
    }
}
