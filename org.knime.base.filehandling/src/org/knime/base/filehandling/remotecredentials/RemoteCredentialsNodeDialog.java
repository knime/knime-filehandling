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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

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

    private JComboBox<String> m_protocol;

    private JTextField m_user;

    private JTextField m_host;

    private JSpinner m_port;

    private JPasswordField m_password;

    private JLabel m_keyfileLabel;

    private FilesHistoryPanel m_keyfile;

    private FlowVariableModelButton m_keyfilefvm;

    private JCheckBox m_usecertificate;

    private JLabel m_certificateLabel;

    private FilesHistoryPanel m_certificate;

    private FlowVariableModelButton m_certificatefvm;

    /**
     * New pane for configuring the node dialog.
     */
    public RemoteCredentialsNodeDialog() {
        addTab("Options", initLayout());
    }

    private JPanel initLayout() {
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
        // Certificate
        m_usecertificate = new JCheckBox("Use custom certificate");
        m_usecertificate.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                updateEnabledState();
            }
        });
        m_certificateLabel = new JLabel("Certificate:");
        m_certificate = new FilesHistoryPanel("certificateHistory", false);
        m_certificate.setSelectMode(JFileChooser.FILES_ONLY);
        m_certificatefvm =
                new FlowVariableModelButton(createFlowVariableModel(
                        "certificate", FlowVariable.Type.STRING));
        m_certificatefvm.getFlowVariableModel().addChangeListener(
                new ChangeListener() {
                    @Override
                    public void stateChanged(final ChangeEvent e) {
                        updateEnabledState();
                    }
                });
        // Certificate panel
        resetGBC(gbc);
        JPanel certificatePanel = new JPanel(new GridBagLayout());
        gbc.weightx = 1;
        gbc.insets = new Insets(0, 0, 0, 5);
        certificatePanel.add(m_certificate, gbc);
        gbc.weightx = 0;
        gbc.gridx++;
        gbc.insets = new Insets(0, 0, 0, 0);
        certificatePanel.add(m_certificatefvm, gbc);
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
        gbc.gridy++;
        gbc.gridwidth = 2;
        panel.add(m_usecertificate, gbc);
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.gridy++;
        panel.add(m_certificateLabel, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        panel.add(certificatePanel, gbc);
        return panel;
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
        boolean keyfile = protocol.hasKeyfileSupport();
        boolean keyfileReplacement =
                m_keyfilefvm.getFlowVariableModel()
                        .isVariableReplacementEnabled();
        m_keyfileLabel.setEnabled(keyfile);
        m_keyfile.setEnabled(keyfile && !keyfileReplacement);
        m_keyfilefvm.setEnabled(keyfile);
        boolean certificate = protocol.hasCertificateSupport();
        boolean usecertificate = m_usecertificate.isSelected();
        boolean certificateReplacement =
                m_certificatefvm.getFlowVariableModel()
                        .isVariableReplacementEnabled();
        m_usecertificate.setEnabled(certificate);
        m_certificateLabel.setEnabled(certificate && usecertificate);
        m_certificate.setEnabled(certificate && usecertificate
                && !certificateReplacement);
        m_certificatefvm.setEnabled(certificate && usecertificate);
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
        RemoteCredentialsConfiguration config =
                new RemoteCredentialsConfiguration();
        config.setProtocol((String)m_protocol.getSelectedItem());
        config.setUser(m_user.getText());
        config.setHost(m_host.getText());
        config.setPort((Integer)m_port.getValue());
        // TODO authmethod
        config.setPassword(new String(m_password.getPassword()));
        config.setKeyfile(m_keyfile.getSelectedFile());
        config.setUsecertificate(m_usecertificate.isSelected());
        config.setCertificate(m_certificate.getSelectedFile());
        config.save(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        RemoteCredentialsConfiguration config =
                new RemoteCredentialsConfiguration();
        config.loadInDialog(settings);
        m_protocol.setSelectedItem(config.getProtocol());
        m_user.setText(config.getUser());
        m_host.setText(config.getHost());
        m_port.setValue(config.getPort());
        // TODO authmethod
        m_password.setText(config.getPassword());
        m_keyfile.setSelectedFile(config.getKeyfile());
        m_usecertificate.setSelected(config.getUsecertificate());
        m_certificate.setSelectedFile(config.getCertificate());
        updateEnabledState();
    }

}
