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
 *   2022-04-27 (Dragan Keselj): created
 */
package org.knime.archive.zip.filehandling.node;

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
 * Node factory for the ArchiveZip Connector.
 *
 * @author Dragan Keselj, KNIME GmbH
 * @author Halil Yerlikaya, KNIME GmbH, Berlin, Germany
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings({"restriction", "removal"})
public class ArchiveZipConnectorNodeFactory extends ConfigurableNodeFactory<ArchiveZipConnectorNodeModel>
    implements NodeDialogFactory, KaiNodeInterfaceFactory {
    /**
     * Connect group ID of dynamic port.
     */
    public static final String FS_CONNECT_GRP_ID = "Input File System";

    @Override
    protected ArchiveZipConnectorNodeModel createNodeModel(final NodeCreationConfiguration cfg) {
        return new ArchiveZipConnectorNodeModel(cfg);
    }

    @Override
    protected Optional<PortsConfigurationBuilder> createPortsConfigBuilder() {
        final var builder = new PortsConfigurationBuilder();

        builder.addOptionalInputPortGroup(FS_CONNECT_GRP_ID, FileSystemPortObject.TYPE);

        builder.addFixedOutputPortGroup("ZIP Archive File System Connection", FileSystemPortObject.TYPE);
        return Optional.of(builder);
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<ArchiveZipConnectorNodeModel> createNodeView(final int viewIndex,
        final ArchiveZipConnectorNodeModel nodeModel) {
        return null;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    private static final String NODE_NAME = "ZIP Archive Connector";

    private static final String NODE_ICON = "./file_system_connector.png";

    private static final String SHORT_DESCRIPTION = """
            This node creates a file system connection that allows to read the files/folders stored inside a ZIP
                archive.
            """;

    private static final String FULL_DESCRIPTION = """
            <p>
                This node creates a file system connection that allows to read the files/folders stored inside a ZIP
                archive. The resulting file system connection output port allows downstream nodes to read the compressed
                files from a zip archive file.
            </p>
            <p>
                <b>Path syntax:</b> Paths for this connector are specified with a UNIX-like syntax such as
                <code>/myfolder/myfile</code>. An absolute path consists of:
                <ul>
                    <li>A leading slash ("/").</li>
                    <li>Followed by the path to the file ("myfolder/myfile" in the above example).</li>
                </ul>
            </p>
            <p>
                <b>Note:</b> When the ZIP file changes, this node has to be reset and re-executed, otherwise
                its behavior is undefined, which may result in errors and/or invalid data being read.
            </p>
            """;

    private static final List<PortDescription> INPUT_PORTS =
        List.of(dynamicPort(FS_CONNECT_GRP_ID, FS_CONNECT_GRP_ID, """
                File system that can be used to provide a zip archive file.
                """));

    private static final List<PortDescription> OUTPUT_PORTS =
        List.of(fixedPort("ZIP Archive File System Connection", """
                ZIP Archive File System Connection
                """));

    @Override
    public NodeDialogPane createNodeDialogPane(final NodeCreationConfiguration cfg) {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, ArchiveZipConnectorNodeParameters.class);
    }

    @Override
    public NodeDescription createNodeDescription() {
        return DefaultNodeDescriptionUtil.createNodeDescription( //
            NODE_NAME, //
            NODE_ICON, //
            INPUT_PORTS, //
            OUTPUT_PORTS, //
            SHORT_DESCRIPTION, //
            FULL_DESCRIPTION, //
            List.of(), //
            ArchiveZipConnectorNodeParameters.class, //
            null, //
            NodeType.Source, //
            List.of(), //
            null //
        );
    }

    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, ArchiveZipConnectorNodeParameters.class));
    }

}
