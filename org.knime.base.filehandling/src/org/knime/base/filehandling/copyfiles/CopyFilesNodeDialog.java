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
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.uri.URIDataValue;
import org.knime.core.node.FlowVariableModelButton;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;
import org.knime.core.node.util.FilesHistoryPanel;
import org.knime.core.node.workflow.FlowVariable;

/**
 * <code>NodeDialog</code> for the node.
 * 
 * 
 * @author Patrick Winter, University of Konstanz
 */
public class CopyFilesNodeDialog extends NodeDialogPane {

    private ColumnSelectionComboxBox m_sourcecolumn;

    private ColumnSelectionComboxBox m_targetcolumn;

    private FilesHistoryPanel m_outputdirectory;

    private FlowVariableModelButton m_outputdirectoryfvm;

    private JRadioButton m_copy;

    private JRadioButton m_move;

    private ButtonGroup m_copyormove;

    private JRadioButton m_fromseparatecolumn;

    private JRadioButton m_fromsourcename;

    private ButtonGroup m_filenamehandling;

    private JRadioButton m_overwrite;

    private JRadioButton m_abort;

    private ButtonGroup m_ifexists;

    /**
     * New pane for configuring the node dialog.
     */
    @SuppressWarnings("unchecked")
    public CopyFilesNodeDialog() {
        // Copy or move
        m_copy = new JRadioButton(CopyOrMove.COPY.getName());
        m_copy.setActionCommand(CopyOrMove.COPY.getName());
        m_move = new JRadioButton(CopyOrMove.MOVE.getName());
        m_move.setActionCommand(CopyOrMove.MOVE.getName());
        m_copyormove = new ButtonGroup();
        m_copyormove.add(m_copy);
        m_copyormove.add(m_move);
        // Source column
        m_sourcecolumn =
                new ColumnSelectionComboxBox((Border)null, URIDataValue.class);
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
                new ColumnSelectionComboxBox((Border)null, URIDataValue.class);
        // Output directory
        m_outputdirectory = new FilesHistoryPanel("copymoveFiles", false);
        m_outputdirectory.setSelectMode(JFileChooser.DIRECTORIES_ONLY);
        m_outputdirectoryfvm =
                new FlowVariableModelButton(createFlowVariableModel(
                        "outputdirectory", FlowVariable.Type.STRING));
        m_outputdirectoryfvm.getFlowVariableModel().addChangeListener(
                new ChangeListener() {
                    @Override
                    public void stateChanged(final ChangeEvent e) {
                        enableFilenamehandlingComponents();
                    }
                });
        // If exists
        m_overwrite = new JRadioButton(OverwritePolicy.OVERWRITE.getName());
        m_overwrite.setActionCommand(OverwritePolicy.OVERWRITE.getName());
        m_abort = new JRadioButton(OverwritePolicy.ABORT.getName());
        m_abort.setActionCommand(OverwritePolicy.ABORT.getName());
        m_ifexists = new ButtonGroup();
        m_ifexists.add(m_overwrite);
        m_ifexists.add(m_abort);
        // Create layout
        addTab("Options", initLayout());
    }

