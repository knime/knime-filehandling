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
 *   2021-03-06 (Alexander Bondaletov): created
 */
package org.knime.ext.smb.filehandling.node;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.event.ChangeListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.ext.smb.filehandling.fs.SmbFSConnection;
import org.knime.ext.smb.filehandling.fs.SmbFSConnectionConfig;
import org.knime.ext.smb.filehandling.fs.SmbFSConnectionConfig.ConnectionMode;
import org.knime.ext.smb.filehandling.fs.SmbProtocolVersion;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.base.auth.AuthPanel;
import org.knime.filehandling.core.connections.base.auth.AuthSettings;
import org.knime.filehandling.core.connections.base.auth.EmptyAuthProviderPanel;
import org.knime.filehandling.core.connections.base.auth.StandardAuthTypes;
import org.knime.filehandling.core.connections.base.auth.UserPasswordAuthProviderPanel;
import org.knime.filehandling.core.connections.base.ui.WorkingDirectoryChooser;
import org.knime.filehandling.core.util.GBCBuilder;

/**
 * SMB connector node dialog.
 *
 * @author Alexander Bondaletov
 */
class SmbConnectorNodeDialog extends NodeDialogPane {

    private final SmbConnectorSettings m_settings;

    private final Map<ConnectionMode, JRadioButton> m_connectionModes = new EnumMap<>(ConnectionMode.class);
    private final JRadioButton m_radioFileserver;
    private final JRadioButton m_radioDomain;

    private final JPanel m_connectionCardsPanel;

    private final DialogComponentString m_fileserverHost;
    private final DialogComponentNumber m_fileserverPort;
    private final DialogComponentString m_fileserverShare;

    private final DialogComponentString m_domainName;
    private final DialogComponentString m_domainNamespace;

    private final AuthPanel m_authPanel;

    private final JComboBox<SmbProtocolVersion> m_protocolVersionCombo;

    private final WorkingDirectoryChooser m_workingDirChooser;
    private final ChangeListener m_workdirListener;

    /**
     * Creates new instance
     */
    public SmbConnectorNodeDialog() {
        m_settings = new SmbConnectorSettings();

        final var group = new ButtonGroup();
        m_radioFileserver = createConnectionModeButton(ConnectionMode.FILESERVER, group);
        m_radioDomain = createConnectionModeButton(ConnectionMode.DOMAIN, group);

        m_connectionCardsPanel = new JPanel(new CardLayout());

        m_fileserverHost = new DialogComponentString(m_settings.getFileserverHostModel(), "Host:  ", false, 30);
        m_fileserverPort = new DialogComponentNumber(m_settings.getFileserverPortModel(), "Port:", 1);
        m_fileserverShare = new DialogComponentString(m_settings.getFileserverShareModel(), "Share:", false, 30);

        m_domainName = new DialogComponentString(m_settings.getDomainNameModel(), "Domain:                   ", false,
                30);
        m_domainNamespace = new DialogComponentString(m_settings.getDomainNamespaceModel(), "Share/Namespace:", false,
                30);

        m_workingDirChooser = new WorkingDirectoryChooser("smb.workingDir", this::createFSConnection);
        m_workdirListener = e -> m_settings.getWorkingDirectoryModel()
                .setStringValue(m_workingDirChooser.getSelectedWorkingDirectory());

        final var authSettings = m_settings.getAuthSettings();
        m_authPanel = new AuthPanel(authSettings, //
                Arrays.asList( //
                        new UserPasswordAuthProviderPanel( //
                                authSettings.getSettingsForAuthType(StandardAuthTypes.USER_PASSWORD), //
                                this::getCredentialsProvider), //
                        new EmptyAuthProviderPanel( //
                                authSettings.getSettingsForAuthType(SmbFSConnectionConfig.KERBEROS_AUTH_TYPE)), //
                        new EmptyAuthProviderPanel( //
                                authSettings.getSettingsForAuthType(SmbFSConnectionConfig.GUEST_AUTH_TYPE)), //
                        new EmptyAuthProviderPanel( //
                                authSettings.getSettingsForAuthType(StandardAuthTypes.ANONYMOUS))));

        m_protocolVersionCombo = createProtocolVersionCombo();

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
        final SmbFSConnectionConfig config = m_settings.createFSConnectionConfig(credentialsProvider::get);
        return new SmbFSConnection(config);
    }

    private JComponent createSettingsPanel() {
        final var box = new Box(BoxLayout.Y_AXIS);
        box.add(createConnectionPanel());
        box.add(createAuthPanel());
        box.add(createFilesystemPanel());
        return box;
    }

    private JComponent createConnectionPanel() {
        final var panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Connection"));

        final var gbc = new GBCBuilder();

        gbc.resetPos().insetLeft(5).anchorWest().fillNone();
        panel.add(new JLabel("Connect to: "), gbc.build());

        gbc.incX();
        panel.add(m_radioFileserver, gbc.build());

        gbc.incX();
        panel.add(m_radioDomain, gbc.build());

        gbc.incX().fillHorizontal().setWeightX(1);
        panel.add(Box.createHorizontalGlue(), gbc.build());

        gbc.resetX().incY().insetLeft(30).insetTop(10).setWeightX(1).widthRemainder();
        m_connectionCardsPanel.add(createFileserverSettingsPanel(), ConnectionMode.FILESERVER.toString());
        m_connectionCardsPanel.add(createDomainSettingsPanel(), ConnectionMode.DOMAIN.toString());
        panel.add(m_connectionCardsPanel, gbc.build());

        return panel;
    }

