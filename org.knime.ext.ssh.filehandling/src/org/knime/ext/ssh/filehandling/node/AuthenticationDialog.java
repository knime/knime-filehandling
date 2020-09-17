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

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentFlowVariableNameSelection2;
import org.knime.core.node.defaultnodesettings.DialogComponentPasswordField;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.VariableType.CredentialsType;
import org.knime.ext.ssh.filehandling.node.SshAuthenticationSettingsModel.AuthType;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.DialogComponentReaderFileChooser;

/**
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
@SuppressWarnings("serial")
public class AuthenticationDialog extends JPanel {

    private static final String KEY_FILE_HISTORY_ID = "sshFs.keyFile";

    private static final int LEFT_INSET = 23;

    private final SshAuthenticationSettingsModel m_settings; // NOSONAR we are not using serialization

    private final ButtonGroup m_authTypeGroup = new ButtonGroup();

    private JRadioButton m_typeUserPwd;
    private JRadioButton m_typeCredential;
    private JRadioButton m_typeKeyFile;

    private JLabel m_usernameLabel = new JLabel("Username:");
    private JLabel m_passwordLabel = new JLabel("Password:");
    private JLabel m_keyFileUsernameLabel = new JLabel("Username:");
    private JLabel m_keyFileLabel = new JLabel("Key file:");

    private DialogComponentFlowVariableNameSelection2 m_credentialFlowVarChooser; // NOSONAR not using serialization
    private DialogComponentString m_username; // NOSONAR not using serialization
    private DialogComponentPasswordField m_password; // NOSONAR not using serialization
    private DialogComponentString m_keyUsername; // NOSONAR not using serialization
    private DialogComponentBoolean m_useKeyPassphrase; // NOSONAR not using serialization
    private DialogComponentPasswordField m_keyPassphrase; // NOSONAR not using serialization
    private DialogComponentReaderFileChooser m_keyFileChooser; // NOSONAR not using serialization


    private static JRadioButton createAuthTypeButton(
            final AuthType type,
            final ButtonGroup group) {

        final JRadioButton button = new JRadioButton(type.getText());
        button.setToolTipText(type.getToolTip());
        if (type.isDefault()) {
            button.setSelected(true);
        }
        group.add(button);
        return button;
    }

    /**
     * Constructor.
     *
     * @param settings
     *            SSH authentication settings.
     * @param flowVariables
     *            flow variables model.
     * @param parentDialog
     *            The parent dialog pane (required by flow variable dialog component
     *            to list all flow variables).
     */
    public AuthenticationDialog(final SshAuthenticationSettingsModel settings, final FlowVariableModel flowVariables,
            final NodeDialogPane parentDialog) {
        super(new BorderLayout());
        m_settings = settings;

        initFields(flowVariables, parentDialog);
        wireEvents();
        initLayout();
    }

    private void initLayout() {
        final BoxLayout authBoxLayout = new BoxLayout(this, BoxLayout.Y_AXIS);
        this.setLayout(authBoxLayout);

        add(createCredentialPanel());
        add(createUserPwdPanel());
        add(createKeyFilePanel());
    }

    private void initFields(final FlowVariableModel flowVariables,
            final NodeDialogPane parentDialog) {

        m_credentialFlowVarChooser = new DialogComponentFlowVariableNameSelection2(m_settings.getCredentialModel(), "",
                () -> parentDialog.getAvailableFlowVariables(CredentialsType.INSTANCE));

        m_username = new DialogComponentString(m_settings.getUserModel(), "", false, 45);
        m_password = new DialogComponentPasswordField(m_settings.getPasswordModel(), "", 45);

        m_keyUsername = new DialogComponentString(m_settings.getKeyUserModel(), "", false, 40);
        m_useKeyPassphrase = new DialogComponentBoolean(m_settings.getUseKeyPassphraseModel(),
                "Key passphrase:");
        m_keyPassphrase = new DialogComponentPasswordField(m_settings.getKeyPassphraseModel(), "", 40);
        m_keyFileChooser = new DialogComponentReaderFileChooser(
                m_settings.getKeyFileModel(),
                KEY_FILE_HISTORY_ID,
                flowVariables);


        m_typeUserPwd = createAuthTypeButton(AuthType.USER_PWD, m_authTypeGroup);
        m_typeCredential = createAuthTypeButton(AuthType.CREDENTIALS, m_authTypeGroup);
        m_typeKeyFile = createAuthTypeButton(AuthType.KEY_FILE, m_authTypeGroup);
    }

    private void wireEvents() {
        m_typeCredential.addActionListener(makeListener(m_typeCredential, AuthType.CREDENTIALS));
        m_typeUserPwd.addActionListener(makeListener(m_typeUserPwd, AuthType.USER_PWD));
        m_typeKeyFile.addActionListener(makeListener(m_typeKeyFile, AuthType.KEY_FILE));

    }

    private ActionListener makeListener(final JRadioButton radioButton, final AuthType authType) {
        return e -> {
            if (radioButton.isSelected()) {
                m_settings.setAuthType(authType);
                updateComponentsEnablement();
            }
        };
    }

    private JPanel createUserPwdPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 0, 0, 5);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 1;
        gbc.gridwidth = 2;
        panel.add(m_typeUserPwd, gbc);

        gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(0, LEFT_INSET, 0, 5);
        panel.add(m_usernameLabel, gbc);

        gbc.gridx++;
        gbc.insets = new Insets(0, 0, 0, 5);
        panel.add(m_username.getComponentPanel(), gbc);

        gbc.gridx++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        panel.add(Box.createHorizontalGlue(), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(0, LEFT_INSET, 0, 5);
        panel.add(m_passwordLabel, gbc);

        gbc.gridx++;
        gbc.insets = new Insets(0, 0, 0, 5);
        panel.add(m_password.getComponentPanel(), gbc);

        gbc.gridx++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        panel.add(Box.createHorizontalGlue(), gbc);

        return panel;
    }

    private JPanel createKeyFilePanel() {
        final JPanel panel = new JPanel(new GridBagLayout());

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 0, 0, 5);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 1;
        gbc.gridwidth = 3;
        panel.add(m_typeKeyFile, gbc);

        gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(0, LEFT_INSET, 0, 5);
        panel.add(m_keyFileUsernameLabel, gbc);

        gbc.gridx++;
        gbc.insets = new Insets(0, 0, 0, 5);
        panel.add(m_keyUsername.getComponentPanel(), gbc);

        gbc.gridx++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        panel.add(Box.createHorizontalGlue(), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(0, LEFT_INSET - 8, 0, 5); // -8 aligns the dialog component with the labels
        panel.add(m_useKeyPassphrase.getComponentPanel(), gbc);

        gbc.gridx++;
        gbc.insets = new Insets(0, 0, 0, 5);
        panel.add(m_keyPassphrase.getComponentPanel(), gbc);

        gbc.gridx++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        panel.add(Box.createHorizontalGlue(), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(0, LEFT_INSET, 0, 5);
        panel.add(m_keyFileLabel, gbc);

        gbc.gridx++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        panel.add(Box.createHorizontalGlue(), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.gridwidth = 3;
        gbc.insets = new Insets(0, LEFT_INSET + 10, 0, 5);
        panel.add(m_keyFileChooser.getComponentPanel(), gbc);

        return panel;
    }


    private JPanel createCredentialPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(10, 0, 0, 5);
        panel.add(m_typeCredential, gbc);

        gbc.gridx++;
        panel.add(m_credentialFlowVarChooser.getComponentPanel(), gbc);

        gbc.gridx++;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(Box.createHorizontalGlue(), gbc);

        return panel;
    }

    private void updateComponentsEnablement() {
        final AuthType authType = m_settings.getAuthType();

        m_usernameLabel.setEnabled(authType == AuthType.USER_PWD);
        m_passwordLabel.setEnabled(authType == AuthType.USER_PWD);

        m_keyFileUsernameLabel.setEnabled(authType == AuthType.KEY_FILE);
        m_keyFileLabel.setEnabled(authType == AuthType.KEY_FILE);
    }

    /**
     * Initializes from settings.
     *
     **/
    public void onOpen() {
        // init from settings
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

        updateComponentsEnablement();
    }

    /**
     * @param settings
     * @param specs
     * @throws NotConfigurableException
     */
    public void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
            throws NotConfigurableException {
        m_credentialFlowVarChooser.loadSettingsFrom(settings, specs);

        m_username.loadSettingsFrom(settings, specs);
        m_password.loadSettingsFrom(settings, specs);

        m_keyUsername.loadSettingsFrom(settings, specs);
        m_useKeyPassphrase.loadSettingsFrom(settings, specs);
        m_keyPassphrase.loadSettingsFrom(settings, specs);
        m_keyFileChooser.loadSettingsFrom(settings, specs);
    }
}
