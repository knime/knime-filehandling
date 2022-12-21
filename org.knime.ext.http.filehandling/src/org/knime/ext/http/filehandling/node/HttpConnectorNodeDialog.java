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
 *   2020-11-18 (Bjoern Lohrmann): created
 */
package org.knime.ext.http.filehandling.node;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.connections.base.auth.AuthPanel;
import org.knime.filehandling.core.connections.base.auth.AuthSettings;
import org.knime.filehandling.core.connections.base.auth.EmptyAuthProviderPanel;
import org.knime.filehandling.core.connections.base.auth.UserPasswordAuthProviderPanel;

/**
 * HTTP(S) Connector node dialog.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
class HttpConnectorNodeDialog extends NodeDialogPane {

    private final HttpConnectorNodeSettings m_settings;

    private AuthPanel m_authPanel;

    /**
     * Creates new instance.
     */
    HttpConnectorNodeDialog() {
        m_settings = new HttpConnectorNodeSettings();

        initFields();

        addTab("Settings", createSettingsPanel());
        addTab("Advanced", createAdvancedPanel());
    }

    private void initFields() {
        final AuthSettings authSettings = m_settings.getAuthenticationSettings();
        m_authPanel = new AuthPanel(authSettings, //
                Arrays.asList( //
                        new UserPasswordAuthProviderPanel(
                                authSettings.getSettingsForAuthType(HttpAuth.BASIC), this::getCredentialsProvider), //
                        new EmptyAuthProviderPanel(authSettings.getSettingsForAuthType(HttpAuth.NONE))));
    }

    private JComponent createSettingsPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.insets = new Insets(5, 0, 10, 5);
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(createConnectionSettingsPanel(), gbc);

        gbc.gridy++;
        panel.add(createAuthenticationSettingsPanel(), gbc);

        gbc.gridy++;
        addVerticalFiller(panel, gbc.gridy, 1);

        return panel;
    }

    private Component createSslSettingsPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(createTitledBorder("SSL/TLS"));

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 0, 5);
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new DialogComponentBoolean(m_settings.getSslIgnoreHostnameMismatchesModel(),
                "Ignore hostname mismatches").getComponentPanel(), gbc);

        gbc.gridx++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        panel.add(Box.createHorizontalGlue(), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(new DialogComponentBoolean(m_settings.getSslTrustAllCertificatesModel(), "Trust all certificates ")
                .getComponentPanel(), gbc);

        gbc.gridx++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        panel.add(Box.createHorizontalGlue(), gbc);

        return panel;
    }

    private Component createAuthenticationSettingsPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(createTitledBorder("Authentication"));

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(m_authPanel, gbc);

        return panel;
    }

    private Component createConnectionSettingsPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(createTitledBorder("Connection"));

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 0, 5);
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("URL:"), gbc);

        gbc.gridx++;
        panel.add(new DialogComponentString(m_settings.getUrlModel(), "", false, 45).getComponentPanel(), gbc);

        gbc.gridx++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        panel.add(Box.createHorizontalGlue(), gbc);

        return panel;
    }

    /**
     * @param title
     *            border title.
     * @return titled border.
     */
    private static Border createTitledBorder(final String title) {
        return new TitledBorder(new EtchedBorder(EtchedBorder.RAISED), title);
    }

    private JComponent createAdvancedPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.insets = new Insets(5, 0, 10, 5);
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(createAdvancedConnectionSettingsPanel(), gbc);

        gbc.gridy++;
        panel.add(createSslSettingsPanel(), gbc);

        gbc.gridy++;
        addVerticalFiller(panel, gbc.gridy, 1);

        return panel;
    }

    private Component createAdvancedConnectionSettingsPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(createTitledBorder("Connection"));

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 5, 0, 5);
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Connection timeout (seconds):"), gbc);

        gbc.gridx++;
        panel.add(new DialogComponentNumber(m_settings.getConnectionTimeoutModel(), "", 1).getComponentPanel(), gbc);

        gbc.gridx++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        panel.add(Box.createHorizontalGlue(), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(new JLabel("Read timeout (seconds):"), gbc);

        gbc.gridx++;
        panel.add(new DialogComponentNumber(m_settings.getReadTimeoutModel(), "", 1).getComponentPanel(), gbc);

        gbc.gridx++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        panel.add(Box.createHorizontalGlue(), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(0, 0, 0, 5);
        gbc.weightx = 0;
        panel.add(new DialogComponentBoolean(m_settings.getFollowRedirectsModel(), "Follow redirects")
                .getComponentPanel(), gbc);

        gbc.gridx++;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        panel.add(Box.createHorizontalGlue(), gbc);

        return panel;
    }

    private static void addVerticalFiller(final JPanel panel, final int row, final int columnCount) {
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.gridwidth = columnCount;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.weighty = 1;
        panel.add(Box.createVerticalGlue(), gbc);
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_settings.validate();

        m_settings.saveSettingsForDialog(settings);
        m_authPanel.saveSettingsTo(settings.addNodeSettings(AuthSettings.KEY_AUTH));
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO input, final PortObjectSpec[] specs)
            throws NotConfigurableException {

        try {
            m_authPanel.loadSettingsFrom(input.getNodeSettings(AuthSettings.KEY_AUTH), specs);
            m_settings.loadSettingsForDialog(input);
        } catch (final InvalidSettingsException e) { // NOSONAR can be ignored
        }

    }

    @Override
    public void onClose() {
        m_authPanel.onClose();
    }
}
