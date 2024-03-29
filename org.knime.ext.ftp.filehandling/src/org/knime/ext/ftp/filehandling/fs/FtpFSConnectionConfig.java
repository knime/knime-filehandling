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
 *   2020-09-30 (Vyacheslav Soldatov): created
 */
package org.knime.ext.ftp.filehandling.fs;

import java.time.Duration;

import org.knime.filehandling.core.connections.meta.FSConnectionConfig;

/**
 * Connection configuration.
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
public class FtpFSConnectionConfig extends ProtectedHostConfiguration implements FSConnectionConfig {
    /**
     * Anonymous user name.
     */
    public static final String ANONYMOUS_USER = "anonymous";
    /**
     * Default FTP port.
     */
    public static final int DEFAULT_FTP_PORT = 21;
    /**
     * Default connection time out.
     */
    public static final int DEFAULT_MAX_CONNECTIONS = 10;
    /**
     * Default connection timeout.
     */
    public static final Duration DEFAULT_CONNECTION_TIMEOUT = Duration.ofSeconds(60);

    /**
     * Default connection timeout.
     */
    public static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(60);

    /**
     * Minimal connection pools size.
     */
    public static final int DEFAULT_MIN_CONNECTIONS = 1;
    /**
     * Default max inactive time of resource between minimum pool size and core pool
     * size.
     */
    private static final long DEFAULT_MAX_IDLE_TIME = 20000l;

    private ProtectedHostConfiguration m_proxy;
    private int m_maxConnectionPoolSize = DEFAULT_MAX_CONNECTIONS;
    private int m_minConnectionPoolSize = DEFAULT_MIN_CONNECTIONS;
    private int m_coreConnectionPoolSize = (DEFAULT_MIN_CONNECTIONS + DEFAULT_MAX_CONNECTIONS) / 2;
    private long m_maxIdleTime = DEFAULT_MAX_IDLE_TIME;

    private Duration m_connectionTimeOut = DEFAULT_CONNECTION_TIMEOUT;
    private Duration m_readTimeout = DEFAULT_READ_TIMEOUT;
    private Duration m_serverTimeZoneOffset;
    private boolean m_testMode;
    private String m_workingDirectory = "/";
    private boolean m_useFTPS;
    private boolean m_verifyHostname;
    private boolean m_useImplicitFTPS;
    private boolean m_reuseSSLSession;

    /**
     * Default constructor.
     */
    public FtpFSConnectionConfig() {
        super();
        setPort(DEFAULT_FTP_PORT);
        setUser(ANONYMOUS_USER);
    }

    /**
     * @return Proxy settings.
     */
    public ProtectedHostConfiguration getProxy() {
        return m_proxy;
    }

    /**
     * @param proxy
     *            Proxy configuration.
     */
    public void setProxy(final ProtectedHostConfiguration proxy) {
        m_proxy = proxy;
    }

    /**
     * @return max number of opened connections.
     */
    public int getMaxConnectionPoolSize() {
        return m_maxConnectionPoolSize;
    }

    /**
     * @param limit
     *            max number of opened connections.
     */
    public void setMaxConnectionPoolSize(final int limit) {
        m_maxConnectionPoolSize = limit;
    }

    /**
     * @return minimal connection pool size.
     */
    public int getMinConnectionPoolSize() {
        return m_minConnectionPoolSize;
    }

    /**
     * @param size
     *            minimal connection pool size.
     */
    public void setMinConnectionPoolSize(final int size) {
        m_minConnectionPoolSize = size;
    }

    /**
     * @return core connection pools size.
     */
    public int getCoreConnectionPoolSize() {
        return m_coreConnectionPoolSize;
    }

    /**
     * @param size
     *            core connection pools size.
     */
    public void setCoreConnectionPoolSize(final int size) {
        m_coreConnectionPoolSize = size;
    }

    /**
     * @return max inactive time of resource between minimum pool size and core pool
     *         size.
     */
    public long getMaxIdleTime() {
        return m_maxIdleTime;
    }

    /**
     * @param maxIdleTime
     *            max inactive time of resource between minimum pool size and core
     *            pool size.
     */
    public void setMaxIdleTime(final long maxIdleTime) {
        m_maxIdleTime = maxIdleTime;
    }

    /**
     * @return connection time out.
     */
    public Duration getConnectionTimeOut() {
        return m_connectionTimeOut;
    }

    /**
     * @param duration
     *            connection time out.
     */
    public void setConnectionTimeOut(final Duration duration) {
        m_connectionTimeOut = duration;
    }

    /**
     * @param readTimeout
     *            The socket read timeout.
     */
    public void setReadTimeout(final Duration readTimeout) {
        m_readTimeout = readTimeout;
    }

    /**
     * @return socket read time out.
     */
    public Duration getReadTimeout() {
        return m_readTimeout;
    }

    /**
     * @return FTP server time zone offset.
     */
    public Duration getServerTimeZoneOffset() {
        return m_serverTimeZoneOffset;
    }

    /**
     * @param offset
     *            FTP server time zone offset.
     */
    public void setServerTimeZoneOffset(final Duration offset) {
        m_serverTimeZoneOffset = offset;
    }

    /**
     * @param useFTPS
     *            use FTPS
     */
    public void setUseFTPS(final boolean useFTPS) {
        m_useFTPS = useFTPS;
    }

    /**
     * @return true if should use FTPS.
     */
    public boolean isUseFTPS() {
        return m_useFTPS;
    }

    /**
     * @return file system working directory.
     */
    public String getWorkingDirectory() {
        return m_workingDirectory;
    }

    /**
     * @param dir
     *            file system working directory.
     */
    public void setWorkingDirectory(final String dir) {
        m_workingDirectory = dir;
    }

    /**
     * @return true if hostname should be verified
     */
    public boolean isVerifyHostname() {
        return m_verifyHostname;
    }

    /**
     * @param verifyHostname
     *            the verifyHostname to set
     */
    public void setVerifyHostname(final boolean verifyHostname) {
        m_verifyHostname = verifyHostname;
    }

    /**
     * @return true if implicit FTPS should be used
     */
    public boolean isUseImplicitFTPS() {
        return m_useImplicitFTPS;
    }

    /**
     * @param useImplicitFTPS
     *            the useImplicitFTPS to set
     */
    public void setUseImplicitFTPS(final boolean useImplicitFTPS) {
        m_useImplicitFTPS = useImplicitFTPS;
    }

    /**
     * @return true if SSL session should be reused
     */
    public boolean isReuseSSLSession() {
        return m_reuseSSLSession;
    }

    /**
     * @param reuseSSLSession
     *            the reuseSSLSession to set
     */
    public void setReuseSSLSession(final boolean reuseSSLSession) {
        m_reuseSSLSession = reuseSSLSession;
    }

    /**
     * @param testMode
     *            sets the test mode.
     */
    public void setTestMode(final boolean testMode) {
        m_testMode = testMode;
    }

    /**
     * @return whether or not is thee test mode.
     */
    public boolean isTestMode() {
        return m_testMode;
    }
}
