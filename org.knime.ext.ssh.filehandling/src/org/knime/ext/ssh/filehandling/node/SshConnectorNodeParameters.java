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

package org.knime.ext.ssh.filehandling.node;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersInputImpl;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FSConnectionProvider;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSelection;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSelectionWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.LegacyReaderFileSelectionPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.SingleFileSelectionMode;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.WithCustomFileSystem;
import org.knime.core.webui.node.dialog.defaultdialog.setting.credentials.LegacyCredentials;
import org.knime.core.webui.node.dialog.defaultdialog.setting.credentials.LegacyCredentialsAuthProviderSettings;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.custom.CustomValidation;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.custom.CustomValidationProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.custom.SimpleValidation;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.custom.ValidationCallback;
import org.knime.ext.ssh.filehandling.fs.ConnectionToNodeModelBridge;
import org.knime.ext.ssh.filehandling.fs.SshFSConnection;
import org.knime.ext.ssh.filehandling.fs.SshFSConnectionConfig;
import org.knime.ext.ssh.filehandling.fs.SshFileSystem;
import org.knime.ext.ssh.filehandling.node.auth.KeyFileAuthProviderSettings;
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
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.OptionalWidget;
import org.knime.node.parameters.widget.OptionalWidget.DefaultValueProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.credentials.Credentials;
import org.knime.node.parameters.widget.credentials.PasswordWidget;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MaxValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsNonNegativeValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.PatternValidation.IsNotBlankValidation;

