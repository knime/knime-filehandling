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
 *   Oct 29, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.copyfiles;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.uri.URIDataValue;
import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;

/**
 * <code>NodeDialog</code> for the node.
 * 
 * 
 * @author Patrick Winter, University of Konstanz
 */
public class CopyFilesNodeDialog extends NodeDialogPane {

    private DialogComponentButtonGroup m_copyormove;

    private DialogComponentColumnNameSelection m_sourcecolumn;

    private DialogComponentButtonGroup m_filenamehandling;

    private DialogComponentColumnNameSelection m_targetcolumn;

    private DialogComponentFileChooser m_outputdirectory;

    private DialogComponentButtonGroup m_ifexists;

    /**
     * New pane for configuring the node dialog.
     */
    @SuppressWarnings("unchecked")
    public CopyFilesNodeDialog() {
        final SettingsModelString copyormovesettings =
                SettingsFactory.createCopyOrMoveSettings();
        final SettingsModelString sourcecolumnsettings =
                SettingsFactory.createSourceColumnSettings();
        final SettingsModelString filenamehandlingsettings =
                SettingsFactory.createFilenameHandlingSettings();
        final SettingsModelString targetcolumnsettings =
                SettingsFactory
                        .createTargetColumnSettings(filenamehandlingsettings);
        final SettingsModelString outputdirectorysettings =
                SettingsFactory
                        .createOutputDirectorySettings(filenamehandlingsettings);
        final SettingsModelString ifexistssettings =
                SettingsFactory.createIfExistsSettings();
        final FlowVariableModel outputdirectoryFvm =
                super.createFlowVariableModel(outputdirectorysettings);
        // Enable/disable components based on the filename handling
        filenamehandlingsettings.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                String handling = filenamehandlingsettings.getStringValue();
                targetcolumnsettings.setEnabled(handling
                        .equals(FilenameHandling.FROMCOLUMN.getName()));
                outputdirectorysettings.setEnabled(isOutputDirectoryEnabled(
                        filenamehandlingsettings, outputdirectoryFvm));
            }
        });
        // Outer panel
        JPanel panel = new JPanel(new GridBagLayout());
        // Inner panel
        JPanel innerPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.NONE;
        // Copy or move
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        m_copyormove =
                new DialogComponentButtonGroup(copyormovesettings, false,
                        "Copy or move?", CopyOrMove.getAllSettings());
        panel.add(m_copyormove.getComponentPanel(), gbc);
        // Source column
        gbc.anchor = GridBagConstraints.EAST;
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        m_sourcecolumn =
                new DialogComponentColumnNameSelection(sourcecolumnsettings,
                        "Source column", 0, URIDataValue.class);
        panel.add(m_sourcecolumn.getComponentPanel(), gbc);
        // Filename handling
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        m_filenamehandling =
                new DialogComponentButtonGroup(filenamehandlingsettings, true,
                        "", FilenameHandling.getAllSettings());
        innerPanel.add(m_filenamehandling.getComponentPanel(), gbc);
        // Target column
        gbc.anchor = GridBagConstraints.EAST;
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        m_targetcolumn =
                new DialogComponentColumnNameSelection(targetcolumnsettings,
                        "Target column", 0, URIDataValue.class);
        innerPanel.add(m_targetcolumn.getComponentPanel(), gbc);
        // Output directory
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.gridheight = 1;
        m_outputdirectory =
                new DialogComponentFileChooser(outputdirectorysettings,
                        "outputdirectoryHistory", JFileChooser.SAVE_DIALOG,
                        true, outputdirectoryFvm);
        m_outputdirectory.setBorderTitle("Output directory:");
        innerPanel.add(m_outputdirectory.getComponentPanel(), gbc);
        // Inner panel
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.gridheight = 1;
        innerPanel.setBorder(new TitledBorder(new EtchedBorder(),
                "Target filenames..."));
        panel.add(innerPanel, gbc);
        // Overwrite policy
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.gridheight = 1;
        m_ifexists =
                new DialogComponentButtonGroup(ifexistssettings, false,
                        "If a file exists...", OverwritePolicy.getAllSettings());
        panel.add(m_ifexists.getComponentPanel(), gbc);
        addTab("Options", panel);
    }

    /**
     * Checks if the output directory component should be enabled.
     * 
     * 
     * @return true if the output directory component should be enabled
     */
    private boolean isOutputDirectoryEnabled(
            final SettingsModelString filenamehandlingsettings,
            final FlowVariableModel outputdirectoryFvm) {
        return filenamehandlingsettings.getStringValue().equals(
                FilenameHandling.SOURCENAME.getName())
                && !outputdirectoryFvm.isVariableReplacementEnabled();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        m_copyormove.loadSettingsFrom(settings, specs);
        m_sourcecolumn.loadSettingsFrom(settings, specs);
        m_filenamehandling.loadSettingsFrom(settings, specs);
        m_targetcolumn.loadSettingsFrom(settings, specs);
        m_outputdirectory.loadSettingsFrom(settings, specs);
        m_ifexists.loadSettingsFrom(settings, specs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        m_copyormove.saveSettingsTo(settings);
        m_sourcecolumn.saveSettingsTo(settings);
        m_filenamehandling.saveSettingsTo(settings);
        m_targetcolumn.saveSettingsTo(settings);
        m_outputdirectory.saveSettingsTo(settings);
        m_ifexists.saveSettingsTo(settings);
    }

}
