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
 *   2023-02-20 (Alexander Bondaletov, Redfield SE): created
 */
package org.knime.ext.box.filehandling.node;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.ext.box.filehandling.fs.BoxFSConnection;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.base.auth.AuthPanel;
import org.knime.filehandling.core.connections.base.auth.AuthSettings;
import org.knime.filehandling.core.connections.base.auth.SingleSecretAuthProviderPanel;
import org.knime.filehandling.core.connections.base.ui.WorkingDirectoryChooser;

/**
 * The node dialog for the Box Connector.
 *
 * @author Alexander Bondaletov, Redfield SE
 */
public class BoxConnectorNodeDialog extends NodeDialogPane {

    private final BoxConnectorSettings m_settings;
    private final AuthPanel m_authPanel;

    private final WorkingDirectoryChooser m_workingDirChooser;
    private final ChangeListener m_workdirListener;

    /**
     * Creates new instance.
     */
    public BoxConnectorNodeDialog() {
        m_settings = new BoxConnectorSettings();

        m_workingDirChooser = new WorkingDirectoryChooser("box.workingDir", this::createFSConnection);
        m_workdirListener = e -> m_settings.getWorkingDirectoryModel()
                .setStringValue(m_workingDirChooser.getSelectedWorkingDirectory());

        var authSettings = m_settings.getAuthSettings();
        m_authPanel = new AuthPanel(authSettings, //
                Arrays.asList( //
                        new SingleSecretAuthProviderPanel("Developer Token",
                                authSettings.getSettingsForAuthType(BoxConnectorSettings.DEVELOPER_TOKEN_AUTH),
                                this::getCredentialsProvider)));

        addTab("Settings", createSettingsPanel());
        addTab("Advanced", createAdvancedPanel());
    }

    private FSConnection createFSConnection() throws IOException {
        try {
            m_settings.validate();
        } catch (InvalidSettingsException e) {
            throw new IOException(e.getMessage(), e);
        }

        final var credentialsProvider = getCredentialsProvider();
        final var config = m_settings.createFSConnectionConfig(credentialsProvider);
        return new BoxFSConnection(config);
    }

    private Component createSettingsPanel() {
        var box = new Box(BoxLayout.Y_AXIS);
        box.add(createAuthPanel());
        box.add(createFilesystemPanel());
        return box;
    }

    private JComponent createAuthPanel() {
        m_authPanel.setBorder(BorderFactory.createTitledBorder("Authentication"));
        return m_authPanel;
    }

    private JComponent createFilesystemPanel() {
        var panel = new JPanel(new GridBagLayout());
        var c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(0, 10, 0, 0);
        panel.add(m_workingDirChooser, c);

        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.gridy += 1;
        panel.add(Box.createVerticalGlue(), c);

        panel.setBorder(BorderFactory.createTitledBorder("File system settings"));
        return panel;
    }

    private JComponent createAdvancedPanel() {
        final var panel = new JPanel(new GridBagLayout());

        final var gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.insets = new Insets(5, 0, 10, 5);
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(createAdvancedConnectionSettingsPanel(), gbc);

        gbc.gridy++;

        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.weighty = 1;
        panel.add(Box.createVerticalGlue(), gbc);

        return panel;
    }

    private Component createAdvancedConnectionSettingsPanel() {
        final var panel = new JPanel(new GridBagLayout());

        panel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.RAISED), "Connection settings"));

        final var gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 5, 0, 5);
        gbc.anchor = GridBagConstraints.LINE_START;

        panel.add(new JLabel("Connection timeout (seconds):"), gbc);
        gbc.gridx++;
        panel.add(new DialogComponentNumber(m_settings.getConnectionTimeoutModel(), "", 1).getComponentPanel(), gbc);

        gbc.gridx++;
        gbc.weightx = 1;
        panel.add(Box.createHorizontalGlue(), gbc);

        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.weightx = 0;

        panel.add(new JLabel("Read timeout (seconds):"), gbc);
        gbc.gridx++;
        panel.add(new DialogComponentNumber(m_settings.getReadTimeoutModel(), "", 1).getComponentPanel(), gbc);

        gbc.gridx++;
        gbc.weightx = 1;
        panel.add(Box.createHorizontalGlue(), gbc);

        return panel;
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        validateBeforeSaving();
        m_settings.saveForDialog(settings);
        m_authPanel.saveSettingsTo(settings.addNodeSettings(AuthSettings.KEY_AUTH));
    }

    private void validateBeforeSaving() {
        m_workingDirChooser.addCurrentSelectionToHistory();
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
            throws NotConfigurableException {

        try {
            m_authPanel.loadSettingsFrom(settings.getNodeSettings(AuthSettings.KEY_AUTH), specs);
            m_settings.loadSettingsForDialog(settings);
        } catch (InvalidSettingsException | NotConfigurableException ex) { // NOSONAR
            // ignore
        }

        settingsLoaded();
    }

    private void settingsLoaded() {
        m_workingDirChooser.setSelectedWorkingDirectory(m_settings.getWorkingDirectoryModel().getStringValue());
        m_workingDirChooser.addListener(m_workdirListener);
    }

    @Override
    public void onClose() {
        m_workingDirChooser.removeListener(m_workdirListener);
        m_workingDirChooser.onClose();
    }
}
