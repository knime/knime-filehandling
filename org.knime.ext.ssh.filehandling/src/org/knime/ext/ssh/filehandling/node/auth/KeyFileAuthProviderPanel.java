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
 *   2020-12-01 (Bjoern Lohrmann, KNIME GmbH): created
 */
package org.knime.ext.ssh.filehandling.node.auth;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.Box;
import javax.swing.JLabel;

import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentPasswordField;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.connections.base.auth.AuthProviderPanel;
import org.knime.filehandling.core.data.location.variable.FSLocationVariableType;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.DialogComponentReaderFileChooser;

/**
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
@SuppressWarnings("serial")
public class KeyFileAuthProviderPanel extends AuthProviderPanel<KeyFileAuthProviderSettings> {

    private static final String KEY_FILE_HISTORY_ID = "sshFs.keyFile";

    private static final int LEFT_INSET = 23;

    private JLabel m_keyFileUsernameLabel = new JLabel("Username:");
    private JLabel m_keyFileLabel = new JLabel("Key file:");
    private DialogComponentString m_keyUsername; // NOSONAR not using serialization
    private DialogComponentBoolean m_useKeyPassphrase; // NOSONAR not using serialization
    private DialogComponentPasswordField m_keyPassphrase; // NOSONAR not using serialization
    private DialogComponentReaderFileChooser m_keyFileChooser; // NOSONAR not using serialization

    /**
     * Creates a new instance.
     *
     * @param settings
     * @param nodeDialog
     */
    public KeyFileAuthProviderPanel(final KeyFileAuthProviderSettings settings, final NodeDialogPane nodeDialog) {
        super(new GridBagLayout(), settings);

        final String[] flowVariables = settings.getKeyFileModel().getKeysForFSLocation();
        final FlowVariableModel flowVariableModel = nodeDialog.createFlowVariableModel(flowVariables,
                FSLocationVariableType.INSTANCE);

        m_keyUsername = new DialogComponentString(getSettings().getKeyUserModel(), "", false, 40);
        m_useKeyPassphrase = new DialogComponentBoolean(getSettings().getUseKeyPassphraseModel(), "Key passphrase:");
        m_keyPassphrase = new DialogComponentPasswordField(getSettings().getKeyPassphraseModel(), "", 40);
        m_keyFileChooser = new DialogComponentReaderFileChooser(getSettings().getKeyFileModel(), KEY_FILE_HISTORY_ID,
                flowVariableModel);

        initLayout();
    }

    private void initLayout() {
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, LEFT_INSET, 0, 5);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.NONE;
        add(m_keyFileUsernameLabel, gbc);

        gbc.gridx++;
        gbc.insets = new Insets(0, 0, 0, 5);
        add(m_keyUsername.getComponentPanel(), gbc);

        gbc.gridx++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        add(Box.createHorizontalGlue(), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(0, LEFT_INSET - 8, 0, 5); // -8 aligns the dialog component with the labels
        add(m_useKeyPassphrase.getComponentPanel(), gbc);

        gbc.gridx++;
        gbc.insets = new Insets(0, 0, 0, 5);
        add(m_keyPassphrase.getComponentPanel(), gbc);

        gbc.gridx++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        add(Box.createHorizontalGlue(), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(0, LEFT_INSET, 0, 5);
        add(m_keyFileLabel, gbc);

        gbc.gridx++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        add(Box.createHorizontalGlue(), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.gridwidth = 3;
        gbc.insets = new Insets(0, LEFT_INSET + 10, 0, 5);
        add(m_keyFileChooser.getComponentPanel(), gbc);
    }

    @Override
    protected void updateComponentsEnablement() {
        m_keyFileUsernameLabel.setEnabled(isEnabled());
        m_keyFileLabel.setEnabled(isEnabled());
    }

    @Override
    public void onClose() {
        m_keyFileChooser.onClose();
    }

    @Override
    protected void loadAdditionalSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
            throws NotConfigurableException {
        m_keyFileChooser.loadSettingsFrom(settings, specs);
    }

    @Override
    protected void saveAdditionalSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_keyFileChooser.saveSettingsTo(settings);
    }
}
