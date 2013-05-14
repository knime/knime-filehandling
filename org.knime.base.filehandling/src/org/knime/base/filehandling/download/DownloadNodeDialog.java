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
package org.knime.base.filehandling.download;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.filehandling.NodeUtils;
import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformation;
import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformationPortObjectSpec;
import org.knime.base.filehandling.remote.dialog.RemoteFileChooser;
import org.knime.base.filehandling.remote.dialog.RemoteFileChooserPanel;
import org.knime.core.node.FlowVariableModelButton;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.FilesHistoryPanel;
import org.knime.core.node.workflow.FlowVariable;

/**
 * <code>NodeDialog</code> for the node.
 * 
 * 
 * @author Patrick Winter, KNIME.com, Zurich, Switzerland
 */
public class DownloadNodeDialog extends NodeDialogPane {

    private ConnectionInformation m_connectionInformation;

    private JLabel m_info;

    private RemoteFileChooserPanel m_source;

    private FilesHistoryPanel m_target;

    private FlowVariableModelButton m_targetfvm;

    private ButtonGroup m_overwritePolicy;

    private JRadioButton m_overwrite;

    private JRadioButton m_overwriteIfNewer;

    private JRadioButton m_abort;

    private ButtonGroup m_pathhandling;

    private JRadioButton m_fullpath;

    private JRadioButton m_onlyfilename;

    private JRadioButton m_truncate;

    private RemoteFileChooserPanel m_prefix;

    private JCheckBox m_subfolders;

    private ButtonGroup m_filterType;

    private JCheckBox m_useFilter;

    private JRadioButton m_regularExpression;

    private JRadioButton m_wildcard;

    private JLabel m_filterTypeLabel;

    private JTextField m_filterPattern;

