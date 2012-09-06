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
 *   Sep 3, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.zip;

import javax.swing.JFileChooser;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.StringValue;
import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * <code>NodeDialog</code> for the "Zip" Node.
 * 
 * 
 * @author Patrick Winter, University of Konstanz
 */
class ZipNodeDialog extends DefaultNodeSettingsPane {

    private SettingsModelString m_urlcolumn;

    private SettingsModelString m_target;

    private SettingsModelString m_pathhandling;

    private SettingsModelString m_prefix;

    private SettingsModelString m_ifexists;

    private SettingsModelIntegerBounded m_compressionlevel;

    private FlowVariableModel m_targetFvm;

    private FlowVariableModel m_prefixFvm;

    /**
     * New pane for configuring Zip node dialog.
     */
    @SuppressWarnings("unchecked")
    protected ZipNodeDialog() {
        super();
        m_urlcolumn = SettingsFactory.createURLColumnSettings();
        m_target = SettingsFactory.createTargetSettings();
        m_pathhandling = SettingsFactory.createPathHandlingSettings();
        m_prefix = SettingsFactory.createPrefixSettings(m_pathhandling);
        m_ifexists = SettingsFactory.createIfExistsSettings();
        m_compressionlevel = SettingsFactory.createCompressionLevelSettings();
        m_targetFvm = super.createFlowVariableModel(m_target);
        m_prefixFvm = super.createFlowVariableModel(m_prefix);
        m_prefix.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                m_prefix.setEnabled(isPrefixEnabled());
            }
        });
        // URL Column
        addDialogComponent(new DialogComponentColumnNameSelection(m_urlcolumn,
                "URL column", 0, StringValue.class));
        // Target zip file
        addDialogComponent(new DialogComponentFileChooser(m_target,
                "targetHistory", JFileChooser.SAVE_DIALOG, false, m_targetFvm));
        // Compression level
        addDialogComponent(new DialogComponentNumber(m_compressionlevel,
                "Compression level", 1, 1));
        // Path handling
        createNewGroup("Path handling");
        addDialogComponent(new DialogComponentButtonGroup(m_pathhandling,
                false, "", PathHandling.getAllSettings()));
        // Enable/disable prefix setting based on the path handling
        m_pathhandling.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                m_prefix.setEnabled(isPrefixEnabled());
            }
        });
        addDialogComponent(new DialogComponentFileChooser(m_prefix,
                "prefixHistory", JFileChooser.OPEN_DIALOG, true, m_prefixFvm));
        closeCurrentGroup();
        // Overwrite policy
        addDialogComponent(new DialogComponentButtonGroup(m_ifexists, false,
                "If zip file exists...", OverwritePolicy.getAllSettings()));
    }

    /**
     * Checks if the prefix component should be enabled.
     * 
     * 
     * @return true if the prefix component should be enabled
     */
    private boolean isPrefixEnabled() {
        return m_pathhandling.getStringValue().equals(
                PathHandling.TRUNCATE_PREFIX.getName())
                && !m_prefixFvm.isVariableReplacementEnabled();
    }

}
