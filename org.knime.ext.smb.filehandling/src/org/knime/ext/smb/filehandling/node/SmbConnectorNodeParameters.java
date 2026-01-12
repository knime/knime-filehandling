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

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersInputImpl;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FSConnectionProvider;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSelectionWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.SingleFileSelectionMode;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.WithCustomFileSystem;
import org.knime.core.webui.node.dialog.defaultdialog.setting.credentials.LegacyCredentials;
import org.knime.core.webui.node.dialog.defaultdialog.setting.credentials.LegacyCredentialsAuthProviderSettings;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.custom.CustomValidation;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.custom.CustomValidationProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.custom.ValidationCallback;
import org.knime.ext.smb.filehandling.fs.SmbFSConnection;
import org.knime.ext.smb.filehandling.fs.SmbFSConnectionConfig;
import org.knime.ext.smb.filehandling.fs.SmbFileSystem;
import org.knime.ext.smb.filehandling.fs.SmbProtocolVersion;
import org.knime.ext.smb.filehandling.node.SmbConnectorNodeParameters.AuthenticationParameters.AuthenticationMethod;
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
import org.knime.node.parameters.migration.Migrate;
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
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.credentials.Credentials;
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

    @Section(title = "Connection")
    interface ConnectionSection {
    }

    @Section(title = "Authentication")
    @After(ConnectionSection.class)
    interface AuthenticationSection {
    }

    @Section(title = "File System")
    @After(AuthenticationSection.class)
    interface FileSystemSection {
    }

    @Advanced
    @Section(title = "Timeouts")
    @After(FileSystemSection.class)
    interface TimeoutsSection {
    }

    // ----- CONNECTION MODE -----

    enum ConnectionMode {
        @Label(value = "File server", description = """
                Connect directly to a file share on a specific file server. A file server is any machine that runs
                an SMB service, such as Windows Server or Samba. For example, this is similar to connecting to
                \\\\server.company.com\\marketing using Windows Explorer.""")
        FILESERVER,

        @Label(value = "Domain", description = """
                Connect to a file share in a Windows Active Directory domain. For example, this is similar to
                connecting to \\\\company.com\\marketing using Windows Explorer, which first locates and then
                connects to an SMB service that provides the file share or DFS namespace for the domain.""")
        DOMAIN;

        SmbFSConnectionConfig.ConnectionMode toFSConnectionConfigMode() {
            return SmbFSConnectionConfig.ConnectionMode.valueOf(name());
        }
    }

    @Layout(ConnectionSection.class)
    @Widget(title = "Connect to", description = "Specifies whether to connect by specifying a file server host or a "
            + "Windows domain.")
    @ValueSwitchWidget
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
    @Widget(title = "Host", description = "Hostname of the server where the SMB service runs, e.g. server.company.com.")
    @TextInputWidget(patternValidation = IsNotBlankValidation.class)
    @Effect(predicate = IsFileserverMode.class, type = EffectType.SHOW)
    @Persist(configKey = "fileserver.host")
    @ValueReference(FileserverHostRef.class)
    String m_fileserverHost = "";

    static final class FileserverHostRef implements ParameterReference<String> {
    }

    @Layout(ConnectionSection.class)
    @Widget(title = "Port", description = "Port that the SMB server is listening on for incoming connections.")
    @Effect(predicate = IsFileserverMode.class, type = EffectType.SHOW)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class, maxValidation = PortMaxValidation.class)
    @Persist(configKey = "fileserver.port")
    @ValueReference(FileserverPortRef.class)
    int m_fileserverPort = 445;

    static final class FileserverPortRef implements ParameterReference<Integer> {
    }

    static final class PortMaxValidation extends MaxValidation {
        @Override
        protected double getMax() {
            return 65535;
        }
    }

    @Layout(ConnectionSection.class)
    @Widget(title = "Share", description = "The name of the file share provided by the SMB server. The name must not "
            + "contain any backslashes.")
    @TextInputWidget
    @Effect(predicate = IsFileserverMode.class, type = EffectType.SHOW)
    @Persist(configKey = "fileserver.share")
    @ValueReference(FileserverShareRef.class)
    @CustomValidation(FileserverShareValidation.class)
    String m_fileserverShare = "";

    static final class FileserverShareValidation implements CustomValidationProvider<String> {
        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeFromValueSupplier(FileserverShareRef.class);
        }

        static void validateFileserverShare(final String fileserverShare) throws InvalidSettingsException {
            if (StringUtils.isBlank(fileserverShare)) {
                throw new InvalidSettingsException("Share name on fileserver must be specified.");
            }
            if (fileserverShare.contains("\\")) {
                throw new InvalidSettingsException("Share name must not contain backslashes.");
            }
        }

        @Override
        public ValidationCallback<String> computeValidationCallback(final NodeParametersInput parametersInput) {
            return FileserverShareValidation::validateFileserverShare;
        }
    }

    static final class FileserverShareRef implements ParameterReference<String> {
    }

    // ----- DOMAIN SETTINGS -----

    @Layout(ConnectionSection.class)
    @Widget(title = "Domain", description = "The name of the Windows domain (Active Directory), e.g. company.com.")
    @Effect(predicate = IsDomainMode.class, type = EffectType.SHOW)
    @TextInputWidget(patternValidation = IsNotBlankValidation.class)
    @Persist(configKey = "domain.name")
    @ValueReference(DomainNameRef.class)
    String m_domainName = "";

    static final class DomainNameRef implements ParameterReference<String> {
    }

    @Layout(ConnectionSection.class)
    @Widget(title = "Share/Namespace", description = "The name of the file share or DFS namespace to access. The name "
            + "must not contain any backslashes.")
    @TextInputWidget(patternValidation = IsNotBlankValidation.class)
    @Effect(predicate = IsDomainMode.class, type = EffectType.SHOW)
    @Persist(configKey = "domain.namespace")
    @ValueReference(DomainNamespaceRef.class)
    String m_domainNamespace = "";

    static final class DomainNamespaceRef implements ParameterReference<String> {
    }

    // ----- AUTHENTICATION -----

    @Layout(AuthenticationSection.class)
    @Persist(configKey = AuthenticationParameters.CFG_KEY_AUTH)
    @ValueReference(AuthenticationParametersRef.class)
    AuthenticationParameters m_authentication = new AuthenticationParameters();

    static final class AuthenticationParametersRef implements ParameterReference<AuthenticationParameters> {
    }

    // ----- FILE SYSTEM SETTINGS -----

    @Layout(FileSystemSection.class)
    @Widget(title = "Working directory", description = """
            Specify the working directory of the resulting file system connection. The working directory must be
            specified as an absolute path starting with a backslash. A working directory allows downstream nodes
            to access files/folders using relative paths, i.e. paths that do not have a leading backslash.
            The default working directory is the root "\\".""")
    @FileSelectionWidget(value = SingleFileSelectionMode.FOLDER)
    @WithCustomFileSystem(connectionProvider = FileSystemConnectionProvider.class)
    @ValueReference(WorkingDirectoryRef.class)
    @CustomValidation(WorkingDirectoryValidation.class)
    String m_workingDirectory = SmbFileSystem.SEPARATOR;

    static final class WorkingDirectoryRef implements ParameterReference<String> {
    }

    static final class WorkingDirectoryValidation implements CustomValidationProvider<String> {
        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeFromValueSupplier(WorkingDirectoryRef.class);
        }

        @Override
        public ValidationCallback<String> computeValidationCallback(final NodeParametersInput parametersInput) {
            return WorkingDirectoryValidation::validateWorkingDirectory;
        }

        static void validateWorkingDirectory(final String workingDirectory) throws InvalidSettingsException {
            if (StringUtils.isBlank(workingDirectory) || !workingDirectory.startsWith(SmbFileSystem.SEPARATOR)) {
                throw new InvalidSettingsException("Working directory must be set to an absolute path.");
            }
        }
    }

    static final class FileSystemConnectionProvider implements StateProvider<FSConnectionProvider> {

        private Supplier<ConnectionMode> m_connectionModeSupplier;
        private Supplier<String> m_fileserverHostSupplier;
        private Supplier<Integer> m_fileserverPortSupplier;
        private Supplier<String> m_fileserverShareSupplier;
        private Supplier<String> m_domainNameSupplier;
        private Supplier<String> m_domainNamespaceSupplier;
        private Supplier<AuthenticationParameters> m_authParametersSupplier;
        private Supplier<String> m_workingDirectorySupplier;
        private Supplier<Integer> m_timeoutSupplier;
        private Supplier<SmbVersion> m_smbVersionSupplier;
        private Supplier<Boolean> m_useEncryptionSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_connectionModeSupplier = initializer.computeFromValueSupplier(ConnectionModeRef.class);
            m_fileserverHostSupplier = initializer.computeFromValueSupplier(FileserverHostRef.class);
            m_fileserverPortSupplier = initializer.computeFromValueSupplier(FileserverPortRef.class);
            m_fileserverShareSupplier = initializer.computeFromValueSupplier(FileserverShareRef.class);
            m_domainNameSupplier = initializer.computeFromValueSupplier(DomainNameRef.class);
            m_domainNamespaceSupplier = initializer.computeFromValueSupplier(DomainNamespaceRef.class);
            m_authParametersSupplier = initializer.computeFromValueSupplier(AuthenticationParametersRef.class);
            m_workingDirectorySupplier = initializer.computeFromValueSupplier(WorkingDirectoryRef.class);
            m_timeoutSupplier = initializer.computeFromValueSupplier(TimeoutRef.class);
            m_smbVersionSupplier = initializer.computeFromValueSupplier(SmbVersionRef.class);
            m_useEncryptionSupplier = initializer.computeFromValueSupplier(UseEncryptionRef.class);
            initializer.computeAfterOpenDialog();
        }

        @Override
        public FSConnectionProvider computeState(final NodeParametersInput parametersInput) {
            return () -> { // NOSONAR: Longer lambda acceptable, as it improves readability

                final var params = new SmbConnectorNodeParameters();
                params.m_connectionMode = m_connectionModeSupplier.get();
                params.m_fileserverHost = m_fileserverHostSupplier.get();
                params.m_fileserverPort = m_fileserverPortSupplier.get();
                params.m_fileserverShare = m_fileserverShareSupplier.get();
                params.m_domainName = m_domainNameSupplier.get();
                params.m_domainNamespace = m_domainNamespaceSupplier.get();
                params.m_authentication = m_authParametersSupplier.get();
                params.m_workingDirectory = m_workingDirectorySupplier.get();
                params.m_timeout = m_timeoutSupplier.get();
                params.m_smbVersion = m_smbVersionSupplier.get();
                params.m_useEncryption = m_useEncryptionSupplier.get();

                final var cp = getCredentialsProvider(parametersInput);
                params.validateOnConfigure(cp);
                return new SmbFSConnection(params.createFSConnectionConfig(cp));
            };
        }

        private static CredentialsProvider getCredentialsProvider(
                final NodeParametersInput input) {
            return ((NodeParametersInputImpl) input).getCredentialsProvider().orElseThrow();
        }
    }


    @Layout(TimeoutsSection.class)
    @Widget(title = "Read/Write timeout (seconds)", description = "The timeout in seconds for read/write operations.")
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    @ValueReference(TimeoutRef.class)
    int m_timeout = 30;

    static final class TimeoutRef implements ParameterReference<Integer> {
    }

    // ----- ADVANCED SETTINGS -----

    @Layout(ConnectionSection.class)
    @Advanced
    @Widget(title = "SMB version(s)", description = """
            Allows to enforce the usage of specific SMB protocol version(s). Selecting "Auto" will make the node
            choose the highest version supported by both this node and the SMB server.""")
    @Persistor(SmbVersionPersistor.class)
    @ValueReference(SmbVersionRef.class)
    SmbVersion m_smbVersion = SmbVersion.V_2_X;

    static final class SmbVersionRef implements ParameterReference<SmbVersion> {
    }

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

        @Label(value = "3.x (3.1.1, 3.0.2, 3.0)", //
                description = "Use the highest 3.x version supported by both parties.")
        V_3_X;

        SmbProtocolVersion toProtocolVersion() {
            return SmbProtocolVersion.valueOf(name());
        }
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

    @Layout(ConnectionSection.class)
    @Advanced
    @Widget(title = "Use encryption", description = """
            If enabled, the node will use SMB Encryption that provides SMB data end-to-end encryption,
            if also supported by the SMB server. This option requires SMB version 3.x.""")
    @Migrate(loadDefaultIfAbsent = true)
    @ValueReference(UseEncryptionRef.class)
    boolean m_useEncryption;

    static final class UseEncryptionRef implements ParameterReference<Boolean> {
    }

    // ----- INTERNAL AUTHENTICATION PARAMETER CLASS -----

    static final class AuthenticationParameters implements NodeParameters {

        private static final String CFG_KEY_AUTH = "auth";
        private static final String CFG_KEY_USER_PWD = "user_pwd";
        private static final String CFG_KEY_USER_PWD_V2 = "user_pwd_v2";
        private static final String CFG_KEY_KERBEROS = "kerberos";
        private static final String CFG_KEY_GUEST = "guest";
        private static final String CFG_KEY_ANONYMOUS = "anonymous";

        enum AuthenticationMethod {
            @Label(value = "Username & password", description = """
                    Authenticate with a username and password (NTLM). Values entered here will be stored (weakly
                    encrypted) with the workflow. The username field also accepts usernames of the form DOMAIN\\user
                    and user@DOMAIN.""")
            USERNAME_PASSWORD,

            @Label(value = "Kerberos", description = "Authenticate using an existing Kerberos ticket.")
            KERBEROS,

            @Label(value = "Guest", description = "Authenticate as the Guest user (without password).")
            GUEST,

            @Label(value = "Anonymous", description = "Authenticate with an empty username (without password).")
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

        static final class AuthenticationMethodPersistor implements NodeParametersPersistor<AuthenticationMethod> {

            private static final String CFG_KEY_TYPE = "type";

            @Override
            public AuthenticationMethod load(final NodeSettingsRO settings) throws InvalidSettingsException {
                final var authType = settings.getString(CFG_KEY_TYPE, "");

                return switch (authType) {
                case CFG_KEY_USER_PWD, CFG_KEY_USER_PWD_V2 -> AuthenticationMethod.USERNAME_PASSWORD;
                case CFG_KEY_KERBEROS -> AuthenticationMethod.KERBEROS;
                case CFG_KEY_GUEST -> AuthenticationMethod.GUEST;
                case CFG_KEY_ANONYMOUS -> AuthenticationMethod.ANONYMOUS;
                default -> throw new InvalidSettingsException(
                        String.format("Unknown authentication type: '%s'", authType));
                };
            }

            @Override
            public void save(final AuthenticationMethod param, final NodeSettingsWO settings) {
                final var value = switch (param) {
                case USERNAME_PASSWORD -> CFG_KEY_USER_PWD_V2;
                case KERBEROS -> CFG_KEY_KERBEROS;
                case GUEST -> CFG_KEY_GUEST;
                case ANONYMOUS -> CFG_KEY_ANONYMOUS;
                };
                settings.addString(CFG_KEY_TYPE, value);
            }

            @Override
            public String[][] getConfigPaths() {
                return new String[][] { { CFG_KEY_TYPE } };
            }
        }

        // ----- USER PASSWORD AUTHENTICATION SETTINGS -----

        @Persist(configKey = CFG_KEY_USER_PWD_V2)
        @Migration(LoadFromUserPwdAuthMigration.class)
        @Effect(predicate = IsUserPwdAuth.class, type = EffectType.SHOW)
        @Widget(title = "Username & Password", description = "Credentials for username and password authentication.")
        LegacyCredentials m_userPasswordAuth = new LegacyCredentials(new Credentials());

        static final class LoadFromUserPwdAuthMigration
                extends LegacyCredentialsAuthProviderSettings.FromUserPasswordAuthProviderSettingsMigration {

            protected LoadFromUserPwdAuthMigration() {
                super(new UserPasswordAuthProviderSettings(SmbFSConnectionConfig.USER_PASSWORD_AUTH_TYPE, true));
            }
        }
    }

    void validateOnConfigure(final CredentialsProvider cp) throws InvalidSettingsException {
        if (m_connectionMode == ConnectionMode.FILESERVER) {
            validateFileServerSettings();
        } else {
            validateDomainSettings();
        }

        if (m_authentication.m_type == AuthenticationMethod.USERNAME_PASSWORD) {
            final var username = m_authentication.m_userPasswordAuth.toCredentials(cp).getUsername();
            if (StringUtils.isBlank(username)) {
                throw new InvalidSettingsException(
                        "Username must be specified for username & password authentication.");
            }
        }

        WorkingDirectoryValidation.validateWorkingDirectory(m_workingDirectory);

        if (m_timeout < 0) {
            throw new InvalidSettingsException("Timeout must be greater than or equal to zero.");
        }
    }

    private void validateDomainSettings() throws InvalidSettingsException {
        if (StringUtils.isBlank(m_domainName)) {
            throw new InvalidSettingsException("Domain name must be specified");
        }

        if (StringUtils.isBlank(m_domainNamespace)) {
            throw new InvalidSettingsException("Share/Namespace must be specified");
        }
    }

    private void validateFileServerSettings() throws InvalidSettingsException {
        if (StringUtils.isBlank(m_fileserverHost)) {
            throw new InvalidSettingsException("Host must be specified");
        }

        if (m_fileserverPort < 1 || m_fileserverPort > 65535) {
            throw new InvalidSettingsException("Port must be between 1 and 65535");
        }

        FileserverShareValidation.validateFileserverShare(m_fileserverShare);
    }

    SmbFSConnectionConfig createFSConnectionConfig(final CredentialsProvider cp) {

        final var config = new SmbFSConnectionConfig(m_workingDirectory);

        config.setConnectionMode(m_connectionMode.toFSConnectionConfigMode());
        if (m_connectionMode == ConnectionMode.DOMAIN) {
            config.setDomainName(m_domainName.trim().toUpperCase(Locale.US));
            config.setDomainNamespace(m_domainNamespace.trim());
        } else {
            config.setFileserverHost(m_fileserverHost.trim().toUpperCase(Locale.US));
            config.setFileserverPort(m_fileserverPort);
            config.setFileserverShare(m_fileserverShare.trim());
        }

        if (m_authentication.m_type == AuthenticationMethod.USERNAME_PASSWORD) {
            config.setAuthType(SmbFSConnectionConfig.USER_PASSWORD_AUTH_TYPE);
            config.setUser(m_authentication.m_userPasswordAuth.toCredentials(cp).getUsername());
            config.setPassword(
                    Optional.ofNullable(m_authentication.m_userPasswordAuth.toCredentials(cp).getPassword())
                            .orElse(""));
        } else if (m_authentication.m_type == AuthenticationMethod.KERBEROS) {
            config.setAuthType(SmbFSConnectionConfig.KERBEROS_AUTH_TYPE);
        } else if (m_authentication.m_type == AuthenticationMethod.GUEST) {
            config.setAuthType(SmbFSConnectionConfig.GUEST_AUTH_TYPE);
        } else if (m_authentication.m_type == AuthenticationMethod.ANONYMOUS) {
            config.setAuthType(StandardAuthTypes.ANONYMOUS);
        }

        config.setTimeout(Duration.ofSeconds(m_timeout));
        config.setProtocolVersion(m_smbVersion.toProtocolVersion());
        config.setUseEncryption(m_useEncryption);

        return config;
    }
}
