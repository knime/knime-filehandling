/*
 * ------------------------------------------------------------------------
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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 * 
 * History
 *   Sep 5, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.stringtouri;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.StringValue;
import org.knime.core.data.uri.URIDataValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnFilter;

/**
 * <code>NodeDialog</code> for the node.
 * 
 * 
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 */
class StringToURINodeDialog extends DefaultNodeSettingsPane {

    private SettingsModelString m_columnselection;

    private SettingsModelBoolean m_missingfileabort;

    private SettingsModelString m_columnname;

    private SettingsModelString m_replace;

    /**
     * New pane for configuring the node dialog.
     */
    protected StringToURINodeDialog() {
        super();
        m_columnselection = SettingsFactory.createColumnSelectionSettings();
        m_missingfileabort = SettingsFactory.createMissingFileAbortSettings();
        m_replace = SettingsFactory.createReplacePolicySettings();
        m_columnname = SettingsFactory.createColumnNameSettings(m_replace);
        m_replace.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                boolean append = m_replace.getStringValue().equals(ReplacePolicy.APPEND.getName());
                m_columnname.setEnabled(append);
            }
        });
        // Column selection
        addDialogComponent(new DialogComponentColumnNameSelection(m_columnselection, "Column selection", 0,
                new ColumnFilter() {
                    @Override
                    public boolean includeColumn(final DataColumnSpec colSpec) {
                        DataType type = colSpec.getType();
                        return type.isCompatible(StringValue.class) && !type.isCompatible(URIDataValue.class);
                    }

                    @Override
                    public String allFilteredMsg() {
                        return "No applicable column available";
                    }
                }));
        // Missing file abort
        addDialogComponent(new DialogComponentBoolean(m_missingfileabort,
                "Fail if file does not exist (only applies to local files)"));
        createNewGroup("New column...");
        // Replace
        addDialogComponent(new DialogComponentButtonGroup(m_replace, false, "", ReplacePolicy.getAllSettings()));
        // Column name
        addDialogComponent(new DialogComponentString(m_columnname, "Name", true, 20));
        closeCurrentGroup();
    }
}
