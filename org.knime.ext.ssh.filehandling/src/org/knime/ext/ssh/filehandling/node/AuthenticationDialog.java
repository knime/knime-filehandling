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
 *   2020-08-07 (Vyacheslav Soldatov): created
 */

package org.knime.ext.ssh.filehandling.node;

import static org.knime.ext.ssh.filehandling.node.SshConnectionNodeDialog.leftLayout;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Collection;

import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;

import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentAuthentication;
import org.knime.core.node.defaultnodesettings.DialogComponentPasswordField;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.ext.ssh.filehandling.node.SshAuthenticationSettingsModel.AuthType;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.DialogComponentReaderFileChooser;

/**
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 * UI is particularly copied from {@link DialogComponentAuthentication}
 */
public class AuthenticationDialog extends JPanel {
    private static final long serialVersionUID = -2566285025646942450L;
    private static final String KEY_FILE_HISTORY_ID = "sshFs.keyFile";

    private static final Insets NEUTRAL_INSET = new Insets(0, 0, 0, 0);

    private static final int LEFT_INSET = 23;

    //auth type chooser
    private final ButtonGroup m_authTypeGroup = new ButtonGroup();

    private final JRadioButton m_typeUserPwd;
    private final JRadioButton m_typeCredential;
    private final JRadioButton m_typeKeyFile;

    //credentials
    private final JComboBox<String> m_credentialField = new JComboBox<>(
            new DefaultComboBoxModel<>());

    //key file
    private final DialogComponentReaderFileChooser m_keyFileChooser;

    //panels
    private final Component m_credentialPanel;
    private final Component m_userPwdPanel;
    private final Component m_keyFilePanel;

    private final SshAuthenticationSettingsModel m_settings;

    private final ItemListener credentialsListener = new ItemListener() {
        @Override
        public void itemStateChanged(final ItemEvent e) {
            Object selected = e.getItem();
            if (e.getStateChange() == ItemEvent.SELECTED && selected != null) {
                m_settings.getCredentialModel().setStringValue((String) selected);
            }
        }
    };

