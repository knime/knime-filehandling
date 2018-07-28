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
 * ---------------------------------------------------------------------
 *
 * History
 *   Oct 9, 2017 (ferry.abt): created
 */
package org.knime.base.filehandling.remote.connectioninformation.node;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.knime.base.filehandling.NodeUtils;
import org.knime.base.filehandling.remote.connectioninformation.node.ConnectionInformationConfiguration.ProxyConfiguration;
import org.knime.core.util.KnimeEncryption;

/**
 * Panel to configure proxies for the Remote File Handling Nodes.
 *
 * @author Ferry Abt, KNIME GmbH, Konstanz
 */
class ProxyPanel extends JPanel {
    private static final long serialVersionUID = 1131321798685469208L;

    private final ConnectionInformationNodeDialog m_dialog;

    private final JCheckBox m_useProxyChecker;

    private final JLabel m_hostLabel;

    private final JTextField m_hostTextField;

    private final JLabel m_portLabel;

    private final SpinnerModel m_portModel;

    private final JSpinner m_portSpinner;

    private final JCheckBox m_authChecker;

    private final JCheckBox m_useWorkflowCredChecker;

    private final JComboBox<String> m_workflowCredCombo;

    private final JLabel m_userLabel;

    private final JTextField m_userTextField;

    private final JLabel m_passwordLabel;

    private final JPasswordField m_passwordField;

    private final JPanel m_workflowCredentialsPanel;

    private final UpdateListener enabledUpdatelistener;

    private final String m_protocol;

    /**
     * Creates a settings tab for proxy settings for a {@link ConnectionInformationNodeDialog}
     *
     * @param dialog this tab belongs to
     * @param protocol for which this node is for (e.g. HTTP, HTTPS, etc)
     */
    ProxyPanel(final ConnectionInformationNodeDialog dialog, final String protocol) {
        m_dialog = dialog;
        m_protocol = protocol;
        m_useProxyChecker = new JCheckBox("Use " + m_protocol + " Proxy");
        m_hostLabel = new JLabel("Host:");
        m_hostTextField = new JTextField();
        m_portLabel = new JLabel("Port:");
        m_portModel = new SpinnerNumberModel(21, 0, 65535, 1);
        m_portSpinner = new JSpinner(m_portModel);
        m_authChecker = new JCheckBox("User Authentication");
        m_useWorkflowCredChecker = new JCheckBox();
        m_workflowCredCombo = new JComboBox<>();
        m_userLabel = new JLabel("User:");
        m_userTextField = new JTextField();
        m_passwordLabel = new JLabel("Password:");
        m_passwordField = new JPasswordField();

        m_workflowCredentialsPanel = new JPanel(new GridBagLayout());

        enabledUpdatelistener = new UpdateListener();
        m_useProxyChecker.addActionListener(enabledUpdatelistener);
        m_authChecker.addActionListener(enabledUpdatelistener);
        m_useWorkflowCredChecker.addActionListener(enabledUpdatelistener);
        enabledUpdatelistener.actionPerformed(null);

        initLayout();
    }

