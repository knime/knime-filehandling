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
 *   Oct 30, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.zip;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.StringValue;
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
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;

/**
 * <code>NodeDialog</code> for the node.
 * 
 * 
 * @author Patrick Winter, University of Konstanz
 */
public class ZipNodeDialog extends NodeDialogPane {

    private DialogComponentColumnNameSelection m_locationcolumn;

    private DialogComponentFileChooser m_target;

    private DialogComponentNumber m_compressionlevel;

    private DialogComponentButtonGroup m_pathhandling;

    private DialogComponentFileChooser m_prefix;

    private DialogComponentButtonGroup m_ifexists;

    /**
     * New pane for configuring the node dialog.
     */
    @SuppressWarnings("unchecked")
    public ZipNodeDialog() {
        final SettingsModelString locationcolumnsettings =
                SettingsFactory.createLocationColumnSettings();
        final SettingsModelString targetsettings =
                SettingsFactory.createTargetSettings();
        final SettingsModelIntegerBounded compressionlevelsettings =
                SettingsFactory.createCompressionLevelSettings();
        final SettingsModelString pathhandlingsettings =
                SettingsFactory.createPathHandlingSettings();
        final SettingsModelString prefixsettings =
                SettingsFactory.createPrefixSettings(pathhandlingsettings);
        final SettingsModelString ifexistssettings =
                SettingsFactory.createIfExistsSettings();
        final FlowVariableModel targetFvm =
                super.createFlowVariableModel(targetsettings);
        final FlowVariableModel prefixFvm =
                super.createFlowVariableModel(prefixsettings);
        // Enable/disable prefix setting based on the path handling
        pathhandlingsettings.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                prefixsettings.setEnabled(isPrefixEnabled(pathhandlingsettings,
                        prefixFvm));
            }
        });
        // Enable/disable prefix setting based on the flow variable model
        prefixsettings.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                prefixsettings.setEnabled(isPrefixEnabled(pathhandlingsettings,
                        prefixFvm));
            }
        });
        // Outer panel
        JPanel panel = new JPanel(new GridBagLayout());
        // Inner panel
        JPanel innerPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.NONE;
        // Location column
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        m_locationcolumn =
                new DialogComponentColumnNameSelection(locationcolumnsettings,
                        "Location column", 0, false, StringValue.class,
                        URIDataValue.class);
        panel.add(m_locationcolumn.getComponentPanel(), gbc);
        // Compression level
        gbc.anchor = GridBagConstraints.EAST;
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        m_compressionlevel =
                new DialogComponentNumber(compressionlevelsettings,
                        "Compression level", 1, 1);
        panel.add(m_compressionlevel.getComponentPanel(), gbc);
        // Target zip file
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.gridheight = 1;
        m_target =
                new DialogComponentFileChooser(targetsettings, "targetHistory",
                        JFileChooser.SAVE_DIALOG, false, targetFvm);
        m_target.setBorderTitle("Zip output file:");
        panel.add(m_target.getComponentPanel(), gbc);
        // Path handling
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        m_pathhandling =
                new DialogComponentButtonGroup(pathhandlingsettings, false, "",
                        PathHandling.getAllSettings());
        innerPanel.add(m_pathhandling.getComponentPanel(), gbc);
        // Prefix
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        m_prefix =
                new DialogComponentFileChooser(prefixsettings, "prefixHistory",
                        JFileChooser.OPEN_DIALOG, true, prefixFvm);
        m_prefix.setBorderTitle("Prefix directory:");
        innerPanel.add(m_prefix.getComponentPanel(), gbc);
        // Inner panel
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.gridheight = 1;
        innerPanel.setBorder(new TitledBorder(new EtchedBorder(),
                "Path handling"));
        panel.add(innerPanel, gbc);
        // Overwrite policy
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.gridheight = 1;
        m_ifexists =
                new DialogComponentButtonGroup(ifexistssettings, false,
                        "If zip file exists...",
                        OverwritePolicy.getAllSettings());
        panel.add(m_ifexists.getComponentPanel(), gbc);
        addTab("Options", panel);
    }

    /**
     * Checks if the prefix component should be enabled.
     * 
     * 
     * @return true if the prefix component should be enabled
     */
    private boolean isPrefixEnabled(final SettingsModelString pathhandling,
            final FlowVariableModel prefixFvm) {
        return pathhandling.getStringValue().equals(
                PathHandling.TRUNCATE_PREFIX.getName())
                && !prefixFvm.isVariableReplacementEnabled();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        m_locationcolumn.loadSettingsFrom(settings, specs);
        m_target.loadSettingsFrom(settings, specs);
        m_compressionlevel.loadSettingsFrom(settings, specs);
        m_pathhandling.loadSettingsFrom(settings, specs);
        m_prefix.loadSettingsFrom(settings, specs);
        m_ifexists.loadSettingsFrom(settings, specs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        m_locationcolumn.saveSettingsTo(settings);
        m_target.saveSettingsTo(settings);
        m_compressionlevel.saveSettingsTo(settings);
        m_pathhandling.saveSettingsTo(settings);
        m_prefix.saveSettingsTo(settings);
        m_ifexists.saveSettingsTo(settings);
    }

}
