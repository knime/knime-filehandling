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
package org.knime.ext.ssh.filehandling.node;

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

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.base.ui.WorkingDirectoryChooser;
import org.knime.filehandling.core.data.location.variable.FSLocationVariableType;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.DialogComponentReaderFileChooser;

/**
 * SSH Connection node dialog.
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
public class SshConnectionNodeDialog extends NodeDialogPane {

    private static final String KNOWN_HOSTS_HISTORY_ID = "ssh.knownHostsFile";

    private static final String WORKING_DIR_HISTORY_ID = "ssh.workingDir";

    private final SshConnectionSettingsModel m_settings;

    private AuthenticationDialog m_authPanel;

    private final WorkingDirectoryChooser m_workingDirChooser = new WorkingDirectoryChooser(WORKING_DIR_HISTORY_ID,
            this::createFSConnection);

    private DialogComponentReaderFileChooser m_knownHostsChooser;

    /**
     * Creates new instance.
     *
     * @param cfg
     *            node creation configuration.
     */
    public SshConnectionNodeDialog(final NodeCreationConfiguration cfg) {
        m_settings = new SshConnectionSettingsModel(cfg);
        // add user name synchronizer

        initFields();

        addTab("Settings", createSettingsPanel());
        addTab("Advanced", createAdvancedPanel());
    }

    private void initFields() {
        m_authPanel = new AuthenticationDialog(m_settings.getAuthenticationSettings(),
                createFlowVariableModel(SshConnectionSettingsModel.getKeyFileLocationPath(), //
                        FSLocationVariableType.INSTANCE), //
                this);

        m_knownHostsChooser = new DialogComponentReaderFileChooser(m_settings.getKnownHostsFileModel(),
                KNOWN_HOSTS_HISTORY_ID, //
                createFlowVariableModel(SshConnectionSettingsModel.getKnownHostLocationPath(), //
                        FSLocationVariableType.INSTANCE));
    }

    private JComponent createSettingsPanel() {
        final JPanel panel = new JPanel();
        final BoxLayout parentLayout = new BoxLayout(panel, BoxLayout.Y_AXIS);
        panel.setLayout(parentLayout);

        panel.add(createConnectionSettingsPanel());
        panel.add(createAuthenticationSettingsPanel());
        panel.add(createFileSystemSettingsPanel());

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
            final SshConnectionSettingsModel settings = m_settings.createClone();
            return SshConnectionNodeModel.createConnection(settings, getCredentialsProvider());
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
        gbc.insets = new Insets(0, 5, 0, 5);
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Connection timeout (seconds)  :"), gbc);

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
        panel.add(new JLabel("Maximum SFTP sessions:"), gbc);

        gbc.gridx++;
        panel.add(new DialogComponentNumber(m_settings.getMaxSessionCountModel(), "", 1).getComponentPanel(), gbc);

        gbc.gridx++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        panel.add(Box.createHorizontalGlue(), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(0, 0, 0, 5);
        gbc.weightx = 0;
        panel.add(new DialogComponentBoolean(m_settings.getUseKnownHostsFileModel(), "Use known hosts file")
                .getComponentPanel(), gbc);

        gbc.gridx++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.gridwidth = 2;
        panel.add(Box.createHorizontalGlue(), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.insets = new Insets(0, 23, 0, 5);
        gbc.gridwidth = 3;
        panel.add(m_knownHostsChooser.getComponentPanel(), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.weighty = 1;
        panel.add(Box.createVerticalGlue(), gbc);

        return panel;
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO output) throws InvalidSettingsException {
        preSettingsSave();
        m_settings.validate();
        m_settings.saveSettingsTo(output);
    }

    private void preSettingsSave() {
        m_settings.getWorkingDirectoryModel().setStringValue(m_workingDirChooser.getSelectedWorkingDirectory());
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO input, final PortObjectSpec[] specs)
            throws NotConfigurableException {

        try {
            m_settings.loadSettingsFrom(input);
        } catch (final InvalidSettingsException e) { // NOSONAR can be ignored
            // can be ignored
        }

        // call load settings for correct initialize file system dialog
        m_authPanel.loadSettingsFrom(input, specs);
        m_knownHostsChooser.loadSettingsFrom(input, specs);
    }


    @Override
    public void onOpen() {
        m_workingDirChooser.setSelectedWorkingDirectory(m_settings.getWorkingDirectory());
        m_authPanel.onOpen();
    }

    @Override
    public void onClose() {
        m_workingDirChooser.onClose();
    }
}