    private JRadioButton createAuthTypeButton(
            final AuthType type,
            final ButtonGroup group) {

        final JRadioButton button = new JRadioButton(type.getText());
        if (type.isDefault()) {
            button.setSelected(true);
        }
        if (type.getToolTip() != null) {
            button.setToolTipText(type.getToolTip());
        }

        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (button.isSelected()) {
                    m_settings.setAuthType(type);
                }
                updateComponentsEnablement();
            }
        });
        group.add(button);
        return button;
    }

    /**
     * @param settings
     *            SSH authentication settings.
     * @param flowVariables
     *            flow variables model.
     */
    public AuthenticationDialog(final SshAuthenticationSettingsModel settings, final FlowVariableModel flowVariables) {
        super();
        m_settings = settings;

        m_keyFileChooser = new DialogComponentReaderFileChooser(
                settings.getKeyFileModel(),
                KEY_FILE_HISTORY_ID,
                flowVariables);

        m_typeUserPwd = createAuthTypeButton(AuthType.USER_PWD, m_authTypeGroup);
        m_typeCredential = createAuthTypeButton(AuthType.CREDENTIALS, m_authTypeGroup);
        m_typeKeyFile = createAuthTypeButton(AuthType.KEY_FILE, m_authTypeGroup);

        m_credentialPanel = createCredentialPanel();
        m_userPwdPanel = createUserPwdPanel();
        m_keyFilePanel = createKeyFilePanel();

        setLayout(new GridBagLayout());

        final GridBagConstraints gbc = new GridBagConstraints();
        // connection
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        add(createRootPanel(), gbc);
    }

    private JPanel createRootPanel() {
        final JPanel authBox = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.insets = NEUTRAL_INSET;

        //credentials
        gbc.gridy++;
        authBox.add(m_typeCredential, gbc);
        gbc.gridy++;
        authBox.add(m_credentialPanel, gbc);

        //user/password
        gbc.gridy++;
        authBox.add(m_typeUserPwd, gbc);
        gbc.gridy++;
        authBox.add(m_userPwdPanel, gbc);

        gbc.gridy++;
        authBox.add(m_typeKeyFile, gbc);
        gbc.gridy++;
        authBox.add(m_keyFilePanel, gbc);

        final Dimension origSize = authBox.getPreferredSize();
        Dimension preferredSize = getMaxDim(m_credentialPanel.getPreferredSize(), m_userPwdPanel.getPreferredSize());
        preferredSize = getMaxDim(preferredSize, m_keyFilePanel.getPreferredSize());

        final Dimension maxSize = getMaxDim(preferredSize, origSize);
        authBox.setMinimumSize(maxSize);
        authBox.setPreferredSize(maxSize);
        return authBox;
    }

    private static Dimension getMaxDim(final Dimension d1, final Dimension d2) {
        return new Dimension(Math.max(d1.width, d2.width), Math.max(d1.height, d2.height));
    }

    private JPanel createUserPwdPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.BOTH;

        gbc.ipadx = 0;
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0;
        gbc.insets = new Insets(0, LEFT_INSET, 0, 5);

        panel.add(new JLabel("Password:", SwingConstants.LEFT), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.insets = NEUTRAL_INSET;

        DialogComponentPasswordField password = new DialogComponentPasswordField(m_settings.getPasswordModel(), "");
        panel.add(leftLayout(password), gbc);
        return panel;
    }

    private JPanel createKeyFilePanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();

        gbc.ipadx = 0;
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(0, LEFT_INSET, 0, 5);
        gbc.weightx = 1;
        panel.add(m_keyFileChooser.getComponentPanel(), gbc);

        gbc.gridy++;
        gbc.weightx = 0;
        panel.add(new JLabel("Key file password:", SwingConstants.LEFT), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.insets = NEUTRAL_INSET;
        DialogComponentPasswordField keyPassword = new DialogComponentPasswordField(
                m_settings.getKeyFilePasswordModel(), "");
        panel.add(keyPassword.getComponentPanel(), gbc);
        return panel;
    }

    private Component createCredentialPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(0, LEFT_INSET, 0, 0);
        panel.add(m_credentialField, gbc);
        return panel;
    }

    private void updateComponentsEnablement() {
        final boolean credentialsAvailable = m_credentialField.getItemCount() > 0;
        m_typeCredential.setEnabled(credentialsAvailable);
        m_credentialPanel.setVisible(m_typeCredential.isSelected());
        m_userPwdPanel.setVisible(m_typeUserPwd.isSelected());
        m_keyFilePanel.setVisible(m_typeKeyFile.isSelected());
    }

    /**
     * Initializes from settings.
     *
     * @param input
     *            input settings.
     * @param specs
     *            port object specifications.
     * @param cp
     *            credentials provider.
     * @throws NotConfigurableException
     */
    public void updateUi(final NodeSettingsRO input, final PortObjectSpec[] specs, final CredentialsProvider cp)
            throws NotConfigurableException {
        m_credentialField.removeItemListener(credentialsListener);

        //each time need to reload credentials because can be changed
        //independently
        final DefaultComboBoxModel<String> credsModel = (DefaultComboBoxModel<String>) m_credentialField.getModel();
        credsModel.removeAllElements();

        final Collection<String> names = cp.listNames();
        if (names != null && !names.isEmpty()) {
            final String credential = m_settings.getCredential();
            boolean presented = false;
            for (final String option : names) {
                credsModel.addElement(option);
                if (option.equals(credential)) {
                    presented = true;
                }
            }

            if (presented) {
                m_credentialField.setSelectedItem(credential);
            }

            m_credentialField.addItemListener(credentialsListener);
        }

        //init from settings
        switch (m_settings.getAuthType()) {
            case CREDENTIALS:
                m_typeCredential.setSelected(true);
                break;
            case KEY_FILE:
                m_typeKeyFile.setSelected(true);
                break;
            case USER_PWD:
                m_typeUserPwd.setSelected(true);
                break;
        }

        m_keyFileChooser.loadSettingsFrom(input, specs);

        updateComponentsEnablement();
    }
}
