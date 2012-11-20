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
 *   Nov 16, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.remote.dialog;

import java.awt.Container;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.filehandling.NodeUtils;
import org.knime.base.filehandling.remotecredentials.port.RemoteCredentials;
import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.FlowVariableModelButton;
import org.knime.core.node.util.StringHistory;

/**
 * Panel to choose a file from a remote location.
 * 
 * 
 * @author Patrick Winter, University of Konstanz
 */
public class RemoteFileChooserPanel {

    private RemoteCredentials m_credentials;

    private String m_historyID;

    private String m_hostSpecificID;

    private JPanel m_panel;

    private JComboBox<String> m_combobox;

    private JButton m_button;

    private FlowVariableModelButton m_fvmbutton;

    /**
     * Create panel.
     * 
     * 
     * @param parentPanel The parent of this panel
     * @param label Label of the file chooser
     * @param border If a border should be used
     * @param historyID ID for history persistence
     * @param selectionMode Select files, directories or both
     * @param fvm Model for the flow variable button
     * @param credentials Credentials for the remote connection
     */
    public RemoteFileChooserPanel(final JPanel parentPanel, final String label,
            final boolean border, final String historyID,
            final int selectionMode, final FlowVariableModel fvm,
            final RemoteCredentials credentials) {
        m_credentials = credentials;
        m_hostSpecificID = historyID;
        m_historyID = historyID;
        // Combobox
        m_combobox = new JComboBox<String>(new String[0]);
        m_combobox.setEditable(true);
        // Browse button
        m_button = new JButton("Browse");
        m_button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                Frame frame = null;
                Container container = parentPanel.getParent();
                while (container != null) {
                    if (container instanceof Frame) {
                        frame = (Frame)container;
                        break;
                    }
                    container = container.getParent();
                }
                RemoteFileChooser dialog =
                        new RemoteFileChooser(m_credentials.toURI(),
                                m_credentials, selectionMode);
                dialog.open(frame);
                String selected = dialog.getSelectedFile();
                if (selected != null) {
                    StringHistory.getInstance(m_hostSpecificID).add(selected);
                    setSelection(selected);
                    updateHistory();
                }
            }
        });
        // Flow variable model
        fvm.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                setEnabled(m_panel.isEnabled());
            }
        });
        m_fvmbutton = new FlowVariableModelButton(fvm);
        // Outer panel
        m_panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        if (border) {
            m_panel.setBorder(new TitledBorder(new EtchedBorder(), label));
        } else if (!label.equals("")) {
            m_panel.setBorder(new TitledBorder(label));
        }
        NodeUtils.resetGBC(gbc);
        gbc.insets = new Insets(5, 5, 5, 0);
        gbc.weightx = 1;
        m_panel.add(m_combobox, gbc);
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        ++gbc.gridx;
        m_panel.add(m_button, gbc);
        gbc.insets = new Insets(5, 5, 5, 5);
        ++gbc.gridx;
        m_panel.add(m_fvmbutton, gbc);
        // Initialize enabled states
        setEnabled(true);
    }

    /**
     * Set the credentials that will be used for this connection.
     * 
     * 
     * @param credentials The credentials for the connection.
     */
    public void setCredentials(final RemoteCredentials credentials) {
        // Build specific history id by using the host information and the
        // history id
        m_hostSpecificID =
                credentials.toURI().toString().replaceAll("[/@:?&#]", "")
                        + m_historyID;
        updateHistory();
        m_credentials = credentials;
    }

    /**
     * Set the selection of this file chooser.
     * 
     * 
     * @param selection The new selection
     */
    public void setSelection(final String selection) {
        m_combobox.setSelectedItem(selection);
    }

    /**
     * Get the current selection.
     * 
     * 
     * @return The current selection
     */
    public String getSelection() {
        return (String)m_combobox.getSelectedItem();
    }

    /**
     * Get the panel.
     * 
     * 
     * @return The panel
     */
    public JPanel getPanel() {
        return m_panel;
    }

    /**
     * Enable or disable this panel.
     * 
     * 
     * @param enabled If the panel should be enabled
     */
    public void setEnabled(final boolean enabled) {
        // Some components will only be enabled if replacement is not enabled
        boolean replacement =
                m_fvmbutton.getFlowVariableModel()
                        .isVariableReplacementEnabled();
        m_panel.setEnabled(enabled);
        m_combobox.setEnabled(enabled && !replacement);
        m_button.setEnabled(enabled && !replacement);
        m_fvmbutton.setEnabled(enabled);
    }

    /**
     * Update the history of the combo box.
     */
    private void updateHistory() {
        // Get history
        StringHistory history = StringHistory.getInstance(m_hostSpecificID);
        // Get values
        String[] strings = history.getHistory();
        // Make values unique through use of set
        Set<String> set = new LinkedHashSet<String>();
        for (int i = 0; i < strings.length; i++) {
            set.add(strings[i]);
        }
        // Remove old elements
        DefaultComboBoxModel<String> model =
                (DefaultComboBoxModel<String>)m_combobox.getModel();
        model.removeAllElements();
        // Add new elements
        for (String string : set) {
            model.addElement(string);
        }
    }

}
