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
 *   2022-04-27 (Dragan Keselj): created
 */
package org.knime.archive.zip.filehandling.node;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.Closeable;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.event.ChangeListener;

import org.knime.archive.zip.filehandling.fs.ArchiveZipFSConnection;
import org.knime.archive.zip.filehandling.fs.ArchiveZipFSConnectionConfig;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.base.ui.WorkingDirectoryChooser;
import org.knime.filehandling.core.data.location.variable.FSLocationVariableType;
import org.knime.filehandling.core.defaultnodesettings.ExceptionUtil;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.DialogComponentReaderFileChooser;
import org.knime.filehandling.core.encoding.CharsetNamePanel;
import org.knime.filehandling.core.util.GBCBuilder;

/**
 * Node dialog for the ArchiveZip Connector.
 *
 * @author Dragan Keselj, KNIME GmbH
 */
class ArchiveZipConnectorNodeDialog extends NodeDialogPane {

    private static final String WORKING_DIR_HISTORY_ID = "zip.workingDir";

    private static final String FILE_HISTORY_ID = "zip.file";

    private final ArchiveZipConnectorNodeSettings m_settings;

    private final DialogComponentReaderFileChooser m_fileChooser;

    private final WorkingDirectoryChooser m_workingDirChooser;
    private final ChangeListener m_workdirListener;

    private final DialogComponentBoolean m_useDefaultEncoding;
    private final CharsetNamePanel m_encodingPanel;

    /**
     * Creates new instance
     *
     * @param cfg
     *            node creation configuration.
     */
    public ArchiveZipConnectorNodeDialog(final NodeCreationConfiguration cfg) {
        m_settings = new ArchiveZipConnectorNodeSettings(cfg);

        m_fileChooser = new DialogComponentReaderFileChooser(m_settings.getFileModel(), FILE_HISTORY_ID, //
                createFlowVariableModel(m_settings.getFileModel().getKeysForFSLocation(), //
                        FSLocationVariableType.INSTANCE));

        m_workingDirChooser = new WorkingDirectoryChooser(WORKING_DIR_HISTORY_ID, this::createFSConnection);
        m_workdirListener = e -> m_settings.getWorkingDirectoryModel()
                .setStringValue(m_workingDirChooser.getSelectedWorkingDirectory());

        addTab("Settings", createSettingsPanel());

        m_useDefaultEncoding = new DialogComponentBoolean(m_settings.getUseDefaultEncodingModel(),
                "Use default encoding");
        m_encodingPanel = new CharsetNamePanel();
        addTab("Encoding", getEncodingPanel());

        m_settings.getUseDefaultEncodingModel().addChangeListener(e -> updateEnabledness());
    }

    private JComponent createSettingsPanel() {
        final var panel = new JPanel(new GridBagLayout());
        final GBCBuilder gbc = new GBCBuilder().resetX().resetY().fillHorizontal().setWeightX(1);

        panel.add(createInputLocationPanel(), gbc.build());
        gbc.incY().insets(10, 0, 0, 0);
        panel.add(createWorkingDirectoryPanel(), gbc.build());
        gbc.incY().setWeightY(1);
        panel.add(new JPanel(), gbc.build());

        return panel;
    }

    private Component createInputLocationPanel() {
        final var panel = new JPanel(new GridBagLayout());
        final var gbc = new GBCBuilder().resetX().resetY();
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Input location"));
        panel.add(m_fileChooser.getComponentPanel(), gbc.fillHorizontal().setWeightX(1).build());
        return panel;
    }

    private Component createWorkingDirectoryPanel() {
        final var panel = new JPanel(new GridBagLayout());
        GBCBuilder gbc = new GBCBuilder().resetX().resetY();
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Working directory"));
        panel.add(m_workingDirChooser, gbc.fillHorizontal().setWeightX(1).build());
        return panel;
    }

    private JPanel getEncodingPanel() {
        var panel = new JPanel(new GridBagLayout());
        final var gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;

        panel.add(m_useDefaultEncoding.getComponentPanel(), gbc);

        gbc.weightx = 1;
        gbc.gridx++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(Box.createHorizontalGlue(), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(10, 0, 0, 0);
        panel.add(m_encodingPanel, gbc);

        return panel;
    }

    private void updateEnabledness() {
        m_encodingPanel.setEnabled(!m_settings.getUseDefaultEncodingModel().getBooleanValue());
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_useDefaultEncoding.saveSettingsTo(settings);
        m_settings.setEncoding(m_encodingPanel.getSelectedCharsetName().orElse(null));
        validateBeforeSaving();

        m_settings.saveForDialog(settings);
        m_fileChooser.saveSettingsTo(settings);

    }

    private void validateBeforeSaving() throws InvalidSettingsException {
        m_settings.validate();
        m_workingDirChooser.addCurrentSelectionToHistory();
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
            throws NotConfigurableException {
        m_settings.loadForDialog(settings);
        m_fileChooser.loadSettingsFrom(settings, specs);
        m_useDefaultEncoding.loadSettingsFrom(settings, specs);
        m_encodingPanel.loadSettings(m_settings.getEncoding());
        settingsLoaded();
    }

    private void settingsLoaded() {
        // nothing to do
    }

    @Override
    public void onOpen() {
        m_workingDirChooser.setSelectedWorkingDirectory(m_settings.getWorkingDirectory());
        m_workingDirChooser.addListener(m_workdirListener);
    }

    @Override
    public void onClose() {
        m_workingDirChooser.removeListener(m_workdirListener);
        m_workingDirChooser.onClose();
        m_fileChooser.onClose();
    }

    @SuppressWarnings("resource")
    private FSConnection createFSConnection() throws IOException {
        ArchiveZipFSConnectionConfig config = null;
        try {
            config = m_settings.createFSConnectionConfig(s -> {});
            return new ArchiveZipFSConnection(config);
        } catch (Exception e) { //NOSONAR
            closeQuietly(config);
            throw ExceptionUtil.wrapAsIOException(e);
        }
    }

    private static void closeQuietly(final Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException e) { //NOSONAR
            // quietly ignored
        }
    }
}
