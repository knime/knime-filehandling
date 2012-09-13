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
package org.knime.base.filehandling.binaryobjectstofiles;

import javax.swing.JFileChooser;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.StringValue;
import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * <code>NodeDialog</code> for the "Files to Binary Objects" Node.
 * 
 * 
 * @author Patrick Winter, University of Konstanz
 */
class BinaryObjectsToFilesNodeDialog extends DefaultNodeSettingsPane {

    private SettingsModelString m_bocolumn;

    private SettingsModelString m_outputdirectory;

    private SettingsModelString m_filenamehandling;

    private SettingsModelString m_namecolumn;

    private SettingsModelString m_namepattern;

    private SettingsModelString m_ifexists;

    private FlowVariableModel m_outputdirectoryFvm;

    /**
     * New pane for configuring the Binary Objects to Files node dialog.
     */
    @SuppressWarnings("unchecked")
    protected BinaryObjectsToFilesNodeDialog() {
        super();
        m_bocolumn = SettingsFactory.createBinaryObjectColumnSettings();
        m_outputdirectory = SettingsFactory.createOutputDirectorySettings();
        m_filenamehandling = SettingsFactory.createFilenameHandlingSettings();
        m_namecolumn =
                SettingsFactory.createNameColumnSettings(m_filenamehandling);
        m_namepattern =
                SettingsFactory.createNamePatternSettings(m_filenamehandling);
        m_ifexists = SettingsFactory.createIfExistsSettings();
        m_outputdirectoryFvm = super.createFlowVariableModel(m_outputdirectory);
        // Enable/disable settings according to filename handling
        m_filenamehandling.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                String handling = m_filenamehandling.getStringValue();
                m_namecolumn.setEnabled(handling
                        .equals(FilenameHandling.FROMCOLUMN.getName()));
                m_namepattern.setEnabled(handling
                        .equals(FilenameHandling.GENERATE.getName()));
            }
        });
        // Binary object column
        // TODO change classtype to BinaryObjectValue
        addDialogComponent(new DialogComponentColumnNameSelection(m_bocolumn,
                "Binary object column", 0, StringValue.class));
        // Output directory
        addDialogComponent(new DialogComponentFileChooser(m_outputdirectory,
                "outputdirectoryHistory", JFileChooser.SAVE_DIALOG, true,
                m_outputdirectoryFvm));
        // Filename handling
        createNewGroup("Filenames...");
        addDialogComponent(new DialogComponentButtonGroup(m_filenamehandling,
                false, "", FilenameHandling.getAllSettings()));
        // Name column
        // TODO change classtype to URIValue
        addDialogComponent(new DialogComponentColumnNameSelection(m_namecolumn,
                "Name column", 0, StringValue.class));
        // Name pattern
        addDialogComponent(new DialogComponentString(m_namepattern,
                "Name pattern"));
        closeCurrentGroup();
        // Overwrite policy
        addDialogComponent(new DialogComponentButtonGroup(m_ifexists, false,
                "If a file exists...", OverwritePolicy.getAllSettings()));
    }
}
