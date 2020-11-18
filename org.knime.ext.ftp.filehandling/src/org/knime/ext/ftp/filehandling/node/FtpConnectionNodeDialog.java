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
 *   2020-07-28 (Vyacheslav Soldatov): created
 */
package org.knime.ext.ftp.filehandling.node;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.base.ui.WorkingDirectoryChooser;

/**
 * FTP Connection node dialog.
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
public class FtpConnectionNodeDialog extends NodeDialogPane {
    private static final String WORKING_DIR_HISTORY_ID = "ftp.workingDir";

    private final FtpConnectionSettingsModel m_settings;

    private AuthenticationDialog m_authPanel;

    private final WorkingDirectoryChooser m_workingDirChooser = new WorkingDirectoryChooser(WORKING_DIR_HISTORY_ID,
            this::createFSConnection);

    private final ChangeListener m_enablenessUpdater = e -> updateEnabledness();

    /**
     * Creates new instance.
     */
    public FtpConnectionNodeDialog() {
        m_settings = new FtpConnectionSettingsModel();

        initFields();

        addTab("Settings", createSettingsPanel());
        addTab("Advanced", createAdvancedPanel());
    }

    private void initFields() {
        m_authPanel = new AuthenticationDialog(m_settings.getAuthenticationSettings(),
                this);
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
        panel.add(createFileSystemSettingsPanel(), gbc);

        gbc.gridy++;
        addVerticalFiller(panel, gbc.gridy, 1);

        return panel;
    }

    private Component createFileSystemSettingsPanel() {
        final JPanel panel = new JPanel();
        final BoxLayout parentLayout = new BoxLayout(panel, BoxLayout.Y_AXIS);
        panel.setLayout(parentLayout);
        panel.setBorder(createTitledBorder("File System settings"));

        panel.add(m_workingDirChooser);
        return panel;
    }

    private Component createAuthenticationSettingsPanel() {
        final JPanel panel = new JPanel();
        panel.setBorder(createTitledBorder("Authentication settings"));
        panel.add(m_authPanel);
        return panel;
    }

    private Component createConnectionSettingsPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(createTitledBorder("Connection settings"));

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 0, 5);
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Host:"), gbc);

        gbc.gridx++;
        panel.add(new DialogComponentString(m_settings.getHostModel(), "", false, 45).getComponentPanel(), gbc);

        gbc.gridx++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        panel.add(Box.createHorizontalGlue(), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(new JLabel("Port: "), gbc);

        gbc.gridx++;
        gbc.insets = new Insets(0, 0, 0, 5);
        panel.add(new DialogComponentNumber(m_settings.getPortModel(), "", 1).getComponentPanel(), gbc);

        gbc.gridx++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        panel.add(Box.createHorizontalGlue(), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(new JLabel("Use FTPS: "), gbc);

        gbc.gridx++;
        gbc.insets = new Insets(0, 0, 0, 5);
        panel.add(new DialogComponentBoolean(m_settings.getUseFTPSModel(), "").getComponentPanel(), gbc);

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

    private FSConnection createFSConnection() throws IOException {
        try {
            final FtpConnectionSettingsModel settings = m_settings.createClone();
            return FtpConnectionNodeModel.createConnection(settings, getCredentialsProvider());
        } catch (IOException e) {
            throw e;
        } catch (InvalidSettingsException e) {
            // wrap to I/O exception
            throw new IOException("Failed to create node settings", e);
        }
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
        panel.add(createOtherSettingsPanel(), gbc);

        gbc.gridy++;
        addVerticalFiller(panel, gbc.gridy, 1);

        return panel;
    }

    private Component createOtherSettingsPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());

        panel.setBorder(createTitledBorder("Other settings"));

        addGbcRow(panel, 1, //
                "Time zone offset from GMT (minutes)  :", //
                new DialogComponentNumber(m_settings.getTimeZoneOffsetModel(), "", 1));

        return panel;
    }

    private Component createAdvancedConnectionSettingsPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());

        panel.setBorder(createTitledBorder("Connection settings"));

        addGbcRow(panel, 0, //
                "Connection timeout (seconds):", //
                new DialogComponentNumber(m_settings.getConnectionTimeoutModel(), "", 1));
        addGbcRow(panel, 1, //
                "Read timeout (seconds):", //
                new DialogComponentNumber(m_settings.getReadTimeoutModel(), "", 1));
        addGbcRow(panel, 2, "Minimum FTP connections:", //
                new DialogComponentNumber(m_settings.getMinConnectionsModel(), "", 1));
        addGbcRow(panel, 3, "Maximum FTP connections:", //
                new DialogComponentNumber(m_settings.getMaxConnectionsModel(), "", 1));
        addGbcRow(panel, 4, "Use HTTP Proxy:", new DialogComponentBoolean(m_settings.getUseProxyModel(), ""));

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

    private static void addGbcRow(final JPanel panel, final int row, final String label, final DialogComponent comp) {
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.insets = new Insets(0, 5, 0, 5);
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel(label), gbc);

        gbc.gridx++;
        panel.add(comp.getComponentPanel(), gbc);

        gbc.gridx++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        panel.add(Box.createHorizontalGlue(), gbc);
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        preSettingsSave();
        m_settings.validate();

        m_settings.saveSettingsForDialog(settings);
        m_authPanel.saveSettingsTo(settings.addNodeSettings(FtpConnectionSettingsModel.KEY_AUTH));
    }

    private void preSettingsSave() {
        m_settings.getWorkingDirectoryModel().setStringValue(m_workingDirChooser.getSelectedWorkingDirectory());
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO input, final PortObjectSpec[] specs)
            throws NotConfigurableException {
        m_settings.getUseFTPSModel().removeChangeListener(m_enablenessUpdater);

        try {
            m_authPanel.loadSettingsFrom(input.getNodeSettings(FtpConnectionSettingsModel.KEY_AUTH), specs);
            m_settings.loadSettingsForDialog(input);
        } catch (final InvalidSettingsException e) { // NOSONAR can be ignored
        }

        m_settings.getUseFTPSModel().addChangeListener(m_enablenessUpdater);
    }

    private void updateEnabledness() {
        boolean isFtpsUsed = m_settings.getUseFTPSModel().getBooleanValue();

        SettingsModelBoolean proxyModel = m_settings.getUseProxyModel();
        if (isFtpsUsed) {
            if (!proxyModel.isEnabled()) {
                // this is the fix
                // if proxy model is already disabled the model will
                // not notify its change listeners therefore will
                // not update enableness state of check box
                // therefore the state is temporary set to enabled before switch
                // it back to disabled
                proxyModel.setEnabled(true);
            }
            proxyModel.setBooleanValue(false);
        }
        proxyModel.setEnabled(!isFtpsUsed);
    }

    @Override
    public void onOpen() {
        m_workingDirChooser.setSelectedWorkingDirectory(m_settings.getWorkingDirectory());
        m_authPanel.onOpen();
        updateEnabledness();
    }

    @Override
    public void onClose() {
        m_workingDirChooser.onClose();
    }
}
