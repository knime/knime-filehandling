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
package org.knime.base.filehandling.remotecredentials;

import java.awt.Container;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableColumn;

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
 * @author Patrick Winter, University of Konstanz
 */
public class RemoteCredentialsNodeDialog extends NodeDialogPane {

    private static final String ACTION_ADD = "add";

    private static final String ACTION_EDIT = "edit";

    private static final String ACTION_REMOVE = "remove";

    private static final String ACTION_OK = "ok";

    private static final String ACTION_CANCEL = "cancel";

    private JButton m_add;

    private JButton m_edit;

    private JButton m_remove;

    private JComboBox<String> m_protocol;

    private JTextField m_user;

    private JTextField m_host;

    private JSpinner m_port;

    private JPasswordField m_password;

    private JLabel m_keyfileLabel;

    private FilesHistoryPanel m_keyfile;

    private FlowVariableModelButton m_keyfilefvm;

    private JDialog m_dialog;

    /**
     * New pane for configuring the node dialog.
     */
    public RemoteCredentialsNodeDialog() {
        addTab("Options", initLayout());
    }

    private JPanel initLayout() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        // Table
        String[] columns = new String[]{"Protocol", "User", "Host", "Port"};
        Object[][] data = {};
        JTable table = new JTable(data, columns);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        TableColumn protocolCol = table.getColumnModel().getColumn(0);
        protocolCol.setMinWidth(75);
        protocolCol.setMaxWidth(75);
        TableColumn userCol = table.getColumnModel().getColumn(1);
        userCol.setMinWidth(100);
        userCol.setMaxWidth(100);
        TableColumn hostCol = table.getColumnModel().getColumn(2);
        hostCol.setPreferredWidth(150);
        hostCol.setMinWidth(150);
        TableColumn portCol = table.getColumnModel().getColumn(3);
        portCol.setMinWidth(75);
        portCol.setMaxWidth(75);
        // Buttons
        m_add = new JButton("Add");
        m_add.setActionCommand(ACTION_ADD);
        m_add.addActionListener(new ModifyButtonListener());
        m_edit = new JButton("Edit");
        m_edit.setActionCommand(ACTION_EDIT);
        m_edit.addActionListener(new ModifyButtonListener());
        m_remove = new JButton("Remove");
        m_remove.setActionCommand(ACTION_REMOVE);
        m_remove.addActionListener(new ModifyButtonListener());
        JPanel buttonPanel = new JPanel(new GridBagLayout());
        buttonPanel.add(m_add, gbc);
        gbc.gridy++;
        buttonPanel.add(m_edit, gbc);
        gbc.gridy++;
        buttonPanel.add(m_remove, gbc);
        gbc.gridy++;
        gbc.weighty = 1;
        buttonPanel.add(new JPanel(), gbc);
        // Hosts panel
        gbc.gridy = 0;
        gbc.weightx = 1;
        panel.add(new JScrollPane(table), gbc);
        gbc.weightx = 0;
        gbc.gridx++;
        panel.add(buttonPanel, gbc);
        panel.setBorder(new TitledBorder(new EtchedBorder(), "Known hosts"));
        // TODO add certificate panel
        return panel;
    }

    private void openHostDialog(final String action) {
        GridBagConstraints gbc = new GridBagConstraints();
        // Protocol
        JLabel protocolLabel = new JLabel("Protocol:");
        String[] protocols = Protocol.getAllProtocols();
        m_protocol = new JComboBox<String>(protocols);
        m_protocol.addActionListener(new ProtocolListener());
        // User
        JLabel userLabel = new JLabel("User:");
        m_user = new JTextField();
        // Host
        JLabel hostLabel = new JLabel("Host:");
        m_host = new JTextField();
        // Port
        JLabel portLabel = new JLabel("Port:");
        SpinnerModel portModel = new SpinnerNumberModel(0, 0, 65535, 1);
        m_port = new JSpinner(portModel);
        // Password
        JLabel passwordLabel = new JLabel("Password:");
        m_password = new JPasswordField();
        // Keyfile
        m_keyfileLabel = new JLabel("Keyfile:");
        m_keyfile = new FilesHistoryPanel("keyfileHistory", false);
        m_keyfile.setSelectMode(JFileChooser.FILES_ONLY);
        m_keyfilefvm =
                new FlowVariableModelButton(createFlowVariableModel("keyfile",
                        FlowVariable.Type.STRING));
        m_keyfilefvm.getFlowVariableModel().addChangeListener(
                new ChangeListener() {
                    @Override
                    public void stateChanged(final ChangeEvent e) {
                        updateEnabledState();
                    }
                });
        // Buttons
        JButton ok = new JButton("   OK   ");
        ok.setActionCommand(ACTION_OK);
        ok.addActionListener(new DialogButtonListener());
        JButton cancel = new JButton("Cancel");
        cancel.setActionCommand(ACTION_CANCEL);
        cancel.addActionListener(new DialogButtonListener());
        // Button panel
        resetGBC(gbc);
        JPanel buttonPanel = new JPanel(new GridBagLayout());
        buttonPanel.add(ok, gbc);
        gbc.gridx++;
        buttonPanel.add(cancel, gbc);
        // Keyfile panel
        resetGBC(gbc);
        JPanel keyfilePanel = new JPanel(new GridBagLayout());
        gbc.weightx = 1;
        gbc.insets = new Insets(0, 0, 0, 5);
        keyfilePanel.add(m_keyfile, gbc);
        gbc.weightx = 0;
        gbc.gridx++;
        gbc.insets = new Insets(0, 0, 0, 0);
        keyfilePanel.add(m_keyfilefvm, gbc);
        // Outer Panel
        resetGBC(gbc);
        JPanel panel = new JPanel(new GridBagLayout());
        panel.add(protocolLabel, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        panel.add(m_protocol, gbc);
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.gridy++;
        panel.add(userLabel, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        panel.add(m_user, gbc);
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.gridy++;
        panel.add(hostLabel, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        panel.add(m_host, gbc);
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.gridy++;
        panel.add(portLabel, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        panel.add(m_port, gbc);
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.gridy++;
        panel.add(passwordLabel, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        panel.add(m_password, gbc);
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.gridy++;
        panel.add(m_keyfileLabel, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        panel.add(keyfilePanel, gbc);
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.gridy++;
        panel.add(buttonPanel, gbc);
        // Open dialog
        Frame frame = null;
        Container container = getPanel().getParent();
        while (container != null) {
            if (container instanceof Frame) {
                frame = (Frame)container;
                break;
            }
            container = container.getParent();
        }
        m_dialog = new JDialog(frame);
        m_dialog.setContentPane(panel);
        m_dialog.setTitle("Known host");
        m_dialog.pack();
        m_dialog.setModalityType(Dialog.DEFAULT_MODALITY_TYPE);
        updateEnabledState();
        m_dialog.setVisible(true);
        m_dialog.dispose();
    }

    private void resetGBC(final GridBagConstraints gbc) {
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridx = 0;
        gbc.gridy = 0;
    }

    private void updateEnabledState() {
        Protocol protocol =
                Protocol.getProtocol((String)m_protocol.getSelectedItem());
        boolean keyfile = protocol.hasKeyfilesupport();
        boolean replacement =
                m_keyfilefvm.getFlowVariableModel()
                        .isVariableReplacementEnabled();
        m_keyfileLabel.setEnabled(keyfile);
        m_keyfile.setEnabled(keyfile && !replacement);
        m_keyfilefvm.setEnabled(keyfile);
    }

    private class ModifyButtonListener implements ActionListener {

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionPerformed(final ActionEvent e) {
            String action = e.getActionCommand();
            if (action.equals(ACTION_ADD)) {
                openHostDialog(ACTION_ADD);
            } else if (action.equals(ACTION_EDIT)) {
                openHostDialog(ACTION_EDIT);
            } else if (action.equals(ACTION_REMOVE)) {
                // TODO remove selected table entry
            }
        }

    }

    private class DialogButtonListener implements ActionListener {

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionPerformed(final ActionEvent e) {
            String action = e.getActionCommand();
            if (action.equals(ACTION_OK)) {
                // TODO persist data from dialog
                m_dialog.setVisible(false);
            } else if (action.equals(ACTION_CANCEL)) {
                m_dialog.setVisible(false);
            }
        }

    }

    private class ProtocolListener implements ActionListener {

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionPerformed(final ActionEvent e) {
            updateEnabledState();
            Protocol protocol =
                    Protocol.getProtocol((String)m_protocol.getSelectedItem());
            m_port.setValue(protocol.getPort());
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        // not used
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        // not used
    }

}
