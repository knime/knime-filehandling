/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.filehandling.NodeUtils;
import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformation;
import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformationPortObjectSpec;
import org.knime.base.filehandling.remote.dialog.RemoteFileChooser;
import org.knime.base.filehandling.remote.dialog.RemoteFileChooserPanel;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.uri.URIDataValue;
import org.knime.core.node.FlowVariableModelButton;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ColumnSelectionComboxBox;
import org.knime.core.node.util.FilesHistoryPanel;
import org.knime.core.node.workflow.FlowVariable;

/**
 * <code>NodeDialog</code> for the node.
 * 
 * 
 * @author Patrick Winter, KNIME.com, Zurich, Switzerland
 */
public class UploadNodeDialog extends NodeDialogPane {

    private ConnectionInformation m_connectionInformation;

    private JLabel m_info;

    private ColumnSelectionComboxBox m_source;

    private RemoteFileChooserPanel m_target;

    private ButtonGroup m_overwritePolicy;

    private JRadioButton m_overwrite;

    private JRadioButton m_overwriteIfNewer;

    private JRadioButton m_abort;

    private ButtonGroup m_pathhandling;

    private JRadioButton m_fullpath;

    private JRadioButton m_onlyfilename;

    private JRadioButton m_truncate;

    private FilesHistoryPanel m_prefix;

    private FlowVariableModelButton m_prefixfvm;

    /**
     * New pane for configuring the node dialog.
     */
    @SuppressWarnings("unchecked")
    public UploadNodeDialog() {
        // Info
        m_info = new JLabel();
        // Source
        m_source =
                new ColumnSelectionComboxBox((Border)null, URIDataValue.class);
        // Target
        m_target =
                new RemoteFileChooserPanel(getPanel(), "Target folder", true,
                        "targetHistory", RemoteFileChooser.SELECT_DIR,
                        createFlowVariableModel("target",
                                FlowVariable.Type.STRING),
                        m_connectionInformation);
        // Path handling
        m_pathhandling = new ButtonGroup();
        m_fullpath = new JRadioButton(PathHandling.FULL_PATH.getName());
        m_fullpath.setActionCommand(PathHandling.FULL_PATH.getName());
        m_fullpath.addActionListener(new PathHandlingListener());
        m_onlyfilename = new JRadioButton(PathHandling.ONLY_FILENAME.getName());
        m_onlyfilename.setActionCommand(PathHandling.ONLY_FILENAME.getName());
        m_onlyfilename.addActionListener(new PathHandlingListener());
        m_truncate = new JRadioButton(PathHandling.TRUNCATE_PREFIX.getName());
        m_truncate.setActionCommand(PathHandling.TRUNCATE_PREFIX.getName());
        m_truncate.addActionListener(new PathHandlingListener());
        m_pathhandling.add(m_fullpath);
        m_pathhandling.add(m_onlyfilename);
        m_pathhandling.add(m_truncate);
        // Truncate directory
        m_prefix = new FilesHistoryPanel("prefixHistory", false);
        m_prefix.setSelectMode(JFileChooser.DIRECTORIES_ONLY);
        m_prefixfvm =
                new FlowVariableModelButton(createFlowVariableModel(
                        "truncatedirectory", FlowVariable.Type.STRING));
        m_prefixfvm.getFlowVariableModel().addChangeListener(
                new ChangeListener() {
                    @Override
                    public void stateChanged(final ChangeEvent e) {
                        enableComponents();
                    }
                });
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
        enableComponents();
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
        // Path handling
        NodeUtils.resetGBC(gbc);
        gbc.insets = new Insets(0, 0, 0, 0);
        JPanel pathHandlingPanel = new JPanel(new GridBagLayout());
        pathHandlingPanel.add(m_fullpath, gbc);
        gbc.gridx++;
        pathHandlingPanel.add(m_onlyfilename, gbc);
        gbc.gridx++;
        pathHandlingPanel.add(m_truncate, gbc);
        pathHandlingPanel.setBorder(new TitledBorder(new EtchedBorder(),
                "Path handling"));
        // Prefix
        NodeUtils.resetGBC(gbc);
        JPanel prefixPanel = new JPanel(new GridBagLayout());
        gbc.weightx = 1;
        prefixPanel.add(m_prefix, gbc);
        gbc.weightx = 0;
        gbc.gridx++;
        gbc.insets = new Insets(5, 0, 5, 5);
        prefixPanel.add(m_prefixfvm, gbc);
        prefixPanel.setBorder(new TitledBorder(new EtchedBorder(), "Prefix"));
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
        panel.add(m_info, gbc);
        gbc.gridy++;
        panel.add(m_target.getPanel(), gbc);
        gbc.gridy++;
        panel.add(sourcePanel, gbc);
        gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(pathHandlingPanel, gbc);
        gbc.gridy++;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(prefixPanel, gbc);
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridy++;
        panel.add(overwritePolicyPanel, gbc);
        return panel;
    }

