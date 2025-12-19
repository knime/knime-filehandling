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
 *   Oct 9, 2020 (ayazqureshi): created
 */
package org.knime.base.filehandling.binaryobjects.writer;

import static org.knime.node.impl.description.PortDescription.dynamicPort;
import static org.knime.node.impl.description.PortDescription.fixedPort;

import java.util.List;
import java.util.Map;

import org.knime.core.data.blob.BinaryObjectDataValue;
import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.webui.node.dialog.NodeDialog;
import org.knime.core.webui.node.dialog.NodeDialogFactory;
import org.knime.core.webui.node.dialog.NodeDialogManager;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultKaiNodeInterface;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterface;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterfaceFactory;
import org.knime.filehandling.core.node.table.writer.AbstractMultiTableWriterNodeFactory;
import org.knime.node.impl.description.DefaultNodeDescriptionUtil;
import org.knime.node.impl.description.PortDescription;

/**
 * The {@link NodeFactory} to create the node model allowing to convert binary objects to files.
 *
 * @author Ayaz Ali Qureshi, KNIME GmbH, Berlin, Germany
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 * @author Thomas Reifenberger, TNG Technology Consulting GmbH
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
public final class BinaryObjectsToFilesNodeFactory
    extends AbstractMultiTableWriterNodeFactory<BinaryObjectDataValue, BinaryObjectsToFilesNodeConfig, //
            BinaryObjectsToFilesNodeModel, BinaryObjectsToFilesNodeDialog>
    implements NodeDialogFactory, KaiNodeInterfaceFactory {

    @Override
    protected BinaryObjectsToFilesNodeConfig getNodeConfig(final PortsConfiguration portConfig,
        final String portGroupName) {
        return new BinaryObjectsToFilesNodeConfig(portConfig, portGroupName);
    }

    @Override
    protected BinaryObjectsToFilesNodeModel getNodeModel(final PortsConfiguration portConfig,
        final BinaryObjectsToFilesNodeConfig nodeConfig, final int dataTableInputIndex) {
        return new BinaryObjectsToFilesNodeModel(portConfig, nodeConfig, dataTableInputIndex);
    }

    @Override
    protected BinaryObjectsToFilesNodeDialog getDialog(final BinaryObjectsToFilesNodeConfig nodeConfig,
        final int dataTableInputIndex) {
        return new BinaryObjectsToFilesNodeDialog(nodeConfig, dataTableInputIndex);
    }
    private static final String NODE_NAME = "Binary Objects to Files";
    private static final String NODE_ICON = "binaryobjectstofiles16x16.png";
    private static final String SHORT_DESCRIPTION = """
            Writes all binary objects from a specific column to a directory.
            """;
    private static final String FULL_DESCRIPTION = """
            This node takes all binary objects in a certain column of the input table and writes them, each as a
                separate file, into a directory. It will append the paths of the written files to the input table as
                well as the corresponding write status (created, unmodified, overwritten). <p> <i>This node can access a
                variety of different</i> <a
                href="https://docs.knime.com/2021-06/analytics_platform_file_handling_guide/index.html#analytics-platform-file-systems"><i>file
                systems.</i></a> <i>More information about file handling in KNIME can be found in the official</i> <a
                href="https://docs.knime.com/latest/analytics_platform_file_handling_guide/index.html"><i>File Handling
                Guide.</i></a> </p>
            """;
    private static final List<PortDescription> INPUT_PORTS = List.of(
            dynamicPort(CONNECTION_INPUT_PORT_GRP_NAME, "File System Connection", "The file system connection."),
            fixedPort("Input Table", "Table that contains binary objects.")
    );
    private static final List<PortDescription> OUTPUT_PORTS = List.of(
            fixedPort("Output Table", """
                Input table with an additional path column that contains the paths of the written files, as well as
                another String column which holds the write status (created, unmodified, overwritten). The original
                binary object column is removed from the output if the option to remove is checked.
                """)
    );


    @Override
    public NodeDialogPane createNodeDialogPane(final NodeCreationConfiguration creationConfig) {
        /*
         * We need to override the implementation with parameter, even though we don't use it, because
         * otherwise the {@link AbstractMultiTableWriterNodeFactory} and its superclasses take over and return the full
         * legacy dialog by eventually calling {@link BinaryObjectsToFilesNodeFactory#getDialog}.
         * This should be cleaned up once the other node ({@link ImageWriterTableNodeFactory}) using this abstract factory
         * is migrated.
         */
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, BinaryObjectsToFilesNodeParameters.class);
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
            BinaryObjectsToFilesNodeParameters.class,
            null,
            NodeType.Manipulator,
            List.of(),
            null
        );
    }

    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, BinaryObjectsToFilesNodeParameters.class));
    }

}
