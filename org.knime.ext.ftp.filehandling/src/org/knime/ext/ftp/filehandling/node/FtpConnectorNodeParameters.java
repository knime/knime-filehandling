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

package org.knime.ext.ftp.filehandling.node;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.util.proxy.ProxyProtocol;
import org.knime.core.util.proxy.search.GlobalProxySearch;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersInputImpl;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FSConnectionProvider;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSelectionWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.SingleFileSelectionMode;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.WithCustomFileSystem;
import org.knime.core.webui.node.dialog.defaultdialog.setting.credentials.LegacyCredentials;
import org.knime.core.webui.node.dialog.defaultdialog.setting.credentials.LegacyCredentialsAuthProviderSettings;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.custom.CustomValidation;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.custom.CustomValidationProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.custom.SimpleValidation;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.custom.ValidationCallback;
import org.knime.ext.ftp.filehandling.fs.FtpFSConnection;
import org.knime.ext.ftp.filehandling.fs.FtpFSConnectionConfig;
import org.knime.ext.ftp.filehandling.fs.FtpFileSystem;
import org.knime.ext.ftp.filehandling.fs.ProtectedHostConfiguration;
import org.knime.ext.ftp.filehandling.node.FtpConnectorNodeParameters.AuthenticationParameters.AuthenticationMethod;
import org.knime.ext.ftp.filehandling.node.FtpConnectorNodeParameters.AuthenticationParameters.FtpFSConnectionProvider;
import org.knime.ext.ftp.filehandling.node.FtpConnectorNodeParameters.AuthenticationParameters.PortMaxValidation;
import org.knime.ext.ftp.filehandling.node.FtpConnectorNodeParameters.AuthenticationParameters.TimeZoneMaxValidation;
import org.knime.ext.ftp.filehandling.node.FtpConnectorNodeParameters.AuthenticationParameters.TimeZoneMinValidation;
import org.knime.filehandling.core.connections.base.auth.StandardAuthTypes;
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
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.credentials.Credentials;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MaxValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsNonNegativeValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.PatternValidation.IsNotBlankValidation;