/**
 * Node parameters for SSH Connector.
 *
 * @author AI Migration Pipeline v1.1
 * @author Kai Franze, KNIME GmbH, Germany
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class SshConnectorNodeParameters implements NodeParameters {

    // ----- LAYOUTS -----

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

    @Section(title = "Timeouts")
    @After(FileSystemSection.class)
    @Advanced
    interface TimeoutsSection {
    }

    // ----- CONNECTION PARAMETERS -----

    @Layout(ConnectionSection.class)
    @Widget(title = "Host", description = "Address of the host where the SSH server runs.")
    @TextInputWidget(patternValidation = IsNotBlankValidation.class)
    @ValueReference(HostRef.class)
    String m_host = "localhost";

    static interface HostRef extends ParameterReference<String> {
    }

    @Layout(ConnectionSection.class)
    @Widget(title = "Port", description = "Port that the SSH server is listening on for incoming connections.")
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class, maxValidation = PortMaxValidation.class)
    @ValueReference(PortRef.class)
    int m_port = 22;

    static interface PortRef extends ParameterReference<Integer> {
    }

    static final class PortMaxValidation extends MaxValidation {
        @Override
        protected double getMax() {
            return 65535;
        }
    }

    @Layout(ConnectionSection.class)
    @Widget(title = "Maximum SFTP sessions", description = """
            Number of SFTP sessions the node will try to open. Actual number of sessions may be less,
            depending on the limits of the SSH server.""")
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    @ValueReference(MaxSessionCountRef.class)
    @Advanced
    int m_maxSessionCount = SshConnectorNodeModel.DEFAULT_MAX_SESSION_COUNT;

    static interface MaxSessionCountRef extends ParameterReference<Integer> {
    }

    @Layout(ConnectionSection.class)
    @Widget(title = "Maximum concurrent shell sessions", description = """
            Number of concurrent shell sessions to allow. This resource is shared with the SFTP sessions,
            so decreasing the number of SFTP sessions will allow for more shells and vice versa.""")
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    @ValueReference(MaxExecChannelCountRef.class)
    @Advanced
    int m_maxExecChannelCount = SshConnectorNodeModel.DEFAULT_MAX_EXEC_CHANNEL_COUNT;

    static interface MaxExecChannelCountRef extends ParameterReference<Integer> {
    }

    // ----- AUTHENTICATION PARAMETERS -----

    @Layout(AuthenticationSection.class)
    @Persist(configKey = AuthenticationParameters.CFG_KEY)
    @ValueReference(AuthenticationParametersRef.class)
    AuthenticationParameters m_authentication = new AuthenticationParameters();

    static interface AuthenticationParametersRef extends ParameterReference<AuthenticationParameters> {
    }

    @Layout(AuthenticationSection.class)
    @Widget(title = "Use known hosts file", description = """
            If this option is selected, then provided known hosts file will be used to validate the (public) key
            of the SSH server. If not selected, then server key will not be validated.""")
    @Persist(configKey = "useKnownHosts")
    @ValueReference(UseKnownHostsFileRef.class)
    @Advanced
    boolean m_useKnownHostsFile;

    interface UseKnownHostsFileRef extends ParameterReference<Boolean> {
    }

    @Layout(AuthenticationSection.class)
    @Widget(title = "Known hosts file", description = "Path to the known hosts file for SSH server key validation.")
    @Effect(predicate = UseKnownHostsFilePredicate.class, type = EffectType.SHOW)
    @Persistor(KnownHostsFilePersistor.class)
    @ValueReference(KnownHostsFileRef.class)
    @CustomValidation(FileValidator.class)
    @Advanced
    FileSelection m_knownHostsFile = new FileSelection();

    static interface KnownHostsFileRef extends ParameterReference<FileSelection> {
    }

    // ----- FILESYSTEM PARAMETERS -----

    @Layout(FileSystemSection.class)
    @Widget(title = "Working directory", description = """
            Specify the working directory of the resulting file system connection, using the Path syntax explained
            above. The working directory must be specified as an absolute path. A working directory allows downstream
            nodes to access files/folders using relative paths, i.e. paths that do not have a leading slash.
            The default working directory is the root "/".""")
    @FileSelectionWidget(SingleFileSelectionMode.FOLDER)
    @WithCustomFileSystem(connectionProvider = FileSystemConnectionProvider.class)
    @ValueReference(WorkingDirectoryRef.class)
    @CustomValidation(WorkingDirectoryValidator.class)
    String m_workingDirectory = SshFileSystem.PATH_SEPARATOR;

    static class WorkingDirectoryValidator extends SimpleValidation<String> {

        @Override
        public void validate(final String currentValue) throws InvalidSettingsException {
            validateWorkingDirectory(currentValue);
        }
    }

    static interface WorkingDirectoryRef extends ParameterReference<String> {
    }

    // ----- TIMEOUTS PARAMETERS -----

    @Layout(TimeoutsSection.class)
    @Widget(title = "Connection timeout", description = """
            Timeout in seconds to establish a connection or 0 for an infinite timeout.""")
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    @ValueReference(ConnectionTimeoutRef.class)
    int m_connectionTimeout = SshConnectorNodeModel.DEFAULT_CONNECTION_TIMEOUT_SECONDS;

    static interface ConnectionTimeoutRef extends ParameterReference<Integer> {
    }

    /**
     * Provides a {@link FSConnectionProvider} based on the SSH connection settings.
     * We need this to support setting the working directory using the exact same
     * SSH connection this node is providing.
     */
    static final class FileSystemConnectionProvider implements StateProvider<FSConnectionProvider> {

        private Supplier<String> m_hostSupplier;
        private Supplier<Integer> m_portSupplier;
        private Supplier<Integer> m_connectionTimeoutSupplier;
        private Supplier<Integer> m_maxSessionCountSupplier;
        private Supplier<Integer> m_maxExecChannelCountSupplier;
        private Supplier<Boolean> m_useKnownHostsFileSupplier;
        private Supplier<FileSelection> m_knownHostsFileSupplier;
        private Supplier<AuthenticationParameters> m_authParametersSupplier;
        private Supplier<String> m_workingDirectorySupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_hostSupplier = initializer.computeFromValueSupplier(HostRef.class);
            m_portSupplier = initializer.computeFromValueSupplier(PortRef.class);
            m_connectionTimeoutSupplier = initializer.computeFromValueSupplier(ConnectionTimeoutRef.class);
            m_maxSessionCountSupplier = initializer.computeFromValueSupplier(MaxSessionCountRef.class);
            m_maxExecChannelCountSupplier = initializer.computeFromValueSupplier(MaxExecChannelCountRef.class);
            m_useKnownHostsFileSupplier = initializer.computeFromValueSupplier(UseKnownHostsFileRef.class);
            m_knownHostsFileSupplier = initializer.computeFromValueSupplier(KnownHostsFileRef.class);
            m_authParametersSupplier = initializer.computeFromValueSupplier(AuthenticationParametersRef.class);
            m_workingDirectorySupplier = initializer.computeFromValueSupplier(WorkingDirectoryRef.class);
            initializer.computeAfterOpenDialog();
        }

        @Override
        public FSConnectionProvider computeState(final NodeParametersInput parametersInput) {
            return () -> { // NOSONAR: Longer lambda acceptable, as it improves readability

                var workingDir = m_workingDirectorySupplier.get();
                if (StringUtils.isBlank(workingDir) || !workingDir.startsWith(SshFileSystem.PATH_SEPARATOR)) {
                    workingDir = SshFileSystem.PATH_SEPARATOR;
                }
                final var params = new SshConnectorNodeParameters();
                params.m_authentication = m_authParametersSupplier.get();
                params.m_connectionTimeout = m_connectionTimeoutSupplier.get();
                params.m_host = m_hostSupplier.get();
                params.m_knownHostsFile = m_knownHostsFileSupplier.get();
                params.m_maxExecChannelCount = m_maxExecChannelCountSupplier.get();
                params.m_maxSessionCount = m_maxSessionCountSupplier.get();
                params.m_port = m_portSupplier.get();
                params.m_useKnownHostsFile = m_useKnownHostsFileSupplier.get();
                params.m_workingDirectory = workingDir;
                final var credentialsProvider = getCredentialsProvider(parametersInput);
                params.validateOnConfigure(credentialsProvider);

                final var ports = parametersInput.getInPortSpecs();
                final var cfg = SshConnectorNodeModel.createConnectionConfig(params,
                        Optional.ofNullable(ports.length > 0 ? ports[0] : null),
                        credentialsProvider);
                return new SshFSConnection(cfg);
            };
        }
    }

    static final class UseKnownHostsFilePredicate implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getBoolean(UseKnownHostsFileRef.class).isTrue();
        }
    }

    static final class KnownHostsFilePersistor extends LegacyReaderFileSelectionPersistor {

        public KnownHostsFilePersistor() {
            super("knownHostsFile");
        }
    }

    // ----- INTERNAL PARAMETER CLASSES -----

    static final class AuthenticationParameters implements NodeParameters {

        private static final String CFG_KEY = "auth";
        private static final String CFG_KEY_USER_PWD = "user_pwd";
        private static final String CFG_KEY_USER_PWD_V2 = "user_pwd_v2";
        private static final String CFG_KEY_KEY_FILE = "key";

        enum AuthenticationMethod {
            @Label(value = "Username & password", description = """
                    Authenticate with a username and password. Either enter a username and password, in which case
                    the password will be persistently stored (in encrypted form) with the workflow. Or overwrite
                    the setting using a credentials flow variable to supply the username and password. The
                    password may be empty if the SSH server permits empty passwords.""")
            USERNAME_PASSWORD,

            @Label(value = "Key file", description = """
                    Authenticate using a private key file. You have to specify the Username and the private Key file.
                    A Key passphrase can optionally be provided, in case the private key file is
                    passphrase-protected. Note that the passphrase is persistently stored (in encrypted form) in the
                    settings of this node and will be saved with the workflow. This node supports the following
                    private key formats: RFC4716 (default OpenSSH2 private key format), PKCS#8, PKCS#1 (traditional
                    PEM format, OpenSSL-compatible), and ppk (PuTTY format).""")
            KEY_FILE
        }

        @Widget(title = "Authentication method", description = "Specify the authentication method to use.")
        @ValueReference(AuthenticationMethodRef.class)
        @Persistor(AuthenticationMethodPersistor.class)
        AuthenticationMethod m_type = AuthenticationMethod.USERNAME_PASSWORD;

        static interface AuthenticationMethodRef extends ParameterReference<AuthenticationMethod> {
        }

        static final class IsUserPwdAuth implements EffectPredicateProvider {
            @Override
            public EffectPredicate init(final PredicateInitializer i) {
                return i.getEnum(AuthenticationMethodRef.class).isOneOf(AuthenticationMethod.USERNAME_PASSWORD);
            }
        }

        static final class IsKeyFileAuth implements EffectPredicateProvider {
            @Override
            public EffectPredicate init(final PredicateInitializer i) {
                return i.getEnum(AuthenticationMethodRef.class).isOneOf(AuthenticationMethod.KEY_FILE);
            }
        }

        static final class AuthenticationMethodPersistor implements NodeParametersPersistor<AuthenticationMethod> {

            private static final String ENTRY_KEY = "type";

            @Override
            public AuthenticationMethod load(final NodeSettingsRO settings) throws InvalidSettingsException {
                final var typeString = settings.getString(ENTRY_KEY, "");
                if (typeString.equals(CFG_KEY_KEY_FILE)) {
                    return AuthenticationMethod.KEY_FILE;
                } else if (typeString.equals(CFG_KEY_USER_PWD) || typeString.equals(CFG_KEY_USER_PWD_V2)) {
                    return AuthenticationMethod.USERNAME_PASSWORD;
                }
                throw new InvalidSettingsException(
                        String.format("Unknown authentication method: '%s'. Possible values: '%s', '%s'", typeString,
                                CFG_KEY_USER_PWD_V2, CFG_KEY_KEY_FILE));

            }

            @Override
            public void save(final AuthenticationMethod param, final NodeSettingsWO settings) {
                switch (param) {
                case KEY_FILE -> settings.addString(ENTRY_KEY, CFG_KEY_KEY_FILE);
                case USERNAME_PASSWORD -> settings.addString(ENTRY_KEY, CFG_KEY_USER_PWD_V2);
                }
            }

            @Override
            public String[][] getConfigPaths() {
                return new String[][] { { ENTRY_KEY } };
            }
        }

        // ----- SECOND LEVEL NESTING NEEDED FOR BACKWARD COMPATIBILITY -----

        @Persist(configKey = CFG_KEY_USER_PWD_V2)
        @Migration(LoadFromUserPwdAuthMigration.class)
        @Effect(predicate = IsUserPwdAuth.class, type = EffectType.SHOW)
        @Widget(title = "Username & Password", description = "Credentials for username and password authentication.")
        @CustomValidation(UserPasswordValidator.class)
        @ValueReference(UserPasswordRef.class)
        LegacyCredentials m_userPasswordAuth = new LegacyCredentials(
                new Credentials(System.getProperty("user.name"), ""));

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

        @Persist(configKey = CFG_KEY_KEY_FILE)
        @Effect(predicate = IsKeyFileAuth.class, type = EffectType.SHOW)
        KeyFileAuthParameters m_keyFileAuth = new KeyFileAuthParameters();

        static final class KeyFileAuthParameters implements NodeParameters {

            @Widget(title = "Username", description = "Username for key file authentication.")
            @Persistor(KeyFileUserPersistor.class)
            @TextInputWidget(patternValidation = IsNotBlankValidation.class)
            String m_keyFileUser = System.getProperty("user.name");

            @Widget(title = "Key passphrase", description = "Passphrase for key file authentication.")
            @OptionalWidget(defaultProvider = KeyFilePasswordDefaultProvider.class)
            @PasswordWidget
            @Persistor(KeyFilePwdPersistor.class)
            @ValueReference(KeyFilePwdRef.class)
            Optional<Credentials> m_keyFilePwd = Optional.empty();

            @Widget(title = "Key file", description = "Private key file for authentication.")
            @Persistor(KeyFilePersistor.class)
            FileSelection m_keyFile = new FileSelection();

            static interface KeyFilePwdRef extends ParameterReference<Optional<Credentials>> {
            }

            static final class KeyFilePasswordDefaultProvider implements DefaultValueProvider<Credentials> {
                @Override
                public Credentials computeState(final NodeParametersInput parametersInput) {
                    return new Credentials();
                }
            }

            static final class KeyFileUserPersistor implements NodeParametersPersistor<String> {

                private static final String ENTRY_KEY_USER = "user";

                @Override
                public String load(final NodeSettingsRO settings) throws InvalidSettingsException {
                    return settings.getString(ENTRY_KEY_USER, System.getProperty("user.name"));
                }

                @Override
                public void save(final String param, final NodeSettingsWO settings) {
                    settings.addString(ENTRY_KEY_USER, param);
                }

                @Override
                public String[][] getConfigPaths() {
                    return new String[][] { { ENTRY_KEY_USER } };
                }
            }

            static final class KeyFilePwdPersistor implements NodeParametersPersistor<Optional<Credentials>> {

                /**
                 * See {@link KeyFileAuthProviderSettings#SECRET_KEY}.
                 */
                private static final String KEY_FILE_ENCRYPTION_KEY = "ekerjvjhmzle,ptktysq";
                private static final String ENTRY_KEY_PASSPHRASE = "passphrase";
                private static final String ENTRY_KEY_USE_PASSPHRASE = "use_passphrase";

                @Override
                public Optional<Credentials> load(final NodeSettingsRO settings) throws InvalidSettingsException {
                    if (!settings.getBoolean(ENTRY_KEY_USE_PASSPHRASE, false)) {
                        return Optional.empty();
                    }
                    if (!settings.containsKey(ENTRY_KEY_PASSPHRASE)) {
                        return Optional.empty();
                    }

                    final var passphrase = settings.getPassword(ENTRY_KEY_PASSPHRASE, KEY_FILE_ENCRYPTION_KEY, "");
                    if (passphrase == null || passphrase.isEmpty()) {
                        return Optional.empty();
                    }

                    return Optional.of(new Credentials("", passphrase));
                }

                @Override
                public void save(final Optional<Credentials> param, final NodeSettingsWO settings) {
                    final var passphrase = param.map(Credentials::getPassword).orElse("");
                    settings.addPassword(ENTRY_KEY_PASSPHRASE, KEY_FILE_ENCRYPTION_KEY, passphrase);
                    settings.addBoolean(ENTRY_KEY_USE_PASSPHRASE, param.isPresent());
                }

                @Override
                public String[][] getConfigPaths() {
                    return new String[0][]; // See AP-14067: It is not possible to overwrite password fields
                }
            }

            static final class KeyFilePersistor extends LegacyReaderFileSelectionPersistor {

                private static final String CFG_KEY_FILE = "file";

                KeyFilePersistor() {
                    super(CFG_KEY_FILE);
                }
            }
        }
    }

    static class FileValidator extends SimpleValidation<FileSelection> {

        @Override
        public void validate(final FileSelection currentValue) throws InvalidSettingsException {
            validateFile("Value", currentValue);
        }
    }

    void validateOnConfigure(final CredentialsProvider credentialsProvider) throws InvalidSettingsException {
        CheckUtils.checkSetting(StringUtils.isNotBlank(m_host), "Host must be specified.");

        CheckUtils.checkSetting(m_port > 0 && m_port <= 65535, "Port must be between 1 and 65535.");

        CheckUtils.checkSetting(m_maxSessionCount >= 0, "Maximum number of SFTP sessions must not be negative.");
        CheckUtils.checkSetting(m_maxExecChannelCount >= 0,
                "Maximum number of concurrent shell sessions must not be negative.");

        CheckUtils.checkSetting(m_connectionTimeout >= 0, "Connection timeout must not be negative.");

        if (m_useKnownHostsFile) {
            validateFile("Known hosts file", m_knownHostsFile);
        }
        validateWorkingDirectory(m_workingDirectory);

        switch (m_authentication.m_type) {
        case KEY_FILE -> {
            CheckUtils.checkSetting(StringUtils.isNotBlank(m_authentication.m_keyFileAuth.m_keyFileUser),
                    "Username must be specified.");
            validateFile("Key file", m_authentication.m_keyFileAuth.m_keyFile);
        }
        case USERNAME_PASSWORD -> {
            final var name = m_authentication.m_userPasswordAuth.toCredentials(credentialsProvider).getUsername();
            CheckUtils.checkSetting(StringUtils.isNotBlank(name), "Username must be specified.");
        }
        }
    }

    private static void validateFile(final String type, final FileSelection selection) throws InvalidSettingsException {
        CheckUtils.checkSetting(StringUtils.isNotEmpty(selection.getFSLocation().getPath()),
                type + " must be specified.");
    }

    private static void validateWorkingDirectory(final String workingDirectory) throws InvalidSettingsException {
        CheckUtils.checkSetting(StringUtils.isNotEmpty(workingDirectory), "Working directory must be specified.");
        CheckUtils.checkSetting(workingDirectory.startsWith(SshFileSystem.PATH_SEPARATOR),
                "Working directory must be absolute (start with \"" + SshFileSystem.PATH_SEPARATOR + "\").");
    }

    private static CredentialsProvider getCredentialsProvider(final NodeParametersInput input) {
        return ((NodeParametersInputImpl) input).getCredentialsProvider().orElseThrow();
    }

    /**
     * Convert settings to a {@link SshFSConnectionConfig} instance.
     */
    SshFSConnectionConfig toFSConnectionConfig(final CredentialsProvider credentials,
            final ConnectionToNodeModelBridge bridge) throws InvalidSettingsException {

        final SshFSConnectionConfig cfg = new SshFSConnectionConfig(m_workingDirectory);
        cfg.setHost(m_host);
        cfg.setConnectionTimeout(Duration.ofSeconds(m_connectionTimeout));
        cfg.setPort(m_port);
        cfg.setMaxSftpSessionLimit(m_maxSessionCount);
        cfg.setMaxExecChannelLimit(m_maxExecChannelCount);

        // auth
        cfg.setUseKeyFile(m_authentication.m_type == AuthenticationParameters.AuthenticationMethod.KEY_FILE);
        cfg.setUseKnownHosts(m_useKnownHostsFile);

        switch (m_authentication.m_type) {
        case USERNAME_PASSWORD -> {
            final var creds = m_authentication.m_userPasswordAuth.toCredentials(credentials);
            cfg.setUserName(creds.getUsername());
            cfg.setPassword(creds.getPassword());
        }
        case KEY_FILE -> {
            cfg.setUserName(m_authentication.m_keyFileAuth.m_keyFileUser);
            cfg.setKeyFilePassword(
                    m_authentication.m_keyFileAuth.m_keyFilePwd.map(Credentials::getPassword).orElse(""));
        }
        }
        cfg.setBridge(bridge);

        return cfg;
    }
}
