/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Oct 9, 2017 (ferry): created
 */
package org.knime.base.filehandling.remote.connectioninformation.node;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

class ProxyPanel extends JPanel {
    private static final long serialVersionUID = 1131321798685469208L;

    private final JCheckBox useFTPProxyChecker;

    private final JLabel hostLabel;

    private final JTextField host;

    private final JLabel portLabel;

    private final SpinnerModel portModel;

    private final JSpinner port;

    private final JCheckBox authChecker;

    private final JCheckBox useWorkflowCredChecker;

    private final JComboBox workflowCredentials;

    private final JLabel userLabel;

    private final JTextField user;

    private final JLabel passwordLabel;

    private final JPasswordField password;

    private final JPanel workflowCredentialsPanel;

    /**
     *
     */
    ProxyPanel() {
        useFTPProxyChecker = new JCheckBox("Use FTP Proxy");
        hostLabel = new JLabel("Host:");
        host = new JTextField();
        portLabel = new JLabel("Port:");
        portModel = new SpinnerNumberModel(21, 0, 65535, 1);
        port = new JSpinner(portModel);
        authChecker = new JCheckBox("User Authentication");
        useWorkflowCredChecker = new JCheckBox();
        workflowCredentials = new JComboBox();
        userLabel = new JLabel("User:");
        user = new JTextField();
        passwordLabel = new JLabel("Password:");
        password = new JPasswordField();

        workflowCredentialsPanel = new JPanel(new GridBagLayout());

        UpdateListener listener = new UpdateListener();
        useFTPProxyChecker.addActionListener(listener);
        authChecker.addActionListener(listener);
        useWorkflowCredChecker.addActionListener(listener);
        listener.actionPerformed(null);
        initLayout();
    }

    /**
     */
    private void initLayout() {
        final GridBagConstraints gbc = new GridBagConstraints();

        //credentials panel
        NodeUtils.resetGBC(gbc);
        workflowCredentialsPanel.add(useWorkflowCredChecker, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        workflowCredentialsPanel.add(workflowCredentials, gbc);
        workflowCredentialsPanel.setBorder(new TitledBorder(new EtchedBorder(), "Workflow credentials"));

        // FTP proxy panel
        final JPanel ftpProxyPanel = new JPanel(new GridBagLayout());
        ftpProxyPanel.setBorder(new TitledBorder(new EtchedBorder(), "FTP Proxy"));
        NodeUtils.resetGBC(gbc);
        ftpProxyPanel.add(useFTPProxyChecker, gbc);
        gbc.gridy++;
        ftpProxyPanel.add(hostLabel, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        ftpProxyPanel.add(host, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0;
        ftpProxyPanel.add(portLabel, gbc);
        gbc.gridx++;
        gbc.fill = GridBagConstraints.NONE;
        ftpProxyPanel.add(port, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        ftpProxyPanel.add(authChecker, gbc);
        gbc.gridy++;
        gbc.fill = GridBagConstraints.BOTH;
        ftpProxyPanel.add(workflowCredentialsPanel, gbc);
        gbc.gridwidth = 1;
        gbc.gridy++;
        ftpProxyPanel.add(userLabel, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        ftpProxyPanel.add(user, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0;
        ftpProxyPanel.add(passwordLabel, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        ftpProxyPanel.add(password, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.weighty = 1;
        ftpProxyPanel.add(new JPanel(), gbc);

        // Outer Panel
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(ftpProxyPanel);
    }

    void load(final NodeSettingsRO settings) throws NotConfigurableException {
        try {
            final NodeSettingsRO proxySettings = settings.getNodeSettings("proxy");
            useFTPProxyChecker.setSelected(proxySettings.getBoolean("useFTPProxy", false));
            host.setText(proxySettings.getString("ftpHost",""));
            port.getModel().setValue(proxySettings.getInt("ftpPort", 21));
            authChecker.setSelected(proxySettings.getBoolean("ftpUseAuth",false));
            useWorkflowCredChecker.setSelected(proxySettings.getBoolean("ftpUseWFCred",false));
            //TODO get credentials
            user.setText(proxySettings.getString("ftpUser", ""));
            //TODO get password
        } catch (InvalidSettingsException e) {
            // TODO defaults
        }
    }

    void save(final NodeSettingsWO settings) throws InvalidSettingsException {
        //TODO save everything
    }

    private class UpdateListener implements ActionListener {

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionPerformed(final ActionEvent e) {
            boolean status = useFTPProxyChecker.isSelected();
            boolean useAuthentication = authChecker.isSelected();
            boolean useCredentials = useWorkflowCredChecker.isSelected();

            hostLabel.setEnabled(status);
            host.setEnabled(status);
            portLabel.setEnabled(status);
            port.setEnabled(status);
            authChecker.setEnabled(status);
            workflowCredentialsPanel.setEnabled(status && useAuthentication);
            useWorkflowCredChecker.setEnabled(status && useAuthentication);
            workflowCredentials.setEnabled(status && useAuthentication && useCredentials);
            userLabel.setEnabled(status && useAuthentication && !useCredentials);
            user.setEnabled(status && useAuthentication && !useCredentials);
            passwordLabel.setEnabled(status && useAuthentication && !useCredentials);
            password.setEnabled(status && useAuthentication && !useCredentials);
        }

    }
}
