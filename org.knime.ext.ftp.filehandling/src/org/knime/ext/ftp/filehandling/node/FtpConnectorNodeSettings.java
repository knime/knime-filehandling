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
package org.knime.ext.ftp.filehandling.node;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.eclipse.core.net.proxy.IProxyData;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.ext.ftp.filehandling.Activator;
import org.knime.ext.ftp.filehandling.fs.FtpFSConnectionConfig;
import org.knime.ext.ftp.filehandling.fs.FtpFileSystem;
import org.knime.ext.ftp.filehandling.fs.ProtectedHostConfiguration;
import org.knime.filehandling.core.connections.base.auth.AuthSettings;
import org.knime.filehandling.core.connections.base.auth.EmptyAuthProviderSettings;
import org.knime.filehandling.core.connections.base.auth.StandardAuthTypes;
import org.knime.filehandling.core.connections.base.auth.UserPasswordAuthProviderSettings;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;

/**
 * Settings for the FTP Connector node.
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
class FtpConnectorNodeSettings {

    private static final boolean DEFAULT_REUSE_SSL_SESSION = true;

    private static final boolean DEFAULT_USE_IMPLICIT_FTPS = false;

    private static final boolean DEFAULT_VERIFY_HOSTNAME = true;

    private static final boolean DEFAULT_USE_PROXY = false;

    private static final String KEY_WORKING_DIRECTORY = "workingDirectory";

    private static final String KEY_CONNECTION_TIMEOUT = "connectionTimeout";

    private static final String KEY_READ_TIMEOUT = "readTimeout";

    private static final String KEY_PORT = "port";

    private static final String KEY_HOST = "host";

    private static final String KEY_MIN_POOL_SIZE = "minConnections";

    private static final String KEY_MAX_POOL_SIZE = "maxConnections";

    private static final String KEY_USE_PROXY = "useProxy";

    private static final String KEY_USE_FTPS = "useFTPS";

    private static final int DEFAULT_TIMEOUT = 30;

    private static final String KEY_TIME_ZONE_OFFSET = "timeZoneOffset";

    private static final String KEY_VERIFY_HOSTNAME = "verifyHostname";

    private static final String KEY_USE_IMPLICIT_FTPS = "useImplicitFTPS";

    private static final String KEY_REUSE_SSL_SESSION = "reuseSSLSession";

    private final SettingsModelString m_host;
    private final SettingsModelIntegerBounded m_port;
    private final AuthSettings m_authSettings;
    private final SettingsModelString m_workingDirectory;
    private final SettingsModelIntegerBounded m_connectionTimeout;
    private final SettingsModelIntegerBounded m_readTimeout;
    private final SettingsModelIntegerBounded m_minConnections;
    private final SettingsModelIntegerBounded m_maxConnections;
    private final SettingsModelIntegerBounded m_timeZoneOffset;
    private final SettingsModelBoolean m_useProxy;
    private final SettingsModelBoolean m_useFTPS;
    private final SettingsModelBoolean m_verifyHostname;
    private final SettingsModelBoolean m_useImplicitFTPS;
    private final SettingsModelBoolean m_reuseSSLSession;

    FtpConnectorNodeSettings() {
        m_host = new SettingsModelString(KEY_HOST, "localhost");
        m_port = new SettingsModelIntegerBounded(KEY_PORT, 21, 1, 65535);
        m_connectionTimeout = new SettingsModelIntegerBounded(KEY_CONNECTION_TIMEOUT, DEFAULT_TIMEOUT,
                0,
                Integer.MAX_VALUE);
        m_readTimeout = new SettingsModelIntegerBounded(KEY_READ_TIMEOUT, DEFAULT_TIMEOUT, 0, Integer.MAX_VALUE);
        m_minConnections = new SettingsModelIntegerBounded(KEY_MIN_POOL_SIZE,
                FtpFSConnectionConfig.DEFAULT_MIN_CONNECTIONS, 1, Integer.MAX_VALUE);
        m_maxConnections = new SettingsModelIntegerBounded(KEY_MAX_POOL_SIZE,
                FtpFSConnectionConfig.DEFAULT_MAX_CONNECTIONS, 1, Integer.MAX_VALUE);
        m_timeZoneOffset = new SettingsModelIntegerBounded(KEY_TIME_ZONE_OFFSET, 0, (int) -TimeUnit.HOURS.toMinutes(24),
                (int) TimeUnit.HOURS.toMinutes(24));
        m_useProxy = new SettingsModelBoolean(KEY_USE_PROXY, DEFAULT_USE_PROXY);
        m_useFTPS = new SettingsModelBoolean(KEY_USE_FTPS, false);
        m_authSettings = new AuthSettings.Builder() //
                .add(new UserPasswordAuthProviderSettings(StandardAuthTypes.USER_PASSWORD, true)) //
                .add(new EmptyAuthProviderSettings(StandardAuthTypes.ANONYMOUS)) //
                .defaultType(StandardAuthTypes.USER_PASSWORD) //
                .build();
        m_workingDirectory = new SettingsModelString(KEY_WORKING_DIRECTORY, FtpFileSystem.PATH_SEPARATOR);
        m_verifyHostname = new SettingsModelBoolean(KEY_VERIFY_HOSTNAME, DEFAULT_VERIFY_HOSTNAME);
        m_useImplicitFTPS = new SettingsModelBoolean(KEY_USE_IMPLICIT_FTPS, DEFAULT_USE_IMPLICIT_FTPS);
        m_reuseSSLSession = new SettingsModelBoolean(KEY_REUSE_SSL_SESSION, DEFAULT_REUSE_SSL_SESSION);

        m_useFTPS.addChangeListener(e -> updateEnabledness());
        updateEnabledness();
    }

    private void updateEnabledness() {
        m_useProxy.setEnabled(!m_useFTPS.getBooleanValue());
        m_verifyHostname.setEnabled(m_useFTPS.getBooleanValue());
        m_useImplicitFTPS.setEnabled(m_useFTPS.getBooleanValue());
        m_reuseSSLSession.setEnabled(m_useFTPS.getBooleanValue());
    }

    private void save(final NodeSettingsWO settings) {
        m_host.saveSettingsTo(settings);
        m_port.saveSettingsTo(settings);
        m_workingDirectory.saveSettingsTo(settings);
        m_connectionTimeout.saveSettingsTo(settings);
        m_readTimeout.saveSettingsTo(settings);
        m_minConnections.saveSettingsTo(settings);
        m_maxConnections.saveSettingsTo(settings);
        m_useProxy.saveSettingsTo(settings);
        m_useFTPS.saveSettingsTo(settings);
        m_timeZoneOffset.saveSettingsTo(settings);
        m_verifyHostname.saveSettingsTo(settings);
        m_useImplicitFTPS.saveSettingsTo(settings);
        m_reuseSSLSession.saveSettingsTo(settings);
    }

    /**
     * Saves settings to the given {@link NodeSettingsWO} (to be called by the node
     * dialog).
     *
     * @param settings
     */
    public void saveSettingsForDialog(final NodeSettingsWO settings) {
        save(settings);
        // m_authSettings are also saved by AuthenticationDialog
    }

    /**
     * Saves settings to the given {@link NodeSettingsWO} (to be called by the node
     * model).
     *
     * @param settings
     */
    public void saveSettingsForModel(final NodeSettingsWO settings) {
        save(settings);
        m_authSettings.saveSettingsForModel(settings.addNodeSettings(AuthSettings.KEY_AUTH));
    }

    private void load(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_host.loadSettingsFrom(settings);
        m_port.loadSettingsFrom(settings);
        m_workingDirectory.loadSettingsFrom(settings);
        m_connectionTimeout.loadSettingsFrom(settings);
        m_readTimeout.loadSettingsFrom(settings);
        m_minConnections.loadSettingsFrom(settings);
        m_maxConnections.loadSettingsFrom(settings);
        m_useFTPS.loadSettingsFrom(settings);
        m_timeZoneOffset.loadSettingsFrom(settings);
    }

    /**
     * Loads settings from the given {@link NodeSettingsRO} (to be called by the
     * node dialog).
     *
     * @param settings
     * @throws InvalidSettingsException
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings) throws InvalidSettingsException {
        load(settings);
        // m_authSettings and settings whose enabledness depends on m_useFTPS are loaded
        // by the dialog
    }

    /**
     * Loads settings from the given {@link NodeSettingsRO} (to be called by the
     * node model).
     *
     * @param settings
     * @throws InvalidSettingsException
     */
    public void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        load(settings);
        m_authSettings.loadSettingsForModel(settings.getNodeSettings(AuthSettings.KEY_AUTH));

        m_useProxy.loadSettingsFrom(settings);
        if (containsAdvancedFTPSettings(settings)) {
            m_verifyHostname.loadSettingsFrom(settings);
            m_useImplicitFTPS.loadSettingsFrom(settings);
            m_reuseSSLSession.loadSettingsFrom(settings);
        } else {
            m_verifyHostname.setBooleanValue(DEFAULT_VERIFY_HOSTNAME);
            m_useImplicitFTPS.setBooleanValue(DEFAULT_USE_IMPLICIT_FTPS);
            m_reuseSSLSession.setBooleanValue(DEFAULT_REUSE_SSL_SESSION);
        }

    }

    void configureInModel(final PortObjectSpec[] inSpecs, final Consumer<StatusMessage> statusConsumer,
            final CredentialsProvider credentialsProvider)
            throws InvalidSettingsException {
        m_authSettings.configureInModel(inSpecs, statusConsumer, credentialsProvider);
    }

    /**
     * Validates the settings in the given {@link NodeSettingsRO}.
     *
     * @param settings
     * @throws InvalidSettingsException
     */
    public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_host.validateSettings(settings);
        m_port.validateSettings(settings);
        m_authSettings.validateSettings(settings.getNodeSettings(AuthSettings.KEY_AUTH));
        m_workingDirectory.validateSettings(settings);
        m_connectionTimeout.validateSettings(settings);
        m_readTimeout.validateSettings(settings);
        m_minConnections.validateSettings(settings);
        m_maxConnections.validateSettings(settings);
        m_useProxy.validateSettings(settings);
        m_useFTPS.validateSettings(settings);
        m_timeZoneOffset.validateSettings(settings);

        if (containsAdvancedFTPSettings(settings)) {
            m_verifyHostname.validateSettings(settings);
            m_useImplicitFTPS.validateSettings(settings);
            m_reuseSSLSession.validateSettings(settings);
        }
    }

    /**
     * Validates the current settings.
     *
     * @throws InvalidSettingsException
     */
    public void validate() throws InvalidSettingsException {
        if (isEmpty(getHost())) {
            throw new InvalidSettingsException("Host must be specified.");
        }

        if (getPort() < 1 || getPort() > 65535) {
            throw new InvalidSettingsException("Port must be between 1 and 65535.");
        }

        if (isEmpty(getWorkingDirectory())) {
            throw new InvalidSettingsException("Working directory must be specified.");
        }

        if (getMinConnections() > getMaxConnections()) {
            throw new InvalidSettingsException(
                    "Minimum number of FTP connections must be less or equal to maximum number of FTP connections");
        }

        getAuthenticationSettings().validate();
    }

    static boolean isEmpty(final String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * @return working directory.
     */
    public String getWorkingDirectory() {
        return m_workingDirectory.getStringValue();
    }

    /**
     * @return the settings model for the working directory.
     */
    public SettingsModelString getWorkingDirectoryModel() {
        return m_workingDirectory;
    }

    /**
     * @return remote port.
     */
    public int getPort() {
        return m_port.getIntValue();
    }
    /**
     * @return remote host.
     */
    public String getHost() {
        return m_host.getStringValue();
    }

    /**
     * @return connection time out.
     */
    public Duration getConnectionTimeout() {
        return Duration.ofSeconds(m_connectionTimeout.getIntValue());
    }

    /**
     * @return socket read time out.
     */
    public Duration getReadTimeout() {
        return Duration.ofSeconds(m_readTimeout.getIntValue());
    }

    /**
     * @return host settings model.
     */
    public SettingsModelString getHostModel() {
        return m_host;
    }

    /**
     * @return port settings model.
     */
    public SettingsModelIntegerBounded getPortModel() {
        return m_port;
    }

    /**
     * @return connection time out settings model.
     */
    public SettingsModelIntegerBounded getConnectionTimeoutModel() {
        return m_connectionTimeout;
    }

    /**
     * @return read time out settings model.
     */
    public SettingsModelIntegerBounded getReadTimeoutModel() {
        return m_readTimeout;
    }

    /**
     * @return authentication settings.
     */
    public AuthSettings getAuthenticationSettings() {
        return m_authSettings;
    }

    /**
     * @return maximum number of FTP connections.
     */
    public int getMaxConnections() {
        return m_maxConnections.getIntValue();
    }

    /**
     * @return settings model of maximum number of FTP connections.
     */
    public SettingsModelIntegerBounded getMaxConnectionsModel() {
        return m_maxConnections;
    }

    /**
     * @return minimum number of FTP connections.
     */
    public int getMinConnections() {
        return m_minConnections.getIntValue();
    }

    /**
     * @return settings model of minimum number of FTP connections.
     */
    public SettingsModelIntegerBounded getMinConnectionsModel() {
        return m_minConnections;
    }

    /**
     * @return true if uses proxy
     */
    public boolean isUseProxy() {
        return !isUseFTPS() && m_useProxy.getBooleanValue();
    }

    /**
     * @return the use proxy settings.
     */
    public SettingsModelBoolean getUseProxyModel() {
        return m_useProxy;
    }

    /**
     * @return true if use FTPS.
     */
    public boolean isUseFTPS() {
        return m_useFTPS.getBooleanValue();
    }

    /**
     * @return use FTPS model.
     */
    public SettingsModelBoolean getUseFTPSModel() {
        return m_useFTPS;
    }

    /**
     * @return time zone offset in minutes.
     */
    public Duration getTimeZoneOffset() {
        return Duration.ofMinutes(m_timeZoneOffset.getIntValue());
    }

    /**
     * @return time zone offset model
     */
    public SettingsModelIntegerBounded getTimeZoneOffsetModel() {
        return m_timeZoneOffset;
    }

    /**
     * @return true if hostname should be verified.
     */
    public boolean isVerifyHostname() {
        return m_verifyHostname.getBooleanValue();
    }

    /**
     * @return the verify hostname model.
     */
    public SettingsModelBoolean getVerifyHostnameModel() {
        return m_verifyHostname;
    }

    /**
     * @return true if use implicit FTPS.
     */
    public boolean isUseImplicitFTPS() {
        return m_useImplicitFTPS.getBooleanValue();
    }

    /**
     * @return the useImplicitFTPS model.
     */
    public SettingsModelBoolean getUseImplicitFTPSModel() {
        return m_useImplicitFTPS;
    }

    /**
     * @return true if SSL session should be reused.
     */
    public boolean isReuseSSLSession() {
        return m_reuseSSLSession.getBooleanValue();
    }

    /**
     * @return the reuseSSLSession model.
     */
    public SettingsModelBoolean getReuseSSLSessionModel() {
        return m_reuseSSLSession;
    }

    /**
     * @return a (deep) clone of this node settings object.
     */
    public FtpConnectorNodeSettings createClone() {
        final NodeSettings tempSettings = new NodeSettings("ignored");
        saveSettingsForModel(tempSettings);

        final FtpConnectorNodeSettings toReturn = new FtpConnectorNodeSettings();
        try {
            toReturn.loadSettingsForModel(tempSettings);
        } catch (InvalidSettingsException ex) { // NOSONAR can never happen
            // won't happen
        }
        return toReturn;
    }

    /**
     * Convert setting into a {@link FtpFSConnectionConfig} instance.
     *
     * @param credentialsProvider
     *            credentials provider.
     * @return FTP connection configuration.
     * @throws InvalidSettingsException
     */
    public FtpFSConnectionConfig toFSConnectionConfig(final CredentialsProvider credentialsProvider)
            throws InvalidSettingsException {
        final var conf = new FtpFSConnectionConfig();
        conf.setHost(getHost());
        conf.setPort(getPort());
        conf.setMaxConnectionPoolSize(getMaxConnections());
        conf.setMinConnectionPoolSize(getMinConnections());
        conf.setCoreConnectionPoolSize((getMinConnections() + getMaxConnections()) / 2);
        conf.setConnectionTimeOut(getConnectionTimeout());
        conf.setReadTimeout(getReadTimeout());
        conf.setServerTimeZoneOffset(getTimeZoneOffset());
        conf.setUseFTPS(isUseFTPS());
        conf.setWorkingDirectory(getWorkingDirectory());
        conf.setVerifyHostname(isVerifyHostname());
        conf.setUseImplicitFTPS(isUseImplicitFTPS());
        conf.setReuseSSLSession(isReuseSSLSession());

        // authentication
        final var auth = getAuthenticationSettings();
        if (auth.getAuthType() == StandardAuthTypes.USER_PASSWORD) {
            final UserPasswordAuthProviderSettings userPassSettings = auth
                    .getSettingsForAuthType(StandardAuthTypes.USER_PASSWORD);
            conf.setUser(userPassSettings.getUser(credentialsProvider::get));
            conf.setPassword(userPassSettings.getPassword(credentialsProvider::get));
        } else {
            conf.setUser("anonymous");
            conf.setPassword("");
        }

        // Proxy
        if (isUseProxy()) {
            final var proxy = new ProtectedHostConfiguration();
            IProxyData proxyData = Activator.getProxyService().getProxyData(IProxyData.HTTP_PROXY_TYPE);
            if (proxyData == null) {
                throw new InvalidSettingsException("Eclipse HTTP proxy is not configured");
            }

            proxy.setHost(proxyData.getHost());
            proxy.setPort(proxyData.getPort());
            proxy.setUser(proxyData.getUserId());
            proxy.setPassword(proxyData.getPassword());

            conf.setProxy(proxy);
        }

        return conf;
    }

    static boolean containsAdvancedFTPSettings(final NodeSettingsRO settings) {
        return settings.containsKey(KEY_VERIFY_HOSTNAME);
    }

}
