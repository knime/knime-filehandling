/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   Oct 29, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.extracturiinfo;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.knime.core.data.uri.URIDataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;

/**
 * <code>NodeDialog</code> for the node.
 * 
 * 
 * @author Patrick Winter, KNIME.com, Zurich, Switzerland
 */
public class ExtractURIInfoNodeDialog extends NodeDialogPane {

    private DialogComponentColumnNameSelection m_columnselection;

    private DialogComponentBoolean m_authority;

    private DialogComponentBoolean m_fragment;

    private DialogComponentBoolean m_host;

    private DialogComponentBoolean m_path;

    private DialogComponentBoolean m_port;

    private DialogComponentBoolean m_query;

    private DialogComponentBoolean m_scheme;

    private DialogComponentBoolean m_user;

    /**
     * New pane for configuring the node dialog.
     */
    @SuppressWarnings("unchecked")
    public ExtractURIInfoNodeDialog() {
        // Get settings models
        SettingsModelString columnselectionsettings = SettingsFactory.createColumnSelectionSettings();
        SettingsModelBoolean authoritysettings = SettingsFactory.createAuthoritySettings();
        SettingsModelBoolean fragmentsettings = SettingsFactory.createFragmentSettings();
        SettingsModelBoolean hostsettings = SettingsFactory.createHostSettings();
        SettingsModelBoolean pathsettings = SettingsFactory.createPathSettings();
        SettingsModelBoolean portsettings = SettingsFactory.createPortSettings();
        SettingsModelBoolean querysettings = SettingsFactory.createQuerySettings();
        SettingsModelBoolean schemesettings = SettingsFactory.createSchemeSettings();
        SettingsModelBoolean userinfosettings = SettingsFactory.createUserInfoSettings();
        // Outer panel
        JPanel panel = new JPanel(new GridBagLayout());
        // Inner panel
        JPanel innerPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridy = 0;
        // Column selection
        m_columnselection =
                new DialogComponentColumnNameSelection(columnselectionsettings, "Column selection", 0,
                        URIDataValue.class);
        panel.add(m_columnselection.getComponentPanel(), gbc);
        // Authority
        m_authority = new DialogComponentBoolean(authoritysettings, "Authority");
        innerPanel.add(m_authority.getComponentPanel(), gbc);
        // Fragment
        gbc.gridy += 1;
        m_fragment = new DialogComponentBoolean(fragmentsettings, "Fragment");
        innerPanel.add(m_fragment.getComponentPanel(), gbc);
        // Host
        gbc.gridy += 1;
        m_host = new DialogComponentBoolean(hostsettings, "Host");
        innerPanel.add(m_host.getComponentPanel(), gbc);
        // Path
        gbc.gridy += 1;
        m_path = new DialogComponentBoolean(pathsettings, "Path");
        innerPanel.add(m_path.getComponentPanel(), gbc);
        // Port
        gbc.gridy += 1;
        m_port = new DialogComponentBoolean(portsettings, "Port");
        innerPanel.add(m_port.getComponentPanel(), gbc);
        // Query
        gbc.gridy += 1;
        m_query = new DialogComponentBoolean(querysettings, "Query");
        innerPanel.add(m_query.getComponentPanel(), gbc);
        // Scheme
        gbc.gridy += 1;
        m_scheme = new DialogComponentBoolean(schemesettings, "Scheme");
        innerPanel.add(m_scheme.getComponentPanel(), gbc);
        // User info
        gbc.gridy += 1;
        m_user = new DialogComponentBoolean(userinfosettings, "User");
        innerPanel.add(m_user.getComponentPanel(), gbc);
        // Inner panel
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridy = 1;
        innerPanel.setBorder(new TitledBorder(new EtchedBorder(), "Extract..."));
        panel.add(innerPanel, gbc);
        addTab("Options", panel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
            throws NotConfigurableException {
        m_columnselection.loadSettingsFrom(settings, specs);
        m_authority.loadSettingsFrom(settings, specs);
        m_fragment.loadSettingsFrom(settings, specs);
        m_host.loadSettingsFrom(settings, specs);
        m_path.loadSettingsFrom(settings, specs);
        m_port.loadSettingsFrom(settings, specs);
        m_query.loadSettingsFrom(settings, specs);
        m_scheme.loadSettingsFrom(settings, specs);
        m_user.loadSettingsFrom(settings, specs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_columnselection.saveSettingsTo(settings);
        m_authority.saveSettingsTo(settings);
        m_fragment.saveSettingsTo(settings);
        m_host.saveSettingsTo(settings);
        m_path.saveSettingsTo(settings);
        m_port.saveSettingsTo(settings);
        m_query.saveSettingsTo(settings);
        m_scheme.saveSettingsTo(settings);
        m_user.saveSettingsTo(settings);
    }

}
