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

package org.knime.base.filehandling.binaryobjects.writer;

import static org.knime.base.filehandling.binaryobjects.writer.BinaryObjectsToFilesNodeConfigSerializer.CFG_BINARY_COLUMN_NAME;
import static org.knime.base.filehandling.binaryobjects.writer.BinaryObjectsToFilesNodeConfigSerializer.CFG_GENERATE_FILE_NAMES;
import static org.knime.base.filehandling.binaryobjects.writer.BinaryObjectsToFilesNodeConfigSerializer.CFG_REMOVE_BINARY_COLUMN_NAME;

import java.util.List;
import java.util.Optional;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.data.blob.BinaryObjectDataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSelectionWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSystemOption;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.SingleFileSelectionMode;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.WithFileSystem;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.RowIDChoice;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.StringOrEnum;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification.WidgetGroupModifier;
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
import org.knime.node.parameters.persistence.legacy.LegacyFileWriterWithOverwritePolicyOptions;
import org.knime.node.parameters.persistence.legacy.LegacyFileWriterWithOverwritePolicyOptions.OverwritePolicy;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.legacy.AutoGuessValueProvider;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.ColumnChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation;

/**
 * Node parameters for Binary Objects to Files.
 *
 * @author Thomas Reifenberger, TNG Technology Consulting GmbH
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
@LoadDefaultsForAbsentFields
class BinaryObjectsToFilesNodeParameters implements NodeParameters {

    private static final String CFG_OUTPUT_LOCATION = "output_location";

    private static final String CFG_COMPRESS_BINARY_OBJECT_FILES = "compress_binary_object_files";

    private static final String CFG_FILENAME_PATTERN = "filename_pattern";

    private static final String CFG_FILENAME_COLUMN = "filename_column";

    private static final String CFG_USE_ROW_ID = "useRowID";

    private static final String CFG_COLUMN_NAME = "columnName";

    @SuppressWarnings("java:S1176")
    interface DialogLayout {
        @Section(title = "Binary Object")
        interface BinaryObject {
        }

        @Section(title = "File Names")
        @After(BinaryObject.class)
        interface FileNames {
        }

        @Section(title = "Output Location")
        @After(FileNames.class)
        interface OutputLocation {
        }
    }

    @Layout(DialogLayout.OutputLocation.class)
    @Persist(configKey = CFG_OUTPUT_LOCATION)
    @Modification(OutputLocationModification.class)
    LegacyFileWriterWithOverwritePolicyOptions m_outputLocation = new LegacyFileWriterWithOverwritePolicyOptions();

    @Layout(DialogLayout.BinaryObject.class)
    @Widget(title = "Binary object", description = "Column containing the binary objects to write.")
    @Persist(configKey = CFG_BINARY_COLUMN_NAME)
    @ChoicesProvider(BinaryObjectColumnsProvider.class)
    String m_binaryColumn = "";

    @Layout(DialogLayout.BinaryObject.class)
    @Widget(title = "Remove binary object column",
        description = "If checked, the column containing the binary objects is removed from the output table.")
    @Persist(configKey = CFG_REMOVE_BINARY_COLUMN_NAME)
    boolean m_removeBinaryObjectColumn;

    @Layout(DialogLayout.BinaryObject.class)
    @Widget(title = "Compress binary object files (gzip)",
        description = "If checked, the output binary files will be compressed to gzip.")
    @Persist(configKey = CFG_COMPRESS_BINARY_OBJECT_FILES)
    boolean m_compressBinaryFiles;

    @Layout(DialogLayout.FileNames.class)
    @Widget(title = "File generation mode", description = "Select how output file names should be determined.")
    @Persistor(FileGenerationModePersistor.class)
    @ValueSwitchWidget
    @ValueReference(IsUseFileNamePatternRef.class)
    FileGenerationMode m_fileGenerationMode = FileGenerationMode.GENERATE;

    @Layout(DialogLayout.FileNames.class)
    @Widget(title = "File names",
        description = "The file names will be generated using the provided pattern. "
            + "The pattern must contain a single \"?\" symbol. This symbol will, during execution, "
            + "be replaced by an incrementing counter to make the filenames unique. "
            + "This node does not append any file extension.")
    @Persist(configKey = CFG_FILENAME_PATTERN)
    @TextInputWidget(patternValidation = FileNamePatternValidation.class)
    @Effect(predicate = IsUseFileNamePattern.class, type = EffectType.SHOW)
    String m_fileNamePattern = "File_?.dat";

    @Layout(DialogLayout.FileNames.class)
    @Widget(title = "File name column",
        description = "Column containing the file names under which the corresponding binary file will be stored.")
    @Persistor(FileNameColumnFilterPersistor.class)
    @ChoicesProvider(FileNameColumnChoicesProvider.class)
    @ValueReference(FileNameColumnRef.class)
    @ValueProvider(FileNameColumnDefaultProvider.class)
    @Effect(predicate = IsUseFileNamePattern.class, type = EffectType.HIDE)
    StringOrEnum<RowIDChoice> m_fileNameColumn = new StringOrEnum<>(RowIDChoice.ROW_ID);

    private static class OutputLocationModification implements LegacyFileWriterWithOverwritePolicyOptions.Modifier {

        private static final class OverwritePolicyChoicesProvider
            extends LegacyFileWriterWithOverwritePolicyOptions.OverwritePolicyChoicesProvider {

            @Override
            protected List<LegacyFileWriterWithOverwritePolicyOptions.OverwritePolicy> getChoices() {
                return List.of(OverwritePolicy.fail, OverwritePolicy.overwrite, OverwritePolicy.ignore);
            }
        }

        @Override
        public void modify(final WidgetGroupModifier group) {
            restrictOverwritePolicyOptions(group, OverwritePolicyChoicesProvider.class);
            findFileSelection(group) //
                .modifyAnnotation(Widget.class) //
                .withProperty("title", "Folder") //
                .withProperty("description", "The folder to write the binary object files to.") //
                .modify();
            findFileSelection(group) //
                .addAnnotation(FileSelectionWidget.class) //
                .withValue(SingleFileSelectionMode.FOLDER) //
                .modify();
            findFileSelection(group) //
                .addAnnotation(WithFileSystem.class) //
                .withProperty("value", new FileSystemOption[]{FileSystemOption.LOCAL, FileSystemOption.SPACE,
                    FileSystemOption.EMBEDDED, FileSystemOption.CONNECTED})
                .modify();
        }

    }

    private interface IsUseFileNamePatternRef extends ParameterReference<FileGenerationMode> {
    }

    private static class IsUseFileNamePattern implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(IsUseFileNamePatternRef.class).isOneOf(FileGenerationMode.GENERATE);
        }

    }

    private enum FileGenerationMode {
            @Label(value = "Generate",
                description = "Generate file names using a pattern with an incrementing counter.") //
            GENERATE, //
            @Label(value = "From column", description = "Use file names from a selected column.") //
            FROM_COLUMN, //
    }

    private static class FileGenerationModePersistor implements NodeParametersPersistor<FileGenerationMode> {

        @Override
        public FileGenerationMode load(final NodeSettingsRO settings) throws InvalidSettingsException {
            boolean generateFileNames = settings.getBoolean(CFG_GENERATE_FILE_NAMES, true);
            return generateFileNames ? FileGenerationMode.GENERATE : FileGenerationMode.FROM_COLUMN;
        }

        @Override
        public void save(final FileGenerationMode param, final NodeSettingsWO settings) {
            boolean generateFileNames = param == FileGenerationMode.GENERATE;
            settings.addBoolean(CFG_GENERATE_FILE_NAMES, generateFileNames);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{CFG_GENERATE_FILE_NAMES}};
        }
    }

    private static class FileNameColumnFilterPersistor implements NodeParametersPersistor<StringOrEnum<RowIDChoice>> {

        @Override
        public StringOrEnum<RowIDChoice> load(final NodeSettingsRO settings) throws InvalidSettingsException {
            NodeSettingsRO config = settings.getNodeSettings(CFG_FILENAME_COLUMN);
            boolean useRowID = config.getBoolean(CFG_USE_ROW_ID);
            if (useRowID) {
                return new StringOrEnum<>(RowIDChoice.ROW_ID);
            } else {
                String columnName = config.getString(CFG_COLUMN_NAME);
                return new StringOrEnum<>(columnName);
            }
        }

        @Override
        public void save(final StringOrEnum<RowIDChoice> param, final NodeSettingsWO settings) {
            NodeSettingsWO config = settings.addNodeSettings(CFG_FILENAME_COLUMN);
            Optional<RowIDChoice> enumChoice = param.getEnumChoice();
            if (enumChoice.isPresent()) {
                if (enumChoice.get() == RowIDChoice.ROW_ID) {
                    config.addBoolean(CFG_USE_ROW_ID, true);
                    config.addString(CFG_COLUMN_NAME, null);
                } else {
                    throw new IllegalStateException("Unsupported RowIDChoice: " + enumChoice.get());
                }
            } else {
                config.addBoolean(CFG_USE_ROW_ID, false);
                config.addString(CFG_COLUMN_NAME, param.getStringChoice());
            }
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{CFG_FILENAME_COLUMN, CFG_USE_ROW_ID}, {CFG_FILENAME_COLUMN, CFG_COLUMN_NAME}};
        }
    }

    private static class FileNamePatternValidation extends TextInputWidgetValidation.PatternValidation {

        @Override
        public String getErrorMessage() {
            return "The pattern must contain a single '?' symbol.";
        }

        @Override
        public String getPattern() {
            // See {@link AbstractMultiTableWriterNodeConfig}
            return "[^?/\\00]*\\?[^?/\\00]*";
        }
    }

    private static class BinaryObjectColumnsProvider implements ColumnChoicesProvider {

        @Override
        public List<DataColumnSpec> columnChoices(final NodeParametersInput context) {
            var tableSpecs = context.getInPortSpecs();
            var tableSpec = (DataTableSpec)tableSpecs[tableSpecs.length - 1];
            if (tableSpec == null) {
                return List.of();
            }
            return tableSpec.stream() //
                .filter(colSpec -> colSpec.getType().isCompatible(BinaryObjectDataValue.class)) //
                .toList();
        }
    }

    private interface FileNameColumnRef extends ParameterReference<StringOrEnum<RowIDChoice>> {
    }

    private static class FileNameColumnDefaultProvider extends AutoGuessValueProvider<StringOrEnum<RowIDChoice>> {
        protected FileNameColumnDefaultProvider() {
            super(FileNameColumnRef.class);
        }

        @Override
        protected boolean isEmpty(final StringOrEnum<RowIDChoice> value) {
            return value.getEnumChoice().isEmpty() && value.getStringChoice() == null;
        }

        @Override
        protected StringOrEnum<RowIDChoice> autoGuessValue(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            var tableSpecs = parametersInput.getInPortSpecs();
            var tableSpec = (DataTableSpec)tableSpecs[tableSpecs.length - 1];
            if (tableSpec == null) {
                return new StringOrEnum<>(RowIDChoice.ROW_ID);
            }
            return tableSpec.stream() //
                .filter(colSpec -> colSpec.getType().isCompatible(StringValue.class)) //
                .reduce((first, second) -> second) //
                .map(colSpec -> new StringOrEnum<RowIDChoice>(colSpec.getName())) //
                .orElse(new StringOrEnum<>(RowIDChoice.ROW_ID));
        }
    }

    private static class FileNameColumnChoicesProvider implements ColumnChoicesProvider {

        @Override
        public List<DataColumnSpec> columnChoices(final NodeParametersInput context) {
            var tableSpecs = context.getInPortSpecs();
            var tableSpec = (DataTableSpec)tableSpecs[tableSpecs.length - 1];
            if (tableSpec == null) {
                return List.of();
            }
            return tableSpec.stream() //
                .filter(colSpec -> colSpec.getType().isCompatible(StringValue.class)) //
                .toList();
        }
    }
}
