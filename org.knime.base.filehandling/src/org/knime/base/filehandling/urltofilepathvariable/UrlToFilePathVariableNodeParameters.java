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

package org.knime.base.filehandling.urltofilepathvariable;

import java.util.List;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.legacy.AutoGuessValueProvider;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.FlowVariableChoicesProvider;

/**
 * Node parameters for URL to File Path (Variable).
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class UrlToFilePathVariableNodeParameters implements NodeParameters {

    @Section(title = "Failing Behavior")
    interface FailingBehaviorSection {
    }

    @Persistor(FlowVariableNamePersistor.class)
    @Widget(title = "Flow variable containing URLs", description = """
            Select a string flow variable containing the URL string to convert. The node will convert this URL into
            four string flow variables containing the file path, parent folder, file name, and file extension.
            """)
    @ChoicesProvider(StringFlowVariablesProvider.class)
    @ValueReference(FlowVariableNameRef.class)
    @ValueProvider(FlowVariableNameProvider.class)
    String m_flowVariableName = UrlToFilePathVariableNodeModel.DEF_VARNAME;

    static final class FlowVariableNameRef implements ParameterReference<String> {
    }

    @Persist(configKey = UrlToFilePathVariableConfigKeys.ADD_PREFIX_TO_VAR)
    @Widget(title = "Add prefix to variable identifiers", description = """
            If checked, the name of the specified input variable is used as prefix for the names of the output
            variables.
            """)
    boolean m_addPrefixToVariable = UrlToFilePathVariableNodeModel.DEF_ADD_PREFIX_TO_VARIABLE;

    @Persist(configKey = UrlToFilePathVariableConfigKeys.FAIL_ON_INVALID_SYNTAX)
    @Layout(FailingBehaviorSection.class)
    @Widget(title = "Fail if URL has invalid syntax", description = """
            If checked, node will fail if an invalid url string occurs, otherwise an empty string will be set as file
            path, parent folder, file name, and file extension variables.
            """)
    boolean m_failOnInvalidSyntax = UrlToFilePathVariableNodeModel.DEF_FAIL_ON_INVALID_SYNTAX;

    @Persist(configKey = UrlToFilePathVariableConfigKeys.FAIL_ON_INVALID_LOCATION)
    @Layout(FailingBehaviorSection.class)
    @Widget(title = "Fail if file does not exist", description = """
            If checked, node will fail if a file location does not exist, otherwise an empty string will set as file
            path parent folder, file name, and file extension variables.
            """)
    boolean m_failOnInvalidLocation = UrlToFilePathVariableNodeModel.DEF_FAIL_ON_INVALID_LOCATION;

    static final class StringFlowVariablesProvider implements FlowVariableChoicesProvider {

        @Override
        public List<FlowVariable> flowVariableChoices(final NodeParametersInput context) {
            return context.getAvailableInputFlowVariables(VariableType.StringType.INSTANCE).values().stream().toList();
        }

    }

    static final class FlowVariableNameProvider extends AutoGuessValueProvider<String> {

        protected FlowVariableNameProvider() {
            super(FlowVariableNameRef.class);
        }

        @Override
        protected boolean isEmpty(final String name) {
            return name == null || name.isEmpty();
        }

        @Override
        protected String autoGuessValue(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            return parametersInput.getAvailableInputFlowVariables(VariableType.StringType.INSTANCE).values().stream()
                .findFirst().map(v -> v.getName()).orElse(null);
        }

    }

    /**
     * We load null instead of <column name default> to have a nice state if no columns are available for selection. (we
     * have no values present instead of (MISSING) <column name default>)
     */
    static final class FlowVariableNamePersistor implements NodeParametersPersistor<String> {

        @Override
        public String load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var variableName = settings.getString(UrlToFilePathVariableConfigKeys.VARIABLE_NAME);
            return variableName.equals(UrlToFilePathVariableNodeModel.DEF_VARNAME) ? null : variableName;
        }

        @Override
        public void save(final String param, final NodeSettingsWO settings) {
            settings.addString(UrlToFilePathVariableConfigKeys.VARIABLE_NAME,
                param == null ? UrlToFilePathVariableNodeModel.DEF_VARNAME : param);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{UrlToFilePathVariableConfigKeys.VARIABLE_NAME}};
        }

    }

}
