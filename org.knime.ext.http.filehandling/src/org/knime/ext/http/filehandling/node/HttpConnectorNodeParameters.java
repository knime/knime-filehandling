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
 * ------------------------------------------------------------------------
 */

package org.knime.ext.http.filehandling.node;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersInputImpl;
import org.knime.core.webui.node.dialog.defaultdialog.setting.credentials.LegacyCredentials;
import org.knime.core.webui.node.dialog.defaultdialog.setting.credentials.LegacyCredentialsAuthProviderSettings;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.custom.CustomValidation;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.custom.CustomValidationProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.custom.SimpleValidation;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.custom.ValidationCallback;
import org.knime.ext.http.filehandling.fs.HttpFSConnectionConfig;
import org.knime.ext.http.filehandling.fs.HttpFSConnectionConfig.Auth;
import org.knime.ext.http.filehandling.node.HttpConnectorNodeParameters.AuthenticationParameters.AuthenticationMethod;
import org.knime.filehandling.core.connections.base.auth.UserPasswordAuthProviderSettings;
import org.knime.node.parameters.Advanced;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.migration.Migration;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.credentials.Credentials;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsNonNegativeValidation;

/**
 * Node parameters for HTTP(S) Connector.
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class HttpConnectorNodeParameters implements NodeParameters {

    // ----- LAYOUTS -----

    @Section(title = "Connection")
    interface ConnectionSection {
    }

    @Section(title = "Authentication")
    @After(ConnectionSection.class)
    interface AuthenticationSection {
    }

    @Section(title = "Timeouts")
    @After(AuthenticationSection.class)
    @Advanced
    interface TimeoutsSection {
    }

    // ----- SETTINGS PARAMETERS -----

    @Layout(ConnectionSection.class)
    @Widget(title = "URL", description = """
            Base URL that specifies the <i>protocol</i> (<tt>http</tt> or <tt>https</tt>), a <i>host</i>, an
            optional <i>port</i> as well as an optional <i>path</i>, which will be used as the <i>working
            directory</i> of the file system connection.
            Example: <tt>https://hub.knime.com/knime/extensions</tt>.
            The working directory allows downstream nodes to access files using
            <i>relative</i> paths, i.e. paths that do not have a leading slash.
            If no path is specified in the URL, then the working directory is
            assumed to be <tt>/</tt>.""")
    @CustomValidation(UrlValidator.class)
    String m_url = "https://localhost";

    static class UrlValidator extends SimpleValidation<String> {

        @Override
        public void validate(final String currentValue) throws InvalidSettingsException {
            validateUrl(currentValue);
        }
    }

    @Layout(ConnectionSection.class)
    @Advanced
    @Widget(title = "Follow redirects", //
            description = "If checked, the node will follow redirects (HTTP status code <tt>3xx</tt>).")
    boolean m_followRedirects = true;

    @Layout(ConnectionSection.class)
    @Advanced
    @Widget(title = "Ignore hostname mismatches", description = """
            If checked, the node trusts the server's SSL certificate even if it was generated for a different
            host.""")
    boolean m_sslIgnoreHostnameMismatches;

    @Layout(ConnectionSection.class)
    @Advanced
    @Widget(title = "Trust all certificates", description = """
            If checked, the node trusts all SSL certificates regardless of their origin
            or expiration date.""")
    boolean m_sslTrustAllCertificates;

    @Layout(AuthenticationSection.class)
    @ValueReference(AuthenticationParametersRef.class)
    AuthenticationParameters m_auth = new AuthenticationParameters();

    static final class AuthenticationParametersRef implements ParameterReference<AuthenticationParameters> {
    }

    // ----- ADVANCED PARAMETERS -----

    @Layout(TimeoutsSection.class)
    @Widget(title = "Connection timeout", //
            description = "Timeout in seconds to establish a connection, or 0 for an infinite timeout.")
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    int m_connectionTimeout = HttpFSConnectionConfig.DEFAULT_TIMEOUT_SECONDS;

    @Layout(TimeoutsSection.class)
    @Widget(title = "Read timeout", description = """
            Timeout in seconds to read a server response from a connection,
            or 0 for an infinite timeout.""")
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    int m_readTimeout = HttpFSConnectionConfig.DEFAULT_TIMEOUT_SECONDS;

    // ----- INTERNAL PARAMETER CLASSES -----

    static final class AuthenticationParameters implements NodeParameters {

        static final String CFG_KEY = "auth";
        private static final String CFG_KEY_USER_PWD_V2 = "basic_v2";

        enum AuthenticationMethod {
            @Label(value = "Username & password", description = """
                    Authenticate with HTTP Basic authentication. Enter a <i>username</i> and <i>password</i>,
                    in which case the password will be persistently stored (in encrypted form) with the workflow
                    if not provided by a flow variable.""")
            USERNAME_PASSWORD,

            @Label(value = "None", description = "No authentication will be performed.")
            NONE
        }

        @Widget(title = "Authentication", description = "Method of authentication to use.")
        @ValueReference(AuthenticationMethodRef.class)
        @Persistor(AuthenticationMethodPersistor.class)
        AuthenticationMethod m_type = AuthenticationMethod.NONE;

        static final class AuthenticationMethodRef implements ParameterReference<AuthenticationMethod> {
        }

        static final class IsUserPwdAuth implements EffectPredicateProvider {
            @Override
            public EffectPredicate init(final PredicateInitializer i) {
                return i.getEnum(AuthenticationMethodRef.class).isOneOf(AuthenticationMethod.USERNAME_PASSWORD);
            }
        }

        static final class AuthenticationMethodPersistor implements NodeParametersPersistor<AuthenticationMethod> {

            private static final String VAL_BASIC = "basic";
            private static final String VAL_BASIC_V2 = "basic_v2";
            private static final String VAL_NONE = "none";
            private static final String KEY = "type";

            @Override
            public AuthenticationMethod load(final NodeSettingsRO settings) throws InvalidSettingsException {
                final var typeString = settings.getString(KEY, VAL_NONE);
                return switch (typeString) {
                case VAL_BASIC, VAL_BASIC_V2 -> AuthenticationMethod.USERNAME_PASSWORD;
                case VAL_NONE -> AuthenticationMethod.NONE;
                default -> throw new InvalidSettingsException(String.format( //
                        "Unknown authentication method: '%s'. Possible values: '%s', '%s'", typeString, VAL_BASIC_V2,
                        VAL_NONE));
                };
            }

            @Override
            public void save(final AuthenticationMethod param, final NodeSettingsWO settings) {
                switch (param) {
                case USERNAME_PASSWORD -> settings.addString(KEY, VAL_BASIC_V2);
                case NONE -> settings.addString(KEY, VAL_NONE);
                }
            }

            @Override
            public String[][] getConfigPaths() {
                return new String[][] { { KEY } };
            }
        }

        @Widget(title = "Username & password", //
                description = "Enter the username and password for HTTP Basic authentication.")
        @Effect(predicate = IsUserPwdAuth.class, type = EffectType.SHOW)
        @Persist(configKey = CFG_KEY_USER_PWD_V2)
        @Migration(LoadFromBasicAuthMigration.class)
        @CustomValidation(UserPasswordValidator.class)
        @ValueReference(UserPasswordRef.class)
        LegacyCredentials m_userPassword = new LegacyCredentials(new Credentials(System.getProperty("user.name"), ""));

        static final class LoadFromBasicAuthMigration
                extends LegacyCredentialsAuthProviderSettings.FromUserPasswordAuthProviderSettingsMigration {

            protected LoadFromBasicAuthMigration() {
                super(new UserPasswordAuthProviderSettings(HttpAuth.BASIC, true));
            }
        }

        static class UserPasswordValidator implements CustomValidationProvider<LegacyCredentials> {

            private Supplier<LegacyCredentials> m_userPassword;

            @Override
            public void init(final StateProviderInitializer initializer) {
                m_userPassword = initializer.computeFromValueSupplier(UserPasswordRef.class);
                initializer.computeAfterOpenDialog();
            }

            @Override
            public ValidationCallback<LegacyCredentials> computeValidationCallback(
                    final NodeParametersInput parametersInput) {

                final var creds = m_userPassword.get().toCredentials(getCredentialsProvider(parametersInput));
                final var valid = StringUtils.isNotBlank(creds.getUsername());
                return isNull -> CheckUtils.checkSetting(valid, "Username must be specified.");
            }
        }

        interface UserPasswordRef extends ParameterReference<LegacyCredentials> {
        }

        // for backwards compatibility
        NoneParameters m_none = new NoneParameters();

        static final class NoneParameters implements NodeParameters {
            // no-op
        }
    }

    void validateOnConfigure(final CredentialsProvider provider) throws InvalidSettingsException {
        validateUrl(m_url);

        if (m_auth.m_type == AuthenticationMethod.USERNAME_PASSWORD) {
            final var creds = m_auth.m_userPassword.toCredentials(provider);
            if (StringUtils.isBlank(creds.getUsername())) {
                throw new InvalidSettingsException("Username must be specified.");
            }
        }
    }

    private static void validateUrl(final String urlString) throws InvalidSettingsException {
        if (StringUtils.isEmpty(urlString)) {
            throw new InvalidSettingsException("URL must be specified.");
        }
        try {
            final var url = new URI(urlString);

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
    }

    private static CredentialsProvider getCredentialsProvider(final NodeParametersInput input) {
        return ((NodeParametersInputImpl) input).getCredentialsProvider().orElseThrow();
    }

    /**
     * Creates a {@link HttpFSConnectionConfig} that is based on the current
     * settings.
     *
     * @param credentialsProvider
     *            Supplies credentials flow variables if necessary.
     * @return a {@link HttpFSConnectionConfig} based on the current settings
     */
    HttpFSConnectionConfig toFSConnectionConfig(final CredentialsProvider credentialsProvider) {

        final var cfg = new HttpFSConnectionConfig(m_url);
        cfg.setSslIgnoreHostnameMismatches(m_sslIgnoreHostnameMismatches);
        cfg.setSslTrustAllCertificates(m_sslTrustAllCertificates);

        switch (m_auth.m_type) {
        case NONE -> cfg.setAuthType(Auth.NONE);
        case USERNAME_PASSWORD -> {
            final var creds = m_auth.m_userPassword.toCredentials(credentialsProvider);
            cfg.setAuthType(Auth.BASIC);
            cfg.setUsername(creds.getUsername());
            cfg.setPassword(creds.getPassword());
        }
        }
        cfg.setConnectionTimeout(Duration.ofSeconds(m_connectionTimeout));
        cfg.setReadTimeout(Duration.ofSeconds(m_readTimeout));
        cfg.setFollowRedirects(m_followRedirects);

        return cfg;
    }
}
