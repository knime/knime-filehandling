/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 * 
 * History
 *   Oct 30, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.copyfiles;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.EtchedBorder;

import org.knime.base.filehandling.dialogcomponents.ColumnSelection;
import org.knime.base.filehandling.dialogcomponents.CustomButtonGroup;
import org.knime.base.filehandling.dialogcomponents.CustomFileChooser;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.uri.URIDataValue;
import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.workflow.FlowVariable;

/**
 * <code>NodeDialog</code> for the node.
 * 
 * 
 * @author Patrick Winter, University of Konstanz
 */
public class CopyFilesNodeDialogNew extends NodeDialogPane {

    private static final int WEST = GridBagConstraints.WEST;

    // private static final int CENTER = GridBagConstraints.CENTER;

    private static final int NONE = GridBagConstraints.NONE;

    private static final int HORIZONTAL = GridBagConstraints.HORIZONTAL;

    private CustomButtonGroup m_copyormove;

    private ColumnSelection m_sourcecolumn;

    private ColumnSelection m_targetcolumn;

    private CustomFileChooser m_outputdirectory;

    private CustomButtonGroup m_ifexists;

    private JRadioButton m_fromseparatecolumn;

    private JRadioButton m_fromsourcename;

    private ButtonGroup m_filenamehandling;

    /**
     * New pane for configuring the node dialog.
     */
    @SuppressWarnings("unchecked")
    public CopyFilesNodeDialogNew() {
        // Copy or move
        m_copyormove =
                new CustomButtonGroup("", false, CopyOrMove.getAllSettings());
        // Source column
        m_sourcecolumn =
                new ColumnSelection("Source column", URIDataValue.class);
        // Target filenames
        m_fromseparatecolumn = new JRadioButton("Use path from target column");
        m_fromseparatecolumn.setActionCommand(FilenameHandling.FROMCOLUMN
                .getName());
        m_fromseparatecolumn.addActionListener(new FilenamehandlingListener());
        m_fromsourcename =
                new JRadioButton("Use source name and output directory");
        m_fromsourcename
                .setActionCommand(FilenameHandling.SOURCENAME.getName());
        m_fromsourcename.addActionListener(new FilenamehandlingListener());
        m_filenamehandling = new ButtonGroup();
        m_filenamehandling.add(m_fromseparatecolumn);
        m_filenamehandling.add(m_fromsourcename);
        // Target column
        m_targetcolumn =
                new ColumnSelection("Target column", URIDataValue.class);
        // Output directory
        FlowVariableModel outputDirectoryFVM =
                createFlowVariableModel("outputdirectory",
                        FlowVariable.Type.STRING);
        m_outputdirectory =
                new CustomFileChooser("Output directory:", true,
                        "outputdirectoryhistory",
                        CustomFileChooser.DIRECTORIES, outputDirectoryFVM);
        // If exists
        m_ifexists =
                new CustomButtonGroup("If a file exists...", true,
                        OverwritePolicy.getAllSettings());
        // Create panels
        JPanel panel = new JPanel(new GridBagLayout());
        JPanel fromSeparateColumnPanel = new JPanel(new GridBagLayout());
        fromSeparateColumnPanel.add(m_fromseparatecolumn,
                genGBC(0, 0, 1, 1, WEST, NONE));
        fromSeparateColumnPanel.add(m_targetcolumn.getComponent(),
                genGBC(0, 1, 1, 1, WEST, NONE));
        fromSeparateColumnPanel.setBorder(new EtchedBorder());
        JPanel fromSourceNamePanel = new JPanel(new GridBagLayout());
        fromSourceNamePanel.add(m_fromsourcename,
                genGBC(0, 0, 1, 1, WEST, NONE));
        fromSourceNamePanel.add(m_outputdirectory.getComponent(),
                genGBC(0, 1, 1, 1, WEST, NONE));
        fromSourceNamePanel.setBorder(new EtchedBorder());
        // Add components to outer panel
        panel.add(m_copyormove.getComponent(), genGBC(0, 0, 2, 1, WEST, NONE));
        panel.add(m_sourcecolumn.getComponent(), genGBC(0, 1, 1, 1, WEST, NONE));
        panel.add(fromSeparateColumnPanel, genGBC(0, 2, 2, 1, WEST, HORIZONTAL));
        panel.add(fromSourceNamePanel, genGBC(0, 3, 2, 1, WEST, HORIZONTAL));
        panel.add(m_ifexists.getComponent(), genGBC(0, 4, 2, 1, WEST, NONE));
        addTab("Options", panel);
    }

    private void enableFilenamehandlingComponents(final String selection) {
        boolean separateColumn =
                selection.equals(FilenameHandling.FROMCOLUMN.getName());
        m_targetcolumn.setEnabled(separateColumn);
        m_outputdirectory.setEnabled(!separateColumn);
    }

    private class FilenamehandlingListener implements ActionListener {

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionPerformed(final ActionEvent e) {
            enableFilenamehandlingComponents(e.getActionCommand());
        }

    }

    private GridBagConstraints genGBC(final int x, final int y,
            final int width, final int height, final int anchor, final int fill) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = width;
        gbc.gridheight = height;
        gbc.anchor = anchor;
        gbc.fill = fill;
        return gbc;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        m_sourcecolumn.updateColumns(specs[0]);
        m_targetcolumn.updateColumns(specs[0]);
        CopyFilesConfiguration config = new CopyFilesConfiguration();
        config.loadInDialog(settings);
        m_copyormove.setSelection(config.getCopyormove());
        m_sourcecolumn.setSelection(config.getSourcecolumn());
        m_targetcolumn.setSelection(config.getTargetcolumn());
        m_outputdirectory.setSelection(config.getOutputdirectory());
        m_ifexists.setSelection(config.getIfexists());
        String filenamehandling = config.getFilenamehandling();
        if (filenamehandling.equals(m_fromseparatecolumn.getActionCommand())) {
            m_filenamehandling.setSelected(m_fromseparatecolumn.getModel(),
                    true);
        } else if (filenamehandling.equals(m_fromsourcename.getActionCommand())) {
            m_filenamehandling.setSelected(m_fromsourcename.getModel(), true);
        }
        enableFilenamehandlingComponents(filenamehandling);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        CopyFilesConfiguration config = new CopyFilesConfiguration();
        config.setCopyormove(m_copyormove.getSelection());
        config.setSourcecolumn(m_sourcecolumn.getSelection());
        config.setTargetcolumn(m_targetcolumn.getSelection());
        config.setOutputdirectory(m_outputdirectory.getSelection());
        config.setIfexists(m_ifexists.getSelection());
        config.setFilenamehandling(m_filenamehandling.getSelection()
                .getActionCommand());
        config.save(settings);
    }
}
