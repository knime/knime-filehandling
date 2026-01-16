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

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersInputImpl;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FSConnectionProvider;
import org.knime.credentials.base.node.CredentialsSettings.CredentialsFlowVarChoicesProvider;
import org.knime.ext.ftp.filehandling.fs.FtpFSConnection;
import org.knime.ext.ftp.filehandling.fs.FtpFSConnectionConfig;
import org.knime.ext.ftp.filehandling.fs.FtpFileSystem;
import org.knime.filehandling.core.connections.base.auth.IDWithSecretAuthProviderSettings;
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
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.credentials.Credentials;
import org.knime.node.parameters.widget.credentials.CredentialsWidget;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MaxValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsNonNegativeValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.PatternValidation.IsNotEmptyValidation;

/**
 * Node parameters for FTP Connector.
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
final class FtpConnectorNodeParameters implements NodeParameters {

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

    @Section(title = "Time zone")
    @After(FileSystemSection.class)
    @Advanced
    interface TimeZoneSection {
    }

    // ----- CONNECTION PARAMETERS -----

    @Layout(ConnectionSection.class)
    @Widget(title = "Host", description = "Address of the host where the FTP server runs.")
    @TextInputWidget(minLengthValidation = IsNotEmptyValidation.class)
    @ValueReference(HostRef.class)
    String m_host = "localhost";

    interface HostRef extends ParameterReference<String> {
    }

    @Layout(ConnectionSection.class)
    @Widget(title = "Port", description = "Port that the FTP server is listening on for incoming connections.")
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class, maxValidation = PortMaxValidation.class)
    @ValueReference(PortRef.class)
    int m_port = 21;

    interface PortRef extends ParameterReference<Integer> {
    }

    @Layout(ConnectionSection.class)
    @Advanced
    @Widget(title = "Minimum FTP connections", description = """
            Minimum number of (control) connections to open to the FTP server.""")
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    @ValueReference(MinConnectionsRef.class)
    int m_minConnections = FtpFSConnectionConfig.DEFAULT_MIN_CONNECTIONS;

    interface MinConnectionsRef extends ParameterReference<Integer> {
    }

    @Layout(ConnectionSection.class)
    @Advanced
    @Widget(title = "Maximum FTP connections", description = """
            Maximum number of (control) connections to open to the FTP server.""")
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    @ValueReference(MaxConnectionsRef.class)
    int m_maxConnections = FtpFSConnectionConfig.DEFAULT_MAX_CONNECTIONS;

    interface MaxConnectionsRef extends ParameterReference<Integer> {
    }

    @Layout(ConnectionSection.class)
    @Advanced
    @Widget(title = "Use HTTP Proxy", description = """
            If this option is selected, then the currently configured HTTP proxy from the KNIME preferences will be
            used to connect. This option is incompatible with 'Use FTPS'.""")
    @Effect(predicate = UseFtps.class, type = EffectType.HIDE)
    @ValueReference(UseProxyRef.class)
    boolean m_useProxy;

    interface UseProxyRef extends ParameterReference<Boolean> {
    }

    // ----- AUTHENTICATION PARAMETERS -----

    @Layout(AuthenticationSection.class)
    @ValueReference(AuthenticationParametersRef.class)
    AuthenticationParameters m_auth = new AuthenticationParameters();

    static final class AuthenticationParametersRef implements ParameterReference<AuthenticationParameters> {
    }

    // ----- FILESYSTEM PARAMETERS -----

    @Layout(FileSystemSection.class)
    @Widget(title = "Working directory", description = """
            Specify the working directory of the resulting file system connection, using the Path syntax explained
            above. The working directory must be specified as an absolute path. A working directory allows downstream
            nodes to access files/folders using relative paths, i.e. paths that do not have a leading slash.
            The default working directory is the root "/".""")
    @ValueReference(WorkingDirectoryRef.class)
    String m_workingDirectory = FtpFileSystem.PATH_SEPARATOR;

    interface WorkingDirectoryRef extends ParameterReference<String> {
    }

    // ----- ADVANCED CONNECTION PARAMETERS -----

    @Layout(TimeoutsSections.class)
    @Widget(title = "Connection timeout", description = """
            Timeout in seconds to establish a connection or 0 for an infinite timeout.""")
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    @ValueReference(ConnectionTimeoutRef.class)
    int m_connectionTimeout = 30;

    interface ConnectionTimeoutRef extends ParameterReference<Integer> {
    }

    @Layout(TimeoutsSections.class)
    @Widget(title = "Read timeout", description = """
            Timeout in seconds to read a server response from a connection, or 0 for an infinite timeout.""")
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
    @Effect(predicate = UseFtps.class, type = EffectType.SHOW)
    @ValueReference(VerifyHostnameRef.class)
    boolean m_verifyHostname = true;

    interface VerifyHostnameRef extends ParameterReference<Boolean> {
    }

    @Layout(FtpsSection.class)
    @Widget(title = "Use implicit FTPS", description = """
            If checked, the node uses implicit FTPS, otherwise it uses explicit FTPS. Implicit FTPS commonly
            runs on port 990. Please also adjust the port accordingly when enabling this option.""")
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
            Server time zone offset from Greenwich Mean Time (minutes).""")
    @NumberInputWidget(minValidation = TimeZoneMinValidation.class, maxValidation = TimeZoneMaxValidation.class)
    @ValueReference(TimeZoneOffsetRef.class)
    int m_timeZoneOffset;

    interface TimeZoneOffsetRef extends ParameterReference<Integer> {
    }

    // ----- INTERNAL PARAMETER CLASSES -----

    static final class AuthenticationParameters implements NodeParameters {

        private static final String CFG_KEY_USER_PWD = "user_pwd";
        private static final String ENTRY_KEY_USE_CREDENTIALS = "use_credentials";

        enum AuthenticationMethod {
            @Label(value = "Username & password", description = """
                    Authenticate with a username and password, in which case
                    the password will be persistently stored (in encrypted form) with the workflow.""")
            USERNAME_PASSWORD,

            @Label(value = "Use credentials", description = """
                    Authenticate with a username and password stored in a credentials flow variable.""")
            CREDENTIALS,

            @Label(value = "Anonymous", description = """
                    Authenticate with the anonymous user and a blank password.""")
            ANONYMOUS
        }

        @Widget(title = "Authentication", description = "Method of authentication to use.")
        @ValueReference(AuthenticationMethodRef.class)
        @Persistor(AuthenticationMethodPersistor.class)
        AuthenticationMethod m_type = AuthenticationMethod.USERNAME_PASSWORD;

        static final class AuthenticationMethodRef implements ParameterReference<AuthenticationMethod> {
        }

        static final class IsUserPwdAuth implements EffectPredicateProvider {
            @Override
            public EffectPredicate init(final PredicateInitializer i) {
                return i.getEnum(AuthenticationMethodRef.class).isOneOf(AuthenticationMethod.USERNAME_PASSWORD);
            }
        }

        static final class IsCredentialsAuth implements EffectPredicateProvider {
            @Override
            public EffectPredicate init(final PredicateInitializer i) {
                return i.getEnum(AuthenticationMethodRef.class).isOneOf(AuthenticationMethod.CREDENTIALS);
            }
        }

        static final class UseCredentialsValueProvider implements StateProvider<Boolean> {

            private Supplier<AuthenticationMethod> m_authenticationMethodSupplier;

            @Override
            public void init(final StateProviderInitializer initializer) {
                m_authenticationMethodSupplier = initializer.computeFromValueSupplier(AuthenticationMethodRef.class);
            }

            @Override
            public Boolean computeState(final NodeParametersInput parametersInput) {
                return m_authenticationMethodSupplier.get() == AuthenticationMethod.CREDENTIALS;
            }
        }

        static final class AuthenticationMethodPersistor implements NodeParametersPersistor<AuthenticationMethod> {

            private static final String VAL_USER_PASSWORD = "user_pwd";
            private static final String VAL_ANONYMOUS = "anonymous";
            private static final String KEY = "type";

            @Override
            public AuthenticationMethod load(final NodeSettingsRO settings) throws InvalidSettingsException {
                final var typeString = settings.getString(KEY, VAL_USER_PASSWORD);
                return switch (typeString) {
                case VAL_USER_PASSWORD -> {
                    final var useCredentials = settings.getNodeSettings(CFG_KEY_USER_PWD)
                            .getBoolean(ENTRY_KEY_USE_CREDENTIALS, false);
                    yield useCredentials ? AuthenticationMethod.CREDENTIALS : AuthenticationMethod.USERNAME_PASSWORD;
                }
                case VAL_ANONYMOUS -> AuthenticationMethod.ANONYMOUS;
                default -> throw new InvalidSettingsException(
                        String.format("Unknown authentication method: '%s'. Possible values: '%s', '%s'", typeString,
                                VAL_USER_PASSWORD, VAL_ANONYMOUS));
                };
            }

            @Override
            public void save(final AuthenticationMethod param, final NodeSettingsWO settings) {
                switch (param) {
                case USERNAME_PASSWORD, CREDENTIALS -> settings.addString(KEY, VAL_USER_PASSWORD);
                case ANONYMOUS -> settings.addString(KEY, VAL_ANONYMOUS);
                }
            }

            @Override
            public String[][] getConfigPaths() {
                return new String[][] { { KEY }, { CFG_KEY_USER_PWD, ENTRY_KEY_USE_CREDENTIALS } };
            }
        }

        @Persist(configKey = CFG_KEY_USER_PWD)
        UserPwdAuthParameters m_userPwdAuth = new UserPwdAuthParameters();

        static final class UserPwdAuthParameters implements NodeParameters {

            private static final String ENTRY_KEY_CREDENTIALS = "credentials";

            @ValueProvider(UseCredentialsValueProvider.class)
            @Persist(configKey = ENTRY_KEY_USE_CREDENTIALS)
            @ValueReference(UseCredentialsRef.class)
            boolean m_useCredentials;

            interface UseCredentialsRef extends ParameterReference<Boolean> {
            }

            @Widget(title = "Credentials", description = "Select a credentials flow variable.")
            @Effect(predicate = IsCredentialsAuth.class, type = EffectType.SHOW)
            @ChoicesProvider(CredentialsFlowVarChoicesProvider.class)
            @Persist(configKey = ENTRY_KEY_CREDENTIALS)
            @ValueReference(CredentialsFlowVarNameRef.class)
            String m_credentialsFlowVarName;

            interface CredentialsFlowVarNameRef extends ParameterReference<String> {
            }

            @Widget(title = "Username & password", description = """
                    Enter the username and password for authentication. The password will be persistently stored
                    (in encrypted form) with the workflow.""")
            @Effect(predicate = IsUserPwdAuth.class, type = EffectType.SHOW)
            @CredentialsWidget
            @Persistor(UserPasswordPersistor.class)
            @ValueReference(UserPasswordRef.class)
            Credentials m_userPassword = new Credentials(System.getProperty("user.name"), "");

            interface UserPasswordRef extends ParameterReference<Credentials> {
            }

            static final class UserPasswordPersistor implements NodeParametersPersistor<Credentials> {

                /**
                 * See {@link IDWithSecretAuthProviderSettings#SECRET_ENCRYPTION_KEY}.
                 */
                private static final String USER_PWD_ENCRYPTION_KEY = "laig9eeyeix:ae$Lo6lu";
                private static final String ENTRY_KEY_USER = "user";
                private static final String ENTRY_KEY_PASSWORD = "password";

                @Override
                public Credentials load(final NodeSettingsRO settings) throws InvalidSettingsException {
                    final var user = settings.getString(ENTRY_KEY_USER, System.getProperty("user.name"));
                    final var password = settings.getPassword(ENTRY_KEY_PASSWORD, USER_PWD_ENCRYPTION_KEY, "");
                    return new Credentials(user, password);
                }

                @Override
                public void save(final Credentials param, final NodeSettingsWO settings) {
                    settings.addString(ENTRY_KEY_USER, param.getUsername());
                    settings.addPassword(ENTRY_KEY_PASSWORD, USER_PWD_ENCRYPTION_KEY, param.getPassword());
                }

                @Override
                public String[][] getConfigPaths() {
                    return new String[][] { { ENTRY_KEY_USER } }; // Password fields cannot be overwritten
                }
            }
        }

        // For backwards compatibility with anonymous auth settings
        AnonymousParameters m_anonymous = new AnonymousParameters();

        static final class AnonymousParameters implements NodeParameters {
            // empty subsettings for anonymous auth
        }

        /**
         * Provides a {@link FSConnectionProvider} based on the Box connection settings
         * and the credential from the input port.
         */
        @SuppressWarnings("restriction")
        static final class FtpFileSystemConnectionProvider implements StateProvider<FSConnectionProvider> {

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
            private Supplier<Boolean> m_useCredentials;
            private Supplier<String> m_credentialsFlowVarName;
            private Supplier<Credentials> m_userPassword;

            @Override
            public void init(final StateProviderInitializer initializer) {
                initializer.computeAfterOpenDialog();
                m_host = initializer.getValueSupplier(HostRef.class);
                m_port = initializer.getValueSupplier(PortRef.class);
                m_minConnections = initializer.getValueSupplier(MinConnectionsRef.class);
                m_maxConnections = initializer.getValueSupplier(MaxConnectionsRef.class);
                m_useProxy = initializer.getValueSupplier(UseProxyRef.class);
                m_connectionTimeout = initializer.getValueSupplier(ConnectionTimeoutRef.class);
                m_readTimeout = initializer.getValueSupplier(ReadTimeoutRef.class);
                m_useFtps = initializer.getValueSupplier(UseFtpsRef.class);
                m_verifyHostname = initializer.getValueSupplier(VerifyHostnameRef.class);
                m_useImplicitFtps = initializer.getValueSupplier(UseImplicitFtpsRef.class);
                m_reuseSSLSession = initializer.getValueSupplier(ReuseSSLSessionRef.class);
                m_timeZoneOffset = initializer.getValueSupplier(TimeZoneOffsetRef.class);
                m_workingDirectory = initializer.getValueSupplier(WorkingDirectoryRef.class);
                m_authMethod = initializer.getValueSupplier(AuthenticationMethodRef.class);
                m_useCredentials = initializer.getValueSupplier(UserPwdAuthParameters.UseCredentialsRef.class);
                m_credentialsFlowVarName = initializer
                        .getValueSupplier(UserPwdAuthParameters.CredentialsFlowVarNameRef.class);
                m_userPassword = initializer.getValueSupplier(UserPwdAuthParameters.UserPasswordRef.class);
            }

            @Override
            public FSConnectionProvider computeState(final NodeParametersInput parametersInput) {
                return () -> new FtpFSConnection(
                        createSettings().toFSConnectionConfig(getCredentialsProvider(parametersInput)));
            }

            private FtpConnectorNodeSettings createSettings() {
                final var result = new FtpConnectorNodeSettings();
                result.getHostModel().setStringValue(m_host.get());
                result.getPortModel().setIntValue(m_port.get());
                result.getMinConnectionsModel().setIntValue(m_minConnections.get());
                result.getMaxConnectionsModel().setIntValue(m_maxConnections.get());
                result.getUseProxyModel().setBooleanValue(m_useProxy.get());
                result.getConnectionTimeoutModel().setIntValue(m_connectionTimeout.get());
                result.getReadTimeoutModel().setIntValue(m_readTimeout.get());
                result.getUseFTPSModel().setBooleanValue(m_useFtps.get());
                result.getVerifyHostnameModel().setBooleanValue(m_verifyHostname.get());
                result.getUseImplicitFTPSModel().setBooleanValue(m_useImplicitFtps.get());
                result.getReuseSSLSessionModel().setBooleanValue(m_reuseSSLSession.get());
                result.getTimeZoneOffsetModel().setIntValue(m_timeZoneOffset.get());
                result.getWorkingDirectoryModel().setStringValue(m_workingDirectory.get());
                final var auth = result.getAuthenticationSettings();
                final var type = m_authMethod.get();
                switch (type) {
                case ANONYMOUS -> auth.setAuthType(StandardAuthTypes.ANONYMOUS);
                case CREDENTIALS, USERNAME_PASSWORD -> {
                    auth.setAuthType(StandardAuthTypes.USER_PASSWORD);
                    final var unpw = (UserPasswordAuthProviderSettings) auth
                            .getSettingsForAuthType(StandardAuthTypes.USER_PASSWORD);
                    unpw.getUseCredentialsModel().setBooleanValue(m_useCredentials.get());
                    unpw.getCredentialsNameModel().setStringValue(m_credentialsFlowVarName.get());
                    final var creds = m_userPassword.get();
                    unpw.getUserModel().setStringValue(creds.getUsername());
                    unpw.getPasswordModel().setStringValue(creds.getPassword());
                }
                }
                return result;
            }

            private static CredentialsProvider getCredentialsProvider(final NodeParametersInput input) {
                return ((NodeParametersInputImpl) input).getCredentialsProvider().orElseThrow();
            }

        }
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
            return -TimeUnit.HOURS.toMinutes(24);
        }
    }

}
