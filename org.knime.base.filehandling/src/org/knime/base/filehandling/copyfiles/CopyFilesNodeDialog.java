/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 * 
 * History
 *   Sep 5, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.copyfiles;

import javax.swing.JFileChooser;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.uri.URIDataValue;
import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * <code>NodeDialog</code> for the "Copy Files" Node.
 * 
 * 
 * @author Patrick Winter, University of Konstanz
 */
class CopyFilesNodeDialog extends DefaultNodeSettingsPane {

    private SettingsModelString m_copyormove;

    private SettingsModelString m_sourcecolumn;

    private SettingsModelString m_filenamehandling;

    private SettingsModelString m_outputdirectory;

    private SettingsModelString m_targetcolumn;

    private SettingsModelString m_ifexists;

    private FlowVariableModel m_outputdirectoryFvm;

    /**
     * New pane for configuring the copy files node dialog.
     */
    @SuppressWarnings("unchecked")
    protected CopyFilesNodeDialog() {
        super();
        m_copyormove = SettingsFactory.createCopyOrMoveSettings();
        m_sourcecolumn = SettingsFactory.createSourceColumnSettings();
        m_filenamehandling = SettingsFactory.createFilenameHandlingSettings();
        m_outputdirectory =
                SettingsFactory
                        .createOutputDirectorySettings(m_filenamehandling);
        m_targetcolumn =
                SettingsFactory.createTargetColumnSettings(m_filenamehandling);
        m_ifexists = SettingsFactory.createIfExistsSettings();
        m_outputdirectoryFvm = super.createFlowVariableModel(m_outputdirectory);
        // Enable/disable components based on the filename handling
        m_filenamehandling.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                String handling = m_filenamehandling.getStringValue();
                m_targetcolumn.setEnabled(handling
                        .equals(FilenameHandling.FROMCOLUMN.getName()));
                m_outputdirectory.setEnabled(isOutputDirectoryEnabled());
            }
        });
        // Copy or move
        addDialogComponent(new DialogComponentButtonGroup(m_copyormove, false,
                "Copy or move?", CopyOrMove.getAllSettings()));
        // Source column
        addDialogComponent(new DialogComponentColumnNameSelection(
                m_sourcecolumn, "Source column", 0, URIDataValue.class));
        createNewGroup("Target filenames...");
        // Filename handling
        addDialogComponent(new DialogComponentButtonGroup(m_filenamehandling,
                false, "", FilenameHandling.getAllSettings()));
        // Target column
        addDialogComponent(new DialogComponentColumnNameSelection(
                m_targetcolumn, "Target column", 0, URIDataValue.class));
        // Output directory
        addDialogComponent(new DialogComponentFileChooser(m_outputdirectory,
                "outputdirectoryHistory", JFileChooser.SAVE_DIALOG, true,
                m_outputdirectoryFvm));
        closeCurrentGroup();
        // Overwrite policy
        addDialogComponent(new DialogComponentButtonGroup(m_ifexists, false,
                "If a file exists...", OverwritePolicy.getAllSettings()));
    }

    /**
     * Checks if the output directory component should be enabled.
     * 
     * 
     * @return true if the output directory component should be enabled
     */
    private boolean isOutputDirectoryEnabled() {
        return m_filenamehandling.getStringValue().equals(
                FilenameHandling.SOURCENAME.getName())
                && !m_outputdirectoryFvm.isVariableReplacementEnabled();
    }

}