    private Component createDomainSettingsPanel() {
        final var panel = new JPanel(new GridBagLayout());

        final var gbc = new GBCBuilder();

        gbc.resetPos().anchorWest().fillNone();
        panel.add(m_domainName.getComponentPanel(), gbc.build());

        gbc.incX().fillHorizontal().setWeightX(1);
        panel.add(Box.createHorizontalGlue(), gbc.build());

        gbc.resetX().incY().fillNone().setWeightX(0);
        panel.add(m_domainNamespace.getComponentPanel(), gbc.build());

        gbc.incX().fillHorizontal().setWeightX(1).widthRemainder();
        panel.add(Box.createHorizontalGlue(), gbc.build());

        return panel;
    }

    private Component createFileserverSettingsPanel() {
        final var panel = new JPanel(new GridBagLayout());

        final var gbc = new GBCBuilder();

        gbc.resetPos().anchorWest().fillNone();
        panel.add(m_fileserverHost.getComponentPanel(), gbc.build());

        gbc.incX().insetLeft(10);
        panel.add(m_fileserverPort.getComponentPanel(), gbc.build());

        gbc.incX().insetLeft(0).fillHorizontal().setWeightX(1);
        panel.add(Box.createHorizontalGlue(), gbc.build());

        gbc.resetX().incY().fillNone().setWeightX(0);
        panel.add(m_fileserverShare.getComponentPanel(), gbc.build());

        gbc.incX().fillHorizontal().setWeightX(1).widthRemainder();
        panel.add(Box.createHorizontalGlue(), gbc.build());

        return panel;
    }

    private JRadioButton createConnectionModeButton(final ConnectionMode mode, final ButtonGroup group) {
        final var rb = new JRadioButton(mode.toString());
        rb.addActionListener(e -> {
            m_settings.setConnectionMode(mode);
            updateConnectionCards();
        });

        group.add(rb);
        m_connectionModes.put(mode, rb);

        return rb;
    }

    private JComboBox<SmbProtocolVersion> createProtocolVersionCombo() {
        JComboBox<SmbProtocolVersion> combo = new JComboBox<>(SmbProtocolVersion.values());
        combo.addActionListener(e -> m_settings.setProtocolVersion((SmbProtocolVersion) combo.getSelectedItem()));
        return combo;
    }

    private void updateConnectionCards() {
        final var mode = m_settings.getConnectionMode();
        ((CardLayout) m_connectionCardsPanel.getLayout()).show(m_connectionCardsPanel, mode.toString());
    }

    private JComponent createAuthPanel() {
        m_authPanel.setBorder(BorderFactory.createTitledBorder("Authentication"));
        return m_authPanel;
    }

    private JComponent createFilesystemPanel() {
        final var panel = new JPanel(new GridBagLayout());
        final var gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 10, 0, 0);
        panel.add(m_workingDirChooser, gbc);

        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        gbc.gridy += 1;
        panel.add(Box.createVerticalGlue(), gbc);

        panel.setBorder(BorderFactory.createTitledBorder("File system settings"));
        return panel;
    }

    private JComponent createAdvancedPanel() {
        final var timeout = new DialogComponentNumber(m_settings.getTimeoutModel(), "", 1, 12);
        timeout.getComponentPanel().setLayout(new FlowLayout(FlowLayout.LEFT));

        final var panel = new JPanel(new GridBagLayout());
        final var gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Read/Write timeout (seconds): "), gbc);

        gbc.gridy += 1;
        panel.add(new JLabel("SMB version(s): "), gbc);

        gbc.weightx = 1;
        gbc.gridy = 0;
        gbc.gridx += 1;
        panel.add(timeout.getComponentPanel(), gbc);

        gbc.gridy += 1;
        gbc.insets = new Insets(0, 10, 0, 0);
        panel.add(m_protocolVersionCombo, gbc);

        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        gbc.weighty = 1;
        panel.add(Box.createVerticalGlue(), gbc);

        panel.setBorder(BorderFactory.createTitledBorder("Connection settings"));
        return panel;
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        validateBeforeSaving();
        m_settings.saveForDialog(settings);
        m_authPanel.saveSettingsTo(settings.addNodeSettings(AuthSettings.KEY_AUTH));
    }

    private void validateBeforeSaving() throws InvalidSettingsException {
        m_settings.validate();
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

        m_connectionModes.get(m_settings.getConnectionMode()).setSelected(true);
        m_protocolVersionCombo.setSelectedItem(m_settings.getProtocolVersion());
        updateConnectionCards();
    }

    @Override
    public void onClose() {
        m_workingDirChooser.removeListener(m_workdirListener);
        m_workingDirChooser.onClose();
    }
}
