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
package org.knime.ext.ftp.filehandling.testing;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

import org.apache.ftpserver.ftplet.FtpException;
import org.knime.core.node.util.CheckUtils;
import org.knime.ext.ftp.filehandling.fs.FtpConnectionConfiguration;
import org.knime.ext.ftp.filehandling.fs.FtpFileSystem;
import org.knime.ext.ftp.filehandling.fs.FtpPath;
import org.knime.ext.ftp.filehandling.fs.ProtectedHostConfiguration;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.testing.DefaultFSTestInitializer;
import org.knime.filehandling.core.testing.DefaultFSTestInitializerProvider;

/**
 * Initializer provider for FTP.
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
public class FtpTestInitializerProvider extends DefaultFSTestInitializerProvider {

    @Override
    public DefaultFSTestInitializer<FtpPath, FtpFileSystem> setup(final Map<String, String> cfg) throws IOException {
        validateParameters(cfg);

        final String workingDir = generateRandomizedWorkingDir(cfg.get("workingDirPrefix"),
                FtpFileSystem.PATH_SEPARATOR);

        final FtpConnectionConfiguration ftpCfg = new FtpConnectionConfiguration();
        ftpCfg.setMaxConnectionPoolSize(2);
        ftpCfg.setTestMode(true);
        ftpCfg.setHost(cfg.getOrDefault("host", "localhost"));
        ftpCfg.setPort(Integer.parseInt(cfg.getOrDefault("port", "21")));
        ftpCfg.setUser(cfg.getOrDefault("username", "junit"));
        ftpCfg.setPassword(cfg.getOrDefault("password", "password"));
        ftpCfg.setWorkingDirectory(workingDir);
        ftpCfg.setServerTimeZoneOffset(Duration.ofMinutes(Long.parseLong(cfg.getOrDefault("timeZoneOffset", "0"))));
        ftpCfg.setUseSsl(Boolean.parseBoolean(cfg.getOrDefault("ssl", "false")));

        // proxy
        final String proxyHost = cfg.get("proxy.host");
        if (proxyHost != null) {
            ProtectedHostConfiguration proxy = new FtpConnectionConfiguration();
            ftpCfg.setProxy(proxy);

            proxy.setHost(proxyHost);
            proxy.setPort(Integer.parseInt(cfg.getOrDefault("proxy.port", "80")));
            proxy.setUser(cfg.get("proxy.user"));
            proxy.setPassword(cfg.get("proxy.password"));
        }

        if ("true".equals(cfg.get("embedded"))) {
            try {
                return new EmbeddedFtpTestInitializer(ftpCfg);
            } catch (FtpException ex) {
                throw new IOException("Failed to create test initializer", ex);
            }
        } else {
            return new RemoteFtpTestInitializer(ftpCfg);
        }
    }

    private static void validateParameters(final Map<String, String> cfg) throws IOException {
        if (!"true".equals(cfg.get("embedded"))) {
            CheckUtils.checkArgumentNotNull(cfg.get("host"), "'host' must be specified.");

            final String userName = cfg.get("username");
            CheckUtils.checkArgumentNotNull(userName, "'username' must be specified.");
            if (!userName.equals("anonymous")) {
                CheckUtils.checkArgumentNotNull(cfg.get("password"), "'password' must be specified.");
            }
        }
    }

    @Override
    public String getFSType() {
        return FtpFileSystem.FS_TYPE;
    }

    @Override
    public FSLocationSpec createFSLocationSpec(final Map<String, String> cfg) {
        return FtpFileSystem.createFSLocationSpec(cfg.getOrDefault("host", "localhost"));
    }
}
