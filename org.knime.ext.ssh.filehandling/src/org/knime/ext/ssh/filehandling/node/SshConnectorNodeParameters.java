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

import java.util.Optional;
import java.util.function.Supplier;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.ModifiableNodeCreationConfiguration;
import org.knime.core.node.context.ports.ModifiablePortsConfiguration;
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
import org.knime.ext.ssh.filehandling.fs.SshFileSystem;
import org.knime.ext.ssh.filehandling.node.auth.KeyFileAuthProviderSettings;
import org.knime.filehandling.core.connections.FSLocation;
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
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.PatternValidation.IsNotEmptyValidation;

/**
 * Node parameters for SSH Connector.
 *
 * @author AI Migration Pipeline v1.1
 * @author Kai Franze, KNIME GmbH, Germany
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class SshConnectorNodeParameters implements NodeParameters {

    // ----- LAYOUTS -----

    @Section(title = "Connection Settings")
    interface ConnectionSection {
    }

    @Section(title = "Authentication Settings")
    @After(ConnectionSection.class)
    interface AuthenticationSection {
    }

    @Section(title = "File System Settings")
    @After(AuthenticationSection.class)
    interface FileSystemSection {
    }

    @Section(title = "Advanced")
    @After(FileSystemSection.class)
    @Advanced
    interface AdvancedSection {
    }

    // ----- CONNECTION PARAMETERS -----

    @Layout(ConnectionSection.class)
    @Widget(title = "Host", description = "Address of the host where the SSH server runs.")
    @TextInputWidget(minLengthValidation = IsNotEmptyValidation.class)
    @ValueReference(HostRef.class)
    String m_host = "localhost";

    static final class HostRef implements ParameterReference<String> {
    }

    @Layout(ConnectionSection.class)
    @Widget(title = "Port", description = "Port that the SSH server is listening on for incoming connections.")
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class, maxValidation = PortMaxValidation.class)
    @ValueReference(PortRef.class)
    int m_port = 22;

    static final class PortRef implements ParameterReference<Integer> {
    }

    static final class PortMaxValidation extends MaxValidation {
        @Override
        protected double getMax() {
            return 65535;
        }
    }

    // ----- AUTHENTICATION PARAMETERS -----

    @Layout(AuthenticationSection.class)
    @Persist(configKey = AuthenticationParameters.CFG_KEY)
    @ValueReference(AuthenticationParametersRef.class)
    AuthenticationParameters m_authentication = new AuthenticationParameters();

    static final class AuthenticationParametersRef implements ParameterReference<AuthenticationParameters> {
    }

    // ----- FILESYSTEM PARAMETERS -----

    @Layout(FileSystemSection.class)
    @Widget(title = "Working directory", description = """
            Specify the working directory of the resulting file system connection, using the Path syntax explained \
            above. The working directory must be specified as an absolute path. A working directory allows downstream \
            nodes to access files/folders using relative paths, i.e. paths that do not have a leading slash. \
            The default working directory is the root "/".""")
    @FileSelectionWidget(SingleFileSelectionMode.FOLDER)
    @WithCustomFileSystem(connectionProvider = FileSystemConnectionProvider.class)
    String m_workingDirectory = "/";

    // ----- ADVANCED PARAMETERS -----

    @Layout(AdvancedSection.class)
    @Widget(title = "Connection timeout", description = """
            Timeout in seconds to establish a connection or 0 for an infinite timeout.""")
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    @ValueReference(ConnectionTimeoutRef.class)
    int m_connectionTimeout = 30;

    static final class ConnectionTimeoutRef implements ParameterReference<Integer> {
    }

    @Layout(AdvancedSection.class)
    @Widget(title = "Maximum SFTP sessions", description = """
            Number of SFTP sessions the node will try to open. Actual number of sessions may be less, \
            depending on the limits of the SSH server.""")
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    @ValueReference(MaxSessionCountRef.class)
    int m_maxSessionCount = 8;

    static final class MaxSessionCountRef implements ParameterReference<Integer> {
    }

    @Layout(AdvancedSection.class)
    @Widget(title = "Maximum concurrent shell sessions", description = """
            Number of concurrent shell sessions to allow. This resource is shared with the SFTP sessions, \
            so decreasing the number of SFTP sessions will allow for more shells and vice versa.""")
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    @ValueReference(MaxExecChannelCountRef.class)
    int m_maxExecChannelCount = 1;

    static final class MaxExecChannelCountRef implements ParameterReference<Integer> {
    }

    @Layout(AdvancedSection.class)
    @Widget(title = "Use known hosts file", description = """
            If this option is selected, then provided known hosts file will be used to validate the (public) key \
            of the SSH server. If not selected, then server key will not be validated.""")
    @Persist(configKey = SshConnectorNodeSettings.KEY_USE_KNOWN_HOSTS)
    @ValueReference(UseKnownHostsFileRef.class)
    boolean m_useKnownHostsFile;

    interface UseKnownHostsFileRef extends ParameterReference<Boolean> {
    }

    @Layout(AdvancedSection.class)
    @Widget(title = "Known hosts file", description = "Path to the known hosts file for SSH server key validation.")
    @Effect(predicate = UseKnownHostsFilePredicate.class, type = EffectType.SHOW)
    @Persistor(KnownHostsFilePersistor.class)
    @ValueReference(KnownHostsFileRef.class)
    FileSelection m_knownHostsFile = new FileSelection();

    static final class KnownHostsFileRef implements ParameterReference<FileSelection> {
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
            initializer.computeAfterOpenDialog();
        }

        @Override
        public FSConnectionProvider computeState(final NodeParametersInput parametersInput) {
            return () -> { // NOSONAR: Longer lambda acceptable, as it improves readability
                final var connectionProviderConfig = new FileSystemConnectionProviderConfiguration( //
                        m_hostSupplier.get(), //
                        m_portSupplier.get(), //
                        m_connectionTimeoutSupplier.get(), //
                        m_maxSessionCountSupplier.get(), //
                        m_maxExecChannelCountSupplier.get(), //
                        m_useKnownHostsFileSupplier.get(), //
                        m_knownHostsFileSupplier.get().getFSLocation(), //
                        SshFileSystem.PATH_SEPARATOR, // Don't depend on the current working directory here
                        createAuthNodeSettings(m_authParametersSupplier.get()));
                final var nodeSettings = createNodeSettings(parametersInput);
                final var credentialsProvider = getCredentialsProvider(parametersInput);
                nodeSettings.loadSettingsForDialog(connectionProviderConfig);
                return SshConnectorNodeModel.createConnection(nodeSettings, credentialsProvider);
            };
        }

        private static SshConnectorNodeSettings createNodeSettings(final NodeParametersInput input) {
            final var portsConfiguration = (ModifiablePortsConfiguration) input.getPortsConfiguration();
            final var nodeCreationConfig = new ModifiableNodeCreationConfiguration(portsConfiguration);
            return new SshConnectorNodeSettings(nodeCreationConfig);
        }

        private static CredentialsProvider getCredentialsProvider(final NodeParametersInput input) {
            return ((NodeParametersInputImpl) input).getCredentialsProvider().orElseThrow();
        }

        private static final NodeSettingsRO createAuthNodeSettings(final AuthenticationParameters params) {
            final var settings = new NodeSettings(AuthenticationParameters.CFG_KEY);
            AuthenticationParameters.AuthenticationMethodPersistor.saveInternal(params.m_type, settings);
            final var userPwdSettings = createUserPwdAuth(params.m_userPasswordAuth);
            settings.addNodeSettings(userPwdSettings);
            final var keyFileSettings = createKeyFileSettings(params.m_keyFileAuth);
            settings.addNodeSettings(keyFileSettings);
            return settings;
        }

        private static final NodeSettings createUserPwdAuth(final LegacyCredentials params) {
            final var credentialsSettings = new NodeSettings(AuthenticationParameters.CFG_KEY_USER_PWD_V2);
            new LegacyCredentialsAuthProviderSettings(StandardAuthTypes.USER_PASSWORD_V2, true, params)
                    .saveSettingsForModel(credentialsSettings);
            return credentialsSettings;
        }

        private static final NodeSettings createKeyFileSettings(
                final AuthenticationParameters.KeyFileAuthParameters keyFileAuth) {
            final var keyFileSettings = new NodeSettings(AuthenticationParameters.CFG_KEY_KEY_FILE);
            AuthenticationParameters.KeyFileAuthParameters.KeyFileUserPersistor.saveInternal(keyFileAuth.m_keyFileUser,
                    keyFileSettings);
            AuthenticationParameters.KeyFileAuthParameters.KeyFilePwdPersistor.saveInternal(keyFileAuth.m_keyFilePwd,
                    keyFileSettings);
            LegacyReaderFileSelectionPersistor.save(keyFileAuth.m_keyFile, keyFileSettings,
                    AuthenticationParameters.KeyFileAuthParameters.KeyFilePersistor.CFG_KEY_FILE);
            return keyFileSettings;
        }

        static record FileSystemConnectionProviderConfiguration( //
                String host, //
                Integer port, //
                int connectionTimeout, //
                int maxSessionCount, //
                int maxExecChannelCount, //
                boolean useKnownHostsFile, //
                FSLocation knownHostsFile, //
                String workingDirectory, //
                NodeSettingsRO authSettings) {
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
            super(SshConnectorNodeSettings.KEY_KNOWN_HOSTS_FILE);
        }
    }

    // ----- INTERNAL PARAMETER CLASSES -----

    private static final class AuthenticationParameters implements NodeParameters {

        private static final String CFG_KEY = "auth";
        private static final String CFG_KEY_USER_PWD = "user_pwd";
        private static final String CFG_KEY_USER_PWD_V2 = "user_pwd_v2";
        private static final String CFG_KEY_KEY_FILE = "key";
        private static final String ENTRY_KEY_USE_CREDENTIALS = "use_credentials";

        enum AuthenticationMethod {
            @Label(value = "Username & password", description = """
                    Authenticate with a username and password. Either enter a username and password, in which case \
                    the password will be persistently stored (in encrypted form) with the workflow. Or overwrite \
                    the setting using a credentials flow variable to supply the username and password. The \
                    password may be empty if the SSH server permits empty passwords.""")
            USERNAME_PASSWORD,

            @Label(value = "Key file", description = """
                    Authenticate using a private key file. You have to specify the Username and the private Key file. \
                    A Key passphrase can optionally be provided, in case the private key file is \
                    passphrase-protected. Note that the passphrase is persistently stored (in encrypted form) in the \
                    settings of this node and will be saved with the workflow. This node supports the following \
                    private key formats: RFC4716 (default OpenSSH2 private key format), PKCS#8, PKCS#1 (traditional \
                    PEM format, OpenSSL-compatible), and ppk (PuTTY format).""")
            KEY_FILE
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
                saveInternal(param, settings);
            }

            private static void saveInternal(final AuthenticationMethod param, final NodeSettingsWO settings) {
                switch (param) {
                case KEY_FILE -> settings.addString(ENTRY_KEY, CFG_KEY_KEY_FILE);
                case USERNAME_PASSWORD -> settings.addString(ENTRY_KEY, CFG_KEY_USER_PWD_V2);
                }
            }

            @Override
            public String[][] getConfigPaths() {
                return new String[][] { { ENTRY_KEY }, { CFG_KEY_USER_PWD, ENTRY_KEY_USE_CREDENTIALS } };
            }
        }

        // ----- SECOND LEVEL NESTING NEEDED FOR BACKWARD COMPATIBILITY -----

        @Persist(configKey = CFG_KEY_USER_PWD_V2)
        @Migration(LoadFromUserPwdAuthMigration.class)
        @Effect(predicate = IsUserPwdAuth.class, type = EffectType.SHOW)
        @Widget(title = "Username & Password", description = "Credentials for username and password authentication.")
        LegacyCredentials m_userPasswordAuth = new LegacyCredentials(new Credentials());

        static final class LoadFromUserPwdAuthMigration
                extends LegacyCredentialsAuthProviderSettings.FromUserPasswordAuthProviderSettingsMigration {

            protected LoadFromUserPwdAuthMigration() {
                super(new UserPasswordAuthProviderSettings(StandardAuthTypes.USER_PASSWORD, true));
            }

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

            static final class KeyFilePwdRef implements ParameterReference<Optional<Credentials>> {
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
                    saveInternal(param, settings);
                }

                private static void saveInternal(final String param, final NodeSettingsWO settings) {
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
                    saveInternal(param, settings);
                }

                private static void saveInternal(final Optional<Credentials> param, final NodeSettingsWO settings) {
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
}
