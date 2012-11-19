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
 *   Nov 13, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.remote.dialog;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.knime.base.filehandling.NodeUtils;
import org.knime.base.filehandling.remote.files.ConnectionMonitor;
import org.knime.base.filehandling.remote.files.RemoteFile;
import org.knime.base.filehandling.remote.files.RemoteFileFactory;
import org.knime.base.filehandling.remotecredentials.port.RemoteCredentials;

/**
 * Dialog that presents the file structure of a remote folder in a tree.
 * 
 * 
 * @author Patrick Winter, University of Konstanz
 */
public final class RemoteFileChooser {

    /**
     * Select a file.
     */
    public static final int SELECT_FILE = 0;

    /**
     * Select a directory.
     */
    public static final int SELECT_DIR = 1;

    /**
     * Select a file or a directory.
     */
    public static final int SELECT_FILE_OR_DIR = 2;

    private URI m_uri;

    private RemoteCredentials m_credentials;

    private int m_selectionType;

    private String m_selectedFile;

    private JDialog m_dialog;

    private JTree m_tree;

    /**
     * Creates remote file chooser for the specified folder.
     * 
     * 
     * @param uri The URI
     * @param credentials Credentials to the URI
     * @param selectionType Whether a file or a directory should be selected
     */
    public RemoteFileChooser(final URI uri,
            final RemoteCredentials credentials, final int selectionType) {
        m_uri = uri;
        m_credentials = credentials;
        m_selectionType = selectionType;
        m_selectedFile = null;
    }

    /**
     * Get the selected file.
     * 
     * 
     * Returns null if no selection has been made
     * 
     * @return the selectedFile or null if no file has been selected
     */
    public String getSelectedFile() {
        return m_selectedFile;
    }

    /**
     * Opens the actual dialog window.
     * 
     * 
     * @param parent Parent of this dialog
     */
    public void open(final Frame parent) {
        try {
            // Create remote file to the root of the tree
            RemoteFile root =
                    RemoteFileFactory.createRemoteFile(m_uri, m_credentials);
            JPanel panel = initPanel(root);
            // Create dialog
            m_dialog = new JDialog(parent);
            m_dialog.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            NodeUtils.resetGBC(gbc);
            gbc.weightx = 1;
            gbc.weighty = 1;
            gbc.insets = new Insets(0, 0, 0, 0);
            m_dialog.add(panel, gbc);
            m_dialog.setTitle("Files on server");
            m_dialog.pack();
            m_dialog.setModalityType(Dialog.DEFAULT_MODALITY_TYPE);
            m_dialog.setSize(400, 600);
            m_dialog.setVisible(true);
            // Stops here as long as the dialog is open, then disposes of the
            // dialog
            m_dialog.dispose();
        } catch (Exception e) {
            // Show error if connection problem
            JOptionPane.showMessageDialog(parent, "Could not connect to "
                    + m_uri, "No connection", JOptionPane.ERROR_MESSAGE);
        }
        // Close used connections
        ConnectionMonitor.closeAll();
    }

    /**
     * Initializes the panel of this dialog.
     * 
     * 
     * @param root Root directory of the tree
     * @return Panel with all components of this dialog
     */
    private JPanel initPanel(final RemoteFile root) throws Exception {
        GridBagConstraints gbc = new GridBagConstraints();
        // Tree
        RemoteFileTreeNode rootNode = new RemoteFileTreeNode(root);
        m_tree = new JTree(rootNode);
        m_tree.getSelectionModel().setSelectionMode(
                TreeSelectionModel.SINGLE_TREE_SELECTION);
        // Buttons
        NodeUtils.resetGBC(gbc);
        JPanel buttonPanel = new JPanel(new GridBagLayout());
        JButton ok = new JButton("OK");
        ok.setActionCommand("ok");
        ok.addActionListener(new ButtonListener());
        JButton cancel = new JButton("Cancel");
        cancel.setActionCommand("cancel");
        cancel.addActionListener(new ButtonListener());
        ok.setPreferredSize(cancel.getPreferredSize());
        buttonPanel.add(ok, gbc);
        gbc.gridx++;
        buttonPanel.add(cancel, gbc);
        // Outer panel
        JPanel panel = new JPanel(new GridBagLayout());
        NodeUtils.resetGBC(gbc);
        gbc.weightx = 1;
        gbc.weighty = 1;
        panel.add(new JScrollPane(m_tree), gbc);
        gbc.weighty = 0;
        gbc.gridy++;
        panel.add(buttonPanel, gbc);
        return panel;
    }

    /**
     * Listener that performs the appropriate actions for button clicks.
     */
    private class ButtonListener implements ActionListener {

        @Override
        public void actionPerformed(final ActionEvent e) {
            // Get action of the button
            String action = e.getActionCommand();
            if (action.equals("ok")) {
                // Get selected nodes
                TreePath[] paths =
                        m_tree.getSelectionModel().getSelectionPaths();
                if (paths.length > 0) {
                    // Get file from selected node (single selection)
                    RemoteFile file =
                            (RemoteFile)((RemoteFileTreeNode)paths[0]
                                    .getLastPathComponent()).getUserObject();
                    try {
                        // Check if the selection has the correct type
                        boolean typeOk = false;
                        switch (m_selectionType) {
                        case SELECT_DIR:
                            typeOk = file.isDirectory();
                            break;
                        case SELECT_FILE:
                            typeOk = !file.isDirectory();
                            break;
                        case SELECT_FILE_OR_DIR:
                            typeOk = true;
                            break;
                        }
                        // Save and close only if the type is correct
                        if (typeOk) {
                            saveAndClose(file);
                        }
                    } catch (Exception ex) {
                        // do not save or close
                    }
                }
            } else if (action.equals("cancel")) {
                // Close dialog on cancel
                m_dialog.dispose();
                ConnectionMonitor.closeAll();
            }
        }

        private void saveAndClose(final RemoteFile file) {
            try {
                // Save path of the selected file in variable
                m_selectedFile = file.getFullName();
                // Close dialog and connections
                m_dialog.dispose();
                ConnectionMonitor.closeAll();
            } catch (Exception e) {
                // do not close in case of exception
            }
        }
    }

    /**
     * Extended tree node that uses lazy loading.
     * 
     * 
     * @author Patrick Winter, University of Konstanz
     */
    private class RemoteFileTreeNode extends DefaultMutableTreeNode {

        /**
         * Serial ID.
         */
        private static final long serialVersionUID = 1215339655731965368L;

        private boolean m_loaded;

        /**
         * Create a tree node to a remote file.
         * 
         * 
         * @param file The remote file
         */
        public RemoteFileTreeNode(final RemoteFile file) {
            super(file);
            m_loaded = false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isLeaf() {
            boolean result = true;
            try {
                // If the file is a directory than the node is no leaf
                result = !((RemoteFile)getUserObject()).isDirectory();
            } catch (Exception e) {
                // in case of exception the node is a leaf
            }
            return result;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getChildCount() {
            // When this method gets loaded the children have to be initialized
            // Load children if this is not done yet
            if (!m_loaded) {
                loadChildren();
            }
            return super.getChildCount();
        }

        /**
         * Loads the children of this node by using the list files ability of
         * the remote file.
         */
        private void loadChildren() {
            m_loaded = true;
            try {
                // List files in directory
                RemoteFile[] files = ((RemoteFile)getUserObject()).listFiles();
                // Add every file to the node
                for (int i = 0; i < files.length; i++) {
                    add(new RemoteFileTreeNode(files[i]));
                }
            } catch (Exception e) {
                // do not add anything
            }
        }
    }

}
