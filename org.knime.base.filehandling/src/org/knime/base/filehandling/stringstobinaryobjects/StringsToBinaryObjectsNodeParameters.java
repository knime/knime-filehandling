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

import java.util.Optional;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.StringValue;
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
import org.knime.node.parameters.legacy.updates.ColumnNameAutoGuessValueProvider;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider;

/**
 * Node parameters for Strings to Binary Objects.
 *
 * @author Tim Crundall, TNG Technology Consulting GmbH
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class StringsToBinaryObjectsNodeParameters implements NodeParameters {

    @Widget(title = "Column selection",
        description = "Column containing the strings that will be converted to binary objects.")
    @ChoicesProvider(StringColumnsProvider.class)
    @ValueProvider(InputColumnProvider.class)
    @Persist(configKey = "columnselection")
    @ValueReference(InputColumnRef.class)
    String m_columnSelection = "";

    private interface InputColumnRef extends ParameterReference<String> {
    }

    private static final class StringColumnsProvider extends CompatibleColumnsProvider {
        protected StringColumnsProvider() {
            super(StringValue.class);
        }
    }

    private static final class InputColumnProvider extends ColumnNameAutoGuessValueProvider {
        protected InputColumnProvider() {
            super(InputColumnRef.class);
        }

        @Override
        protected Optional<DataColumnSpec> autoGuessColumn(final NodeParametersInput parametersInput) {
            final var compatibleColumns = ColumnSelectionUtil.getCompatibleColumnsOfFirstPort( //
                parametersInput, StringValue.class //
            );
            return compatibleColumns.isEmpty() //
                ? Optional.empty() //
                : Optional.of(compatibleColumns.get(compatibleColumns.size() - 1));
        }
    }

    @Widget(title = "Encoding", description = """
            Character set used to encode the string into binary object bytes. Common choices are \
            UTF-8 (the default), UTF-16, and US-ASCII.\
            """)
    @Persistor(EncodingPersistor.class)
    @Migrate(loadDefaultIfAbsent = true)
    Encoding m_encoding = Encoding.UTF_8;

    private enum Encoding {
            @Label(value = "US-ASCII", description = "Seven-bit ASCII (ISO646-US / Basic Latin block of Unicode).")
            US_ASCII(Encodings.US_ASCII), //
            @Label(value = "ISO-8859-1", description = "ISO Latin Alphabet No. 1 (ISO-LATIN-1).")
            ISO_8859_1(Encodings.ISO_8859_1), //
            @Label(value = "UTF-8", description = "Eight-bit UCS Transformation Format.")
            UTF_8(Encodings.UTF_8), //
            @Label(value = "UTF-16BE", description = "Sixteen-bit UCS Transformation Format, big-endian byte order.")
            UTF_16BE(Encodings.UTF_16BE), //
            @Label(value = "UTF-16LE", description = """
                    Sixteen-bit UCS Transformation Format, little-endian byte order.\
                    """)
            UTF_16LE(Encodings.UTF_16LE), //
            @Label(value = "UTF-16", description = """
                    Sixteen-bit UCS Transformation Format, byte order identified by an optional byte-order mark.\
                    """)
            UTF_16(Encodings.UTF_16);

        private final String m_name;

        Encoding(final String name) {
            m_name = name;
        }

        String getName() {
            return m_name;
        }
    }

    private static final class EncodingPersistor implements NodeParametersPersistor<Encoding> {

        private static final String CFG_KEY = "encoding";

        @Override
        public Encoding load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final String value = settings.getString(CFG_KEY);
            for (final Encoding encoding : Encoding.values()) {
                if (encoding.getName().equals(value)) {
                    return encoding;
                }
            }
            throw new InvalidSettingsException("Unknown encoding: \"" + value + "\".");
        }

        @Override
        public void save(final Encoding obj, final NodeSettingsWO settings) {
            settings.addString(CFG_KEY, obj.getName());
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{CFG_KEY}};
        }
    }

    @Widget(title = "Output handling", description = """
            Determines whether to append a new binary object column or to replace the selected string column.\
            """)
    @ValueSwitchWidget
    @ValueReference(OutputModeRef.class)
    @Persistor(OutputModePersistor.class)
    @Migrate(loadDefaultIfAbsent = true)
    OutputMode m_outputMode = OutputMode.REPLACE;

    private interface OutputModeRef extends ParameterReference<OutputMode> {
    }

    private enum OutputMode {
            @Label(value = "Replace", description = """
                    Replaces the selected string column with the binary object column.\
                    """)
            REPLACE, //
            @Label(value = "Append", description = "Appends a new binary object column to the table.")
            APPEND;
    }

    private static final class OutputModePersistor implements NodeParametersPersistor<OutputMode> {

        private static final String CFG_KEY = "replace";

        @Override
        public OutputMode load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final String value = settings.getString(CFG_KEY);
            for (final ReplacePolicy policy : ReplacePolicy.values()) {
                if (policy.getName().equals(value)) {
                    return policy == ReplacePolicy.APPEND ? OutputMode.APPEND : OutputMode.REPLACE;
                }
            }
            throw new InvalidSettingsException("Unknown replace policy: \"" + value + "\".");
        }

        @Override
        public void save(final OutputMode obj, final NodeSettingsWO settings) {
            final var policy = obj == OutputMode.APPEND ? ReplacePolicy.APPEND : ReplacePolicy.REPLACE;
            settings.addString(CFG_KEY, policy.getName());
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{CFG_KEY}};
        }
    }

    @Widget(title = "New column name", description = """
            Name of the appended column. Only applicable when "Append" is selected.\
            """)
    @Effect(predicate = IsAppend.class, type = EffectType.SHOW)
    @Persist(configKey = "columnname")
    String m_newColumnName = "Binary Object";

    private static final class IsAppend implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(OutputModeRef.class).isOneOf(OutputMode.APPEND);
        }
    }

}
