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
 *   2020-10-16 (Vyacheslav Soldatov): created
 */
package org.knime.ext.ftp.filehandling.node;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.runtime.CoreException;
import org.junit.Test;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.workflow.ICredentials;
import org.knime.ext.ftp.filehandling.fs.FtpConnectionConfiguration;
import org.knime.ext.ftp.filehandling.fs.ProtectedHostConfiguration;
import org.knime.ext.ftp.filehandling.node.FtpAuthenticationSettingsModel.AuthType;

/**
 * Unit tests for {@link FtpConnectionNodeModel}
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
public class FtpConnectionNodeModelTest {
    private final MockProxyService m_proxyService = new MockProxyService();

    /**
     * Default constructor.
     */
    public FtpConnectionNodeModelTest() {
    }

    /**
     * Tests base configuration values are correct populated from from settings.
     *
     * @throws InvalidSettingsException
     */
    @Test
    public void testCreateBaseConfiguration() throws InvalidSettingsException {
        final FtpConnectionSettingsModel settings = new FtpConnectionSettingsModel();

        final String host = "junitservices.io";
        final int port = 12345;
        final int connectionTimeOut = 321;
        final int maxSessionsCount = 4567;
        final int minSessionsCount = 4000;
        final int coreSessionsCount = 4111;
        final int maxIdleTime = 10;
        final String workingDir = "/junit/tests/workingDir";
        final int timeZoneOffsetMinutes = 425;
        final boolean useSsl = true;

        //populate settings
        settings.getHostModel().setStringValue(host);
        settings.getPortModel().setIntValue(port);
        settings.getConnectionTimeoutModel().setIntValue(connectionTimeOut);
        settings.getMaxConnectionPoolSizeModel().setIntValue(maxSessionsCount);
        settings.getMinConnectionPoolSizeModel().setIntValue(minSessionsCount);
        settings.getCoreConnectionPoolSizeModel().setIntValue(coreSessionsCount);
        settings.getMaxIdleTimeModel().setIntValue(maxIdleTime);
        settings.getWorkingDirectoryModel().setStringValue(workingDir);
        settings.getTimeZoneOffsetModel().setIntValue(timeZoneOffsetMinutes);
        settings.getUseSslModel().setBooleanValue(useSsl);

        final FtpConnectionConfiguration config = FtpConnectionNodeModel.createConfiguration(settings,
                createCredentialsProvider(), m_proxyService);

        // test correct created
        assertEquals(config.getHost(), settings.getHost());
        assertEquals(config.getPort(), settings.getPort());
        assertEquals(config.getConnectionTimeOut(), 1000 * settings.getConnectionTimeout());
        assertEquals(config.getMaxConnectionPoolSize(), settings.getMaxConnectionPoolSize());
        assertEquals(config.getMinConnectionPoolSize(), settings.getMinConnectionPoolSize());
        assertEquals(config.getCoreConnectionPoolSize(), settings.getCoreConnectionPoolSize());
        assertEquals(config.getMaxIdleTime(), TimeUnit.SECONDS.toMillis(settings.getMaxIdleTime()));
        assertEquals(config.getWorkingDirectory(), settings.getWorkingDirectory());
        assertEquals(config.getServerTimeZoneOffset(), TimeUnit.MINUTES.toMillis(settings.getTimeZoneOffset()));
        assertEquals(config.isUseSsl(), settings.isUseSsl());
    }

    /**
     * Tests correct filling of proxy data from settings to FTP configuration.
     *
     * @throws CoreException
     * @throws InvalidSettingsException
     */
    @Test
    public void testCreateConfigurationUseProxy() throws CoreException, InvalidSettingsException {
        final String host = "anyhost";
        final int port = 9999;
        final String user = "anyuser";
        final String password = "anypassword";

        final IProxyData proxyData = createProxyData(host, port, user, password);
        m_proxyService.setProxyData(new IProxyData[] { proxyData });

        final FtpConnectionSettingsModel settings = createSettings();
        settings.getUseProxyModel().setBooleanValue(true);

        // test proxy
        final FtpConnectionConfiguration config = FtpConnectionNodeModel.createConfiguration(settings,
                createCredentialsProvider(), m_proxyService);
        final ProtectedHostConfiguration proxy = config.getProxy();

        assertNotNull(proxy);
        assertEquals(host, proxy.getHost());
        assertEquals(port, proxy.getPort());
        assertEquals(user, proxy.getUser());
        assertEquals(password, proxy.getPassword());
    }

    /**
     * Tests invalid settings exception is thrown when attempt to use proxy which is
     * not configured on Eclipse IDE level.
     *
     * @throws InvalidSettingsException
     * @throws CoreException
     */
    @Test(expected = InvalidSettingsException.class)
    public void testCreateConfigurationProxyNotFound() throws InvalidSettingsException, CoreException {
        final FtpConnectionSettingsModel settings = createSettings();
        settings.getUseProxyModel().setBooleanValue(true);

        // test proxy
        FtpConnectionNodeModel.createConfiguration(settings,
                createCredentialsProvider(), m_proxyService);
    }

    /**
     * Tests anonymous authentication settings.
     *
     * @throws InvalidSettingsException
     */
    @Test
    public void testCredentialsAnonymousAuth() throws InvalidSettingsException {
        final FtpConnectionSettingsModel settings = createSettings();
        settings.getAuthenticationSettings().setAuthType(AuthType.ANONYMOUS);

        final FtpConnectionConfiguration config = FtpConnectionNodeModel.createConfiguration(settings,
                createCredentialsProvider(), m_proxyService);

        assertEquals("anonymous", config.getUser());
        assertEquals("", config.getPassword());
    }

    /**
     * Tests username & password authentication.
     *
     * @throws InvalidSettingsException
     */
    @Test
    public void testCredentialsUsenamePasswordAuth() throws InvalidSettingsException {
        final String user = "anyuser";
        final String password = "anypassword";

        final FtpConnectionSettingsModel settings = createSettings();
        settings.getAuthenticationSettings().setAuthType(AuthType.USER_PWD);
        settings.getAuthenticationSettings().getUserModel().setStringValue(user);
        settings.getAuthenticationSettings().getPasswordModel().setStringValue(password);

        final FtpConnectionConfiguration config = FtpConnectionNodeModel.createConfiguration(settings,
                createCredentialsProvider(), m_proxyService);

        assertEquals(user, config.getUser());
        assertEquals(password, config.getPassword());
    }

    /**
     * Test validate settings with empty host.
     *
     * @throws InvalidSettingsException
     */
    @Test(expected = InvalidSettingsException.class)
    public void testValidateEmptyHost() throws InvalidSettingsException {
        FtpConnectionSettingsModel settings = createSettings();
        settings.getHostModel().setStringValue(null);
        settings.validate();
    }

    /**
     * Test validate settings with empty directory.
     *
     * @throws InvalidSettingsException
     */
    @Test(expected = InvalidSettingsException.class)
    public void testValidateEmptyDirectory() throws InvalidSettingsException {
        FtpConnectionSettingsModel settings = createSettings();
        settings.getWorkingDirectoryModel().setStringValue("");
        settings.validate();
    }

    /**
     * Test validate settings with incorrect minimal and core pool sizes.
     *
     * @throws InvalidSettingsException
     */
    @Test(expected = InvalidSettingsException.class)
    public void testValidateMinCorePoolSize() throws InvalidSettingsException {
        FtpConnectionSettingsModel settings = createSettings();
        settings.getCoreConnectionPoolSizeModel().setIntValue(1);
        settings.getMinConnectionPoolSizeModel().setIntValue(2);
        settings.validate();
    }

    /**
     * Test validate settings with incorrect maximal and core pool sizes.
     *
     * @throws InvalidSettingsException
     */
    @Test(expected = InvalidSettingsException.class)
    public void testValidateMaxCorePoolSize() throws InvalidSettingsException {
        FtpConnectionSettingsModel settings = createSettings();
        settings.getMinConnectionPoolSizeModel().setIntValue(1);
        settings.getCoreConnectionPoolSizeModel().setIntValue(3);
        settings.getMaxConnectionPoolSizeModel().setIntValue(2);
        settings.validate();
    }

    /**
     * Test validate correct settings.
     *
     * @throws InvalidSettingsException
     */
    @Test
    public void testValidateOk() throws InvalidSettingsException {
        createSettings().validate();
    }

    /**
     * Tests credentials based authentication
     *
     * @throws InvalidSettingsException
     */
    @Test
    public void testCredentialsAuthNotCredentials() throws InvalidSettingsException {
        final String user = "anyuser";
        final String password = "anypassword";
        final String credentials = "anycreds";

        final FtpConnectionSettingsModel settings = createSettings();
        settings.getAuthenticationSettings().setAuthType(AuthType.CREDENTIALS);
        settings.getAuthenticationSettings().getCredentialModel().setStringValue(credentials);

        final ICredentials creds = new ICredentials() {
            @Override
            public String getPassword() {
                return password;
            }

            @Override
            public String getName() {
                return credentials;
            }

            @Override
            public String getLogin() {
                return user;
            }
        };

        final FtpConnectionConfiguration config = FtpConnectionNodeModel.createConfiguration(settings,
                createCredentialsProvider(creds), m_proxyService);

        assertEquals(user, config.getUser());
        assertEquals(password, config.getPassword());
    }

    /**
     * Tests credentials authentication when credentials not found.
     *
     * @throws InvalidSettingsException
     */
    @Test(expected = InvalidSettingsException.class)
    public void testCredentialsAuthNotCredentialsFound() throws InvalidSettingsException {
        final FtpConnectionSettingsModel settings = createSettings();
        settings.getAuthenticationSettings().setAuthType(AuthType.CREDENTIALS);

        FtpConnectionNodeModel.createConfiguration(settings, createCredentialsProvider(), m_proxyService);
    }

    private static Function<String, ICredentials> createCredentialsProvider(final ICredentials... creds) {
        return name -> {
            for (ICredentials cred : creds) {
                if (cred.getName().equals(name)) {
                    return cred;
                }
            }
            return null;
        };
    }

    /**
     * @return settings populated with any values.
     */
    private static FtpConnectionSettingsModel createSettings() {
        final FtpConnectionSettingsModel settings = new FtpConnectionSettingsModel();

        final String host = "junitservices.io";
        final int port = 12345;
        final int connectionTimeOut = 321;
        final int maxSessionsCount = 4567;
        final String workingDir = "/junit/tests/workingDir";
        final int timeZoneOffsetMinutes = 425;
        final boolean useSsl = true;

        // populate settings
        settings.getHostModel().setStringValue(host);
        settings.getPortModel().setIntValue(port);
        settings.getConnectionTimeoutModel().setIntValue(connectionTimeOut);
        settings.getMaxConnectionPoolSizeModel().setIntValue(maxSessionsCount);
        settings.getWorkingDirectoryModel().setStringValue(workingDir);
        settings.getTimeZoneOffsetModel().setIntValue(timeZoneOffsetMinutes);
        settings.getUseSslModel().setBooleanValue(useSsl);
        return settings;
    }

    private static IProxyData createProxyData(final String host, final int port, final String user,
            final String password) {
        ProxyDataImpl proxy = new ProxyDataImpl(host, port);
        proxy.setUserid(user);
        proxy.setPassword(password);
        return proxy;
    }
}
