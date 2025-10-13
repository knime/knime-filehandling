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

package org.knime.base.filehandling.uritostring;

import java.util.Optional;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.uri.URIDataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
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
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.legacy.ColumnNameAutoGuessValueProvider;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider;
import org.knime.node.parameters.widget.text.TextInputWidget;

/**
 * Node parameters for URI to String.
 *
 * @author Halil Yerlikaya, KNIME GmbH, Berlin, Germany
 * @author AI Migration Pipeline v1.1
 */
@LoadDefaultsForAbsentFields
final class URIToStringNodeParameters implements NodeParameters {

    URIToStringNodeParameters() {
    }

    URIToStringNodeParameters(final NodeParametersInput input) {
        m_columnSelection = guessColumnName(input).map(DataColumnSpec::getName).orElse("");
    }

    private static Optional<DataColumnSpec> guessColumnName(final NodeParametersInput input) {
        return ColumnSelectionUtil.getFirstCompatibleColumnOfFirstPort(input, URIDataValue.class);
    }

    static class OnlyURIColumnsProvider extends CompatibleColumnsProvider {
        protected OnlyURIColumnsProvider() {
            super(URIDataValue.class);
        }
    }

    @Section(title = "Column Configuration")
    interface ColumnConfigurationSection {
    }

    @Section(title = "Output Configuration")
    @After(ColumnConfigurationSection.class)
    interface OutputConfigurationSection {
    }

    interface ColumnSelectionReference extends ParameterReference<String> {
    }

    @SuppressWarnings("restriction")
    static final class ColumnSelectionValueProvider extends ColumnNameAutoGuessValueProvider {
        ColumnSelectionValueProvider() {
            super(ColumnSelectionReference.class);
        }

        @Override
        protected Optional<DataColumnSpec> autoGuessColumn(final NodeParametersInput parametersInput) {
            return guessColumnName(parametersInput);
        }
    }

    /**
     * Column selection setting. Column that will be converted to string.
     */
    @Layout(ColumnConfigurationSection.class)
    @Persist(configKey = "columnselection")
    @Widget(title = "Column selection",
        description = "Column that will be converted.")
    @ValueReference(ColumnSelectionReference.class)
    @ValueProvider(ColumnSelectionValueProvider.class)
    @ChoicesProvider(OnlyURIColumnsProvider.class)
    String m_columnSelection = "URI";

    /**
     * Replace policy setting.
     * Append or replace configuration for output column handling.
     */
    @Layout(OutputConfigurationSection.class)
    @Persistor(ReplacePolicyPersistor.class)
    @Widget(title = "Append or replace",
            description = "Choose whether to append a new column or replace the selected column.")
    @ValueSwitchWidget
    @RadioButtonsWidget
    @ValueReference(ReplacePolicyValueRef.class)
    ReplacePolicy m_replacePolicy = ReplacePolicy.APPEND;

    /**
     * New column name setting.
     * Name of the appended column.
     */
    @Layout(OutputConfigurationSection.class)
    @Persist(configKey = "columnname")
    @Widget(title = "New column name",
            description = "Name of the appended column.")
    @TextInputWidget
    @Effect(predicate = ReplacePolicyIsAppend.class, type = Effect.EffectType.SHOW)
    String m_columnName = "String";

    /**
     * Persistor for ReplacePolicy enum to handle backward compatibility with string values.
     */
    static final class ReplacePolicyPersistor implements NodeParametersPersistor<ReplacePolicy> {

        static final String CFG_KEY = "replace";

        @Override
        public ReplacePolicy load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var name = settings.getString(CFG_KEY);
            for (final ReplacePolicy rp : ReplacePolicy.values()) {
                if (rp.getName().equals(name)) {
                    return rp;
                }
            }
            throw new InvalidSettingsException("Unknown replace policy name: " + name);
        }

        @Override
        public void save(final ReplacePolicy replacePolicy, final NodeSettingsWO settings) {
            settings.addString(CFG_KEY, replacePolicy.getName());
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{CFG_KEY}};
        }
    }

    /**
     * Reference interface for the replace policy field.
     */
    interface ReplacePolicyValueRef extends ParameterReference<ReplacePolicy> {
    }

    /**
     * Predicate to check if replace policy is APPEND.
     */
    static final class ReplacePolicyIsAppend implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(ReplacePolicyValueRef.class).isOneOf(ReplacePolicy.APPEND);
        }
    }
}
