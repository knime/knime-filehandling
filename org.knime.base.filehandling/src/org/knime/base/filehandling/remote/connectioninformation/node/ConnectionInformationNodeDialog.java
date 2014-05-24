/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 * 
 * History
 *   Oct 30, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.remote.connectioninformation.node;

import java.awt.Container;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.filehandling.NodeUtils;
import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformation;
import org.knime.core.node.FlowVariableModelButton;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.FilesHistoryPanel;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.util.KnimeEncryption;

/**
 * <code>NodeDialog</code> for the node.
 * 
 * 
 * @author Patrick Winter, KNIME.com, Zurich, Switzerland
 */
public class ConnectionInformationNodeDialog extends NodeDialogPane {

    private Protocol m_protocol;

    private JTextField m_host;

    private JSpinner m_port;

    private JRadioButton m_authnone;

    private JRadioButton m_authpassword;

    private JRadioButton m_authkeyfile;

    private ButtonGroup m_authmethod;

    private JLabel m_userLabel;

    private JTextField m_user;

    private JLabel m_passwordLabel;

    private JPasswordField m_password;

    private JLabel m_keyfileLabel;

    private FilesHistoryPanel m_keyfile;

    private FlowVariableModelButton m_keyfilefvm;

    private JCheckBox m_useknownhosts;

    private JLabel m_knownhostsLabel;

    private FilesHistoryPanel m_knownhosts;

    private FlowVariableModelButton m_knownhostsfvm;

    private JButton m_testconnection;

    private JCheckBox m_useworkflowcredentials;

    private JComboBox m_workflowcredentials;

    private JPanel m_workflowcredentialspanel;

    /**
     * New pane for configuring the node dialog.
     * 
     * 
     * @param protocol The protocol of this connection information dialog
     */
    public ConnectionInformationNodeDialog(final Protocol protocol) {
        m_protocol = protocol;
        // Host
        m_host = new JTextField();
        // Port
        SpinnerModel portModel = new SpinnerNumberModel(0, 0, 65535, 1);
        m_port = new JSpinner(portModel);
        // Authentication method
        m_authnone = new JRadioButton(AuthenticationMethod.NONE.getName());
        m_authnone.setActionCommand(AuthenticationMethod.NONE.getName());
        m_authnone.addChangeListener(new UpdateListener());
        m_authpassword = new JRadioButton(AuthenticationMethod.PASSWORD.getName());
        m_authpassword.setActionCommand(AuthenticationMethod.PASSWORD.getName());
        m_authpassword.addChangeListener(new UpdateListener());
        m_authkeyfile = new JRadioButton(AuthenticationMethod.KEYFILE.getName());
        m_authkeyfile.setActionCommand(AuthenticationMethod.KEYFILE.getName());
        m_authkeyfile.addChangeListener(new UpdateListener());
        m_authmethod = new ButtonGroup();
        m_authmethod.add(m_authnone);
        m_authmethod.add(m_authpassword);
        m_authmethod.add(m_authkeyfile);
        // Workflow credentials
        m_useworkflowcredentials = new JCheckBox();
        m_useworkflowcredentials.addChangeListener(new UpdateListener());
        m_workflowcredentials = new JComboBox();
        // User
        m_userLabel = new JLabel("User:");
        m_user = new JTextField();
        // Password
        m_passwordLabel = new JLabel("Password:");
        m_password = new JPasswordField();
        // Keyfile
        m_keyfileLabel = new JLabel("Keyfile:");
        m_keyfile = new FilesHistoryPanel("keyfileHistory", false);
        m_keyfile.setSelectMode(JFileChooser.FILES_ONLY);
        m_keyfilefvm = new FlowVariableModelButton(createFlowVariableModel("keyfile", FlowVariable.Type.STRING));
        m_keyfilefvm.getFlowVariableModel().addChangeListener(new UpdateListener());
        // Known hosts
        m_useknownhosts = new JCheckBox("Use known hosts");
        m_useknownhosts.addChangeListener(new UpdateListener());
        m_knownhostsLabel = new JLabel("Known hosts:");
        m_knownhosts = new FilesHistoryPanel("knownhostsHistory", false);
        m_knownhosts.setSelectMode(JFileChooser.FILES_ONLY);
        m_knownhostsfvm = new FlowVariableModelButton(createFlowVariableModel("knownhosts", FlowVariable.Type.STRING));
        m_knownhostsfvm.getFlowVariableModel().addChangeListener(new UpdateListener());
        // Test connection
        m_testconnection = new JButton("Test connection");
        m_testconnection.addActionListener(new TestConnectionListener());
        addTab("Options", initLayout());
    }