    /**
     * New pane for configuring the node dialog.
     */
    public DownloadNodeDialog() {
        // Info
        m_info = new JLabel();
        // Source
        m_source =
                new RemoteFileChooserPanel(getPanel(), "Source file or folder", true, "sourceHistory",
                        RemoteFileChooser.SELECT_FILE_OR_DIR, createFlowVariableModel("source",
                                FlowVariable.Type.STRING), m_connectionInformation);
        // Path handling
        m_pathhandling = new ButtonGroup();
        m_fullpath = new JRadioButton(PathHandling.FULL_PATH.getName());
        m_fullpath.setActionCommand(PathHandling.FULL_PATH.getName());
        m_fullpath.addActionListener(new UpdateListener());
        m_onlyfilename = new JRadioButton(PathHandling.ONLY_FILENAME.getName());
        m_onlyfilename.setActionCommand(PathHandling.ONLY_FILENAME.getName());
        m_onlyfilename.addActionListener(new UpdateListener());
        m_truncate = new JRadioButton(PathHandling.TRUNCATE_PREFIX.getName());
        m_truncate.setActionCommand(PathHandling.TRUNCATE_PREFIX.getName());
        m_truncate.addActionListener(new UpdateListener());
        m_pathhandling.add(m_fullpath);
        m_pathhandling.add(m_onlyfilename);
        m_pathhandling.add(m_truncate);
        // Truncate directory
        m_prefix =
                new RemoteFileChooserPanel(getPanel(), "Prefix", true, "prefixHistory", RemoteFileChooser.SELECT_DIR,
                        createFlowVariableModel("prefix", FlowVariable.Type.STRING), m_connectionInformation);
        // Target
        m_target = new FilesHistoryPanel("targetHistory", false);
        m_target.setSelectMode(JFileChooser.DIRECTORIES_ONLY);
        m_targetfvm = new FlowVariableModelButton(createFlowVariableModel("target", FlowVariable.Type.STRING));
        m_targetfvm.getFlowVariableModel().addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                enableComponents();
            }
        });
        // Subfolders
        m_subfolders = new JCheckBox("Download subfolders (if applicable)");
        // File filter
        m_useFilter = new JCheckBox("Only download files that match pattern");
        m_useFilter.addActionListener(new UpdateListener());
        m_filterType = new ButtonGroup();
        m_regularExpression = new JRadioButton(FilterType.REGULAREXPRESSION.getName());
        m_regularExpression.setActionCommand(FilterType.REGULAREXPRESSION.getName());
        m_regularExpression.addActionListener(new UpdateListener());
        m_wildcard = new JRadioButton(FilterType.WILDCARD.getName());
        m_wildcard.setActionCommand(FilterType.WILDCARD.getName());
        m_wildcard.addActionListener(new UpdateListener());
        m_filterType.add(m_regularExpression);
        m_filterType.add(m_wildcard);
        m_filterTypeLabel = new JLabel("Pattern is:");
        m_filterPattern = new JTextField();
        // Overwrite policy
        m_overwritePolicy = new ButtonGroup();
        m_overwrite = new JRadioButton(OverwritePolicy.OVERWRITE.getName());
        m_overwrite.setActionCommand(OverwritePolicy.OVERWRITE.getName());
        m_overwriteIfNewer = new JRadioButton(OverwritePolicy.OVERWRITEIFNEWER.getName());
        m_overwriteIfNewer.setActionCommand(OverwritePolicy.OVERWRITEIFNEWER.getName());
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
        // Path handling
        NodeUtils.resetGBC(gbc);
        gbc.insets = new Insets(0, 0, 0, 0);
        JPanel pathHandlingPanel = new JPanel(new GridBagLayout());
        pathHandlingPanel.add(m_fullpath, gbc);
        gbc.gridx++;
        pathHandlingPanel.add(m_onlyfilename, gbc);
        gbc.gridx++;
        pathHandlingPanel.add(m_truncate, gbc);
        pathHandlingPanel.setBorder(new TitledBorder(new EtchedBorder(), "Path handling"));
        // Target
        NodeUtils.resetGBC(gbc);
        JPanel targetPanel = new JPanel(new GridBagLayout());
        gbc.weightx = 1;
        targetPanel.add(m_target, gbc);
        gbc.weightx = 0;
        gbc.gridx++;
        gbc.insets = new Insets(5, 0, 5, 5);
        targetPanel.add(m_targetfvm, gbc);
        targetPanel.setBorder(new TitledBorder(new EtchedBorder(), "Target folder"));
        // Filter
        NodeUtils.resetGBC(gbc);
        JPanel filterPanel = new JPanel(new GridBagLayout());
        gbc.insets = new Insets(0, 0, 0, 0);
        filterPanel.add(m_useFilter, gbc);
        gbc.insets = new Insets(1, 50, 0, 0);
        gbc.gridy++;
        gbc.weightx = 1;
        filterPanel.add(m_filterPattern, gbc);
        gbc.gridy++;
        gbc.weightx = 0;
        filterPanel.add(m_filterTypeLabel, gbc);
        gbc.gridy++;
        filterPanel.add(m_regularExpression, gbc);
        gbc.gridy++;
        filterPanel.add(m_wildcard, gbc);
        // Overwrite policy
        NodeUtils.resetGBC(gbc);
        gbc.insets = new Insets(0, 0, 0, 0);
        JPanel overwritePolicyPanel = new JPanel(new GridBagLayout());
        overwritePolicyPanel.add(m_overwrite, gbc);
        gbc.gridx++;
        overwritePolicyPanel.add(m_overwriteIfNewer, gbc);
        gbc.gridx++;
        overwritePolicyPanel.add(m_abort, gbc);
        overwritePolicyPanel.setBorder(new TitledBorder(new EtchedBorder(), "If exists..."));
        // Outer panel
        NodeUtils.resetGBC(gbc);
        gbc.weightx = 1;
        panel.add(m_info, gbc);
        gbc.gridy++;
        panel.add(m_source.getPanel(), gbc);
        gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(pathHandlingPanel, gbc);
        gbc.gridy++;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(m_prefix.getPanel(), gbc);
        gbc.gridy++;
        panel.add(targetPanel, gbc);
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridy++;
        panel.add(m_subfolders, gbc);
        gbc.gridy++;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(filterPanel, gbc);
        gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(overwritePolicyPanel, gbc);
        return panel;
    }

    /**
     * Listener that updates the enabled state of the components.
     * 
     * 
     * @author Patrick Winter, KNIME.com, Zurich, Switzerland
     */
    private class UpdateListener implements ActionListener {

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
        boolean replacement = m_targetfvm.getFlowVariableModel().isVariableReplacementEnabled();
        boolean useFilter = m_useFilter.isSelected();
        m_prefix.setEnabled(usePrefix);
        m_target.setEnabled(!replacement);
        m_regularExpression.setEnabled(useFilter);
        m_wildcard.setEnabled(useFilter);
        m_filterTypeLabel.setEnabled(useFilter);
        m_filterPattern.setEnabled(useFilter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
            throws NotConfigurableException {
        // Check if a port object is available
        if (specs[0] == null) {
            throw new NotConfigurableException("No connection information available");
        }
        ConnectionInformationPortObjectSpec object = (ConnectionInformationPortObjectSpec)specs[0];
        m_connectionInformation = object.getConnectionInformation();
        // Check if the port object has connection information
        if (m_connectionInformation == null) {
            throw new NotConfigurableException("No connection information available");
        }
        m_source.setConnectionInformation(m_connectionInformation);
        m_prefix.setConnectionInformation(m_connectionInformation);
        m_info.setText("Download from: " + m_connectionInformation.toURI());
        // Load configuration
        DownloadConfiguration config = new DownloadConfiguration();
        config.load(settings);
        m_source.setSelection(config.getSource());
        m_target.setSelectedFile(config.getTarget());
        String overwritePolicy = config.getOverwritePolicy();
        if (overwritePolicy.equals(OverwritePolicy.OVERWRITE.getName())) {
            m_overwritePolicy.setSelected(m_overwrite.getModel(), true);
        } else if (overwritePolicy.equals(OverwritePolicy.OVERWRITEIFNEWER.getName())) {
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
        m_prefix.setSelection(config.getPrefix());
        m_subfolders.setSelected(config.getSubfolders());
        m_useFilter.setSelected(config.getUseFilter());
        String filterType = config.getFilterType();
        if (filterType.equals(FilterType.REGULAREXPRESSION.getName())) {
            m_filterType.setSelected(m_regularExpression.getModel(), true);
        } else if (filterType.equals(FilterType.WILDCARD.getName())) {
            m_filterType.setSelected(m_wildcard.getModel(), true);
        }
        m_filterPattern.setText(config.getFilterPattern());
        enableComponents();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        DownloadConfiguration config = new DownloadConfiguration();
        config.setTarget(m_target.getSelectedFile());
        config.setSource(m_source.getSelection());
        config.setOverwritePolicy(m_overwritePolicy.getSelection().getActionCommand());
        config.setPathHandling(m_pathhandling.getSelection().getActionCommand());
        config.setPrefix(m_prefix.getSelection());
        config.setSubfolders(m_subfolders.isSelected());
        config.setUseFilter(m_useFilter.isSelected());
        config.setFilterType(m_filterType.getSelection().getActionCommand());
        config.setFilterPattern(m_filterPattern.getText());
        config.save(settings);
    }
}
