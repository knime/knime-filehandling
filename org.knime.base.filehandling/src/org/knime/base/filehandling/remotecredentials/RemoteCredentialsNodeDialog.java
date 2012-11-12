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

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
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

import org.knime.core.node.FlowVariableModelButton;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
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
 * @author Patrick Winter, University of Konstanz
 */
public class RemoteCredentialsNodeDialog extends NodeDialogPane {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(RemoteCredentialsNodeDialog.class);

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

    private boolean m_passwordChanged;

    private JLabel m_keyfileLabel;

    private FilesHistoryPanel m_keyfile;

    private FlowVariableModelButton m_keyfilefvm;

    private JCheckBox m_usecertificate;

    private JLabel m_certificateLabel;

    private FilesHistoryPanel m_certificate;

    private FlowVariableModelButton m_certificatefvm;

    /**
     * New pane for configuring the node dialog.
     * 
     * @param protocol The protocol of this credentials dialog
     */
    public RemoteCredentialsNodeDialog(final Protocol protocol) {
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
        m_authpassword =
                new JRadioButton(AuthenticationMethod.PASSWORD.getName());
        m_authpassword
                .setActionCommand(AuthenticationMethod.PASSWORD.getName());
        m_authpassword.addChangeListener(new UpdateListener());
        m_authkeyfile =
                new JRadioButton(AuthenticationMethod.KEYFILE.getName());
        m_authkeyfile.setActionCommand(AuthenticationMethod.KEYFILE.getName());
        m_authkeyfile.addChangeListener(new UpdateListener());
        m_authmethod = new ButtonGroup();
        m_authmethod.add(m_authnone);
        m_authmethod.add(m_authpassword);
        m_authmethod.add(m_authkeyfile);
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
        m_keyfilefvm =
                new FlowVariableModelButton(createFlowVariableModel("keyfile",
                        FlowVariable.Type.STRING));
        m_keyfilefvm.getFlowVariableModel().addChangeListener(
                new UpdateListener());
        // Certificate
        m_usecertificate = new JCheckBox("Use custom certificate");
        m_usecertificate.addChangeListener(new UpdateListener());
        m_certificateLabel = new JLabel("Certificate:");
        m_certificate = new FilesHistoryPanel("certificateHistory", false);
        m_certificate.setSelectMode(JFileChooser.FILES_ONLY);
        m_certificatefvm =
                new FlowVariableModelButton(createFlowVariableModel(
                        "certificate", FlowVariable.Type.STRING));
        m_certificatefvm.getFlowVariableModel().addChangeListener(
                new UpdateListener());
        addTab("Options", initLayout());
    }

