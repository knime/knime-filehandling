/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by 
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
package org.knime.base.filehandling.listdirectory;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
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
public class ListDirectoryNodeDialog extends NodeDialogPane {

    private ConnectionInformation m_connectionInformation;

    private JLabel m_info;

    private RemoteFileChooserPanel m_directory;

    private JCheckBox m_recursive;

    private FilesHistoryPanel m_localdirectory;

    private FlowVariableModelButton m_directoryfvm;

    private JPanel m_localdirectoryPanel;

    /**
     * New pane for configuring the node dialog.
     */
    public ListDirectoryNodeDialog() {
        // Info
        m_info = new JLabel();
        // Directory (remote location)
        m_directoryfvm = new FlowVariableModelButton(createFlowVariableModel("directory", FlowVariable.Type.STRING));
        m_directory =
                new RemoteFileChooserPanel(getPanel(), "Directory", true, "directoryHistory",
                        RemoteFileChooser.SELECT_DIR, m_directoryfvm.getFlowVariableModel(), m_connectionInformation);
        // Directory (local location)
        m_localdirectory = new FilesHistoryPanel("localDirectoryHistory", false);
        m_localdirectory.setSelectMode(JFileChooser.DIRECTORIES_ONLY);
        m_directoryfvm.getFlowVariableModel().addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                boolean replacement = m_directoryfvm.getFlowVariableModel().isVariableReplacementEnabled();
                m_localdirectory.setEnabled(!replacement);
            }
        });
        // Recursive
        m_recursive = new JCheckBox("Recursive");
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
        // Directory
        NodeUtils.resetGBC(gbc);
        m_localdirectoryPanel = new JPanel(new GridBagLayout());
        gbc.weightx = 1;
        m_localdirectoryPanel.add(m_localdirectory, gbc);
        gbc.weightx = 0;
        gbc.gridx++;
        gbc.insets = new Insets(5, 0, 5, 5);
        m_localdirectoryPanel.add(m_directoryfvm, gbc);
        m_localdirectoryPanel.setBorder(new TitledBorder(new EtchedBorder(), "Directory"));
        // Outer panel
        NodeUtils.resetGBC(gbc);
        gbc.weightx = 1;
        panel.add(m_info, gbc);
        gbc.gridy++;
        panel.add(m_directory.getPanel(), gbc);
        gbc.gridy++;
        panel.add(m_localdirectoryPanel, gbc);
        gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridy++;
        panel.add(m_recursive, gbc);
        return panel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
            throws NotConfigurableException {
        // Check if a port object is available
        if (specs[0] != null) {
            ConnectionInformationPortObjectSpec object = (ConnectionInformationPortObjectSpec)specs[0];
            m_connectionInformation = object.getConnectionInformation();
            // Check if the port object has connection information
            if (m_connectionInformation == null) {
                throw new NotConfigurableException("No connection information available");
            }
            m_info.setText("List from: " + m_connectionInformation.toURI());
        } else {
            m_connectionInformation = null;
            m_info.setText("List from: local maschine");
        }
        // Show only one of the location panels
        m_directory.getPanel().setVisible(m_connectionInformation != null);
        m_localdirectoryPanel.setVisible(m_connectionInformation == null);
        m_directory.setConnectionInformation(m_connectionInformation);
        // Load configuration
        ListDirectoryConfiguration config = new ListDirectoryConfiguration();
        config.load(settings);
        m_directory.setSelection(config.getDirectory());
        m_localdirectory.setSelectedFile(config.getDirectory());
        m_recursive.setSelected(config.getRecursive());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        ListDirectoryConfiguration config = new ListDirectoryConfiguration();
        // Set setting only from the correct panel
        if (m_connectionInformation != null) {
            config.setDirectory(m_directory.getSelection());
        } else {
            config.setDirectory(m_localdirectory.getSelectedFile());
        }
        config.setRecursive(m_recursive.isSelected());
        config.save(settings);
    }
}