    /**
     * Create and initialize the panel for this dialog.
     * 
     * 
     * @return The initialized panel
     */
    private JPanel initLayout() {
        GridBagConstraints gbc = new GridBagConstraints();
        // Host
        JLabel hostLabel = new JLabel("Host:");
        // Port
        JLabel portLabel = new JLabel("Port:");
        // Authentication panel
        NodeUtils.resetGBC(gbc);
        JPanel authenticationPanel = new JPanel(new GridBagLayout());
        authenticationPanel.setBorder(new TitledBorder(new EtchedBorder(), "Authentication"));
        if (m_protocol.hasAuthNoneSupport()) {
            authenticationPanel.add(m_authnone);
            gbc.gridx++;
        }
        authenticationPanel.add(m_authpassword);
        if (m_protocol.hasKeyfileSupport()) {
            gbc.gridx++;
            authenticationPanel.add(m_authkeyfile);
        }
        // Workflow credentials
        NodeUtils.resetGBC(gbc);
        m_workflowcredentialspanel = new JPanel(new GridBagLayout());
        m_workflowcredentialspanel.add(m_useworkflowcredentials, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        m_workflowcredentialspanel.add(m_workflowcredentials, gbc);
        m_workflowcredentialspanel.setBorder(new TitledBorder(new EtchedBorder(), "Workflow credentials"));
        // Keyfile panel
        JPanel keyfilePanel = null;
        if (m_protocol.hasKeyfileSupport()) {
            NodeUtils.resetGBC(gbc);
            keyfilePanel = new JPanel(new GridBagLayout());
            gbc.weightx = 1;
            gbc.insets = new Insets(0, 0, 0, 5);
            keyfilePanel.add(m_keyfile, gbc);
            gbc.weightx = 0;
            gbc.gridx++;
            gbc.insets = new Insets(0, 0, 0, 0);
            keyfilePanel.add(m_keyfilefvm, gbc);
        }
        // Known hosts panel
        JPanel knownhostsPanel = null;
        if (m_protocol.hasKnownhostsSupport()) {
            NodeUtils.resetGBC(gbc);
            knownhostsPanel = new JPanel(new GridBagLayout());
            gbc.weightx = 1;
            gbc.insets = new Insets(0, 0, 0, 5);
            knownhostsPanel.add(m_knownhosts, gbc);
            gbc.weightx = 0;
            gbc.gridx++;
            gbc.insets = new Insets(0, 0, 0, 0);
            knownhostsPanel.add(m_knownhostsfvm, gbc);
        }
        // Outer Panel
        NodeUtils.resetGBC(gbc);
        JPanel panel = new JPanel(new GridBagLayout());
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
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(authenticationPanel, gbc);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridy++;
        panel.add(m_workflowcredentialspanel, gbc);
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.gridy++;
        panel.add(m_userLabel, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        panel.add(m_user, gbc);
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.gridy++;
        panel.add(m_passwordLabel, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        panel.add(m_password, gbc);
        if (m_protocol.hasKeyfileSupport()) {
            gbc.gridx = 0;
            gbc.weightx = 0;
            gbc.gridy++;
            panel.add(m_keyfileLabel, gbc);
            gbc.gridx++;
            gbc.weightx = 1;
            panel.add(keyfilePanel, gbc);
        }
        if (m_protocol.hasKnownhostsSupport()) {
            gbc.gridx = 0;
            gbc.gridy++;
            gbc.gridwidth = 2;
            panel.add(m_useknownhosts, gbc);
            gbc.gridwidth = 1;
            gbc.weightx = 0;
            gbc.gridy++;
            panel.add(m_knownhostsLabel, gbc);
            gbc.gridx++;
            gbc.weightx = 1;
            panel.add(knownhostsPanel, gbc);
        }
        if (m_protocol.hasTestSupport()) {
            gbc.gridx = 0;
            gbc.gridy++;
            gbc.gridwidth = 2;
            gbc.fill = GridBagConstraints.NONE;
            panel.add(m_testconnection, gbc);
        }
        return panel;
    }

    /**
     * Update the enabled / disabled state of the UI elements.
     */
    private void updateEnabledState() {
        // If a password should be used
        boolean usePassword =
                m_authmethod.getSelection() != null ? m_authmethod.getSelection().getActionCommand()
                        .equals(AuthenticationMethod.PASSWORD.getName()) : false;
        // If a keyfile should be used
        boolean useKeyfile =
                m_authmethod.getSelection() != null ? m_authmethod.getSelection().getActionCommand()
                        .equals(AuthenticationMethod.KEYFILE.getName()) : false;
        // Check if credentials are available
        boolean credentialsAvailable = m_workflowcredentials.getItemCount() > 0;
        // Check if credentials can be selected
        boolean credentialsSelectable = (usePassword || useKeyfile) && credentialsAvailable;
        // Check if the user and password have to be set manually
        boolean manualCredentials = (usePassword || useKeyfile) && !m_useworkflowcredentials.isSelected();
        // Disable workflow credentials if auth method is none or no credentials
        // are available
        m_workflowcredentialspanel.setEnabled(credentialsSelectable);
        m_useworkflowcredentials.setEnabled(credentialsSelectable);
        m_workflowcredentials.setEnabled(credentialsSelectable && m_useworkflowcredentials.isSelected());
        // Disable user if auth method is none
        m_userLabel.setEnabled(manualCredentials);
        m_user.setEnabled(manualCredentials);
        // Password should be enabled if the password or the keyfile get used
        m_passwordLabel.setEnabled(manualCredentials);
        m_password.setEnabled(manualCredentials);
        // Do this only if the protocol supports keyfiles
        if (m_protocol.hasKeyfileSupport()) {
            boolean keyfileReplacement = m_keyfilefvm.getFlowVariableModel().isVariableReplacementEnabled();
            m_keyfileLabel.setEnabled(useKeyfile);
            // Enable file panel only if the flow variable replacement is not
            // active
            m_keyfile.setEnabled(useKeyfile && !keyfileReplacement);
            m_keyfilefvm.setEnabled(useKeyfile);
        }
        // Do this only if the protocol supports known hosts
        if (m_protocol.hasKnownhostsSupport()) {
            boolean useknownhosts = m_useknownhosts.isSelected();
            boolean knownhostsReplacement = m_knownhostsfvm.getFlowVariableModel().isVariableReplacementEnabled();
            m_knownhostsLabel.setEnabled(useknownhosts);
            // Enable file panel only if the flow variable replacement is not
            // active
            m_knownhosts.setEnabled(useknownhosts && !knownhostsReplacement);
            m_knownhostsfvm.setEnabled(useknownhosts);
        }
    }

    /**
     * Listener that updates the states of the UI elements.
     * 
     * 
     * @author Patrick Winter, KNIME.com, Zurich, Switzerland
     */
    private class UpdateListener implements ChangeListener {

        /**
         * {@inheritDoc}
         */
        @Override
        public void stateChanged(final ChangeEvent e) {
            updateEnabledState();
        }

    }

    /**
     * Listener that opens the test connection dialog.
     * 
     * 
     * @author Patrick Winter, KNIME.com, Zurich, Switzerland
     */
    private class TestConnectionListener implements ActionListener {

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionPerformed(final ActionEvent e) {
            // Get frame
            Frame frame = null;
            Container container = getPanel().getParent();
            while (container != null) {
                if (container instanceof Frame) {
                    frame = (Frame)container;
                    break;
                }
                container = container.getParent();
            }
            try {
                // Get connection information to current settings
                ConnectionInformation connectionInformation =
                        createConfig().getConnectionInformation(getCredentialsProvider());
                // Open dialog
                new TestConnectionDialog(connectionInformation).open(frame);
            } catch (InvalidSettingsException e2) {
                JOptionPane.showMessageDialog(new JFrame(), e2.getMessage(), "Invalid settings",
                        JOptionPane.ERROR_MESSAGE);
            }
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        ConnectionInformationConfiguration config = createConfig();
        config.save(settings);
    }

    /**
     * Create a configuration object with the currently set settings.
     * 
     * 
     * @return The configuration object
     * @throws InvalidSettingsException
     */
    private ConnectionInformationConfiguration createConfig() throws InvalidSettingsException {
        ConnectionInformationConfiguration config = new ConnectionInformationConfiguration(m_protocol);
        config.setUseworkflowcredentials(m_useworkflowcredentials.isSelected());
        config.setWorkflowcredentials((String)m_workflowcredentials.getSelectedItem());
        config.setUser(m_user.getText());
        String host = m_host.getText();
        if (!DomainValidator.isValidDomain(host)) {
            throw new InvalidSettingsException("Host invalid. Host must not include scheme, user, port, path or query");
        }
        config.setHost(host);
        config.setPort((Integer)m_port.getValue());
        config.setAuthenticationmethod(m_authmethod.getSelection().getActionCommand());
        try {
            if (m_password.getPassword().length > 0) {
                config.setPassword(KnimeEncryption.encrypt(m_password.getPassword()));
            }
        } catch (Exception e) {
            // Do not change password
        }
        // Only save if the protocol supports keyfiles
        if (m_protocol.hasKeyfileSupport()) {
            config.setKeyfile(m_keyfile.getSelectedFile());
        }
        // Only save if the protocol supports known hosts
        if (m_protocol.hasKnownhostsSupport()) {
            config.setUseknownhosts(m_useknownhosts.isSelected());
            config.setKnownhosts(m_knownhosts.getSelectedFile());
        }
        return config;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
            throws NotConfigurableException {
        Collection<String> credentials = getCredentialsNames();
        m_workflowcredentials.removeAllItems();
        for (String credential : credentials) {
            m_workflowcredentials.addItem(credential);
        }
        ConnectionInformationConfiguration config = new ConnectionInformationConfiguration(m_protocol);
        config.load(settings);
        if (m_workflowcredentials.getItemCount() > 0) {
            m_useworkflowcredentials.setSelected(config.getUseworkflowcredentials());
        }
        m_workflowcredentials.setSelectedItem(config.getWorkflowcredentials());
        m_user.setText(config.getUser());
        m_host.setText(config.getHost());
        m_port.setValue(config.getPort());
        String authmethod = config.getAuthenticationmethod();
        // Select correct auth method
        if (authmethod.equals(AuthenticationMethod.NONE.getName())) {
            m_authmethod.setSelected(m_authnone.getModel(), true);
        } else if (authmethod.equals(AuthenticationMethod.PASSWORD.getName())) {
            m_authmethod.setSelected(m_authpassword.getModel(), true);
        } else if (authmethod.equals(AuthenticationMethod.KEYFILE.getName())) {
            m_authmethod.setSelected(m_authkeyfile.getModel(), true);
        }
        try {
            m_password.setText(KnimeEncryption.decrypt(config.getPassword()));
        } catch (Exception e) {
            // Leave empty
        }
        // Only load if the protocol supports keyfiles
        if (m_protocol.hasKeyfileSupport()) {
            m_keyfile.setSelectedFile(config.getKeyfile());
        }
        // Only load if the protocol supports known hosts
        if (m_protocol.hasKnownhostsSupport()) {
            m_useknownhosts.setSelected(config.getUseknownhosts());
            m_knownhosts.setSelectedFile(config.getKnownhosts());
        }
        updateEnabledState();
    }

}
