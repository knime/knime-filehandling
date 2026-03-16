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

package org.knime.base.filehandling.stringstobinaryobjects;

import java.util.List;
import java.util.stream.Stream;

import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.EnumChoicesProvider;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.util.AllColumnsProvider;
import org.knime.node.parameters.widget.text.TextInputWidget;

/**
 * Node parameters for Strings to Binary Objects.
 *
 * @author Jannik Semperowitsch, KNIME GmbH, Konstanz, Germany
 */
@LoadDefaultsForAbsentFields
class StringsToBinaryObjectsNodeParameters implements NodeParameters {
    /**
     * Column selection: Column that will be converted.
     */
    @Widget(title = "Column selection", description = """
            Select columns to be included in the Cronbach Alpha calculation.
            Only numeric columns are available for selection.
            """)
    @ChoicesProvider(AllColumnsProvider.class)
    String m_columnselection = "";

    /**
     * Encoding: Encoding of the string.
     */
    @Widget(title = "Encoding",
        description = "Encoding of the string.")
    @ChoicesProvider(EncodingChoicesProvider.class)
    String m_encoding = Encoding.UTF_8.name();

    /**
     * Enum for available encodings.
     */
    public enum Encoding {
        @org.knime.node.parameters.widget.choices.Label("US-ASCII")
        US_ASCII,
        @org.knime.node.parameters.widget.choices.Label("ISO-8859-1")
        ISO_8859_1,
        @org.knime.node.parameters.widget.choices.Label("UTF-8")
        UTF_8,
        @org.knime.node.parameters.widget.choices.Label("UTF-16BE")
        UTF_16BE,
        @org.knime.node.parameters.widget.choices.Label("UTF-16LE")
        UTF_16LE,
        @org.knime.node.parameters.widget.choices.Label("UTF-16")
        UTF_16
    }

    /**
     * Append or replace: Append or replace the selected column with the new column.
     */
    @Widget(title = "Append or replace",
            description = "Choose whether to append a new column or replace the selected column.")
    @ValueSwitchWidget
    @RadioButtonsWidget
    AppendOrReplace m_replace = AppendOrReplace.APPEND;

    /**
     * New column name: Name of the appended column.
     */
    @Widget(title = "New column name",
        description = "Name of the appended column.")
    @TextInputWidget(placeholder = "New column name")
    String m_columnname = "New Column";

    /**
     * Enum for append or replace option.
     */
    enum AppendOrReplace {
        @org.knime.node.parameters.widget.choices.Label("Append")
        APPEND,
        @org.knime.node.parameters.widget.choices.Label("Replace")
        REPLACE
    }

    static final class EncodingChoicesProvider implements EnumChoicesProvider<Encoding> {

        @Override
        public List<Encoding> choices(final NodeParametersInput context) {
            return Stream.of(Encodings.getAllEncodings())
                    .map(Encoding::valueOf)
                    .toList();
        }
    }
}