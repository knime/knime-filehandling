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
 *   Nov 28, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.remote.connectioninformation.node;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import org.knime.base.filehandling.NodeUtils;
import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformation;
import org.knime.base.filehandling.remote.files.ConnectionMonitor;
import org.knime.base.filehandling.remote.files.RemoteFileFactory;
import org.knime.core.node.NodeLogger;

/**
 * 
 * @author Patrick Winter, KNIME.com, Zurich, Switzerland
 */
public class TestConnectionDialog {

    private final ConnectionInformation m_connectionInformation;

    private JDialog m_dialog;

    private ConnectionMonitor m_monitor;

    private JLabel m_info;

    private JProgressBar m_progress;

    private JButton m_button1;

    private JButton m_cancel;

    private Dimension m_buttonSize;

    /**
     * Create a test connection dialog.
     * 
     * 
     * @param connectionInformation The connection information to test
     */
    public TestConnectionDialog(
            final ConnectionInformation connectionInformation) {
        m_connectionInformation = connectionInformation;
    }

    /**
     * Open the dialog.
     * 
     * 
     * @param parent Parent frame of this dialog
     */
    public void open(final Frame parent) {
        m_monitor = new ConnectionMonitor();
        JPanel panel = initPanel();
        // Create dialog
        m_dialog = new JDialog(parent);
        m_dialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        NodeUtils.resetGBC(gbc);
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        m_dialog.add(panel, gbc);
        m_dialog.setTitle("Test connection");
        m_dialog.pack();
        m_dialog.setModalityType(Dialog.DEFAULT_MODALITY_TYPE);
        m_dialog.setSize(600, 110);
        tryConnection();
        m_dialog.setVisible(true);
        // Stops here as long as the dialog is open, then disposes of the
        // dialog
        m_dialog.dispose();
        // Close used connections
        m_monitor.closeAll();
    }

    /**
     * Create and initialize the panel of this dialog.
     * 
     * 
     * @return The initialized panel
     */
    private JPanel initPanel() {
        GridBagConstraints gbc = new GridBagConstraints();
        // Info
        NodeUtils.resetGBC(gbc);
        JPanel infoPanel = new JPanel(new GridBagLayout());
        m_info = new JLabel();
        m_progress = new JProgressBar();
        m_progress.setIndeterminate(true);
        m_progress.setPreferredSize(new Dimension(50, 10));
        gbc.insets = new Insets(0, 5, 0, 5);
        infoPanel.add(m_progress, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        infoPanel.add(m_info, gbc);
        // Buttons
        NodeUtils.resetGBC(gbc);
        JPanel buttonPanel = new JPanel(new GridBagLayout());
        m_buttonSize = new JButton("Try again").getPreferredSize();
        m_button1 = new JButton();
        m_button1.addActionListener(new ButtonListener());
        m_cancel = new JButton("Cancel");
        m_cancel.setActionCommand("cancel");
        m_cancel.addActionListener(new ButtonListener());
        m_cancel.setPreferredSize(m_buttonSize);
        buttonPanel.add(m_button1, gbc);
        gbc.gridx++;
        buttonPanel.add(m_cancel, gbc);
        // Outer panel
        JPanel panel = new JPanel(new GridBagLayout());
        NodeUtils.resetGBC(gbc);
        gbc.weightx = 1;
        panel.add(infoPanel, gbc);
        gbc.gridy++;
        panel.add(buttonPanel, gbc);
        return panel;
    }

    /**
     * Try if the connection works and change dialog accordingly.
     */
    private void tryConnection() {
        m_button1.setText("OK");
        m_button1.setPreferredSize(m_buttonSize);
        m_button1.setActionCommand("ok");
        m_button1.setEnabled(false);
        m_info.setText("Testing connection to "
                + m_connectionInformation.toURI());
        m_progress.setVisible(true);
        new TestWorker().execute();
    }

    /**
     * Listener that performs the appropriate actions for button clicks.
     */
    private class ButtonListener implements ActionListener {

        @Override
        public void actionPerformed(final ActionEvent e) {
            // Get action of the button
            String action = e.getActionCommand();
            if (action.equals("ok") || action.equals("cancel")) {
                // Close dialog on ok or cancel
                m_dialog.dispose();
                m_monitor.closeAll();
            } else if (action.equals("tryagain")) {
                // Try connection again
                tryConnection();
            }
        }
    }

    /**
     * Swing worker that tests the connection information.
     * 
     * 
     * @author Patrick Winter, KNIME.com, Zurich, Switzerland
     */
    private class TestWorker extends SwingWorker<Void, Void> {

        private boolean m_success;

        private String m_error;

        /**
         * {@inheritDoc}
         */
        @Override
        protected Void doInBackground() throws Exception {
            try {
                ConnectionMonitor monitor = new ConnectionMonitor();
                RemoteFileFactory.createRemoteFile(
                        m_connectionInformation.toURI(),
                        m_connectionInformation, monitor);
                monitor.closeAll();
                m_success = true;
            } catch (Exception e) {
                NodeLogger.getLogger(getClass()).warn("Couldn't connect", e);
                m_error = e.getMessage();
                m_success = false;
            }
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void done() {
            m_progress.setVisible(false);
            m_button1.setEnabled(true);
            if (m_success) {
                m_info.setText("Connection to "
                        + m_connectionInformation.toURI() + " succeeded");
            } else {
                m_info.setText("<html>Connection to "
                        + m_connectionInformation.toURI() + " failed<br />"
                        + m_error + "</html>");
                m_button1.setText("Try again");
                m_button1.setPreferredSize(m_buttonSize);
                m_button1.setActionCommand("tryagain");
            }
        }

    }

}
