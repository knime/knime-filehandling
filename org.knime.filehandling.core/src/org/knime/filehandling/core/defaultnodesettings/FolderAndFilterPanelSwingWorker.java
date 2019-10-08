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
 *   Oct 8, 2019 (julian): created
 */
package org.knime.filehandling.core.defaultnodesettings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import javax.swing.JPanel;
import javax.swing.SwingWorker;

import org.knime.core.node.FSConnectionFlowVariableProvider;
import org.knime.core.node.NodeLogger;

/**
 * SwingWorker to check whether the JPanel holding filter and folder options should be enabled and visible.
 *
 * @author Julian Bunzel, KNIME GmbH, Berlin, Germany
 */
final class FolderAndFilterPanelSwingWorker extends SwingWorker<Boolean, Boolean> {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DialogComponentFileChooser2.class);

    private final JPanel m_filterAndFolderSettingsPanel;

    private final FSConnectionFlowVariableProvider m_connectionFlowVariableProvider;

    private final SettingsModelFileChooser2 m_settingsModel;

    FolderAndFilterPanelSwingWorker(final FSConnectionFlowVariableProvider connectionFlowVariableProvider,
        final SettingsModelFileChooser2 settingsModel, final JPanel filterAndFolderSettingsPanel) {
        m_connectionFlowVariableProvider = connectionFlowVariableProvider;
        m_settingsModel = settingsModel;
        m_filterAndFolderSettingsPanel = filterAndFolderSettingsPanel;
    }

    @Override
    protected Boolean doInBackground() throws Exception {
        if (m_settingsModel.getFileSystemChoice().equals(FileSystemChoice.getCustomFsUrlChoice())
            || m_settingsModel.getPathOrURL() == null || m_settingsModel.getPathOrURL().isEmpty()) {
            return Boolean.FALSE;
        }

        // sleep for 200ms to allow for quick cancellation without getting stuck in IO
        Thread.sleep(200);

        // get file systems
        final FileChooserHelper helper;
        try {
            helper = new FileChooserHelper(m_connectionFlowVariableProvider, m_settingsModel);
        } catch (Exception e) {
            final String msg = "Could not get file system: " + ExceptionUtil.getDeepestErrorMessage(e, true);
            LOGGER.debug(msg, e);
            return Boolean.FALSE;
        }

        // instantiate a path
        final Path fileOrFolder;
        try {
            fileOrFolder = helper.getFileSystem().getPath(m_settingsModel.getPathOrURL());
        } catch (InvalidPathException e) {
            return Boolean.FALSE;
        }

        // fetch file attributes for path, so we can determine wheter it exists, is accessible
        // and whether it is file or folder
        final BasicFileAttributes basicAttributes;
        try {
            basicAttributes = Files.getFileAttributeView(fileOrFolder, BasicFileAttributeView.class).readAttributes();
        } catch (IOException e) {
            return Boolean.FALSE;
        }

        if (basicAttributes.isDirectory()) {
            return Boolean.TRUE;
        }

        return Boolean.FALSE;
    }

    @Override
    protected void done() {
        try {
            m_filterAndFolderSettingsPanel.setEnabled(get());
            m_filterAndFolderSettingsPanel.setVisible(get());
        } catch (ExecutionException e) {
            if (!(e.getCause() instanceof InterruptedException)) {
                LOGGER.debug(e.getMessage(), e);
            }
        } catch (InterruptedException | CancellationException ex) {
            // ignore
        }
    }
}
