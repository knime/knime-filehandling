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
 *   2020-11-18 (Bjoern Lohrmann): created
 */
package org.knime.ext.http.filehandling.node;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.base.auth.AuthSettings;
import org.knime.filehandling.core.connections.base.auth.EmptyAuthProviderSettings;
import org.knime.filehandling.core.connections.base.auth.UserPasswordAuthProviderSettings;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;

/**
 * Settings for {@link HttpConnectorNodeModel}.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public class HttpConnectorNodeSettings {

    private static final String KEY_URL = "url";

    private static final String KEY_SSL_IGNORE_HOSTNAME_MISMATCHES = "sslIgnoreHostnameMismatches";

    private static final String KEY_SSL_TRUST_ALL_CERTIFICATES = "sslTrustAllCertificates";

    private static final String KEY_CONNECTION_TIMEOUT = "connectionTimeout";

    private static final String KEY_READ_TIMEOUT = "readTimeout";

    private static final String KEY_FOLLOW_REDIRECTS = "followRedirects";

    /**
     * Default timeout to use in seconds.
     */
    public static final int DEFAULT_TIMEOUT = 30;

    private final SettingsModelString m_url;
    private final SettingsModelBoolean m_sslIgnoreHostnameMismatches;
    private final SettingsModelBoolean m_sslTrustAllCertificates;
    private final AuthSettings m_authSettings;
    private final SettingsModelIntegerBounded m_connectionTimeout;
    private final SettingsModelIntegerBounded m_readTimeout;
    private final SettingsModelBoolean m_followRedirects;

    /**
     * Constructor.
     */
    public HttpConnectorNodeSettings() {
        m_url = new SettingsModelString(KEY_URL, "");
        m_sslIgnoreHostnameMismatches = new SettingsModelBoolean(KEY_SSL_IGNORE_HOSTNAME_MISMATCHES, false);
        m_sslTrustAllCertificates = new SettingsModelBoolean(KEY_SSL_TRUST_ALL_CERTIFICATES, false);

        m_authSettings = new AuthSettings.Builder() //
                .add(new UserPasswordAuthProviderSettings(HttpAuth.BASIC, true)) //
                .add(new EmptyAuthProviderSettings(HttpAuth.NONE)) //
                .defaultType(HttpAuth.NONE) //
                .build();

        m_connectionTimeout = new SettingsModelIntegerBounded(KEY_CONNECTION_TIMEOUT, DEFAULT_TIMEOUT, 0,
                Integer.MAX_VALUE);
        m_readTimeout = new SettingsModelIntegerBounded(KEY_READ_TIMEOUT, DEFAULT_TIMEOUT, 0, Integer.MAX_VALUE);
        m_followRedirects = new SettingsModelBoolean(KEY_FOLLOW_REDIRECTS, true);
    }

    private void save(final NodeSettingsWO settings) {
        m_url.saveSettingsTo(settings);
        m_sslIgnoreHostnameMismatches.saveSettingsTo(settings);
        m_sslTrustAllCertificates.saveSettingsTo(settings);
        m_connectionTimeout.saveSettingsTo(settings);
        m_readTimeout.saveSettingsTo(settings);
        m_followRedirects.saveSettingsTo(settings);
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
        m_url.loadSettingsFrom(settings);
        m_sslIgnoreHostnameMismatches.loadSettingsFrom(settings);
        m_sslTrustAllCertificates.loadSettingsFrom(settings);
        m_connectionTimeout.loadSettingsFrom(settings);
        m_readTimeout.loadSettingsFrom(settings);
        m_followRedirects.loadSettingsFrom(settings);
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
        m_authSettings.loadSettingsForModel(settings.getNodeSettings(AuthSettings.KEY_AUTH));
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
        m_url.validateSettings(settings);
        m_sslIgnoreHostnameMismatches.validateSettings(settings);
        m_sslTrustAllCertificates.validateSettings(settings);
        m_authSettings.validateSettings(settings.getNodeSettings(AuthSettings.KEY_AUTH));
        m_connectionTimeout.validateSettings(settings);
        m_readTimeout.validateSettings(settings);
        m_followRedirects.validateSettings(settings);
    }

    /**
     * Validates the current settings.
     *
     * @throws InvalidSettingsException
     */
    public void validate() throws InvalidSettingsException {
        if (isEmpty(m_url.getStringValue())) {
            throw new InvalidSettingsException("URL must be specified.");
        }
        try {
            final URI url = new URI(m_url.getStringValue());

            final String scheme = url.getScheme();
            if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                throw new InvalidSettingsException("URL must specify http or https as scheme.");
            }

            if (StringUtils.isBlank(url.getHost())) {
                throw new InvalidSettingsException("URL must specify a host to connect to.");
            }

            if (!StringUtils.isBlank(url.getQuery()) || !StringUtils.isBlank(url.getFragment())) {
                throw new InvalidSettingsException(
                        "URL must not specify a query (indicated by '?') or fragment (indicated by '#').");
            }
        } catch (URISyntaxException ex) {
            throw new InvalidSettingsException("Invalid URL: " + ex.getMessage(), ex);
        }

        getAuthenticationSettings().validate();
    }

    static boolean isEmpty(final String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * @return the url
     */
    public String getUrl() {
        return m_url.getStringValue();
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
    public SettingsModelString getUrlModel() {
        return m_url;
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
     * @return settings model for whether to ignore SSL certificate hostname
     *         mismatches or not.
     */
    public SettingsModelBoolean getSslIgnoreHostnameMismatchesModel() {
        return m_sslIgnoreHostnameMismatches;
    }

    /**
     * @return whether to ignore SSL hostname mismatches or not.
     */
    public boolean sslIgnoreHostnameMismatches() {
        return m_sslIgnoreHostnameMismatches.getBooleanValue();
    }

    /**
     * @return settings model for whether to trust all SSL certificate.
     */
    public SettingsModelBoolean getSslTrustAllCertificatesModel() {
        return m_sslTrustAllCertificates;
    }

    /**
     * @return whether to trust all SSL certificates.
     */
    public boolean sslTrustAllCertificates() {
        return m_sslTrustAllCertificates.getBooleanValue();
    }

    /**
     * @return settings model for whether to follow HTTP redirects or not.
     */
    public SettingsModelBoolean getFollowRedirectsModel() {
        return m_followRedirects;
    }

    /**
     * @return whether to follow HTTP redirects or not.
     */
    public boolean followRedirects() {
        return m_followRedirects.getBooleanValue();
    }

    /**
     * @return a (deep) clone of this node settings object.
     */
    public HttpConnectorNodeSettings createClone() {
        final NodeSettings tempSettings = new NodeSettings("ignored");
        saveSettingsForModel(tempSettings);

        final HttpConnectorNodeSettings toReturn = new HttpConnectorNodeSettings();
        try {
            toReturn.loadSettingsForModel(tempSettings);
        } catch (InvalidSettingsException ex) { // NOSONAR can never happen
            // won't happen
        }
        return toReturn;
    }
}
