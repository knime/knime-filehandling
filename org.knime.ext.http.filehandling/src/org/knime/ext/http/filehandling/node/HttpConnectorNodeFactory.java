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
 * ---------------------------------------------------------------------
 *
 * History
 *   2020-11-18 (Bjoern Lohrmann): created
 */
package org.knime.ext.http.filehandling.node;

import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;
import org.knime.filehandling.core.port.FileSystemPortObject;

/**
 * Factory class for HTTP(S) Connector Node.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
public class HttpConnectorNodeFactory extends WebUINodeFactory<HttpConnectorNodeModel> {

    private static final String NODE_NAME = "HTTP(S) Connector";

    private static final String NODE_ICON = "./file_system_connector.png";

    private static final String SHORT_DESCRIPTION = """
            Connects to a web server with HTTP(S) in order to read files in downstream nodes.
            """;

    private static final String FULL_DESCRIPTION = """
            <p>
                This node connects to a web server with HTTP(S). The resulting output port allows downstream nodes to
                read <i>files</i> from a webserver.
            </p>
            <p>
                <b>Note: This connector provides very limited functionality!</b> It does not support listing, writing
                or deleting files/folders, nor is it possible to create folders on the webserver. The only operation
                supported is reading <i>single</i> files. Hence, with this file system it is not possible to
                interactively browse files, use Writer nodes (e.g. CSV Writer), or read multiple files with a Reader
                node.
            </p>
            <p>
                <b>Path syntax:</b> Paths for HTTP(S) are specified with a UNIX-like syntax, and may be suffixed with
                a query ('?') and or fragment ('#). Non-alphanumeric characters in paths - such as whitespace (" ") -
                must be <a href="https://en.wikipedia.org/wiki/Percent-encoding">percent-encoded</a>. For example
                <tt>/my%20folder/resource?myparam=myvalue#myfragment</tt> is an absolute path that consists of:
                <ol>
                    <li>A leading slash (<tt>/</tt>).</li>
                    <li>The name of a folder (<tt>my folder</tt> with percent-encoding), followed by a slash.</li>
                    <li>Followed by the name of a file/resource (<tt>resource</tt>).</li>
                    <li>Followed by an (optional) query (<tt>?myparam=myvalue</tt>).</li>
                    <li>Followed by an (optional) fragment (<tt>#myfragment</tt>).</li>
                </ol>
            </p>
            """;

    private static final WebUINodeConfiguration CONFIGURATION = WebUINodeConfiguration.builder() //
        .name(NODE_NAME) //
        .icon(NODE_ICON) //
        .shortDescription(SHORT_DESCRIPTION) //
        .fullDescription(FULL_DESCRIPTION) //
        .modelSettingsClass(HttpConnectorNodeParameters.class) //
        .nodeType(NodeType.Source) //
        .sinceVersion(4, 3, 0) //
        .keywords("http", "https", "web", "url", "file system", "connector") //
        .addOutputPort("HTTP(S) File System Connection", FileSystemPortObject.TYPE, "HTTP(S) File System Connection.")
        .build();

    /**
     * Create a new factory.
     */
    public HttpConnectorNodeFactory() {
        super(CONFIGURATION);
    }

    @Override
    public HttpConnectorNodeModel createNodeModel() {
        return new HttpConnectorNodeModel();
    }
}
