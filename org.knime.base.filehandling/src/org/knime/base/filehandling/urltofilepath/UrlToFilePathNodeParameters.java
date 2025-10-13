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

package org.knime.base.filehandling.urltofilepath;

import java.util.List;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.StringValue;
import org.knime.core.data.uri.URIDataValue;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.legacy.AutoGuessValueProvider;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider;

/**
 * Node parameters for URL to File Path.
 *
 * @author Halil Yerlikaya, KNIME GmbH, Berlin, Germany
 * @author AI Migration Pipeline v1.1
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class UrlToFilePathNodeParameters implements NodeParameters {

    UrlToFilePathNodeParameters() {
    }

    UrlToFilePathNodeParameters(final NodeParametersInput input) {
        m_columnName = guessColumnName(input);
    }

    private static String guessColumnName(final NodeParametersInput input) {
        return ColumnSelectionUtil.getCompatibleColumnsOfFirstPort(input, StringValue.class, URIDataValue.class)
            .stream().findFirst().map(DataColumnSpec::getName).orElse(UrlToFilePathNodeModel.DEF_COLNAME);
    }

    static class URLColumnProvider extends CompatibleColumnsProvider {
        URLColumnProvider() {
            super(List.of(StringValue.class, URIDataValue.class));
        }
    }

    interface ColumnNameReference extends ParameterReference<String> {
    }

    static final class ColumnNameValueProvider extends AutoGuessValueProvider<String> {
        ColumnNameValueProvider() {
            super(ColumnNameReference.class);
        }

        @Override
        protected boolean isEmpty(final String value) {
            // If value is the default, it has likely not been set by the user, so consider guessing a sensible name
            return UrlToFilePathNodeModel.DEF_COLNAME.equals(value);
        }

        @Override
        protected String autoGuessValue(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {

            final var spec = parametersInput.getInTableSpec(0);
            // if input table is not available or default column name actually exists, abort update
            if (spec.isEmpty() || spec.get().containsName(UrlToFilePathNodeModel.DEF_COLNAME)) {
                throw new StateComputationFailureException();
            }
            return guessColumnName(parametersInput);
        }
    }

    @Section(title = "Column Selection")
    interface ColumnSelectionSection {
    }

    @Section(title = "Failing Behavior")
    interface FailingBehaviorSection {
    }

    /**
     * Column containing URLs setting. The column containing the URL strings.
     */
    @Layout(ColumnSelectionSection.class)
    @Persist(configKey = UrlToFilePathConfigKeys.COLUMN_NAME)
    @Widget(title = "Column containing URLs", description = "The column containing the URL strings.")
    @ValueReference(ColumnNameReference.class)
    @ValueProvider(ColumnNameValueProvider.class)
    @ChoicesProvider(URLColumnProvider.class)
    String m_columnName = UrlToFilePathNodeModel.DEF_COLNAME;

    /**
     * Fail if URL has invalid syntax setting. If checked, node will fail if an invalid URL string occurs, otherwise a
     * missing value will be inserted as file path, parent folder, file name, and file extension.
     */
    @Layout(FailingBehaviorSection.class)
    @Persist(configKey = UrlToFilePathConfigKeys.FAIL_ON_INVALID_SYNTAX)
    @Widget(title = "Fail if URL has invalid syntax",
        description = "If checked, node will fail if an invalid URL string occurs, otherwise a missing value "
            + "will be inserted as file path, parent folder, file name, and file extension.")
    boolean m_failOnInvalidSyntax = UrlToFilePathNodeModel.DEF_FAIL_ON_INVALID_SYNTAX;

    /**
     * Fail if file does not exist setting. If checked, node will fail if a file location does not exist, otherwise a
     * missing value will be inserted as file path, parent folder, file name, and file extension.
     */
    @Layout(FailingBehaviorSection.class)
    @Persist(configKey = UrlToFilePathConfigKeys.FAIL_ON_INVALID_LOCATION)
    @Widget(title = "Fail if file does not exist",
        description = "If checked, node will fail if a file location does not exist, otherwise a missing value "
            + "will be inserted as file path, parent folder, file name, and file extension.")
    boolean m_failOnInvalidLocation = UrlToFilePathNodeModel.DEF_FAIL_ON_INVALID_LOCATION;
}
