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

package org.knime.base.filehandling.binaryobjectstopngs;

import java.util.Optional;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.blob.BinaryObjectDataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.migration.Migrate;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.legacy.ColumnNameAutoGuessValueProvider;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider;

/**
 * Node parameters for Binary Objects to PNGs.
 *
 * @author Tim Crundall, TNG Technology Consulting GmbH
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class BinaryObjectsToPNGsNodeParameters implements NodeParameters {

    @Widget(title = "Column selection",
        description = "Column containing the binary objects that will be converted to PNG images.")
    @ChoicesProvider(BinaryObjectColumnsProvider.class)
    @ValueProvider(InputColumnProvider.class)
    @Persist(configKey = "columnselection")
    @ValueReference(InputColumnRef.class)
    String m_columnSelection = "";

    private interface InputColumnRef extends ParameterReference<String> {
    }

    private static final class BinaryObjectColumnsProvider extends CompatibleColumnsProvider {
        protected BinaryObjectColumnsProvider() {
            super(BinaryObjectDataValue.class);
        }
    }

    private static final class InputColumnProvider extends ColumnNameAutoGuessValueProvider {
        protected InputColumnProvider() {
            super(InputColumnRef.class);
        }

        @Override
        protected Optional<DataColumnSpec> autoGuessColumn(final NodeParametersInput parametersInput) {
            final var compatibleColumns = ColumnSelectionUtil.getCompatibleColumnsOfFirstPort(
                parametersInput, BinaryObjectDataValue.class);
            return compatibleColumns.isEmpty() ? Optional.empty() :
                Optional.of(compatibleColumns.get(compatibleColumns.size() - 1));
        }
    }

    @Widget(title = "Output handling",
        description = "Determines whether to append a new PNG column or to replace the selected binary object column.")
    @ValueSwitchWidget
    @ValueReference(OutputModeRef.class)
    @Persistor(OutputModePersistor.class)
    @Migrate(loadDefaultIfAbsent = true)
    OutputMode m_outputMode = OutputMode.REPLACE;

    private interface OutputModeRef extends ParameterReference<OutputMode> {
    }

    private enum OutputMode {
            @Label(value = "Replace", description = "Replaces the selected binary object column with the PNG column.")
            REPLACE, //
            @Label(value = "Append", description = "Appends a new PNG column to the table.")
            APPEND;
    }

    /**
     * Persists the OutputMode enum as the legacy string values "Append" / "Replace" under key "replace",
     * matching via {@link ReplacePolicy#getName()}.
     */
    private static final class OutputModePersistor implements NodeParametersPersistor<OutputMode> {

        private static final String CFG_KEY = "replace";

        @Override
        public OutputMode load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final String value = settings.getString(CFG_KEY);
            if (ReplacePolicy.APPEND.getName().equals(value)) {
                return OutputMode.APPEND;
            }
            return OutputMode.REPLACE;
        }

        @Override
        public void save(final OutputMode obj, final NodeSettingsWO settings) {
            final ReplacePolicy policy = obj == OutputMode.APPEND ? ReplacePolicy.APPEND : ReplacePolicy.REPLACE;
            settings.addString(CFG_KEY, policy.getName());
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{CFG_KEY}};
        }
    }

    @Widget(title = "New column name",
        description = "Name of the appended column. Only applicable when \"Append\" is selected.")
    @Effect(predicate = IsAppend.class, type = EffectType.SHOW)
    @Persist(configKey = "columnname")
    String m_newColumnName = "PNG";

    private static final class IsAppend implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(OutputModeRef.class).isOneOf(OutputMode.APPEND);
        }
    }

}
