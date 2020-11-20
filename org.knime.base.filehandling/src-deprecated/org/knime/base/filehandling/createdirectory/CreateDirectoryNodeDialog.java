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
 *   Oct 30, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.createdirectory;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.filehandling.NodeUtils;
import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformation;
import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformationPortObjectSpec;
import org.knime.base.filehandling.remote.dialog.RemoteFileChooser;
import org.knime.base.filehandling.remote.dialog.RemoteFileChooserPanel;
import org.knime.core.node.FlowVariableModelButton;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.FilesHistoryPanel;
import org.knime.core.node.workflow.FlowVariable;

/**
 * <code>NodeDialog</code> for the node.
 *
 *
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 */
@Deprecated
public class CreateDirectoryNodeDialog extends NodeDialogPane {

    private ConnectionInformation m_connectionInformation;

    private JLabel m_info;

    private RemoteFileChooserPanel m_target;

    private FilesHistoryPanel m_localtarget;

    private FlowVariableModelButton m_targetfvm;

    private JPanel m_localtargetPanel;

    private JLabel m_nameLabel;

    private JTextField m_name;

    private JCheckBox m_abortifexists;

    private JLabel m_variablenameLabel;

    private JTextField m_variablename;

    /**
     * New pane for configuring the node dialog.
     */
    public CreateDirectoryNodeDialog() {
        // Info
        m_info = new JLabel();
        // Target (remote location)
        m_targetfvm = new FlowVariableModelButton(createFlowVariableModel("target", FlowVariable.Type.STRING));
        m_target =
                new RemoteFileChooserPanel(getPanel(), "Location", true, "targetFolderHistory",
                        RemoteFileChooser.SELECT_DIR, m_targetfvm.getFlowVariableModel(), m_connectionInformation);
        // Local target (local location)
        m_localtarget = new FilesHistoryPanel("localTargetFolderHistory", false);
        m_localtarget.setSelectMode(JFileChooser.DIRECTORIES_ONLY);
        m_targetfvm.getFlowVariableModel().addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                boolean replacement = m_targetfvm.getFlowVariableModel().isVariableReplacementEnabled();
                m_localtarget.setEnabled(!replacement);
            }
        });
        // Name
        m_nameLabel = new JLabel("Name:");
        m_name = new JTextField();
        // Abort if exists
        m_abortifexists = new JCheckBox("Abort if directory already exists");
        // Variable name
        m_variablenameLabel = new JLabel("Variable name:");
        m_variablename = new JTextField();
        // Set layout
        addTab("Options", initLayout());
    }

    /**
     * Create and fill panel for the dialog.
     *
     *
     * @return The panel for the dialog
     */
    private JPanel initLayout() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        // Filter
        NodeUtils.resetGBC(gbc);
        JPanel namePanel = new JPanel(new GridBagLayout());
        gbc.weightx = 0;
        namePanel.add(m_nameLabel, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        namePanel.add(m_name, gbc);
        // Target
        NodeUtils.resetGBC(gbc);
        m_localtargetPanel = new JPanel(new GridBagLayout());
        gbc.weightx = 1;
        m_localtargetPanel.add(m_localtarget, gbc);
        gbc.weightx = 0;
        gbc.gridx++;
        gbc.insets = new Insets(5, 0, 5, 5);
        m_localtargetPanel.add(m_targetfvm, gbc);
        m_localtargetPanel.setBorder(new TitledBorder(new EtchedBorder(), "Location"));
        // Variable name
        NodeUtils.resetGBC(gbc);
        JPanel variablenamePanel = new JPanel(new GridBagLayout());
        gbc.weightx = 0;
        variablenamePanel.add(m_variablenameLabel, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        variablenamePanel.add(m_variablename, gbc);
        // Outer panel
        NodeUtils.resetGBC(gbc);
        gbc.weightx = 1;
        panel.add(m_info, gbc);
        gbc.gridy++;
        panel.add(m_target.getPanel(), gbc);
        gbc.gridy++;
        panel.add(m_localtargetPanel, gbc);
        gbc.gridy++;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(namePanel, gbc);
        gbc.gridy++;
        panel.add(variablenamePanel, gbc);
        gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(m_abortifexists, gbc);
        return panel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
            throws NotConfigurableException {
        // Check if a port object is available
        if (specs[0] != null) {
            ConnectionInformationPortObjectSpec object = (ConnectionInformationPortObjectSpec)specs[0];
            m_connectionInformation = object.getConnectionInformation();
        }
        m_target.setConnectionInformation(m_connectionInformation);
        if (m_connectionInformation != null) {
            m_info.setText("Create on: " + m_connectionInformation.toURI());
        } else {
            m_info.setText("Create on: local machine");
        }
        // Show only one of the location panels
        m_target.getPanel().setVisible(m_connectionInformation != null);
        m_localtargetPanel.setVisible(m_connectionInformation == null);
        // Load configuration
        CreateDirectoryConfiguration config = new CreateDirectoryConfiguration();
        config.load(settings);
        m_target.setSelection(config.getTarget());
        m_localtarget.setSelectedFile(config.getTarget());
        m_name.setText(config.getName());
        m_abortifexists.setSelected(config.getAbortifexists());
        m_variablename.setText(config.getVariablename());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        CreateDirectoryConfiguration config = new CreateDirectoryConfiguration();
        config.setName(m_name.getText());
        config.setAbortifexists(m_abortifexists.isSelected());
        // Set setting only from the correct panel
        if (m_connectionInformation != null) {
            config.setTarget(m_target.getSelection());
        } else {
            config.setTarget(m_localtarget.getSelectedFile());
        }
        if (m_variablename.getText().isEmpty()) {
            throw new InvalidSettingsException("Variable name missing");
        }
        config.setVariablename(m_variablename.getText());
        config.save(settings);
        m_localtarget.addToHistory();
    }
}
