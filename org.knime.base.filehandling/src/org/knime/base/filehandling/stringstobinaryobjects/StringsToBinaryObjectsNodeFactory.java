/*
 * ------------------------------------------------------------------------
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
 *
 * History
 *   Sep 5, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.stringstobinaryobjects;

import static org.knime.node.impl.description.PortDescription.fixedPort;

import java.util.List;
import java.util.Map;

import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.knime.core.webui.node.dialog.NodeDialog;
import org.knime.core.webui.node.dialog.NodeDialogFactory;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultKaiNodeInterface;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterface;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterfaceFactory;
import org.knime.node.impl.description.DefaultNodeDescriptionUtil;
import org.knime.node.impl.description.PortDescription;

/**
 * <code>NodeFactory</code> for node.
 *
 *
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 */
public class StringsToBinaryObjectsNodeFactory extends NodeFactory<StringsToBinaryObjectsNodeModel>
    implements NodeDialogFactory, KaiNodeInterfaceFactory{

    /**
     * {@inheritDoc}
     */
    @Override
    public StringsToBinaryObjectsNodeModel createNodeModel() {
        return new StringsToBinaryObjectsNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<StringsToBinaryObjectsNodeModel> createNodeView(final int viewIndex,
            final StringsToBinaryObjectsNodeModel nodeModel) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        return true;
    }

    private static final String NODE_NAME = "Strings to Binary Objects";
    private static final String NODE_ICON = "stringstobinaryobjects16x16.png";
    private static final String SHORT_DESCRIPTION = "Transforms string data into binary objects using a specified encoding.";
    private static final String FULL_DESCRIPTION = """
            The Strings to Binary Objects node converts string data from a selected column into binary objects based on a user-defined encoding.

            This transformation is essential for scenarios where binary representations of data are required,
            such as when preparing data for machine learning algorithms or when interfacing with systems that require binary input.

            Users can specify the encoding to ensure that the conversion process accurately reflects the intended character set,
            and they can choose whether to replace the original column or append the new binary object column to the existing data table.
            """;
    private static final List<PortDescription> INPUT_PORTS = List.of(
        fixedPort("Data Table", "The input data table containing the string column to be converted.")
    );
    private static final List<PortDescription> OUTPUT_PORTS = List.of(
        fixedPort("Data Table", "The output data table containing the binary object column resulting from the conversion.")
    );

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new StringsToBinaryObjectsNodeDialog();
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
            StringsToBinaryObjectsNodeParameters.class, //
            List.of(), //
            NodeFactory.NodeType.Source, //
            List.of(), //
            null //
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, StringsToBinaryObjectsNodeParameters.class));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, StringsToBinaryObjectsNodeParameters.class);
    }

}
