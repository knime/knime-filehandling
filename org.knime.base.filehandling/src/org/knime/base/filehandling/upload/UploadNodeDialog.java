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
package org.knime.base.filehandling.upload;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.knime.base.filehandling.NodeUtils;
import org.knime.base.filehandling.remote.dialog.RemoteFileChooser;
import org.knime.base.filehandling.remote.dialog.RemoteFileChooserPanel;
import org.knime.base.filehandling.remotecredentials.port.RemoteCredentials;
import org.knime.base.filehandling.remotecredentials.port.RemoteCredentialsPortObjectSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.uri.URIDataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ColumnSelectionComboxBox;
import org.knime.core.node.workflow.FlowVariable;

/**
 * <code>NodeDialog</code> for the node.
 * 
 * 
 * @author Patrick Winter, University of Konstanz
 */
public class UploadNodeDialog extends NodeDialogPane {

    private RemoteCredentials m_credentials;

    private ColumnSelectionComboxBox m_source;

    private RemoteFileChooserPanel m_target;

    private ButtonGroup m_overwritePolicy;

    private JRadioButton m_overwrite;

    private JRadioButton m_overwriteIfNewer;

    private JRadioButton m_abort;

    /**
     * New pane for configuring the node dialog.
     */
    @SuppressWarnings("unchecked")
    public UploadNodeDialog() {
        // Source
        m_source =
                new ColumnSelectionComboxBox((Border)null, URIDataValue.class);
        // Target
        m_target =
                new RemoteFileChooserPanel(getPanel(), "Remote folder", true,
                        "targetHistory", RemoteFileChooser.SELECT_DIR,
                        createFlowVariableModel("target",
                                FlowVariable.Type.STRING), m_credentials);
        // Overwrite policy
        m_overwritePolicy = new ButtonGroup();
        m_overwrite = new JRadioButton(OverwritePolicy.OVERWRITE.getName());
        m_overwrite.setActionCommand(OverwritePolicy.OVERWRITE.getName());
        m_overwriteIfNewer =
                new JRadioButton(OverwritePolicy.OVERWRITEIFNEWER.getName());
        m_overwriteIfNewer.setActionCommand(OverwritePolicy.OVERWRITEIFNEWER
                .getName());
        m_abort = new JRadioButton(OverwritePolicy.ABORT.getName());
        m_abort.setActionCommand(OverwritePolicy.ABORT.getName());
        m_overwritePolicy.add(m_overwrite);
        m_overwritePolicy.add(m_overwriteIfNewer);
        m_overwritePolicy.add(m_abort);
        // Set layout
        addTab("Options", initLayout());
    }

    /**
     * Create and fill panel for the dialog.
     * 
     * 
     * @return The panel for the dialog
     */
    private JPanel initLayout() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        // Source column
        NodeUtils.resetGBC(gbc);
        JPanel sourcePanel = new JPanel(new GridBagLayout());
        JLabel sourceLabel = new JLabel("Source");
        sourcePanel.add(sourceLabel, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        sourcePanel.add(m_source, gbc);
        // Overwrite policy
        NodeUtils.resetGBC(gbc);
        gbc.insets = new Insets(0, 0, 0, 0);
        JPanel overwritePolicyPanel = new JPanel(new GridBagLayout());
        overwritePolicyPanel.add(m_overwrite, gbc);
        gbc.gridx++;
        overwritePolicyPanel.add(m_overwriteIfNewer, gbc);
        gbc.gridx++;
        overwritePolicyPanel.add(m_abort, gbc);
        overwritePolicyPanel.setBorder(new TitledBorder(new EtchedBorder(),
                "If exists..."));
        // Outer panel
        NodeUtils.resetGBC(gbc);
        gbc.weightx = 1;
        panel.add(sourcePanel, gbc);
        gbc.gridy++;
        panel.add(m_target.getPanel(), gbc);
        gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(overwritePolicyPanel, gbc);
        return panel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        // Check if a port object is available
        if (specs[0] == null) {
            throw new NotConfigurableException("No credentials available");
        }
        RemoteCredentialsPortObjectSpec object =
                (RemoteCredentialsPortObjectSpec)specs[0];
        m_credentials = object.getCredentials();
        // Check if the port object has credentials
        if (m_credentials == null) {
            throw new NotConfigurableException("No credentials available");
        }
        m_target.setCredentials(m_credentials);
        // Load configuration
        UploadConfiguration config = new UploadConfiguration();
        config.load(settings);
        m_target.setSelection(config.getTarget());
        m_source.update((DataTableSpec)specs[1], config.getSource());
        String overwritePolicy = config.getOverwritePolicy();
        if (overwritePolicy.equals(OverwritePolicy.OVERWRITE.getName())) {
            m_overwritePolicy.setSelected(m_overwrite.getModel(), true);
        }
        if (overwritePolicy.equals(OverwritePolicy.OVERWRITEIFNEWER.getName())) {
            m_overwritePolicy.setSelected(m_overwriteIfNewer.getModel(), true);
        }
        if (overwritePolicy.equals(OverwritePolicy.ABORT.getName())) {
            m_overwritePolicy.setSelected(m_abort.getModel(), true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        UploadConfiguration config = new UploadConfiguration();
        config.setTarget(m_target.getSelection());
        config.setSource(m_source.getSelectedColumn());
        config.setOverwritePolicy(m_overwritePolicy.getSelection()
                .getActionCommand());
        config.save(settings);
    }
}
