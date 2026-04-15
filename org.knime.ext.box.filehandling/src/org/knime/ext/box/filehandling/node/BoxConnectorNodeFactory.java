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
 *   2023-02-15 (Alexander Bondaletov): created
 */
package org.knime.ext.box.filehandling.node;

import org.knime.node.parameters.legacy.nodeimpl.WebUINodeConfiguration;
import org.knime.node.parameters.legacy.nodeimpl.WebUINodeFactory;
import org.knime.credentials.base.CredentialPortObject;
import org.knime.filehandling.core.port.FileSystemPortObject;

/**
 * Node factory for the Box Connector.
 *
 * @author Alexander Bondaletov, Redfield SE
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings({ "restriction", "deprecation" })
public class BoxConnectorNodeFactory extends WebUINodeFactory<BoxConnectorNodeModel> {

    private static final String NODE_NAME = "Box Connector";

    private static final String NODE_ICON = "./file_system_connector.png";

    private static final String SHORT_DESCRIPTION = "Connects to Box in order to read/write files in downstream nodes.";

    private static final String FULL_DESCRIPTION = """
            <p>This node connects to Box. The resulting output port allows downstream nodes to access
            <i>files</i>, e.g. to read or write, or to perform other file system operations
            (browse/list files, copy, move, ...).</p>

            <p><b>Path syntax:</b> Paths for this connector are specified with a UNIX-like syntax such as
            /myfolder/myfile. An absolute path consists of:
                <ol>
                    <li>A leading slash ("/").</li>
                    <li>Followed by the path to the file ("myfolder/myfile" in the above example).</li>
                </ol>
            </p>""";

    private static final WebUINodeConfiguration CONFIGURATION = WebUINodeConfiguration.builder() //
        .name(NODE_NAME) //
        .icon(NODE_ICON) //
        .shortDescription(SHORT_DESCRIPTION) //
        .fullDescription(FULL_DESCRIPTION) //
        .modelSettingsClass(BoxConnectorNodeParameters.class) //
        .nodeType(NodeType.Source) //
        .sinceVersion(5, 1, 0) //
        .addInputPort("Box Credential", CredentialPortObject.TYPE, "Box Credential") //
        .addOutputPort("Box File System Connection", FileSystemPortObject.TYPE, "Box File System Connection") //
        .build();

    /**
     * Create a new factory.
     */
    public BoxConnectorNodeFactory() {
        super(CONFIGURATION);
    }

    @Override
    public BoxConnectorNodeModel createNodeModel() {
        return new BoxConnectorNodeModel();
    }

}
