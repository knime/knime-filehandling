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
 *   2020-07-28 (Vyacheslav Soldatov): created
 */
package org.knime.ext.ssh.filehandling.node;

import static org.knime.node.impl.description.PortDescription.dynamicPort;
import static org.knime.node.impl.description.PortDescription.fixedPort;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.knime.core.node.ConfigurableNodeFactory;
import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeView;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.webui.node.dialog.NodeDialog;
import org.knime.core.webui.node.dialog.NodeDialogFactory;
import org.knime.core.webui.node.dialog.NodeDialogManager;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultKaiNodeInterface;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterface;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterfaceFactory;
import org.knime.filehandling.core.port.FileSystemPortObject;
import org.knime.node.impl.description.DefaultNodeDescriptionUtil;
import org.knime.node.impl.description.PortDescription;

/**
 * Factory class for SSH Connection Node.
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 * @author Kai Franze, KNIME GmbH, Germany
 * @author AI Migration Pipeline v1.1
 */
@SuppressWarnings("restriction")
public class SshConnectorNodeFactory extends ConfigurableNodeFactory<SshConnectorNodeModel>
        implements NodeDialogFactory, KaiNodeInterfaceFactory {

    /**
     * Connect group ID of dynamic port.
     */
    public static final String FS_CONNECT_GRP_ID = "Input File System";

    private static final String NODE_NAME = "SSH Connector";

    private static final String NODE_ICON = "./file_system_connector.png";

    private static final String SHORT_DESCRIPTION = """
            Connects to remote file system via SSH in order to read/write files in downstream nodes.
            """;

    private static final String FULL_DESCRIPTION = """
            <p>
                This node connects to a remote SSH server using SFTP. The resulting output port allows downstream
                nodes to access the <i>files</i> of the remote server, e.g. to read or write, or to perform other file
                system operations (browse/list files, copy, move, ...).
            </p>
            <p>
                <b>Path syntax:</b> Paths for SSH are specified with a UNIX-like syntax such as /myfolder/myfile.
                An absolute path for SSH consists of:
                <ol>
                    <li>A leading slash ("/").</li>
                    <li>Followed by the path to the file ("myfolder/myfile" in the above example).</li>
                </ol>
            </p>
            """;

    private static final List<PortDescription> INPUT_PORTS = List
            .of(dynamicPort(FS_CONNECT_GRP_ID, FS_CONNECT_GRP_ID, """
                    File system that can be used to provide an SSH private key and/or known hosts file.
                    """));

    private static final List<PortDescription> OUTPUT_PORTS = List.of(fixedPort("SSH File System Connection", """
            SSH File System Connection.
            """));

    @Override
    protected SshConnectorNodeModel createNodeModel(
            final NodeCreationConfiguration creationConfig) {
        return new SshConnectorNodeModel(creationConfig);
    }
    @Override
    protected Optional<PortsConfigurationBuilder> createPortsConfigBuilder() {
        final PortsConfigurationBuilder builder = new PortsConfigurationBuilder();

        builder.addOptionalInputPortGroup(FS_CONNECT_GRP_ID, FileSystemPortObject.TYPE);

        builder.addFixedOutputPortGroup("SSH File System Connection", FileSystemPortObject.TYPE);
        return Optional.of(builder);
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<SshConnectorNodeModel> createNodeView(final int viewIndex, final SshConnectorNodeModel nodeModel) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    @Override
    public NodeDialogPane createNodeDialogPane(final NodeCreationConfiguration cfg) {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, SshConnectorNodeParameters.class);
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
            SshConnectorNodeParameters.class,
            null,
            NodeType.Source,
            List.of(),
            null
        );
    }

    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, SshConnectorNodeParameters.class));
    }
}
