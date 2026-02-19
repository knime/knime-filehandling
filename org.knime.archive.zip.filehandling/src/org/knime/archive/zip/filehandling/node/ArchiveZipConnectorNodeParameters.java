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

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.knime.archive.zip.filehandling.fs.ArchiveZipFSConnection;
import org.knime.archive.zip.filehandling.fs.ArchiveZipFSConnectionConfig;
import org.knime.archive.zip.filehandling.fs.ArchiveZipFileSystem;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FSConnectionProvider;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSelection;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.LegacyReaderFileSelectionPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSelectionWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.SingleFileSelectionMode;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.WithCustomFileSystem;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.custom.CustomValidation;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.custom.SimpleValidation;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileReaderWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.PersistWithin;
import org.knime.filehandling.core.defaultnodesettings.EnumConfig;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;
import org.knime.filehandling.core.port.FileSystemPortObjectSpec;
import org.knime.node.parameters.Advanced;
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
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.PatternValidation.IsNotBlankValidation;

/**
 * Node parameters for ZIP Archive Connector.
 *
 * @author Halil Yerlikaya, KNIME GmbH, Berlin, Germany
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
class ArchiveZipConnectorNodeParameters implements NodeParameters {

    private static final String DEFAULT_ENCODING = "UTF-8";
    private static final String[] FILE_EXTENSIONS = {".zip", ".jar"};

    // ----- LAYOUTS -----

    @Section(title = "Input File")
    interface InputFileSection {
    }

    @Section(title = "Encoding")
    @After(InputFileSection.class)
    @Advanced
    interface EncodingSection {
    }

    @Section(title = "File System")
    @After(EncodingSection.class)
    interface FileSystemSection {
    }

    // ----- SETTINGS PARAMETERS -----

    @Widget(title = "ZIP file", description = "Select the ZIP or JAR archive file.")
    @Layout(InputFileSection.class)
    @FileReaderWidget(fileExtensions = {".zip", ".jar"})
    @ValueReference(FileSelectionRef.class)
    @Persistor(FileSelectionPersistor.class)
    @Migrate
    FileSelection m_file = new FileSelection();

    static final class FileSelectionRef implements ParameterReference<FileSelection> {
    }

    static final class FileSelectionPersistor extends LegacyReaderFileSelectionPersistor {
        private static final String KEY_FILE = "file";

        public FileSelectionPersistor() {
            super(KEY_FILE);
        }
    }

    static final class WorkingDirectoryPersistor implements NodeParametersPersistor<String> {
        private static final String KEY = "working_directory";

        @Override
        public String load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return settings.getString(KEY, ArchiveZipFileSystem.SEPARATOR);
        }

        @Override
        public void save(final String value, final NodeSettingsWO settings) {
            settings.addString(KEY, value);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{KEY}};
        }
    }

    @Widget(title = "Working directory", description = """
            Specify the <i>working directory</i> of the resulting file system connection.
            The working directory must be specified as an absolute path. A
            working directory allows downstream nodes to access files/folders using <i>relative</i> paths,
            i.e. paths that do not have a leading backslash. The default working directory is "/".""")
    @TextInputWidget
    @Persistor(WorkingDirectoryPersistor.class)
    @Layout(FileSystemSection.class)
    @FileSelectionWidget(SingleFileSelectionMode.FOLDER)
    @WithCustomFileSystem(connectionProvider = FileSystemConnectionProvider.class)
    @CustomValidation(WorkingDirectoryValidator.class)
    @ValueReference(WorkingDirectoryRef.class)
    String m_workingDirectory = ArchiveZipFileSystem.SEPARATOR;

    static final class WorkingDirectoryValidator extends SimpleValidation<String> {
        @Override
        public void validate(final String value) throws InvalidSettingsException {
            if (StringUtils.isBlank(value) || !value.startsWith(ArchiveZipFileSystem.SEPARATOR)) {
                throw new InvalidSettingsException("Working directory must be an absolute path (start with /).");
            }
        }
    }

    interface WorkingDirectoryRef extends ParameterReference<String> {
    }

    // ----- ENCODING PARAMETERS -----

    @Widget(title = "Encoding", description = """
            Sets the character set/encoding to use when reading the names of the compressed files in the \
            archive. By default, UTF-8 is chosen. You can specify any other encoding supported by Java. Choosing \
            "OS default" uses the default encoding of the Java VM, which may depend on the locale or the Java \
            property "file.encoding".""")
    @Layout(EncodingSection.class)
    @ValueReference(EncodingRef.class)
        @PersistWithin.PersistEmbedded
        @Persistor(EncodingParametersPersistor.class)
    EncodingParameters m_encoding = new EncodingParameters();

    static final class EncodingRef implements ParameterReference<EncodingParameters> {
    }

    // ----- CONNECTION PROVIDER -----

    /**
     * Provides a {@link FSConnectionProvider} based on the Zip connection settings. We need this to support setting the
     * working directory using the exact same Zip connection this node is providing.
     */
    static final class FileSystemConnectionProvider implements StateProvider<FSConnectionProvider> {

        private static final Consumer<StatusMessage> NO_OP_CONSUMER = s -> {}; // NOSONAR

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
            return () -> createConnection(parametersInput);
        }

        private ArchiveZipFSConnection createConnection(final NodeParametersInput parametersInput) throws IOException {
            final var params = new ArchiveZipConnectorNodeParameters();
            params.m_file = m_fileSupplier.get();
            params.m_encoding = m_encodingSupplier.get();
            params.m_workingDirectory = m_workingDirectorySupplier.get();
            
            if (StringUtils.isBlank(params.m_workingDirectory) || !params.m_workingDirectory.startsWith(ArchiveZipFileSystem.SEPARATOR)) {
                params.m_workingDirectory = ArchiveZipFileSystem.SEPARATOR;
            }

            final var portsConfiguration = parametersInput.getPortsConfiguration();
            final var inSpecs = parametersInput.getInPortSpecs();

            ArchiveZipFSConnectionConfig config = null;
            try {
                config = params.createFSConnectionConfig(portsConfiguration, inSpecs, NO_OP_CONSUMER);
                return new ArchiveZipFSConnection(config);
            } catch (final IOException t) {
                if (config != null) {
                    try {
                        config.close();
                    } catch (final IOException e) {
                        t.addSuppressed(e);
                    }
                }
                throw t;
            } catch (InvalidSettingsException e) {
                if (config != null) {
                    try {
                        config.close();
                    } catch (final IOException ioe) {
                        e.addSuppressed(ioe);
                    }
                }
                throw new IOException(e);
            }
        }
    }

    static final class EncodingParameters implements NodeParameters {

        @Widget(title = "File encoding", description = """
                Sets the character set/encoding to use when reading the names of the compressed files in the \
                archive. By default, UTF-8 is chosen.""")
        @ChoicesProvider(EncodingChoicesProvider.class)
        @ValueReference(EncodingSelectionRef.class)
        FileEncodingOption m_encoding = FileEncodingOption.DEFAULT;

        static final class EncodingSelectionRef implements ParameterReference<FileEncodingOption> {
        }

        @Widget(title = "Custom encoding", description = "Enter a custom encoding.")
        @Effect(predicate = EncodingParameters.IsOtherEncoding.class, type = EffectType.SHOW)
        @TextInputWidget(patternValidation = IsNotBlankValidation.class)
        @ValueReference(CustomEncodingRef.class)
        @Layout(EncodingSection.class)
        String m_customEncoding = DEFAULT_ENCODING;

        static final class CustomEncodingRef implements ParameterReference<String> {
        }

        static final class IsOtherEncoding implements EffectPredicateProvider {
            @Override
            public EffectPredicate init(final PredicateInitializer i) {
                return i.getEnum(EncodingSelectionRef.class).isOneOf(FileEncodingOption.OTHER);
            }
        }
    }

    static final class EncodingParametersPersistor implements NodeParametersPersistor<EncodingParameters> {

        private static final String KEY_ENCODING = "encoding";
        private static final String CFG_USE_DEFAULT_ENCODING = "useDefaultEncoding";

        @Override
        public EncodingParameters load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var params = new EncodingParameters();
            params.m_encoding = FileEncodingOption.DEFAULT;

            if (settings.containsKey(KEY_ENCODING)) {
                var loadedEncoding = settings.getString(KEY_ENCODING, DEFAULT_ENCODING);
                params.m_encoding = FileEncodingOption.fromPersistId(loadedEncoding);
                if (params.m_encoding == FileEncodingOption.OTHER) {
                    params.m_customEncoding = loadedEncoding;
                }
            }

            if (settings.containsKey(CFG_USE_DEFAULT_ENCODING) && settings.getBoolean(CFG_USE_DEFAULT_ENCODING)) {
                params.m_encoding = FileEncodingOption.DEFAULT;
            }
            return params;
        }

        @Override
        public void save(final EncodingParameters params, final NodeSettingsWO settings) {
            final boolean useDefault = params.m_encoding == FileEncodingOption.DEFAULT;
            settings.addBoolean(CFG_USE_DEFAULT_ENCODING, useDefault);

            String encoding = DEFAULT_ENCODING;
            if (!useDefault) {
                if (params.m_encoding == FileEncodingOption.OTHER) {
                    encoding = params.m_customEncoding;
                } else if (params.m_encoding == FileEncodingOption.OS_DEFAULT) {
                    encoding = FileEncodingOption.OS_DEFAULT.m_persistId;
                } else {
                    encoding = params.m_encoding.m_persistId;
                }
            }
            settings.addString(KEY_ENCODING, encoding);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{KEY_ENCODING}, {CFG_USE_DEFAULT_ENCODING}};
        }
    }

    enum FileEncodingOption {
            @Label(value = "Default", description = "Default (currently UTF-8)")
            DEFAULT(null),
            @Label(value = "OS default", description = "Uses the default encoding of the Java VM") //
            OS_DEFAULT("OS default", "OS default (" + Charset.defaultCharset().name() + ")"), //
            @Label("CP437") //
            CP437("CP437"), //
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
                return DEFAULT;
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

    ArchiveZipFSConnectionConfig createFSConnectionConfig(final PortsConfiguration portsConfig,
        final PortObjectSpec[] inSpecs, final Consumer<StatusMessage> statusConsumer)
        throws IOException, InvalidSettingsException {

        final var fileChooser = new SettingsModelReaderFileChooser( //
            FileSelectionPersistor.KEY_FILE, //
            portsConfig, //
            ArchiveZipConnectorNodeFactory.FS_CONNECT_GRP_ID, EnumConfig.create(FilterMode.FILE),
            FILE_EXTENSIONS);

        // Populate fileChooser from FileSelection
        final var tempSettings = new NodeSettings("tmpsettings");
        new FileSelectionPersistor().save(m_file, tempSettings);
        fileChooser.loadSettingsFrom(tempSettings);

        if (inSpecs != null) {
            fileChooser.configureInModel(inSpecs, statusConsumer);
        }

        final var fsConfig = new ArchiveZipFSConnectionConfig(m_workingDirectory);
        final var pathAccessor = fileChooser.createReadPathAccessor();

        try {
            fsConfig.setCloseable(pathAccessor);
            final FSPath zipFilePath = pathAccessor.getRootPath(statusConsumer);
            fsConfig.setZipFilePath(zipFilePath);

            final boolean useDefault = m_encoding.m_encoding == FileEncodingOption.DEFAULT;
            fsConfig.setUseDefaultEncoding(useDefault);

            String encoding = DEFAULT_ENCODING;
            if (!useDefault) {
                if (m_encoding.m_encoding == FileEncodingOption.OTHER) {
                    encoding = m_encoding.m_customEncoding;
                } else if (m_encoding.m_encoding == FileEncodingOption.OS_DEFAULT) {
                    encoding = Charset.defaultCharset().name();
                } else {
                    encoding = m_encoding.m_encoding.m_persistId;
                }
            }
            fsConfig.setEncoding(encoding);

        } catch (IOException e) { //NOSONAR
            try {
                pathAccessor.close();
            } catch (Exception ignored) {
                // ignore
            }
            throw e;
        }
        return fsConfig;
    }

    @Override
    public void validate() throws InvalidSettingsException {
        if (m_file.getFSLocation() == null || StringUtils.isBlank(m_file.getFSLocation().getPath())) {
            throw new InvalidSettingsException("File must be specified.");
        }
        if (StringUtils.isBlank(m_workingDirectory) || !m_workingDirectory.startsWith(ArchiveZipFileSystem.SEPARATOR)) {
            throw new InvalidSettingsException("Working directory must be an absolute path (start with /).");
        }
    }

    static final class EncodingChoicesProvider implements EnumChoicesProvider<FileEncodingOption> {
        @Override
        public List<EnumChoice<FileEncodingOption>> computeState(final NodeParametersInput context) {
            return Arrays.stream(FileEncodingOption.values()).map(FileEncodingOption::toEnumChoice).toList();
        }
    }
}