    /**
     * Listener that updates the enabled state of the components.
     * 
     * 
     * @author Patrick Winter, KNIME.com, Zurich, Switzerland
     */
    private class PathHandlingListener implements ActionListener {

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionPerformed(final ActionEvent arg0) {
            enableComponents();
        }

    }

    /**
     * Will enable and disable the components based on the current
     * configuration.
     */
    private void enableComponents() {
        boolean usePrefix = m_truncate.isSelected();
        boolean replacement =
                m_prefixfvm.getFlowVariableModel()
                        .isVariableReplacementEnabled();
        m_prefix.setEnabled(usePrefix && !replacement);
        m_prefixfvm.setEnabled(usePrefix);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        // Check if a port object is available
        if (specs[0] == null) {
            throw new NotConfigurableException(
                    "No connection information available");
        }
        ConnectionInformationPortObjectSpec object =
                (ConnectionInformationPortObjectSpec)specs[0];
        m_connectionInformation = object.getConnectionInformation();
        // Check if the port object has connection information
        if (m_connectionInformation == null) {
            throw new NotConfigurableException(
                    "No connection information available");
        }
        m_target.setConnectionInformation(m_connectionInformation);
        m_info.setText("Upload to: " + m_connectionInformation.toURI());
        // Load configuration
        UploadConfiguration config = new UploadConfiguration();
        config.load(settings);
        m_target.setSelection(config.getTarget());
        m_source.update((DataTableSpec)specs[1], config.getSource());
        String overwritePolicy = config.getOverwritePolicy();
        if (overwritePolicy.equals(OverwritePolicy.OVERWRITE.getName())) {
            m_overwritePolicy.setSelected(m_overwrite.getModel(), true);
        } else if (overwritePolicy.equals(OverwritePolicy.OVERWRITEIFNEWER
                .getName())) {
            m_overwritePolicy.setSelected(m_overwriteIfNewer.getModel(), true);
        } else if (overwritePolicy.equals(OverwritePolicy.ABORT.getName())) {
            m_overwritePolicy.setSelected(m_abort.getModel(), true);
        }
        String pathHandling = config.getPathHandling();
        if (pathHandling.equals(PathHandling.FULL_PATH.getName())) {
            m_pathhandling.setSelected(m_fullpath.getModel(), true);
        } else if (pathHandling.equals(PathHandling.ONLY_FILENAME.getName())) {
            m_pathhandling.setSelected(m_onlyfilename.getModel(), true);
        } else if (pathHandling.equals(PathHandling.TRUNCATE_PREFIX.getName())) {
            m_pathhandling.setSelected(m_truncate.getModel(), true);
        }
        m_prefix.setSelectedFile(config.getPrefix());
        enableComponents();
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
        config.setPathHandling(m_pathhandling.getSelection().getActionCommand());
        config.setPrefix(m_prefix.getSelectedFile());
        config.save(settings);
    }
}