    private JPanel initLayout() {
        GridBagConstraints gbc = new GridBagConstraints();
        // Host
        JLabel hostLabel = new JLabel("Host:");
        // Port
        JLabel portLabel = new JLabel("Port:");
        // Authentication panel
        resetGBC(gbc);
        JPanel authenticationPanel = new JPanel(new GridBagLayout());
        authenticationPanel.setBorder(new TitledBorder(new EtchedBorder(),
                "Authentication"));
        authenticationPanel.add(m_authnone);
        gbc.gridx++;
        authenticationPanel.add(m_authpassword);
        if (m_protocol.hasKeyfileSupport()) {
            gbc.gridx++;
            authenticationPanel.add(m_authkeyfile);
        }
        // Keyfile panel
        JPanel keyfilePanel = null;
        if (m_protocol.hasKeyfileSupport()) {
            resetGBC(gbc);
            keyfilePanel = new JPanel(new GridBagLayout());
            gbc.weightx = 1;
            gbc.insets = new Insets(0, 0, 0, 5);
            keyfilePanel.add(m_keyfile, gbc);
            gbc.weightx = 0;
            gbc.gridx++;
            gbc.insets = new Insets(0, 0, 0, 0);
            keyfilePanel.add(m_keyfilefvm, gbc);
        }
        // Certificate panel
        JPanel certificatePanel = null;
        if (m_protocol.hasCertificateSupport()) {
            resetGBC(gbc);
            certificatePanel = new JPanel(new GridBagLayout());
            gbc.weightx = 1;
            gbc.insets = new Insets(0, 0, 0, 5);
            certificatePanel.add(m_certificate, gbc);
            gbc.weightx = 0;
            gbc.gridx++;
            gbc.insets = new Insets(0, 0, 0, 0);
            certificatePanel.add(m_certificatefvm, gbc);
        }
        // Outer Panel
        resetGBC(gbc);
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
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        gbc.gridy++;
        gbc.fill = GridBagConstraints.BOTH;
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
        if (m_protocol.hasCertificateSupport()) {
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
        }
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
        boolean usePassword =
                m_authmethod.getSelection() != null ? m_authmethod
                        .getSelection().getActionCommand()
                        .equals(AuthenticationMethod.PASSWORD.getName())
                        : false;
        boolean useKeyfile =
                m_authmethod.getSelection() != null ? m_authmethod
                        .getSelection().getActionCommand()
                        .equals(AuthenticationMethod.KEYFILE.getName()) : false;
        m_userLabel.setEnabled(usePassword || useKeyfile);
        m_user.setEnabled(usePassword || useKeyfile);
        m_passwordLabel.setEnabled(usePassword);
        m_password.setEnabled(usePassword);
        if (m_protocol.hasKeyfileSupport()) {
            boolean keyfileReplacement =
                    m_keyfilefvm.getFlowVariableModel()
                            .isVariableReplacementEnabled();
            m_keyfile.setEnabled(!keyfileReplacement);
            m_keyfileLabel.setVisible(useKeyfile);
            m_keyfile.setVisible(useKeyfile);
            m_keyfilefvm.setVisible(useKeyfile);
            m_passwordLabel.setVisible(!useKeyfile);
            m_password.setVisible(!useKeyfile);
        }
        if (m_protocol.hasCertificateSupport()) {
            boolean usecertificate = m_usecertificate.isSelected();
            boolean certificateReplacement =
                    m_certificatefvm.getFlowVariableModel()
                            .isVariableReplacementEnabled();
            m_certificateLabel.setEnabled(usecertificate);
            m_certificate.setEnabled(usecertificate && !certificateReplacement);
            m_certificatefvm.setEnabled(usecertificate);
        }
    }

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
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        RemoteCredentialsConfiguration config =
                new RemoteCredentialsConfiguration(m_protocol);
        config.setUser(m_user.getText());
        config.setHost(m_host.getText());
        config.setPort((Integer)m_port.getValue());
        config.setAuthenticationmethod(m_authmethod.getSelection()
                .getActionCommand());
        if (m_passwordChanged) {
            try {
                config.setPassword(KnimeEncryption.encrypt(m_password
                        .getPassword()));
            } catch (Throwable t) {
                LOGGER.error(
                        "Could not encrypt password, reason: " + t.getMessage(),
                        t);
            }
        } else {
            config.setPassword(new String(m_password.getPassword()));
        }
        if (m_protocol.hasKeyfileSupport()) {
            config.setKeyfile(m_keyfile.getSelectedFile());
        }
        if (m_protocol.hasCertificateSupport()) {
            config.setUsecertificate(m_usecertificate.isSelected());
            config.setCertificate(m_certificate.getSelectedFile());
        }
        config.save(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        RemoteCredentialsConfiguration config =
                new RemoteCredentialsConfiguration(m_protocol);
        config.loadInDialog(settings);
        m_user.setText(config.getUser());
        m_host.setText(config.getHost());
        m_port.setValue(config.getPort());
        String authmethod = config.getAuthenticationmethod();
        if (authmethod.equals(AuthenticationMethod.NONE.getName())) {
            m_authmethod.setSelected(m_authnone.getModel(), true);
        } else if (authmethod.equals(AuthenticationMethod.PASSWORD.getName())) {
            m_authmethod.setSelected(m_authpassword.getModel(), true);
        } else if (authmethod.equals(AuthenticationMethod.KEYFILE.getName())) {
            m_authmethod.setSelected(m_authkeyfile.getModel(), true);
        }
        m_passwordChanged = false;
        m_password.setText(config.getPassword());
        if (m_protocol.hasKeyfileSupport()) {
            m_keyfile.setSelectedFile(config.getKeyfile());
        }
        if (m_protocol.hasCertificateSupport()) {
            m_usecertificate.setSelected(config.getUsecertificate());
            m_certificate.setSelectedFile(config.getCertificate());
        }
        updateEnabledState();
    }

}
