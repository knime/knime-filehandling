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
 *   2021-03-06 (Alexander Bondaletov): created
 */
package org.knime.ext.smb.filehandling.node;

import static org.knime.node.impl.description.PortDescription.fixedPort;

import java.util.List;
import java.util.Map;

import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.knime.core.webui.node.dialog.NodeDialog;
import org.knime.core.webui.node.dialog.NodeDialogFactory;
import org.knime.core.webui.node.dialog.NodeDialogManager;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultKaiNodeInterface;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterface;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterfaceFactory;
import org.knime.node.impl.description.DefaultNodeDescriptionUtil;
import org.knime.node.impl.description.PortDescription;

/**
 * Factory class for the {@link SmbConnectorNodeModel} node.
 *
 * @author Alexander Bondaletov
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
public class SmbConnectorNodeFactory extends NodeFactory<SmbConnectorNodeModel>
        implements NodeDialogFactory, KaiNodeInterfaceFactory {

    private static final String NODE_NAME = "SMB Connector";

    private static final String NODE_ICON = "./file_system_connector.png";

    private static final String SHORT_DESCRIPTION = """
            Connects to an SMB server (e.g. Samba, or Windows Server) in order to read/write files in downstream nodes.
            """;

    private static final String FULL_DESCRIPTION = """
            <p>
                This node connects to a remote SMB server (e.g. Samba, or Windows Server). The resulting output port
                allows downstream nodes to access <i>files</i>, e.g. to read or write, or to perform other file system
                operations (browse/list files, copy, move, ...). This node generally supports versions 2 and 3 of the
                SMB protocol. It also supports connecting to a
                <a href="https://docs.microsoft.com/en-us/windows-server/storage/dfs-namespaces/dfs-overview">Windows
                DFS namespace</a>.
            </p>
            <p>
                <b>Path syntax:</b> Paths for SMB are specified similar to Windows Paths, but with a leading backslash
                (<tt>\\</tt>). The path is interpreted relative to the chosen <i>Share</i> (or DFS namespace). For
                example <tt>\\myfolder\\file.csv</tt>, is an absolute path that consists of:
                <ol>
                    <li>A leading backslash (<tt>\\</tt>).</li>
                    <li>The name of a folder (<tt>myfolder</tt>), followed by a backslash.</li>
                    <li>Followed by the name of a file (<tt>file.csv</tt>).</li>
                </ol>
            </p>
            """;

    private static final List<PortDescription> INPUT_PORTS = List.of();

    private static final List<PortDescription> OUTPUT_PORTS = List.of(fixedPort("SMB File System Connection", """
            SMB File System Connection.
            """));

    @Override
    public SmbConnectorNodeModel createNodeModel() {
        return new SmbConnectorNodeModel();
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<SmbConnectorNodeModel> createNodeView(final int viewIndex, final SmbConnectorNodeModel nodeModel) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, SmbConnectorNodeParameters.class);
    }

    @Override
    public NodeDescription createNodeDescription() {
        return DefaultNodeDescriptionUtil.createNodeDescription(
            NODE_NAME,
            NODE_ICON,
            INPUT_PORTS,
            OUTPUT_PORTS,
            SHORT_DESCRIPTION,
            FULL_DESCRIPTION,
            List.of(),
            SmbConnectorNodeParameters.class,
            null,
            NodeType.Source,
            List.of(),
            null
        );
    }

    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, SmbConnectorNodeParameters.class));
    }
}

