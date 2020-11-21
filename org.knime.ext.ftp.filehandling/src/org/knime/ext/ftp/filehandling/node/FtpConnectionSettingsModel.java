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

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.ext.ftp.filehandling.fs.FtpConnectionConfiguration;
import org.knime.ext.ftp.filehandling.fs.FtpFileSystem;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;

/**
 * Settings for {@link FtpConnectionNodeModel}.
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
public class FtpConnectionSettingsModel {

    /**
     * Settings key for the authentication sub-settings. Must be public for dialog.
     */
    public static final String KEY_AUTH = "auth";

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

    private final SettingsModelString m_host;
    private final SettingsModelIntegerBounded m_port;
    private final FtpAuthenticationSettingsModel m_authSettings;
    private final SettingsModelString m_workingDirectory;
    private final SettingsModelIntegerBounded m_connectionTimeout;
    private final SettingsModelIntegerBounded m_readTimeout;
    private final SettingsModelIntegerBounded m_minConnections;
    private final SettingsModelIntegerBounded m_maxConnections;
    private final SettingsModelIntegerBounded m_timeZoneOffset;
    private final SettingsModelBoolean m_useProxy;
    private final SettingsModelBoolean m_useFTPS;

    /**
     */
    public FtpConnectionSettingsModel() {
        m_host = new SettingsModelString(KEY_HOST, "localhost");
        m_port = new SettingsModelIntegerBounded(KEY_PORT, 21, 1, 65535);
        m_connectionTimeout = new SettingsModelIntegerBounded(KEY_CONNECTION_TIMEOUT, DEFAULT_TIMEOUT,
                0,
                Integer.MAX_VALUE);
        m_readTimeout = new SettingsModelIntegerBounded(KEY_READ_TIMEOUT, DEFAULT_TIMEOUT, 0, Integer.MAX_VALUE);
        m_minConnections = new SettingsModelIntegerBounded(KEY_MIN_POOL_SIZE,
                FtpConnectionConfiguration.DEFAULT_MIN_CONNECTIONS, 1, Integer.MAX_VALUE);
        m_maxConnections = new SettingsModelIntegerBounded(KEY_MAX_POOL_SIZE,
                FtpConnectionConfiguration.DEFAULT_MAX_CONNECTIONS, 1, Integer.MAX_VALUE);
        m_timeZoneOffset = new SettingsModelIntegerBounded(KEY_TIME_ZONE_OFFSET, 0, (int) -TimeUnit.HOURS.toMinutes(24),
                (int) TimeUnit.HOURS.toMinutes(24));
        m_useProxy = new SettingsModelBoolean(KEY_USE_PROXY, false);
        m_useFTPS = new SettingsModelBoolean(KEY_USE_FTPS, false);
        m_authSettings = new FtpAuthenticationSettingsModel();
        m_workingDirectory = new SettingsModelString(KEY_WORKING_DIRECTORY, FtpFileSystem.PATH_SEPARATOR);
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
        m_authSettings.saveSettingsForModel(settings.addNodeSettings(KEY_AUTH));
    }

    private void load(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_host.loadSettingsFrom(settings);
        m_port.loadSettingsFrom(settings);
        m_workingDirectory.loadSettingsFrom(settings);
        m_connectionTimeout.loadSettingsFrom(settings);
        m_readTimeout.loadSettingsFrom(settings);
        m_minConnections.loadSettingsFrom(settings);
        m_maxConnections.loadSettingsFrom(settings);
        m_useProxy.loadSettingsFrom(settings);
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
        // m_authSettings are loaded by AuthenticationDialog
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
        m_authSettings.loadSettingsForModel(settings.getNodeSettings(KEY_AUTH));
    }

    /**
     * Forwards the given {@link PortObjectSpec} and status message consumer to the
     * file chooser settings models to they can configure themselves properly.
     *
     * @param inSpecs
     *            input specifications.
     * @param statusConsumer
     *            status consumer.
     * @throws InvalidSettingsException
     */
    public void configureInModel(final PortObjectSpec[] inSpecs, final Consumer<StatusMessage> statusConsumer)
            throws InvalidSettingsException {
        // nothing
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
        m_authSettings.validateSettings(settings.getNodeSettings(KEY_AUTH));
        m_workingDirectory.validateSettings(settings);
        m_connectionTimeout.validateSettings(settings);
        m_readTimeout.validateSettings(settings);
        m_minConnections.validateSettings(settings);
        m_maxConnections.validateSettings(settings);
        m_useProxy.validateSettings(settings);
        m_useFTPS.validateSettings(settings);
        m_timeZoneOffset.validateSettings(settings);

        final FtpConnectionSettingsModel temp = new FtpConnectionSettingsModel();
        temp.loadSettingsForModel(settings);
        temp.validate();
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
     * @param location
     *            location to test.
     * @return true if the location is NULL location in fact.
     */
    static boolean isEmptyLocation(final FSLocation location) {
        return location == null || isEmpty(location.getPath());
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
    public FtpAuthenticationSettingsModel getAuthenticationSettings() {
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
        return m_useProxy.getBooleanValue();
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
     * @return a (deep) clone of this node settings object.
     */
    public FtpConnectionSettingsModel createClone() {
        final NodeSettings tempSettings = new NodeSettings("ignored");
        saveSettingsForModel(tempSettings);

        final FtpConnectionSettingsModel toReturn = new FtpConnectionSettingsModel();
        try {
            toReturn.loadSettingsForModel(tempSettings);
        } catch (InvalidSettingsException ex) { // NOSONAR can never happen
            // won't happen
        }
        return toReturn;
    }
}
