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
package org.knime.ext.smb.filehandling.node;

import java.util.function.Supplier;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.credentials.base.node.CredentialsSettings.CredentialsFlowVarChoicesProvider;
import org.knime.ext.smb.filehandling.fs.SmbFileSystem;
import org.knime.ext.smb.filehandling.fs.SmbProtocolVersion;
import org.knime.filehandling.core.connections.base.auth.IDWithSecretAuthProviderSettings;
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
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsNonNegativeValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.PatternValidation.IsNotBlankValidation;

/**
 * Node parameters for SMB Connector.
 *
 * @author AI Migration Pipeline v1.2
 * @author Bjoern Lohrmann, KNIME GmbH
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class SmbConnectorNodeParameters implements NodeParameters {

    // ----- LAYOUT DEFINITIONS -----

    @Section(title = "Connection Settings")
    interface ConnectionSection {
    }

    @Section(title = "Authentication")
    @After(ConnectionSection.class)
    interface AuthenticationSection {
    }

    @Section(title = "File System Settings")
    @After(AuthenticationSection.class)
    interface FileSystemSection {
    }

    @Section(title = "Connection")
    @After(FileSystemSection.class)
    @Advanced
    interface AdvancedSection {
    }

    // ----- CONNECTION MODE -----

    enum ConnectionMode {
        @Label(value = "File server", description = """
                Connect directly to a file share on a specific file server. A file server is any machine that runs \
                an SMB service, such as Windows Server or Samba. For example, this is similar to connecting to \
                \\\\server.company.com\\marketing using Windows Explorer.""")
        FILESERVER,

        @Label(value = "Domain", description = """
                Connect to a file share in a Windows Active Directory domain. For example, this is similar to \
                connecting to \\\\company.com\\marketing using Windows Explorer, which first locates and then \
                connects to an SMB service that provides the file share or DFS namespace for the domain.""")
        DOMAIN
    }

    @Layout(ConnectionSection.class)
    @Widget(title = "Connect to", description = """
            Specifies whether to connect by specifying a file server host or a Windows domain.""")
    @ValueReference(ConnectionModeRef.class)
    @Persistor(ConnectionModePersistor.class)
    ConnectionMode m_connectionMode = ConnectionMode.FILESERVER;

    static final class ConnectionModeRef implements ParameterReference<ConnectionMode> {
    }

    static final class IsFileserverMode implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(ConnectionModeRef.class).isOneOf(ConnectionMode.FILESERVER);
        }
    }

    static final class IsDomainMode implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(ConnectionModeRef.class).isOneOf(ConnectionMode.DOMAIN);
        }
    }

    static final class ConnectionModePersistor implements NodeParametersPersistor<ConnectionMode> {

        private static final String KEY = "connectionMode";

        @Override
        public ConnectionMode load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var value = settings.getString(KEY, "fileserver");
            return switch (value) {
                case "fileserver" -> ConnectionMode.FILESERVER;
                case "domain" -> ConnectionMode.DOMAIN;
                default -> throw new InvalidSettingsException("Unknown connection mode: " + value);
            };
        }

        @Override
        public void save(final ConnectionMode param, final NodeSettingsWO settings) {
            final var value = switch (param) {
                case FILESERVER -> "fileserver";
                case DOMAIN -> "domain";
            };
            settings.addString(KEY, value);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][] { { KEY } };
        }
    }

    // ----- FILE SERVER SETTINGS -----

    @Layout(ConnectionSection.class)
    @Widget(title = "Host", description = """
            Hostname of the server where the SMB service runs, e.g. server.company.com.""")
    @Effect(predicate = IsFileserverMode.class, type = EffectType.SHOW)
    @TextInputWidget(patternValidation = IsNotBlankValidation.class)
    @Persist(configKey = "fileserver.host")
    String m_fileserverHost = "";

    @Layout(ConnectionSection.class)
    @Widget(title = "Port", description = """
            Port that the SMB server is listening on for incoming connections.""")
    @Effect(predicate = IsFileserverMode.class, type = EffectType.SHOW)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class, maxValidation = PortMaxValidation.class)
    @Persist(configKey = "fileserver.port")
    int m_fileserverPort = 445;

    static final class PortMaxValidation extends MaxValidation {
        @Override
        protected double getMax() {
            return 65535;
        }
    }

    @Layout(ConnectionSection.class)
    @Widget(title = "Share", description = """
            The name of the file share provided by the SMB server. The name must not contain any backslashes.""")
    @Effect(predicate = IsFileserverMode.class, type = EffectType.SHOW)
    @Persist(configKey = "fileserver.share")
    String m_fileserverShare = "";

    // ----- DOMAIN SETTINGS -----

    @Layout(ConnectionSection.class)
    @Widget(title = "Domain", description = """
            The name of the Windows domain (Active Directory), e.g. company.com.""")
    @Effect(predicate = IsDomainMode.class, type = EffectType.SHOW)
    @TextInputWidget(patternValidation = IsNotBlankValidation.class)
    @Persist(configKey = "domain.name")
    String m_domainName = "";

    @Layout(ConnectionSection.class)
    @Widget(title = "Share/Namespace", description = """
            The name of the file share or DFS namespace to access. The name must not contain any backslashes.""")
    @Effect(predicate = IsDomainMode.class, type = EffectType.SHOW)
    @Persist(configKey = "domain.namespace")
    String m_domainNamespace = "";

    // ----- AUTHENTICATION -----

    @Layout(AuthenticationSection.class)
    @Persist(configKey = AuthenticationParameters.CFG_KEY)
    @ValueReference(AuthenticationParametersRef.class)
    AuthenticationParameters m_authentication = new AuthenticationParameters();

    static final class AuthenticationParametersRef implements ParameterReference<AuthenticationParameters> {
    }

    // ----- FILE SYSTEM SETTINGS -----

    @Layout(FileSystemSection.class)
    @Widget(title = "Working directory", description = """
            Specify the working directory of the resulting file system connection. The working directory must be \
            specified as an absolute path starting with a backslash. A working directory allows downstream nodes \
            to access files/folders using relative paths, i.e. paths that do not have a leading backslash. \
            The default working directory is the root "\\".""")
    @Persist(configKey = "workingDirectory")
    String m_workingDirectory = SmbFileSystem.SEPARATOR;

    // ----- ADVANCED SETTINGS -----

    @Layout(AdvancedSection.class)
    @Widget(title = "Read/Write timeout (seconds)", description = """
            The timeout in seconds for read/write operations.""")
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    @Persist(configKey = "timeout")
    int m_timeout = 30;

    @Layout(AdvancedSection.class)
    @Widget(title = "SMB version(s)", description = """
            Allows to enforce the usage of specific SMB protocol version(s). Selecting "Auto" will make the node \
            choose the highest version supported by both this node and the SMB server.""")
    @Persistor(SmbVersionPersistor.class)
    SmbVersion m_smbVersion = SmbVersion.V_2_X;

    enum SmbVersion {
        @Label(value = "Auto", description = "Automatically choose the highest version supported by both parties.")
        AUTO,
        @Label(value = "2.0.2", description = "Use SMB version 2.0.2.")
        V_2_0_2,
        @Label(value = "2.1", description = "Use SMB version 2.1.")
        V_2_1,
        @Label(value = "2.x (2.1, 2.0.2)", description = "Use the highest 2.x version supported by both parties.")
        V_2_X,
        @Label(value = "3.0", description = "Use SMB version 3.0.")
        V_3_0,
        @Label(value = "3.0.2", description = "Use SMB version 3.0.2.")
        V_3_0_2,
        @Label(value = "3.1.1", description = "Use SMB version 3.1.1.")
        V_3_1_1,
        @Label(value = "3.x (3.1.1, 3.0.2, 3.0)", description = "Use the highest 3.x version supported by both parties.")
        V_3_X
    }

    static final class SmbVersionPersistor implements NodeParametersPersistor<SmbVersion> {

        private static final String KEY = "smbVersion";

        @Override
        public SmbVersion load(final NodeSettingsRO settings) throws InvalidSettingsException {
            if (!settings.containsKey(KEY)) {
                return SmbVersion.V_2_X; // default for backwards compatibility
            }
            final var value = settings.getString(KEY);
            return switch (value) {
                case "auto" -> SmbVersion.AUTO;
                case "2.0.2" -> SmbVersion.V_2_0_2;
                case "2.1" -> SmbVersion.V_2_1;
                case "2.x" -> SmbVersion.V_2_X;
                case "3.0" -> SmbVersion.V_3_0;
                case "3.0.2" -> SmbVersion.V_3_0_2;
                case "3.1.1" -> SmbVersion.V_3_1_1;
                case "3.x" -> SmbVersion.V_3_X;
                default -> throw new InvalidSettingsException("Unknown SMB version: " + value);
            };
        }

        @Override
        public void save(final SmbVersion param, final NodeSettingsWO settings) {
            final var value = switch (param) {
                case AUTO -> "auto";
                case V_2_0_2 -> "2.0.2";
                case V_2_1 -> "2.1";
                case V_2_X -> "2.x";
                case V_3_0 -> "3.0";
                case V_3_0_2 -> "3.0.2";
                case V_3_1_1 -> "3.1.1";
                case V_3_X -> "3.x";
            };
            settings.addString(KEY, value);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][] { { KEY } };
        }
    }

    SmbProtocolVersion getSmbProtocolVersion() {
        return switch (m_smbVersion) {
            case AUTO -> SmbProtocolVersion.AUTO;
            case V_2_0_2 -> SmbProtocolVersion.V_2_0_2;
            case V_2_1 -> SmbProtocolVersion.V_2_1;
            case V_2_X -> SmbProtocolVersion.V_2_X;
            case V_3_0 -> SmbProtocolVersion.V_3_0;
            case V_3_0_2 -> SmbProtocolVersion.V_3_0_2;
            case V_3_1_1 -> SmbProtocolVersion.V_3_1_1;
            case V_3_X -> SmbProtocolVersion.V_3_X;
        };
    }

    @Layout(AdvancedSection.class)
    @Widget(title = "Use encryption", description = """
            If enabled, the node will use SMB Encryption that provides SMB data end-to-end encryption, \
            if also supported by the SMB server. This option requires SMB version 3.x.""")
    @Persist(configKey = "useEncryption")
    boolean m_useEncryption = false;

    // ----- INTERNAL AUTHENTICATION PARAMETER CLASS -----

    static final class AuthenticationParameters implements NodeParameters {

        static final String CFG_KEY = "auth";
        private static final String CFG_KEY_USER_PWD = "user_pwd";
        private static final String ENTRY_KEY_USE_CREDENTIALS = "use_credentials";

        enum AuthenticationMethod {
            @Label(value = "Username & password", description = """
                    Authenticate with a username and password using NTLM. Either enter a username and password, \
                    in which case the password will be persistently stored (in encrypted form) with the workflow. \
                    Or select "Use credentials" and choose a credentials flow variable to supply the username and \
                    password. The username field also accepts usernames of the form DOMAIN\\user and user@DOMAIN.""")
            USERNAME_PASSWORD,

            @Label(value = "Use credentials", description = """
                    Authenticate with a username and password stored in a credentials flow variable using NTLM. \
                    The username field also accepts usernames of the form DOMAIN\\user and user@DOMAIN.""")
            CREDENTIALS,

            @Label(value = "Kerberos", description = """
                    Authenticate using an existing Kerberos ticket.""")
            KERBEROS,

            @Label(value = "Guest", description = """
                    Authenticate as the Guest user (without password).""")
            GUEST,

            @Label(value = "Anonymous", description = """
                    Authenticate with an empty username (without password).""")
            ANONYMOUS
        }

        @Widget(title = "Authentication method", description = "Specify the authentication method to use.")
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

            private static final String ENTRY_KEY = "type";

            @Override
            public AuthenticationMethod load(final NodeSettingsRO settings) throws InvalidSettingsException {
                final var typeString = settings.getString(ENTRY_KEY, "");
                return switch (typeString) {
                    case "user_pwd" -> {
                        final var useCredentials = settings.getNodeSettings(CFG_KEY_USER_PWD)
                                .getBoolean(ENTRY_KEY_USE_CREDENTIALS, false);
                        yield useCredentials ? AuthenticationMethod.CREDENTIALS : AuthenticationMethod.USERNAME_PASSWORD;
                    }
                    case "kerberos" -> AuthenticationMethod.KERBEROS;
                    case "guest" -> AuthenticationMethod.GUEST;
                    case "anonymous" -> AuthenticationMethod.ANONYMOUS;
                    default -> throw new InvalidSettingsException(
                            String.format("Unknown authentication type: '%s'", typeString));
                };
            }

            @Override
            public void save(final AuthenticationMethod param, final NodeSettingsWO settings) {
                final var value = switch (param) {
                    case USERNAME_PASSWORD, CREDENTIALS -> "user_pwd";
                    case KERBEROS -> "kerberos";
                    case GUEST -> "guest";
                    case ANONYMOUS -> "anonymous";
                };
                settings.addString(ENTRY_KEY, value);
            }

            @Override
            public String[][] getConfigPaths() {
                return new String[][] { { ENTRY_KEY }, { CFG_KEY_USER_PWD, ENTRY_KEY_USE_CREDENTIALS } };
            }
        }

        // ----- USER PASSWORD AUTHENTICATION SETTINGS -----

        @Persist(configKey = CFG_KEY_USER_PWD)
        UserPwdAuthParameters m_userPwdAuth = new UserPwdAuthParameters();

        static final class UserPwdAuthParameters implements NodeParameters {

            private static final String ENTRY_KEY_CREDENTIALS = "credentials";

            @Widget(title = "Username & password", description = """
                    Authentication settings for username and password. Select "Use credentials" as authentication \
                    method to provide the username and password via a credentials flow variable.""")
            @Effect(predicate = IsUserPwdAuth.class, type = EffectType.SHOW)
            @CredentialsWidget
            @Persistor(UserPasswordPersistor.class)
            Credentials m_userPassword = new Credentials("", "");

            @ValueProvider(UseCredentialsValueProvider.class)
            @Persist(configKey = ENTRY_KEY_USE_CREDENTIALS)
            boolean m_useCredentials;

            @Widget(title = "Credentials", description = "Use credentials from a flow variable.")
            @Effect(predicate = IsCredentialsAuth.class, type = EffectType.SHOW)
            @ChoicesProvider(CredentialsFlowVarChoicesProvider.class)
            @Persist(configKey = ENTRY_KEY_CREDENTIALS)
            String m_credentialsFlowVarName;

            static final class UserPasswordPersistor implements NodeParametersPersistor<Credentials> {

                /**
                 * See {@link IDWithSecretAuthProviderSettings#SECRET_ENCRYPTION_KEY}.
                 */
                private static final String USER_PWD_ENCRYPTION_KEY = "laig9eeyeix:ae$Lo6lu";
                private static final String ENTRY_KEY_USER = "user";
                private static final String ENTRY_KEY_PASSWORD = "password";

                @Override
                public Credentials load(final NodeSettingsRO settings) throws InvalidSettingsException {
                    final var user = settings.getString(ENTRY_KEY_USER, "");
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
                    // See AP-14067: It is not possible to overwrite password fields
                    return new String[][] { { ENTRY_KEY_USER } };
                }
            }
        }
    }
}
