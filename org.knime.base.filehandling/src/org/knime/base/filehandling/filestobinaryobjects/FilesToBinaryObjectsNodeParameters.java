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
package org.knime.base.filehandling.filestobinaryobjects;

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
 * Node parameters for Files to Binary Objects.
 *
 * @author Thomas Reifenberger, TNG Technology Consulting GmbH
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
class FilesToBinaryObjectsNodeParameters implements NodeParameters {

    @Section(title = "Column Selection")
    private interface ColumnSelectionSection {
    }

    @Section(title = "Output Column")
    @After(ColumnSelectionSection.class)
    private interface OutputColumnSection {
    }

    @Layout(ColumnSelectionSection.class)
    @Widget(title = "URI column",
        description = "The column containing the paths to the files that should be loaded as binary objects.")
    @ChoicesProvider(URIColumnsProvider.class)
    @Persist(configKey = "uricolumn")
    @ValueReference(URIColumnRef.class)
    @ValueProvider(URIColumnAutoGuesser.class)
    String m_uriColumn = "";

    @Layout(OutputColumnSection.class)
    @Widget(title = "Append or replace",
        description = "Determines whether the binary object column is appended to the table or replaces the URI column.")
    @ValueSwitchWidget
    @ValueReference(AppendOrReplaceRef.class)
    @Persistor(AppendOrReplacePersistor.class)
    AppendOrReplace m_appendOrReplace = AppendOrReplace.APPEND;

    @Layout(OutputColumnSection.class)
    @Widget(title = "New column name",
        description = "The name for the newly appended binary object column. Only relevant when \"Append\" is selected.")
    @Effect(predicate = IsAppend.class, type = EffectType.SHOW)
    @Persist(configKey = "binaryobjectcolumnname")
    String m_binaryObjectColumnName = "BinaryObject";

    private static final class URIColumnsProvider extends CompatibleColumnsProvider {
        URIColumnsProvider() {
            super(URIDataValue.class);
        }
    }

    private interface URIColumnRef extends ParameterReference<String> {
    }

    private static final class URIColumnAutoGuesser extends ColumnNameAutoGuessValueProvider {
        URIColumnAutoGuesser() {
            super(URIColumnRef.class);
        }

        @Override
        protected Optional<DataColumnSpec> autoGuessColumn(final NodeParametersInput parametersInput) {
            return ColumnSelectionUtil.getFirstCompatibleColumnOfFirstPort(parametersInput, URIDataValue.class);
        }
    }

    private enum AppendOrReplace {
            @Label("Append")
            APPEND("Append"),

            @Label("Replace")
            REPLACE("Replace");

        private final String m_legacyValue;

        AppendOrReplace(final String legacyValue) {
            m_legacyValue = legacyValue;
        }

        String getLegacyValue() {
            return m_legacyValue;
        }

        static AppendOrReplace fromLegacyValue(final String legacyValue) {
            for (final AppendOrReplace v : values()) {
                if (v.getLegacyValue().equals(legacyValue)) {
                    return v;
                }
            }
            throw new IllegalArgumentException("Unknown legacy value: " + legacyValue);
        }
    }

    private static final class AppendOrReplaceRef implements ParameterReference<AppendOrReplace> {
    }

    private static final class IsAppend implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(AppendOrReplaceRef.class).isOneOf(AppendOrReplace.APPEND);
        }
    }

    private static final class AppendOrReplacePersistor implements NodeParametersPersistor<AppendOrReplace> {

        private static final String CONFIG_KEY = "replace";

        @Override
        public AppendOrReplace load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return AppendOrReplace.fromLegacyValue(settings.getString(CONFIG_KEY));
        }

        @Override
        public void save(final AppendOrReplace value, final NodeSettingsWO settings) {
            settings.addString(CONFIG_KEY, value.getLegacyValue());
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{CONFIG_KEY}};
        }
    }

}