    private void initLayout() {
        final GridBagConstraints gbc = new GridBagConstraints();

        //credentials panel
        NodeUtils.resetGBC(gbc);
        m_workflowCredentialsPanel.add(m_useWorkflowCredChecker, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        m_workflowCredentialsPanel.add(m_workflowCredCombo, gbc);
        m_workflowCredentialsPanel.setBorder(new TitledBorder(new EtchedBorder(), "Workflow credentials"));

        // ftp proxy panel
        final JPanel proxyPanel = new JPanel(new GridBagLayout());
        proxyPanel.setBorder(new TitledBorder(new EtchedBorder(), m_protocol + " Proxy"));
        NodeUtils.resetGBC(gbc);
        proxyPanel.add(m_useProxyChecker, gbc);
        gbc.gridy++;
        proxyPanel.add(m_hostLabel, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        proxyPanel.add(m_hostTextField, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0;
        proxyPanel.add(m_portLabel, gbc);
        gbc.gridx++;
        gbc.fill = GridBagConstraints.NONE;
        proxyPanel.add(m_portSpinner, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        proxyPanel.add(m_authChecker, gbc);
        gbc.gridy++;
        gbc.fill = GridBagConstraints.BOTH;
        proxyPanel.add(m_workflowCredentialsPanel, gbc);
        gbc.gridwidth = 1;
        gbc.gridy++;
        proxyPanel.add(m_userLabel, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        proxyPanel.add(m_userTextField, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0;
        proxyPanel.add(m_passwordLabel, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        proxyPanel.add(m_passwordField, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.weighty = 1;
        //empty panel to eat up space
        proxyPanel.add(new JPanel(), gbc);

        // Outer Panel
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(proxyPanel);
    }

    /**
     * loads the proxy defined by the {@code ProxyConfiguration} into the dialog
     *
     * @param config defining a proxy
     */
    void load(final ProxyConfiguration config) {
        m_useProxyChecker.setSelected(config.isUseProxy());
        m_hostTextField.setText(config.getProxyHost());
        m_portSpinner.getModel().setValue(config.getProxyPort());
        m_authChecker.setSelected(config.isUserAuth());
        final Collection<String> credentials = m_dialog.getCredentialsNames();
        m_workflowCredCombo.removeAllItems();
        for (final String credential : credentials) {
            m_workflowCredCombo.addItem(credential);
        }
        if (credentials.size() > 0) {
            m_useWorkflowCredChecker.setSelected(config.isUseWorkflowCredentials());
        }
        m_workflowCredCombo.setSelectedItem(config.getProxyWorkflowCredentials());
        m_userTextField.setText(config.getProxyUser());
        try {
            m_passwordField.setText(KnimeEncryption.decrypt(config.getPassword()));
        } catch (final Exception e) {
            //Leave empty
        }
        enabledUpdatelistener.actionPerformed(null);
    }

    /**
     * @param config to store the settings entered by the user into
     */
    void createConfig(final ProxyConfiguration config) {
        config.setUseProxy(m_useProxyChecker.isSelected());
        config.setProxyHost(m_hostTextField.getText());
        config.setProxyPort((int)m_portSpinner.getModel().getValue());
        config.setUserAuth(m_authChecker.isSelected());
        config.setUseWorkflowCredentials(m_useWorkflowCredChecker.isSelected());
        config.setProxyWorkflowCredentials((String)m_workflowCredCombo.getSelectedItem());
        config.setProxyUser(m_userTextField.getText());
        try {
            if (m_passwordField.getPassword().length > 0) {
                config.setPassword(KnimeEncryption.encrypt(m_passwordField.getPassword()));
            }
        } catch (final Exception e) {
            //Do not change password
        }
    }

    /**
     * {@link ActionListener} that sets the enabled state of the different settings depending on the checkboxes
     *
     * @author ferry.abt
     */
    private class UpdateListener implements ActionListener {

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionPerformed(final ActionEvent e) {
            boolean status = m_useProxyChecker.isSelected();
            m_hostLabel.setEnabled(status);
            m_hostTextField.setEnabled(status);
            m_portLabel.setEnabled(status);
            m_portSpinner.setEnabled(status);
            m_authChecker.setEnabled(status);

            if (status) {
                status = status & m_authChecker.isSelected();
            }
            m_workflowCredentialsPanel.setEnabled(status);
            m_useWorkflowCredChecker.setEnabled(status);

            boolean useCredentials = m_useWorkflowCredChecker.isSelected();
            m_workflowCredCombo.setEnabled(status && useCredentials);
            m_userLabel.setEnabled(status && !useCredentials);
            m_userTextField.setEnabled(status && !useCredentials);
            m_passwordLabel.setEnabled(status && !useCredentials);
            m_passwordField.setEnabled(status && !useCredentials);
        }

    }
}
