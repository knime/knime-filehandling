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
package org.knime.base.filehandling.extracturiinfo;

import org.knime.core.data.uri.URIDataValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * <code>NodeDialog</code> for the node.
 * 
 * 
 * @author Patrick Winter, University of Konstanz
 */
class ExtractURIInfoNodeDialog extends DefaultNodeSettingsPane {

    private SettingsModelString m_columnselection;

    private SettingsModelBoolean m_authority;

    private SettingsModelBoolean m_fragment;

    private SettingsModelBoolean m_host;

    private SettingsModelBoolean m_path;

    private SettingsModelBoolean m_port;

    private SettingsModelBoolean m_query;

    private SettingsModelBoolean m_scheme;

    private SettingsModelBoolean m_userinfo;

    /**
     * New pane for configuring the node dialog.
     */
    @SuppressWarnings("unchecked")
    protected ExtractURIInfoNodeDialog() {
        super();
        m_columnselection = SettingsFactory.createColumnSelectionSettings();
        m_authority = SettingsFactory.createAuthoritySettings();
        m_fragment = SettingsFactory.createFragmentSettings();
        m_host = SettingsFactory.createHostSettings();
        m_path = SettingsFactory.createPathSettings();
        m_port = SettingsFactory.createPortSettings();
        m_query = SettingsFactory.createQuerySettings();
        m_scheme = SettingsFactory.createSchemeSettings();
        m_userinfo = SettingsFactory.createUserInfoSettings();
        // Column selection
        addDialogComponent(new DialogComponentColumnNameSelection(
                m_columnselection, "Column selection", 0, URIDataValue.class));
        createNewGroup("Extract...");
        // Authority
        addDialogComponent(new DialogComponentBoolean(m_authority, "Authority"));
        // Fragment
        addDialogComponent(new DialogComponentBoolean(m_fragment, "Fragment"));
        // Host
        addDialogComponent(new DialogComponentBoolean(m_host, "Host"));
        // Path
        addDialogComponent(new DialogComponentBoolean(m_path, "Path"));
        // Port
        addDialogComponent(new DialogComponentBoolean(m_port, "Port"));
        // Query
        addDialogComponent(new DialogComponentBoolean(m_query, "Query"));
        // Scheme
        addDialogComponent(new DialogComponentBoolean(m_scheme, "Scheme"));
        // User info
        addDialogComponent(new DialogComponentBoolean(m_userinfo, "User"));
        closeCurrentGroup();
    }
}