/**
 * Node parameters for FTP Connector.
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings({ "restriction", "removal" })
final class FtpConnectorNodeParameters implements NodeParameters {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(FtpConnectorNodeParameters.class);

    // ----- LAYOUTS -----

    @Section(title = "Connection")
    interface ConnectionSection {
    }

    @Section(title = "Authentication")
    @After(ConnectionSection.class)
    interface AuthenticationSection {
    }

    @Section(title = "FTPS")
    @After(AuthenticationSection.class)
    interface FtpsSection {
    }

    @Section(title = "File System")
    @After(FtpsSection.class)
    interface FileSystemSection {
    }

    @Section(title = "Timeouts")
    @After(FileSystemSection.class)
    @Advanced
    interface TimeoutsSections {
    }

    @Section(title = "Time Zone")
    @After(FileSystemSection.class)
    @Advanced
    interface TimeZoneSection {
    }

    // ----- CONNECTION PARAMETERS -----

    @Layout(ConnectionSection.class)
    @Widget(title = "Host", description = "Address of the host where the FTP server runs.")
    @TextInputWidget(patternValidation = IsNotBlankValidation.class)
    @ValueReference(HostRef.class)
    String m_host = "localhost";

    interface HostRef extends ParameterReference<String> {
    }

    @Layout(ConnectionSection.class)
    @Widget(title = "Port", description = "Port that the FTP server is listening on for incoming connections.")
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class, maxValidation = PortMaxValidation.class)
    @ValueReference(PortRef.class)
    @ValueProvider(DefaultPortProvider.class)
    int m_port = 21;

    interface PortRef extends ParameterReference<Integer> {
    }

    static class DefaultPortProvider implements StateProvider<Integer> {

        private static final int DEFAULT_EXPLICIT = 21;
        private static final int DEFAULT_IMPLICIT = 990;

        private Supplier<Integer> m_currentValue;
        private Supplier<Boolean> m_isImplicit;
        private Supplier<Boolean> m_useFtps;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_currentValue = initializer.getValueSupplier(PortRef.class);
            m_useFtps = initializer.computeFromValueSupplier(UseFtpsRef.class);
            m_isImplicit = initializer.computeFromValueSupplier(UseImplicitFtpsRef.class);
        }

        @Override
        public Integer computeState(final NodeParametersInput parametersInput) throws StateComputationFailureException {
            final int current = m_currentValue.get();
            final boolean implicit = m_useFtps.get() && m_isImplicit.get();

            if (implicit && current == DEFAULT_EXPLICIT) {
                return DEFAULT_IMPLICIT;
            } else if (!implicit && current == DEFAULT_IMPLICIT) {
                return DEFAULT_EXPLICIT;
            } else {
                return current;
            }
        }
    }

    @Layout(ConnectionSection.class)
    @Advanced
    @Widget(title = "Minimum FTP connections", description = """
            Minimum number of (control) connections to open to the FTP server.
            """)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    @ValueReference(MinConnectionsRef.class)
    int m_minConnections = FtpFSConnectionConfig.DEFAULT_MIN_CONNECTIONS;

    interface MinConnectionsRef extends ParameterReference<Integer> {
    }

    @Layout(ConnectionSection.class)
    @Advanced
    @Widget(title = "Maximum FTP connections", description = """
            Maximum number of (control) connections to open to the FTP server.
            """)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    @CustomValidation(MaxConnectionsValidator.class)
    @ValueReference(MaxConnectionsRef.class)
    int m_maxConnections = FtpFSConnectionConfig.DEFAULT_MAX_CONNECTIONS;

    interface MaxConnectionsRef extends ParameterReference<Integer> {
    }

    static class MaxConnectionsValidator implements CustomValidationProvider<Integer>, ValidationCallback<Integer> {

        private Supplier<Integer> m_minConnections;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_minConnections = initializer.computeFromValueSupplier(MinConnectionsRef.class);
            initializer.computeAfterOpenDialog();
        }

        @Override
        public ValidationCallback<Integer> computeValidationCallback(final NodeParametersInput parametersInput) {
            return this;
        }

        @Override
        public void validate(final Integer currentValue) throws InvalidSettingsException {
            // avoid two validations at the same time
            CheckUtils.checkSetting(currentValue < 1 || m_minConnections.get() <= currentValue,
                    "The value must not be smaller than the minimum number of connections.");
        }
    }

    @Layout(ConnectionSection.class)
    @Advanced
    @Widget(title = "Use HTTP proxy", description = """
            If this option is selected, then the currently configured HTTP proxy from the KNIME preferences will be
            used to connect. This option is incompatible with 'Use FTPS'.""")
    @Effect(predicate = UseFtps.class, type = EffectType.HIDE)
    @ValueReference(UseProxyRef.class)
    boolean m_useProxy;

    interface UseProxyRef extends ParameterReference<Boolean> {
    }

    // ----- AUTHENTICATION PARAMETERS -----

    @Layout(AuthenticationSection.class)
    AuthenticationParameters m_auth = new AuthenticationParameters();

    // ----- FILESYSTEM PARAMETERS -----

    @Layout(FileSystemSection.class)
    @Widget(title = "Working directory", description = """
            Specify the working directory of the resulting file system connection, using the Path syntax explained
            above. The working directory must be specified as an absolute path. A working directory allows downstream
            nodes to access files/folders using relative paths, i.e. paths that do not have a leading slash.
            The default working directory is the root "/".""")
    @ValueReference(WorkingDirectoryRef.class)
    @FileSelectionWidget(value = SingleFileSelectionMode.FOLDER)
    @WithCustomFileSystem(connectionProvider = FtpFSConnectionProvider.class)
    @CustomValidation(WorkingDirectoryValidator.class)
    String m_workingDirectory = FtpFileSystem.PATH_SEPARATOR;

    interface WorkingDirectoryRef extends ParameterReference<String> {
    }

    static class WorkingDirectoryValidator extends SimpleValidation<String> {

        static void validateWorkingDirectory(final String workingDirectory) throws InvalidSettingsException {
            if (StringUtils.isBlank(workingDirectory)) {
                throw new InvalidSettingsException(
                        "The field cannot be blank (it must contain at least one non-whitespace character).");
            }
            if (!workingDirectory.strip().startsWith(FtpFileSystem.PATH_SEPARATOR)) {
                throw new InvalidSettingsException(
                        "The path must be absolute (start with \"" + FtpFileSystem.PATH_SEPARATOR + "\").");
            }
        }

        @Override
        public void validate(final String currentValue) throws InvalidSettingsException {
            validateWorkingDirectory(currentValue);
        }
    }

    // ----- ADVANCED CONNECTION PARAMETERS -----

    @Layout(TimeoutsSections.class)
    @Widget(title = "Connection timeout", description = """
            Timeout in seconds to establish a connection or 0 for an infinite timeout.
            """)
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    @ValueReference(ConnectionTimeoutRef.class)
    int m_connectionTimeout = 30;

    interface ConnectionTimeoutRef extends ParameterReference<Integer> {
    }

    @Layout(TimeoutsSections.class)
    @Widget(title = "Read timeout", description = """
            Timeout in seconds to read a server response from a connection, or 0 for an infinite timeout.
            """)
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    @ValueReference(ReadTimeoutRef.class)
    int m_readTimeout = 30;

    interface ReadTimeoutRef extends ParameterReference<Integer> {
    }

    // ----- FTPS PARAMETERS -----

    @Layout(FtpsSection.class)
    @Widget(title = "Use FTPS", description = """
            Attempts to communicate with the server using TLS-encryption (FTPS in explicit mode). If this option is
            selected, then it is not possible to connect using an HTTP proxy (see below).""")
    @Persist(configKey = "useFTPS")
    @ValueReference(UseFtpsRef.class)
    boolean m_useFtps;

    interface UseFtpsRef extends ParameterReference<Boolean> {
    }

    @Layout(FtpsSection.class)
    @Widget(title = "Verify hostname", description = """
            If checked, the hostname will be verified against the certificate. Otherwise, the node trusts the
            server's SSL certificate even if it was generated for a different hostname.""")
    @Advanced
    @Effect(predicate = UseFtps.class, type = EffectType.SHOW)
    @ValueReference(VerifyHostnameRef.class)
    boolean m_verifyHostname = true;

    interface VerifyHostnameRef extends ParameterReference<Boolean> {
    }

    @Layout(FtpsSection.class)
    @Widget(title = "Use implicit FTPS", description = """
            If checked, the node uses implicit FTPS, otherwise it uses explicit FTPS. Implicit FTPS commonly
            runs on port 990. Please also adjust the port accordingly if you changed it.""")
    @Advanced
    @Effect(predicate = UseFtps.class, type = EffectType.SHOW)
    @Persist(configKey = "useImplicitFTPS")
    @ValueReference(UseImplicitFtpsRef.class)
    boolean m_useImplicitFtps;

    interface UseImplicitFtpsRef extends ParameterReference<Boolean> {
    }

    @Layout(FtpsSection.class)
    @Widget(title = "Reuse SSL session", description = """
            If checked the SSL/TLS session of the control connection is reused for the data connections. Most
            FTPS servers require SSL session reuse as a security measure in order to prevent attackers from
            hijacking data connections.""")
    @Effect(predicate = UseFtps.class, type = EffectType.SHOW)
    @Advanced
    @ValueReference(ReuseSSLSessionRef.class)
    boolean m_reuseSSLSession = true;

    interface ReuseSSLSessionRef extends ParameterReference<Boolean> {
    }

    static final class UseFtps implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getBoolean(UseFtpsRef.class).isTrue();
        }
    }

    // ----- OTHER PARAMETERS -----

    @Layout(TimeZoneSection.class)
    @Widget(title = "Time zone offset from GMT", description = """
            Server time zone offset from Greenwich Mean Time (minutes).
            """)
    @NumberInputWidget(minValidation = TimeZoneMinValidation.class, maxValidation = TimeZoneMaxValidation.class)
    @ValueReference(TimeZoneOffsetRef.class)
    int m_timeZoneOffset;

    interface TimeZoneOffsetRef extends ParameterReference<Integer> {
    }

    // ----- INTERNAL PARAMETER CLASSES -----

    static final class AuthenticationParameters implements NodeParameters {

        private static final String CFG_KEY_USER_PWD_V2 = "user_pwd_v2";

        enum AuthenticationMethod {
            @Label(value = "Username & password", description = """
                    Authenticate with a credentials flow variable or by entering username and password directly.
                    In the latter case the password will be persistently stored (in encrypted form) with the workflow.
                    """)
            USERNAME_PASSWORD,

            @Label(value = "Anonymous", description = """
                    Authenticate with the anonymous user and a blank password.
                    """)
            ANONYMOUS
        }

        @Widget(title = "Authentication", description = "Method of authentication to use.")
        @ValueReference(AuthenticationMethodRef.class)
        @Persistor(AuthenticationMethodPersistor.class)
        AuthenticationMethod m_type = AuthenticationMethod.USERNAME_PASSWORD;

        interface AuthenticationMethodRef extends ParameterReference<AuthenticationMethod> {
        }

        static final class IsUserPwdAuth implements EffectPredicateProvider {
            @Override
            public EffectPredicate init(final PredicateInitializer i) {
                return i.getEnum(AuthenticationMethodRef.class).isOneOf(AuthenticationMethod.USERNAME_PASSWORD);
            }
        }

        static final class AuthenticationMethodPersistor implements NodeParametersPersistor<AuthenticationMethod> {

            private static final String VAL_USER_PASSWORD = "user_pwd";
            private static final String VAL_USER_PASSWORD_V2 = "user_pwd_v2";
            private static final String VAL_ANONYMOUS = "anonymous";
            private static final String KEY = "type";

            @Override
            public AuthenticationMethod load(final NodeSettingsRO settings) throws InvalidSettingsException {

                final var typeString = settings.getString(KEY, VAL_USER_PASSWORD);

                return switch (typeString) {
                case VAL_USER_PASSWORD, VAL_USER_PASSWORD_V2 -> AuthenticationMethod.USERNAME_PASSWORD;
                case VAL_ANONYMOUS -> AuthenticationMethod.ANONYMOUS;
                default -> throw new InvalidSettingsException(
                        String.format("Unknown authentication method: '%s'. Possible values: '%s', '%s'", typeString,
                                VAL_USER_PASSWORD_V2, VAL_ANONYMOUS));
                };
            }

            @Override
            public void save(final AuthenticationMethod param, final NodeSettingsWO settings) {
                switch (param) { // NOSONAR
                case USERNAME_PASSWORD -> settings.addString(KEY, VAL_USER_PASSWORD_V2);
                case ANONYMOUS -> settings.addString(KEY, VAL_ANONYMOUS);
                }
            }

            @Override
            public String[][] getConfigPaths() {
                return new String[][] { { KEY } };
            }
        }

        @Widget(title = "Username & password", description = """
                Enter the username and password for authentication. The password will be persistently stored
                (in encrypted form) with the workflow if it is not provided via a flow variable.""")
        @Persist(configKey = CFG_KEY_USER_PWD_V2)
        @Migration(LoadFromUserPwdAuthMigration.class)
        @CustomValidation(UserPasswordValidator.class)
        @Effect(predicate = IsUserPwdAuth.class, type = EffectType.SHOW)
        @ValueReference(UserPasswordRef.class)
        LegacyCredentials m_userPassword = new LegacyCredentials(new Credentials(System.getProperty("user.name"), ""));

        static final class LoadFromUserPwdAuthMigration
                extends LegacyCredentialsAuthProviderSettings.FromUserPasswordAuthProviderSettingsMigration {

            protected LoadFromUserPwdAuthMigration() {
                super(new UserPasswordAuthProviderSettings(StandardAuthTypes.USER_PASSWORD, true));
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

        // For backwards compatibility with anonymous auth settings
        AnonymousParameters m_anonymous = new AnonymousParameters();

        static final class AnonymousParameters implements NodeParameters {
            // empty subsettings for anonymous auth
        }

        static final class FtpFSConnectionProvider implements StateProvider<FSConnectionProvider> {

            private Supplier<String> m_host;
            private Supplier<Integer> m_port;
            private Supplier<Integer> m_minConnections;
            private Supplier<Integer> m_maxConnections;
            private Supplier<Boolean> m_useProxy;
            private Supplier<Integer> m_connectionTimeout;
            private Supplier<Integer> m_readTimeout;
            private Supplier<Boolean> m_useFtps;
            private Supplier<Boolean> m_verifyHostname;
            private Supplier<Boolean> m_useImplicitFtps;
            private Supplier<Boolean> m_reuseSSLSession;
            private Supplier<Integer> m_timeZoneOffset;
            private Supplier<String> m_workingDirectory;
            private Supplier<AuthenticationMethod> m_authMethod;
            private Supplier<LegacyCredentials> m_userPassword;

            @Override
            public void init(final StateProviderInitializer initializer) {
                initializer.computeAfterOpenDialog();
                m_host = initializer.computeFromValueSupplier(HostRef.class);
                m_port = initializer.computeFromValueSupplier(PortRef.class);
                m_minConnections = initializer.computeFromValueSupplier(MinConnectionsRef.class);
                m_maxConnections = initializer.computeFromValueSupplier(MaxConnectionsRef.class);
                m_useProxy = initializer.computeFromValueSupplier(UseProxyRef.class);
                m_connectionTimeout = initializer.computeFromValueSupplier(ConnectionTimeoutRef.class);
                m_readTimeout = initializer.computeFromValueSupplier(ReadTimeoutRef.class);
                m_useFtps = initializer.computeFromValueSupplier(UseFtpsRef.class);
                m_verifyHostname = initializer.computeFromValueSupplier(VerifyHostnameRef.class);
                m_useImplicitFtps = initializer.computeFromValueSupplier(UseImplicitFtpsRef.class);
                m_reuseSSLSession = initializer.computeFromValueSupplier(ReuseSSLSessionRef.class);
                m_timeZoneOffset = initializer.computeFromValueSupplier(TimeZoneOffsetRef.class);
                m_workingDirectory = initializer.computeFromValueSupplier(WorkingDirectoryRef.class);
                m_authMethod = initializer.computeFromValueSupplier(AuthenticationMethodRef.class);
                m_userPassword = initializer.computeFromValueSupplier(UserPasswordRef.class);
            }

            @Override
            public FSConnectionProvider computeState(final NodeParametersInput parametersInput) {
                return () -> {
                    final var credprov = getCredentialsProvider(parametersInput);
                    return new FtpFSConnection(createParameters(credprov).toFSConnectionConfig(credprov));
                };
            }

            private FtpConnectorNodeParameters createParameters(final CredentialsProvider credprov)
                    throws InvalidSettingsException {

                final var result = new FtpConnectorNodeParameters();
                result.m_host = m_host.get();
                result.m_port = m_port.get();
                result.m_minConnections = m_minConnections.get();
                result.m_maxConnections = m_maxConnections.get();
                result.m_useProxy = m_useProxy.get();
                result.m_connectionTimeout = m_connectionTimeout.get();
                result.m_readTimeout = m_readTimeout.get();
                result.m_useFtps = m_useFtps.get();
                result.m_verifyHostname = m_verifyHostname.get();
                result.m_useImplicitFtps = m_useImplicitFtps.get();
                result.m_reuseSSLSession = m_reuseSSLSession.get();
                result.m_timeZoneOffset = m_timeZoneOffset.get();
                result.m_auth.m_type = m_authMethod.get();
                result.m_auth.m_userPassword = m_userPassword.get();

                result.m_workingDirectory = m_workingDirectory.get();
                if (StringUtils.isBlank(result.m_workingDirectory)) {
                    result.m_workingDirectory = FtpFileSystem.PATH_SEPARATOR;
                }
                result.validateOnConfigure(credprov);
                return result;
            }

        }

        private static CredentialsProvider getCredentialsProvider(final NodeParametersInput input) {
            return ((NodeParametersInputImpl) input).getCredentialsProvider().orElseThrow();
        }

        static final class PortMaxValidation extends MaxValidation {
            @Override
            protected double getMax() {
                return 65535;
            }
        }

        static final class TimeZoneMinValidation extends MinValidation {
            @Override
            protected double getMin() {
                return -TimeUnit.HOURS.toMinutes(24);
            }
        }

        static final class TimeZoneMaxValidation extends MaxValidation {
            @Override
            protected double getMax() {
                return TimeUnit.HOURS.toMinutes(24);
            }
        }
    }

    void validateOnConfigure(final CredentialsProvider provider) throws InvalidSettingsException {
        if (StringUtils.isBlank(m_host)) {
            throw new InvalidSettingsException("Host must be specified.");
        }

        if (m_port < 1 || m_port > 65535) {
            throw new InvalidSettingsException("Port must be between 1 and 65535.");
        }

        WorkingDirectoryValidator.validateWorkingDirectory(m_workingDirectory);

        if (m_minConnections > m_maxConnections) {
            throw new InvalidSettingsException(
                    "Minimum number of FTP connections must be less or equal to maximum number of FTP connections");
        }

        if (m_auth.m_type == AuthenticationMethod.USERNAME_PASSWORD) {
            final var creds = m_auth.m_userPassword.toCredentials(provider);
            if (StringUtils.isBlank(creds.getUsername())) {
                throw new InvalidSettingsException("Username must be specified.");
            }
        }
    }

    /**
     * @return connection time out.
     */
    public Duration getConnectionTimeout() {
        return Duration.ofSeconds(m_connectionTimeout);
    }

    /**
     * @return socket read time out.
     */
    public Duration getReadTimeout() {
        return Duration.ofSeconds(m_readTimeout);
    }

    /**
     * @return true if uses proxy
     */
    public boolean isUseProxy() {
        return !m_useFtps && m_useProxy;
    }

    /**
     * @return time zone offset in minutes.
     */
    public Duration getTimeZoneOffset() {
        return Duration.ofMinutes(m_timeZoneOffset);
    }

    FtpFSConnectionConfig toFSConnectionConfig(final CredentialsProvider credentialsProvider)
            throws InvalidSettingsException {

        final var conf = new FtpFSConnectionConfig();
        conf.setHost(m_host);
        conf.setPort(m_port);
        conf.setMaxConnectionPoolSize(m_maxConnections);
        conf.setMinConnectionPoolSize(m_minConnections);
        conf.setCoreConnectionPoolSize((m_minConnections + m_maxConnections) / 2);
        conf.setConnectionTimeOut(getConnectionTimeout());
        conf.setReadTimeout(getReadTimeout());
        conf.setServerTimeZoneOffset(getTimeZoneOffset());
        conf.setUseFTPS(m_useFtps);
        conf.setWorkingDirectory(m_workingDirectory);
        conf.setVerifyHostname(m_verifyHostname);
        conf.setUseImplicitFTPS(m_useImplicitFtps);
        conf.setReuseSSLSession(m_reuseSSLSession);

        if (m_auth.m_type == AuthenticationMethod.USERNAME_PASSWORD) {
            final var creds = m_auth.m_userPassword.toCredentials(credentialsProvider);
            conf.setUser(creds.getUsername());
            conf.setPassword(creds.getPassword());
        } else {
            conf.setUser("anonymous"); // NOSONAR: standard anonymous user
            conf.setPassword("");
        }

        if (isUseProxy()) {
            final var uri = createNullableURI(m_host, m_port);
            final var proxyData = GlobalProxySearch.getCurrentFor(uri, ProxyProtocol.HTTP, ProxyProtocol.HTTPS) //
                    .filter(cfg -> !cfg.isHostExcluded(uri)) //
                    .orElseThrow(() -> new InvalidSettingsException("Eclipse HTTP proxy is not configured"));
            final var proxy = new ProtectedHostConfiguration();
            proxy.setHost(proxyData.host());
            proxy.setPort(proxyData.intPort());
            proxy.setUser(proxyData.username());
            proxy.setPassword(proxyData.password());
            conf.setProxy(proxy);
        }

        return conf;
    }

    static URI createNullableURI(final String host, final int port) {
        try {
            return new URIBuilder().setScheme("ftp").setHost(host).setPort(port).build();
        } catch (URISyntaxException e) {
            LOGGER.debug("Could not parse URL as URI for proxy selection", e);
            return null;
        }
    }

}
