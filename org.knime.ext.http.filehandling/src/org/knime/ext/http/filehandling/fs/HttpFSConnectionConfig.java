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
 *   2020-11-20 (Bjoern Lohrmann, KNIME GmbH): created
 */
package org.knime.ext.http.filehandling.fs;

import java.net.URI;
import java.time.Duration;
import java.util.Locale;

import org.knime.filehandling.core.connections.DefaultFSLocationSpec;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.connections.meta.FSConnectionConfig;

/**
 * {@link FSConnectionConfig} for the HTTP(S) file system.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public class HttpFSConnectionConfig implements FSConnectionConfig {

    /**
     * Default timeout to use in seconds.
     */
    public static final int DEFAULT_TIMEOUT_SECONDS = 30;

    private final String m_url;

    private boolean m_sslIgnoreHostnameMismatches = false;

    private boolean m_sslTrustAllCertificates = false;

    private Auth m_authType;

    private String m_username = null;

    private String m_password = null;

    private Duration m_connectionTimeout = Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS);

    private Duration m_readTimeout = Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS);

    private boolean m_followRedirects = true;


    /**
     * How to authenticate.
     */
    public enum Auth {
        /**
         * No authentication.
         */
        NONE, //

        /**
         * HTTP Basic authentication.
         */
        BASIC;
    }

    /**
     * Creates a new instance.
     *
     * @param url
     *            The HTTP base URL.
     */
    public HttpFSConnectionConfig(final String url) {
        m_url = url;
    }

    /**
     * @return whether to ignore ssl hostname mismatches or not.
     */
    public boolean sslIgnoreHostnameMismatches() {
        return m_sslIgnoreHostnameMismatches;
    }

    /**
     * @param sslIgnoreHostnameMismatches
     *            whether to ignore ssl hostname mismatches or not.
     */
    public void setSslIgnoreHostnameMismatches(final boolean sslIgnoreHostnameMismatches) {
        m_sslIgnoreHostnameMismatches = sslIgnoreHostnameMismatches;
    }

    /**
     * @return whether to trust all SSL certificates.
     */
    public boolean isSslTrustAllCertificates() {
        return m_sslTrustAllCertificates;
    }

    /**
     * @param sslTrustAllCertificates
     *            whether to trust all SSL certificates.
     */
    public void setSslTrustAllCertificates(final boolean sslTrustAllCertificates) {
        m_sslTrustAllCertificates = sslTrustAllCertificates;
    }

    /**
     * @return the type if authentication to perform.
     */
    public Auth getAuthType() {
        return m_authType;
    }

    /**
     * @param authType
     *            the type if authentication to perform.
     */
    public void setAuthType(final Auth authType) {
        m_authType = authType;
    }

    /**
     * @return the username
     */
    public String getUsername() {
        return m_username;
    }

    /**
     * @param username
     *            the username to set
     */
    public void setUsername(final String username) {
        m_username = username;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return m_password;
    }

    /**
     * @param password
     *            the password to set
     */
    public void setPassword(final String password) {
        m_password = password;
    }

    /**
     * @return the connection timeout
     */
    public Duration getConnectionTimeout() {
        return m_connectionTimeout;
    }

    /**
     * @param connectionTimeout
     *            the connection timeout to set
     */
    public void setConnectionTimeout(final Duration connectionTimeout) {
        m_connectionTimeout = connectionTimeout;
    }

    /**
     * @return the read timeout
     */
    public Duration getReadTimeout() {
        return m_readTimeout;
    }

    /**
     * @param readTimeout
     *            the read timeout to set
     */
    public void setReadTimeout(final Duration readTimeout) {
        m_readTimeout = readTimeout;
    }

    /**
     * @return whether to follow redirects.
     */
    public boolean isFollowRedirects() {
        return m_followRedirects;
    }

    /**
     * @param followRedirects
     *            Whether to follow redirects.
     */
    public void setFollowRedirects(final boolean followRedirects) {
        m_followRedirects = followRedirects;
    }

    /**
     * @return the HTTP base URL.
     */
    public String getUrl() {
        return m_url;
    }

    /**
     * @return the {@link FSLocationSpec} for an HTTP file system.
     */
    public DefaultFSLocationSpec createFSLocationSpec() {
        final String host = URI.create(getUrl()).getHost().toLowerCase(Locale.ENGLISH);
        return new DefaultFSLocationSpec(FSCategory.CONNECTED, HttpFSDescriptorProvider.FS_TYPE + ":" + host);
    }
}
