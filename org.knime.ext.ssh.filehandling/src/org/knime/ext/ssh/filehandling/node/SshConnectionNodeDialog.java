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

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.IOException;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.ext.ssh.filehandling.node.SshAuthenticationSettingsModel.AuthType;
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
    private static final String KNOWN_HOSTS_HISTORY_ID = "sshFs.knowhHostsFile";

    private final WorkingDirectoryChooser m_workingDirChooser = new WorkingDirectoryChooser("ssh.workingDir",
            this::createFSConnection);

    private AuthenticationDialog m_authDialog;

    private final JCheckBox m_useKnownHostsField = new JCheckBox("Use known hosts");
    private DialogComponentReaderFileChooser m_knownHostsChooser;

    private final SshConnectionSettingsModel m_settings;

    private PortObjectSpec[] m_inputSpecs;

    /**
     * Creates new instance.
     *
     * @param cfg
     *            node creation configuration.
     */
    public SshConnectionNodeDialog(final NodeCreationConfiguration cfg) {
        m_settings = new SshConnectionSettingsModel("justForDialog", cfg);
        // add user name synchronizer

        addTab("Settings", createSettingsPanel());
        addTab("Advanced", createAdvancedPanel());
    }

    private JComponent createSettingsPanel() {
        //components
        final JPanel parent = new JPanel(new BorderLayout());

        //add other components
        final JPanel connections = new JPanel(new BorderLayout());
        connections.setBorder(createTitledBorder("Connection settings"));

        final JPanel conTop = new JPanel(new GridBagLayout());
        connections.add(conTop, BorderLayout.NORTH);

        DialogComponentStringJustified hostComponent = new DialogComponentStringJustified(m_settings.getHostModel(), "");
        hostComponent.getComponentLayout().setHgap(5);
        hostComponent.getComponentPanel().setBorder(new EmptyBorder(5, 5, 5, 0));

        addLabeledComponent(conTop, "Host", hostComponent.getComponentPanel(), 0);
        addLabeledComponent(conTop, "Port", leftLayout(new DialogComponentNumber(m_settings.getPortModel(), "", 1)), 1);

        final JPanel conCenter = new JPanel(new BorderLayout());
        conCenter.setBorder(createTitledBorder("Authentication"));
        connections.add(conCenter, BorderLayout.CENTER);

        //authentication component
        m_authDialog = new AuthenticationDialog(m_settings.getAuthenticationSettings(),
                createFlowVariableModel(
                m_settings.getAuthenticationSettings().getKeyFileLocationPath(), FSLocationVariableType.INSTANCE));
        conCenter.add(m_authDialog, BorderLayout.WEST);

        parent.add(connections, BorderLayout.CENTER);

        //add directory chooser panel
        final JPanel south = new JPanel(new BorderLayout());
        south.setBorder(createTitledBorder("File system settings"));
        south.add(m_workingDirChooser, BorderLayout.CENTER);
        parent.add(south, BorderLayout.SOUTH);

        return parent;
    }

    /**
     * @param title border title.
     * @return titled border.
     */
    private static Border createTitledBorder(final String title) {
        return new TitledBorder(new EtchedBorder(EtchedBorder.RAISED), title);
    }

    private static void addLabeledComponent(final JPanel container, final String label,
            final JPanel component, final int row) {
        // add label
        final GridBagConstraints lc = new GridBagConstraints();
        lc.fill = GridBagConstraints.HORIZONTAL;
        lc.gridx = 0;
        lc.gridy = row;
        lc.weightx = 0.;

        final JLabel l = new JLabel(label);
        l.setHorizontalTextPosition(SwingConstants.RIGHT);
        l.setHorizontalAlignment(SwingConstants.RIGHT);

        final JPanel labelWrapper = new JPanel(new BorderLayout());
        labelWrapper.setBorder(new EmptyBorder(0, 0, 0, 5));
        labelWrapper.add(l, BorderLayout.CENTER);
        container.add(labelWrapper, lc);

        // add component.
        final GridBagConstraints cc = new GridBagConstraints();
        cc.fill = GridBagConstraints.HORIZONTAL;
        cc.gridx = 1;
        cc.gridy = row;
        cc.weightx = 1.;
        container.add(component, cc);
    }

    private FSConnection createFSConnection() throws IOException {
        try {
            return SshConnectionNodeModel.createConnection(createSettings(), m_inputSpecs, getCredentialsProvider());
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(getPanel(), e.getMessage(),
                    "Failed to create SSH connection", JOptionPane.ERROR_MESSAGE));
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            // wrap to I/O exception
            throw new IOException("Failed to create node settings", e);
        }
    }

    private JComponent createAdvancedPanel() {
        //wrap to flow panel for avoid of stretching by height
        final JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEADING));

        //components
        final JPanel parent = new JPanel(new BorderLayout());
        wrapper.add(parent);

        //connection timeout
        final JPanel northPanel = new JPanel(new GridBagLayout());
        final JPanel northPanelWrapper = new JPanel(new FlowLayout(FlowLayout.LEADING));
        northPanelWrapper.add(northPanel);
        parent.add(northPanelWrapper, BorderLayout.NORTH);

        // add component.
        addLabeledComponent(northPanel,
                "Connection timeout",
                leftLayout(new DialogComponentNumber(m_settings.getConnectionTimeoutModel(), "", 1)), 1);
        addLabeledComponent(northPanel, "SFTP sessions",
                leftLayout(new DialogComponentNumber(m_settings.getSessionCountModel(), "", 1)), 2);

        //known hosts
        final JPanel knownHostsPanel = new JPanel(new BorderLayout());
        parent.add(knownHostsPanel, BorderLayout.CENTER);

        knownHostsPanel.add(m_useKnownHostsField, BorderLayout.NORTH);

        //known hosts file chooser
        m_knownHostsChooser = new DialogComponentReaderFileChooser(m_settings.getKnownHostsFileModel(),
                KNOWN_HOSTS_HISTORY_ID, createFlowVariableModel(SshConnectionSettingsModel.getKnownHostLocationPath(),
                        FSLocationVariableType.INSTANCE));
        final JPanel fileChooserPanel = m_knownHostsChooser.getComponentPanel();
        fileChooserPanel.setPreferredSize(fileChooserPanel.getMinimumSize());

        knownHostsPanel.add(fileChooserPanel, BorderLayout.CENTER);

        m_useKnownHostsField.addActionListener(
                event -> knownHostsSelectionChanged(m_useKnownHostsField.isSelected()));
        return wrapper;
    }

    static JPanel leftLayout(final DialogComponent comp) {
        final JPanel pane = comp.getComponentPanel();
        pane.setLayout(new FlowLayout(FlowLayout.LEFT));
        return pane;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO output) throws InvalidSettingsException {
        SshConnectionSettingsModel settings = m_settings.createClone();

        if (!m_useKnownHostsField.isSelected()) {
            settings.getKnownHostsFileModel().setLocation(SshConnectionSettingsModel.NULL_LOCATION);
        }

        SshAuthenticationSettingsModel auth = settings.getAuthenticationSettings();
        if (auth.getAuthType() != AuthType.USER_PWD) {
            auth.getPasswordModel().setStringValue("");
        }
        if (auth.getAuthType() != AuthType.KEY_FILE) {
            auth.getKeyFilePasswordModel().setStringValue("");
            auth.getKeyFileModel().setLocation(SshConnectionSettingsModel.NULL_LOCATION);
        }
        if (auth.getAuthType() != AuthType.CREDENTIALS) {
            auth.getCredentialModel().setStringValue("");
        }

        settings.validate();
        settings.saveSettingsTo(output);
    }

    private SshConnectionSettingsModel createSettings() throws InvalidSettingsException {
        NodeSettings ns = new NodeSettings("tmp");
        saveSettingsTo(ns);

        SshConnectionSettingsModel settings = m_settings.createClone();
        settings.loadSettingsFrom(ns);
        settings.configure(m_inputSpecs, e -> {
        });

        return settings;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO input, final PortObjectSpec[] specs)
            throws NotConfigurableException {
        m_inputSpecs = specs;

        try {
            m_settings.loadSettingsForModel(input);
        } catch (final InvalidSettingsException e) {
        }

        // call load settings for correct initialize file system dialog
        m_knownHostsChooser.loadSettingsFrom(input, specs);

        m_authDialog.updateUi(input, specs, getCredentialsProvider());

        //Known hosts
        final boolean useKnownHosts = m_settings.hasKnownHostsFile();
        m_useKnownHostsField.setSelected(useKnownHosts);
        knownHostsSelectionChanged(useKnownHosts);

        m_workingDirChooser.setSelectedWorkingDirectory(m_settings.getWorkingDirectory());
    }

    private void knownHostsSelectionChanged(final boolean selected) {
        m_knownHostsChooser.getComponentPanel().setVisible(selected);
    }

    @Override
    public void onClose() {
        m_workingDirChooser.onClose();
    }
}
