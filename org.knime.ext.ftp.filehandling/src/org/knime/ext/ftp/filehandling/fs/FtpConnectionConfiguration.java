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

/**
 * Connection configuration.
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
public class FtpConnectionConfiguration extends ProtectedHostConfiguration {
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
    public static final int DEFAULT_MAX_POOL_SIZE = 15;
    /**
     * Default connection timeout.
     */
    public static final Duration DEFAULT_CONNECTION_TIMEOUT = Duration.ofSeconds(60);
    /**
     * Minimal connection pools size.
     */
    public static final int DEFAULT_MIN_POOL_SIZE = 1;
    /**
     * Default max inactive time of resource between minimum pool size and core pool
     * size.
     */
    private static final long DEFAULT_MAX_IDLE_TIME = 10000l;

    private ProtectedHostConfiguration m_proxy;
    private int m_maxConnectionPoolSize = DEFAULT_MAX_POOL_SIZE;
    private int m_minConnectionPoolSize = DEFAULT_MIN_POOL_SIZE;
    private int m_coreConnectionPoolSize = (DEFAULT_MIN_POOL_SIZE + DEFAULT_MAX_POOL_SIZE) / 2;
    private long m_maxIdleTime = DEFAULT_MAX_IDLE_TIME;

    private Duration m_connectionTimeOut = DEFAULT_CONNECTION_TIMEOUT;
    private Duration m_serverTimeZoneOffset;
    private boolean m_testMode;
    private String m_workingDirectory = "/";
    private boolean m_useSsl;

    /**
     * Default constructor.
     */
    public FtpConnectionConfiguration() {
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
     * @param useSsl
     *            use SSL.
     */
    public void setUseSsl(final boolean useSsl) {
        m_useSsl = useSsl;
    }

    /**
     * @return true if should use SSL.
     */
    public boolean isUseSsl() {
        return m_useSsl;
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