    private JPanel initLayout() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        // From separate column
        resetGBC(gbc);
        JPanel fromSeparateColumnPanel = new JPanel(new GridBagLayout());
        fromSeparateColumnPanel.setBorder(new EtchedBorder());
        fromSeparateColumnPanel.add(m_fromseparatecolumn, gbc);
        gbc.weightx = 1;
        gbc.gridy++;
        fromSeparateColumnPanel.add(m_targetcolumn, gbc);
        // From source name
        resetGBC(gbc);
        JPanel fromSourceNamePanel = new JPanel(new GridBagLayout());
        fromSourceNamePanel.setBorder(new EtchedBorder());
        fromSourceNamePanel.add(m_fromsourcename, gbc);
        gbc.weightx = 1;
        gbc.gridy++;
        fromSourceNamePanel.add(m_outputdirectory, gbc);
        gbc.weightx = 0;
        gbc.gridx++;
        gbc.insets = new Insets(5, 0, 5, 5);
        fromSourceNamePanel.add(m_outputdirectoryfvm, gbc);
        // If exists
        resetGBC(gbc);
        JPanel ifExistsPanel = new JPanel(new GridBagLayout());
        ifExistsPanel.setBorder(new TitledBorder(new EtchedBorder(),
                "If a file exists..."));
        ifExistsPanel.add(m_overwrite, gbc);
        gbc.gridx++;
        ifExistsPanel.add(m_abort, gbc);
        // Outer panel
        resetGBC(gbc);
        panel.add(m_copy, gbc);
        gbc.gridx++;
        panel.add(m_move, gbc);
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        panel.add(m_sourcecolumn, gbc);
        gbc.gridy++;
        panel.add(fromSeparateColumnPanel, gbc);
        gbc.gridy++;
        panel.add(fromSourceNamePanel, gbc);
        gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(ifExistsPanel, gbc);
        return panel;
    }

    private void resetGBC(final GridBagConstraints gbc) {
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridx = 0;
        gbc.gridy = 0;
    }

    private void enableFilenamehandlingComponents() {
        boolean separateColumn = m_fromseparatecolumn.isSelected();
        boolean replacement =
                m_outputdirectoryfvm.getFlowVariableModel()
                        .isVariableReplacementEnabled();
        m_targetcolumn.setEnabled(separateColumn);
        m_outputdirectory.setEnabled(!separateColumn && !replacement);
        m_outputdirectoryfvm.setEnabled(!separateColumn);
    }

    private class FilenamehandlingListener implements ActionListener {

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionPerformed(final ActionEvent e) {
            enableFilenamehandlingComponents();
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        CopyFilesConfiguration config = new CopyFilesConfiguration();
        config.loadInDialog(settings);
        m_sourcecolumn.update(specs[0], config.getSourcecolumn());
        m_targetcolumn.update(specs[0], config.getTargetcolumn());
        m_outputdirectory.updateHistory();
        m_outputdirectory.setSelectedFile(config.getOutputdirectory());
        String copyormove = config.getCopyormove();
        if (copyormove.equals(m_copy.getActionCommand())) {
            m_copyormove.setSelected(m_copy.getModel(), true);
        } else if (copyormove.equals(m_move.getActionCommand())) {
            m_copyormove.setSelected(m_move.getModel(), true);
        }
        String filenamehandling = config.getFilenamehandling();
        if (filenamehandling.equals(m_fromseparatecolumn.getActionCommand())) {
            m_filenamehandling.setSelected(m_fromseparatecolumn.getModel(),
                    true);
        } else if (filenamehandling.equals(m_fromsourcename.getActionCommand())) {
            m_filenamehandling.setSelected(m_fromsourcename.getModel(), true);
        }
        String ifexists = config.getIfexists();
        if (ifexists.equals(m_overwrite.getActionCommand())) {
            m_ifexists.setSelected(m_overwrite.getModel(), true);
        } else if (ifexists.equals(m_abort.getActionCommand())) {
            m_ifexists.setSelected(m_abort.getModel(), true);
        }
        enableFilenamehandlingComponents();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        CopyFilesConfiguration config = new CopyFilesConfiguration();
        config.setCopyormove(m_copyormove.getSelection().getActionCommand());
        config.setSourcecolumn(m_sourcecolumn.getSelectedColumn());
        config.setTargetcolumn(m_targetcolumn.getSelectedColumn());
        config.setOutputdirectory(m_outputdirectory.getSelectedFile());
        config.setFilenamehandling(m_filenamehandling.getSelection()
                .getActionCommand());
        config.setIfexists(m_ifexists.getSelection().getActionCommand());
        config.save(settings);
    }
}
