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

package org.knime.base.filehandling.extracturiinfo;

import java.util.Optional;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.uri.URIDataValue;
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
import org.knime.node.parameters.updates.legacy.ColumnNameAutoGuessValueProvider;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider;

/**
 * Node parameters for Extract URI Info.
 *
 * @author Halil Yerlikaya, KNIME GmbH, Berlin, Germany
 * @author AI Migration Pipeline v1.1
 */
@LoadDefaultsForAbsentFields
class ExtractURIInfoNodeParameters implements NodeParameters {

    @Section(title = "Extract Options")
    interface ExtractOptionsSection {
    }

    ExtractURIInfoNodeParameters() {
    }

    ExtractURIInfoNodeParameters(final NodeParametersInput input) {
        m_columnSelection = ColumnSelectionUtil.getFirstCompatibleColumnOfFirstPort(input, URIDataValue.class)
            .map(DataColumnSpec::getName).orElse("");
    }

    static class OnlyURIColumnsProvider extends CompatibleColumnsProvider {
        protected OnlyURIColumnsProvider() {
            super(URIDataValue.class);
        }
    }

    interface ColumnSelectionRef extends ParameterReference<String> {
    }

    @SuppressWarnings("restriction")
    static final class ColumnSelectionProvider extends ColumnNameAutoGuessValueProvider {
        ColumnSelectionProvider() {
            super(ColumnSelectionRef.class);
        }

        @Override
        protected Optional<DataColumnSpec> autoGuessColumn(final NodeParametersInput parametersInput) {
            return ColumnSelectionUtil.getFirstCompatibleColumnOfFirstPort(parametersInput, URIDataValue.class);
        }
    }

    @Persist(configKey = "columnselection")
    @Widget(title = "Column selection", description = """
            Column containing the URIs to extract information from.
            URIs are made up of several components, such as scheme, authority, path, query, and fragment.
            For instance, in the URI "https://www.example.com:8080/path/to/resource?query=example#fragment",
            the scheme is "https", the authority is "www.example.com:8080", the path is "/path/to/resource",
            the query is "query=example", and the fragment is "fragment".
            """)
    @ChoicesProvider(OnlyURIColumnsProvider.class)
    @ValueReference(ColumnSelectionRef.class)
    @ValueProvider(ColumnSelectionProvider.class)
    String m_columnSelection = "";

    @Layout(ExtractOptionsSection.class)
    @Persist(configKey = "authority")
    @Widget(title = "Authority", description = """
            Append column with authority information of the URI.
            The authority component of a URI is typically composed of the user, host, and port.
            """)
    boolean m_authority = false;

    @Layout(ExtractOptionsSection.class)
    @Persist(configKey = "fragment")
    @Widget(title = "Fragment", description = """
            Append column with fragment information of the URI.
            The fragment component of a URI is the last part of the URI that starts after the '#' symbol.
            It is often used to identify a specific section or element within the resource.
            """)
    boolean m_fragment = false;

    @Layout(ExtractOptionsSection.class)
    @Persist(configKey = "Host")
    @Widget(title = "Host", description = """
            Append column with host information of the URI.
            The host component of a URI typically represents the domain name or IP address of the server where the
            resource is located.
            """)
    boolean m_host = false;

    @Layout(ExtractOptionsSection.class)
    @Persist(configKey = "path")
    @Widget(title = "Path", description = """
            Append column with path information of the URI.
            The path component of a URI is the section that comes after the authority.
            It specifies the hierarchical location of the resource on the server.
            """)
    boolean m_path = false;

    @Layout(ExtractOptionsSection.class)
    @Persist(configKey = "port")
    @Widget(title = "Port", description = """
            Append column with port information of the URI.
            The port component of a URI specifies the network port number on the server where the resource is hosted.
            """)
    boolean m_port = false;

    @Layout(ExtractOptionsSection.class)
    @Persist(configKey = "query")
    @Widget(title = "Query", description = """
            Append column with query information of the URI.
            The query component of a URI is the part that comes after the '?' symbol.
            It typically contains key-value pairs that provide additional parameters or instructions for accessing
            the resource.
            """)
    boolean m_query = false;

    @Layout(ExtractOptionsSection.class)
    @Persist(configKey = "scheme")
    @Widget(title = "Scheme", description = """
            Append column with scheme information of the URI.
            The scheme component of a URI indicates the protocol or method used to access the resource,
            such as "http", "https", "ftp", etc.
            """)
    boolean m_scheme = false;

    @Layout(ExtractOptionsSection.class)
    @Persist(configKey = "userinfo")
    @Widget(title = "User", description = """
            Append column with user information of the URI.
            The user component of a URI typically contains the username and, optionally, the password
            required to access the resource.
            """)
    boolean m_user = false;
}
