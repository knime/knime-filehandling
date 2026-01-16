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
 *   2020-10-01 (Vyacheslav Soldatov): created
 */
package org.knime.ext.ftp.filehandling.node;

import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;
import org.knime.filehandling.core.port.FileSystemPortObject;

/**
 * Factory class for FTP Connection Node.
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings({ "restriction", "deprecation" })
public class FtpConnectorNodeFactory extends WebUINodeFactory<FtpConnectorNodeModel> {

    private static final String NODE_NAME = "FTP Connector";

    private static final String NODE_ICON = "./file_system_connector.png";

    private static final String SHORT_DESCRIPTION = """
            Connects to remote file system via FTP in order to read/write files in downstream nodes.
            """;

    private static final String FULL_DESCRIPTION = """
            <p>
                This node connects to a remote FTP server. The resulting output port allows downstream nodes to
                access the <i>files</i> of the remote server, e.g. to read or write, or to perform other file system
                operations (browse/list files, copy, move, ...).
            </p>
            <p>
                <b>Path syntax:</b> Paths for FTP are specified with a UNIX-like syntax, for example
                <tt>/myfolder/file.csv</tt>, which is an absolute path that consists of:
                <ol>
                    <li>A leading slash (<tt>/</tt>).</li>
                    <li>The name of a folder (<tt>myfolder</tt>), followed by a slash.</li>
                    <li>Followed by the name of a file (<tt>file.csv</tt>).</li>
                </ol>
            </p>
            """;

    private static final WebUINodeConfiguration CONFIGURATION = WebUINodeConfiguration.builder() //
            .name(NODE_NAME) //
            .icon(NODE_ICON) //
            .shortDescription(SHORT_DESCRIPTION) //
            .fullDescription(FULL_DESCRIPTION) //
            .modelSettingsClass(FtpConnectorNodeParameters.class) //
            .nodeType(NodeType.Source) //
            .sinceVersion(4, 3, 0) //
            .addOutputPort("FTP File System Connection", FileSystemPortObject.TYPE, "FTP File System Connection.") //
            .keywords("file", "remote", "transfer", "protocol", "FTP", "FTPS") //
            .build();

    /**
     * Create a new factory.
     */
    public FtpConnectorNodeFactory() {
        super(CONFIGURATION);
    }

    @Override
    public FtpConnectorNodeModel createNodeModel() {
        return new FtpConnectorNodeModel();
    }
}
