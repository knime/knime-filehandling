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

package org.knime.archive.zip.filehandling.node;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import org.knime.archive.zip.filehandling.fs.ArchiveZipFSConnection;
import org.knime.archive.zip.filehandling.fs.ArchiveZipFSConnectionConfig;
import org.knime.archive.zip.filehandling.fs.ArchiveZipFileSystem;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.context.ModifiableNodeCreationConfiguration;
import org.knime.core.node.context.ports.ModifiablePortsConfiguration;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FSConnectionProvider;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSelection;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSelectionWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.LegacyReaderFileSelectionPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.SingleFileSelectionMode;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.WithCustomFileSystem;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.Migrate;
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
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.EnumChoice;
import org.knime.node.parameters.widget.choices.EnumChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;

/**
 * Node parameters for ZIP Archive Connector.
 *
 * @author Halil Yerlikaya, KNIME GmbH, Berlin, Germany
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
class ArchiveZipConnectorNodeParameters implements NodeParameters {

    private static final String KEY_WORKING_DIRECTORY = "workingDirectory";

    private static final String KEY_FILE = "file";

    private static final String KEY_ENCODING = "encoding";

    private static final String DEFAULT_ENCODING = "CP437";

    private static final String CFG_USE_DEFAULT_ENCODING = "useDefaultEncoding";

    // ----- LAYOUTS -----

    @Section(title = "Settings")
    interface SettingsSection {
    }

    @Section(title = "Encoding")
    @After(SettingsSection.class)
    interface EncodingSection {
    }

    // ----- SETTINGS PARAMETERS -----

    @Widget(title = "Read from", description = """
            Select a file system which stores the data you want to read. There are four default file system \
            options to choose from: <br /> <ul> <li><i>Local File System:</i> Allows you to select a \
            file/folder from your local system. </li> <li><i>Mountpoint:</i> Allows you to read from a \
            mountpoint. When selected, a new drop-down menu appears to choose the mountpoint. Unconnected \
            mountpoints are greyed out but can still be selected (note that browsing is disabled in this \
            case). Go to the KNIME Explorer and connect to the mountpoint to enable browsing. A mountpoint is \
            displayed in red if it was previously selected but is no longer available. You won't be able to \
            save the dialog as long as you don't select a valid i.e. known mountpoint. </li> <li><i>Relative \
            to:</i> Allows you to choose whether to resolve the path relative to the current mountpoint, \
            current workflow or the current workflow's data area. When selected a new drop-down menu appears \
            to choose which of the three options to use. </li> <li><i>Custom/KNIME URL:</i> Allows to specify \
            a URL (e.g. file://, http:// or knime:// protocol). When selected, a spinner appears that allows \
            you to specify the desired connection and read timeout in milliseconds. In case it takes longer \
            to connect to the host / read the file, the node fails to execute. Browsing is disabled for this \
            option. </li> </ul> To read from other file systems, click on <b>...</b> in the bottom left \
            corner of the node icon followed by <i>Add File System Connection port</i>. Afterwards, connect \
            the desired file system connector node to the newly added input port. The file system connection \
            will then be shown in the drop-down menu. It is greyed out if the file system is not connected in \
            which case you have to (re)execute the connector node first. Note: The default file systems \
            listed above can't be selected if a file system is provided via the input port.""")
    @Layout(SettingsSection.class)
    @FileSelectionWidget(SingleFileSelectionMode.FILE)
    @ValueReference(FileSelectionRef.class)
    @Persistor(FileSelectionPersistor.class)
    @Migrate
    FileSelection m_file = new FileSelection();

    static final class FileSelectionRef implements ParameterReference<FileSelection> {
    }

    static final class FileSelectionPersistor extends LegacyReaderFileSelectionPersistor {
        public FileSelectionPersistor() {
            super(KEY_FILE);
        }
    }

    @Widget(title = "Working directory", description = """
            Specify the <i>working directory</i> of the resulting file system connection.
            The working directory must be specified as an absolute path. A
            working directory allows downstream nodes to access files/folders using <i>relative</i> paths,
            i.e. paths that do not have a leading backslash. The default working directory is "/".""")
    @Layout(SettingsSection.class)
    @FileSelectionWidget(SingleFileSelectionMode.FOLDER)
    @WithCustomFileSystem(connectionProvider = FileSystemConnectionProvider.class)
    @Persist(configKey = KEY_WORKING_DIRECTORY)
    @ValueReference(WorkingDirectoryRef.class)
    String m_workingDirectory = ArchiveZipFileSystem.SEPARATOR;

    interface WorkingDirectoryRef extends ParameterReference<String> {
    }

    // ----- ENCODING PARAMETERS -----

    @Widget(title = "Encoding", description = """
            Sets the character set/encoding to use when reading the names of the compressed files in the \
            archive. By default, <a href="https://en.wikipedia.org/wiki/Code_page_437">CP437</a> is chosen, \
            which is used in some ZIP files. You can specify any other encoding supported by Java. Choosing \
            "OS default" uses the default encoding of the Java VM, which may depend on the locale or the Java \
            property "file.encoding".""")
    @Layout(EncodingSection.class)
    @Persistor(EncodingPersistor.class)
    @ValueReference(EncodingRef.class)
    @Migrate
    EncodingParameters m_encoding = new EncodingParameters();

    static final class EncodingRef implements ParameterReference<EncodingParameters> {
    }

    // ----- CONNECTION PROVIDER -----

    /**
     * Provides a {@link FSConnectionProvider} based on the Zip connection settings. We need this to support setting the
     * working directory using the exact same Zip connection this node is providing.
     */
    static final class FileSystemConnectionProvider implements StateProvider<FSConnectionProvider> {

        private Supplier<FileSelection> m_fileSupplier;

        private Supplier<EncodingParameters> m_encodingSupplier;

        private Supplier<String> m_workingDirectorySupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_fileSupplier = initializer.computeFromValueSupplier(FileSelectionRef.class);
            m_encodingSupplier = initializer.computeFromValueSupplier(EncodingRef.class);
            m_workingDirectorySupplier = initializer.computeFromValueSupplier(WorkingDirectoryRef.class);
            initializer.computeAfterOpenDialog();
        }

        @Override
        public FSConnectionProvider computeState(final NodeParametersInput parametersInput) {
            return () -> {// NOSONAR: Longer lambda acceptable, as it improves readability
                final var connectionProviderConfig = new FileSystemConnectionProviderConfiguration( //
                    m_fileSupplier.get(), //
                    m_encodingSupplier.get(), //
                    m_workingDirectorySupplier.get());
                final var nodeSettings = createNodeSettings(parametersInput);
                try {
                    nodeSettings.loadForDialog(createSettingsRO(connectionProviderConfig));
                    ArchiveZipFSConnectionConfig config = null;
                    return createArchiveZipwithConfig(nodeSettings, config);
                } catch (final InvalidSettingsException | IOException | NotConfigurableException e) {
                    throw new IOException(e);
                }
            };
        }


        private static FSConnection createArchiveZipwithConfig(final ArchiveZipConnectorNodeSettings nodeSettings,
            ArchiveZipFSConnectionConfig config) throws InvalidSettingsException, IOException {
            try {
                config = nodeSettings.createFSConnectionConfig(s -> {
                });
                return new ArchiveZipFSConnection(config);
            } catch (final IOException t) {
                if (config != null) {
                    config.close();
                }
                throw t;
            }
        }


        private static ArchiveZipConnectorNodeSettings createNodeSettings(final NodeParametersInput input) {
            final var portsConfiguration = (ModifiablePortsConfiguration)input.getPortsConfiguration();
            final var nodeCreationConfig = new ModifiableNodeCreationConfiguration(portsConfiguration);
            return new ArchiveZipConnectorNodeSettings(nodeCreationConfig);
        }

        private static NodeSettingsRO createSettingsRO(final FileSystemConnectionProviderConfiguration config) {
            final var settings = new NodeSettings("tmpsettings");
            // Save FileSelection
            new FileSelectionPersistor().save(config.file, settings);
            // Save Encoding
            new EncodingPersistor().save(config.encoding, settings);
            // Save Working Directory
            settings.addString(KEY_WORKING_DIRECTORY, config.workingDirectory);
            return settings;
        }

        static record FileSystemConnectionProviderConfiguration( //
            FileSelection file, //
            EncodingParameters encoding, //
            String workingDirectory) {
        }
    }


    static final class EncodingParameters implements NodeParameters {

        static final class EncodingChoicesProvider implements EnumChoicesProvider<FileEncodingOption> {
            @Override
            public List<EnumChoice<FileEncodingOption>> computeState(final NodeParametersInput context) {
                return Arrays.stream(FileEncodingOption.values()).map(FileEncodingOption::toEnumChoice).toList();
            }
        }

        @Widget(title = "File encoding", description = """
                Sets the character set/encoding to use when reading the names of the compressed files in the \
                archive. By default, <a href="https://en.wikipedia.org/wiki/Code_page_437">CP437</a> is chosen, \
                which is used in some ZIP files.""")
        @ChoicesProvider(EncodingChoicesProvider.class)
        @ValueReference(EncodingSelectionRef.class)
        FileEncodingOption m_encodingSelection = FileEncodingOption.OS_DEFAULT;

        static final class EncodingSelectionRef implements ParameterReference<FileEncodingOption> {
        }

        @Widget(title = "Custom encoding", description = "Enter a custom encoding.")
        @Effect(predicate = IsOtherEncoding.class, type = EffectType.SHOW)
        @ValueReference(CustomEncodingRef.class)
        @Layout(EncodingSection.class)
        String m_encoding = DEFAULT_ENCODING;

        static final class CustomEncodingRef implements ParameterReference<String> {
        }

        static final class IsOtherEncoding implements EffectPredicateProvider {
            @Override
            public EffectPredicate init(final PredicateInitializer i) {
                return i.getEnum(EncodingSelectionRef.class).isOneOf(FileEncodingOption.OTHER);
            }
        }
    }

    static final class EncodingPersistor implements NodeParametersPersistor<EncodingParameters> {

        @Override
        public EncodingParameters load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var params = new EncodingParameters();
            var useDefault = true;
            if (settings.containsKey(CFG_USE_DEFAULT_ENCODING)) {
                useDefault = settings.getBoolean(CFG_USE_DEFAULT_ENCODING, true);
            }

            if (useDefault) {
                params.m_encodingSelection = FileEncodingOption.OS_DEFAULT;
            } else {
                if (settings.containsKey(KEY_ENCODING)) {
                    var loadedEncoding = settings.getString(KEY_ENCODING, DEFAULT_ENCODING);
                    params.m_encodingSelection = FileEncodingOption.fromPersistId(loadedEncoding);
                    if (params.m_encodingSelection == FileEncodingOption.OTHER) {
                        params.m_encoding = loadedEncoding;
                    }
                } else {
                    params.m_encodingSelection = FileEncodingOption.OS_DEFAULT;
                }
            }
            return params;
        }

        @Override
        public void save(final EncodingParameters param, final NodeSettingsWO settings) {
            if (param.m_encodingSelection == FileEncodingOption.OS_DEFAULT) {
                settings.addBoolean(CFG_USE_DEFAULT_ENCODING, true);
                settings.addString(KEY_ENCODING, "OS default");
            } else {
                settings.addBoolean(CFG_USE_DEFAULT_ENCODING, false);
                if (param.m_encodingSelection == FileEncodingOption.OTHER) {
                    settings.addString(KEY_ENCODING, param.m_encoding);
                } else {
                    settings.addString(KEY_ENCODING, param.m_encodingSelection.m_persistId);
                }
            }
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{CFG_USE_DEFAULT_ENCODING}, {KEY_ENCODING}};
        }
    }

    enum FileEncodingOption {
            @Label(value = "OS default", description = "Uses the default encoding of the Java VM") //
            OS_DEFAULT(null, "OS default (" + java.nio.charset.Charset.defaultCharset().name() + ")"), //
            @Label("UTF-8") //
            UTF_8("UTF-8"), //
            @Label("UTF-16") //
            UTF_16("UTF-16"), //
            @Label("UTF-16BE") //
            UTF_16BE("UTF-16BE"), //
            @Label("UTF-16LE") //
            UTF_16LE("UTF-16LE"), //
            @Label("US-ASCII") //
            US_ASCII("US-ASCII"), //
            @Label("ISO-8859-1") //
            ISO_8859_1("ISO-8859-1"), //
            @Label(value = "Other", description = "Use another encoding") //
            OTHER("OTHER");

        final String m_persistId;

        final String m_nonConstantDisplayText;

        FileEncodingOption(final String persistId) {
            this(persistId, null);
        }

        FileEncodingOption(final String persistId, final String displayText) {
            m_persistId = persistId;
            m_nonConstantDisplayText = displayText;
        }

        static FileEncodingOption fromPersistId(final String persistId) {
            if (persistId == null) {
                return OS_DEFAULT;
            }
            return Arrays.stream(FileEncodingOption.values()) //
                .filter(option -> Objects.equals(persistId, option.m_persistId)) //
                .findFirst() //
                .orElse(OTHER);
        }

        EnumChoice<FileEncodingOption> toEnumChoice() {
            if (m_nonConstantDisplayText == null) {
                return EnumChoice.fromEnumConst(this);
            }
            return new EnumChoice<>(this, m_nonConstantDisplayText);
        }
    }
}
