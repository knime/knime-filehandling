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

package org.knime.base.filehandling.pngstobinaryobjects;

import java.util.function.Supplier;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.image.png.PNGImageValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
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
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider;
import org.knime.node.parameters.widget.text.TextInputWidget;

/**
 * Node parameters for PNGs to Binary Objects.
 *
 * @author Tobias Koetter, KNIME GmbH, Berlin, Germany
 * @author AI Migration Pipeline v1.1
 */
@LoadDefaultsForAbsentFields
class PNGsToBinaryObjectsNodeSettings implements NodeParameters {

    static final class LegacyReplacePersistor implements NodeParametersPersistor<ReplacePolicy> {

        private static final String LEGACY_CFG_KEY = "replace";

        @Override
        public ReplacePolicy load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final String legacyValue = settings.getString(LEGACY_CFG_KEY);
            return ReplacePolicy.REPLACE.getName().equals(legacyValue) ? ReplacePolicy.REPLACE
                : ReplacePolicy.APPEND;
        }

        @Override
        public void save(final ReplacePolicy param, final NodeSettingsWO settings) {
            settings.addString(LEGACY_CFG_KEY, param.getName());
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{LEGACY_CFG_KEY}};
        }
    }

    interface ReplacePolicyOptionRef extends ParameterReference<ReplacePolicy> {
    }

    static class IsAppend implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(ReplacePolicyOptionRef.class).isOneOf(ReplacePolicy.APPEND);
        }
    }

    /**
     * ChoicesProvider providing all columns which are compatible with {@link PNGImageValue}. Per default,
     * the first input port is used.
     *
     * @author Tobias Koetter, KNIME GmbH, Konstanz, Germany
     */
    static class PNGImageColumnsProvider extends CompatibleColumnsProvider {
        /**
         * Needed for reflection
         */
        protected PNGImageColumnsProvider() {
            super(PNGImageValue.class);
        }
    }


    @Persist(configKey = "columnselection")
    @Widget(title = "Column selection",
        description = "Select the PNG image column that will be converted to binary objects.")
    @ChoicesProvider(PNGImageColumnsProvider.class)
    @ValueReference(ColumnSelectionRef.class)
    @ValueProvider(AutoguessOnEmptySelection.class)
    String m_columnSelection = "";

    @Persistor(LegacyReplacePersistor.class)
    @Widget(title = "Output handling",
        description =
        "Select how the output column with the binary objects should be handled.")
    @ValueReference(ReplacePolicyOptionRef.class)
    @ValueSwitchWidget
    ReplacePolicy m_replacePolicy = ReplacePolicy.REPLACE;

    @Persist(configKey = "columnname")
    @Widget(title = "New column name",
        description = "Name for the new column containing the binary objects. Only used when 'Append' is selected.")
    @TextInputWidget
    @Effect(predicate = IsAppend.class, type = EffectType.SHOW)
    String m_columnName = "BinaryObject";

    interface ColumnSelectionRef extends ParameterReference<String> {
    }

    static final class AutoguessOnEmptySelection implements StateProvider<String> {

        private Supplier<String> m_valueSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeAfterOpenDialog();
            m_valueSupplier = initializer.getValueSupplier(ColumnSelectionRef.class);
        }

        @Override
        public String computeState(final NodeParametersInput parametersInput) {
            final var currentValue = m_valueSupplier.get();
            if (currentValue == null || currentValue.isEmpty()) {
                // autoguess first compatible column
                final var inputSpec = parametersInput.getInTableSpec(0).orElse(null);
                return ColumnSelectionUtil.getFirstCompatibleColumn(inputSpec, PNGImageValue.class)
                    .map(DataColumnSpec::getName).orElse("");
            }
            return currentValue;
        }

    }

}
