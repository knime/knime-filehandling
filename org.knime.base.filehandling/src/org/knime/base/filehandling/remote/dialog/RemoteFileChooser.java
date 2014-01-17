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
 *   Nov 13, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.remote.dialog;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.knime.base.filehandling.NodeUtils;
import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformation;
import org.knime.base.filehandling.remote.files.ConnectionMonitor;
import org.knime.base.filehandling.remote.files.RemoteFile;
import org.knime.base.filehandling.remote.files.RemoteFileFactory;
import org.knime.core.util.SwingWorkerWithContext;

/**
 * Dialog that presents the file structure of a remote folder in a tree.
 *
 *
 * @author Patrick Winter, KNIME.com, Zurich, Switzerland
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

    /**
     * Loading message.
     */
    private static final String LOADING = "Loading...";

    private URI m_uri;

    private ConnectionInformation m_connectionInformation;

    private ConnectionMonitor m_connectionMonitor;

    private Frame m_parent;

    private int m_selectionType;

    private String m_selectedFile;

    private JDialog m_dialog;

    private JTree m_tree;

    private DefaultTreeModel m_treemodel;

    private JLabel m_info;

    private JProgressBar m_progress;

    private List<RemoteFileTreeNodeWorker> m_workers;

    private boolean m_closed;

    /**
     * Creates remote file chooser for the specified folder.
     *
     *
     * @param uri The URI
     * @param connectionInformation Connection information to the URI
     * @param selectionType Whether a file or a directory should be selected
     */
    public RemoteFileChooser(final URI uri, final ConnectionInformation connectionInformation, final int selectionType) {
        m_uri = uri;
        m_connectionInformation = connectionInformation;
        m_connectionMonitor = new ConnectionMonitor();
        m_selectionType = selectionType;
        m_selectedFile = null;
        m_workers = new LinkedList<RemoteFileTreeNodeWorker>();
        m_closed = false;
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
        m_parent = parent;
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
        m_dialog.setTitle("Files on " + m_uri);
        m_dialog.pack();
        m_dialog.setModalityType(Dialog.DEFAULT_MODALITY_TYPE);
        m_dialog.setSize(400, 600);
        m_dialog.setVisible(true);
        // Stops here as long as the dialog is open, then disposes of the
        // dialog
        m_dialog.dispose();
        // Close used connections
        m_connectionMonitor.closeAll();
        m_closed = true;
    }

    /**
     * Initializes the panel of this dialog.
     *
     *
     * @return Panel with all components of this dialog
     */
    private JPanel initPanel() {
        GridBagConstraints gbc = new GridBagConstraints();
        // Info
        NodeUtils.resetGBC(gbc);
        JPanel infoPanel = new JPanel(new GridBagLayout());
        m_info = new JLabel(LOADING);
        m_progress = new JProgressBar();
        m_progress.setIndeterminate(true);
        m_progress.setPreferredSize(new Dimension(50, 10));
        gbc.insets = new Insets(0, 5, 0, 5);
        infoPanel.add(m_progress, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        infoPanel.add(m_info, gbc);
        // Tree
        // Create tree that does not display anything
        m_tree = new JTree(new DefaultMutableTreeNode());
        m_tree.setRootVisible(false);
        m_tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        // Init worker will load the root of the tree
        new InitWorker().execute();
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
        panel.add(infoPanel, gbc);
        gbc.gridy++;
        panel.add(buttonPanel, gbc);
        return panel;
    }

    /**
     * Will set the message to the default.
     *
     *
     * The default message will prompt the user to select the expected type of
     * file.
     */
    private void setDefaultMessage() {
        String message = "Select a ";
        switch (m_selectionType) {
        case SELECT_DIR:
            message += "directory";
            break;
        case SELECT_FILE:
            message += "file";
            break;
        case SELECT_FILE_OR_DIR:
            message += "file or directory";
            break;
        default:
            message += "file or directory";
            break;
        }
        m_info.setText(message);
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
                TreePath[] paths = m_tree.getSelectionModel().getSelectionPaths();
                if (paths.length > 0) {
                    // Get file from selected node (single selection)
                    RemoteFile file = (RemoteFile)((RemoteFileTreeNode)paths[0].getLastPathComponent()).getUserObject();
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
                        default:
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
                m_connectionMonitor.closeAll();
                m_closed = true;
            }
        }

        private void saveAndClose(final RemoteFile file) {
            try {
                // Save path of the selected file in variable
                m_selectedFile = file.getFullName();
                // Close dialog and connections
                m_dialog.dispose();
                m_connectionMonitor.closeAll();
                m_closed = true;
            } catch (Exception e) {
                // do not close in case of exception
            }
        }
    }

    /**
     * Worker that loads the root node and initializes the tree with it.
     *
     *
     * @author Patrick Winter, KNIME.com, Zurich, Switzerland
     */
    private class InitWorker extends SwingWorkerWithContext<Void, Void> {

        private boolean m_success;

        private boolean m_dir;

        /**
         * {@inheritDoc}
         */
        @Override
        protected Void doInBackgroundWithContext() throws Exception {
            m_success = false;
            // Create remote file to the root of the tree
            RemoteFile root = RemoteFileFactory.createRemoteFile(m_uri, m_connectionInformation, m_connectionMonitor);
            m_dir = root.isDirectory();
            RemoteFileTreeNode rootNode = new RemoteFileTreeNode(root);
            // Create tree model
            m_treemodel = new DefaultTreeModel(rootNode);
            m_success = true;
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void doneWithContext() {
            if (m_success && m_dir) {
                // Remove loading message
                m_progress.setVisible(false);
                setDefaultMessage();
                // Initialize tree
                m_tree.setModel(m_treemodel);
                m_tree.setRootVisible(true);
            } else {
                // Close dialog and used connections
                m_dialog.dispose();
                m_connectionMonitor.closeAll();
                if (!m_closed) {
                    String message;
                    if (!m_success) {
                        message = "Could not connect to " + m_uri;
                    } else {
                        message = m_uri + " is not browsable";
                    }
                    // Show error about the connection problem
                    JOptionPane.showMessageDialog(m_parent, message, "Error", JOptionPane.ERROR_MESSAGE);
                }
                m_closed = true;
            }
        }

    }

    /**
     * Swing worker that loads the children of a remote file node in the
     * background.
     *
     *
     * @author Patrick Winter, KNIME.com, Zurich, Switzerland
     */
    private class RemoteFileTreeNodeWorker extends SwingWorkerWithContext<Void, Void> {

        private boolean m_success;

        private RemoteFileTreeNode m_parentNode;

        private RemoteFileTreeNode[] m_nodes;

        public RemoteFileTreeNodeWorker(final RemoteFileTreeNode node) {
            m_parentNode = node;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Void doInBackgroundWithContext() throws Exception {
            m_success = false;
            // List files in directory
            RemoteFile[] files = ((RemoteFile)m_parentNode.getUserObject()).listFiles();
            m_nodes = new RemoteFileTreeNode[files.length];
            // Create node for each file
            for (int i = 0; i < files.length; i++) {
                m_nodes[i] = new RemoteFileTreeNode(files[i]);
            }
            m_success = true;
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void doneWithContext() {
            if (m_success) {
                // Add all nodes to the parent
                for (int i = 0; i < m_nodes.length; i++) {
                    m_treemodel.insertNodeInto(m_nodes[i], m_parentNode, i);
                }
                // Remove this worker
                m_workers.remove(0);
                // Look for other workers
                if (m_workers.size() > 0) {
                    // Start next worker
                    m_workers.get(0).execute();
                } else {
                    // Remove loading message
                    m_progress.setVisible(false);
                    setDefaultMessage();
                }
            } else {
                // Close dialog and used connections
                m_dialog.dispose();
                m_connectionMonitor.closeAll();
                if (!m_closed) {
                    // Show error about the connection problem
                    JOptionPane.showMessageDialog(m_parent, "Connection to " + m_uri + " lost", "No connection",
                            JOptionPane.ERROR_MESSAGE);
                }
                m_closed = true;
            }
        }

    }

    /**
     * Extended tree node that uses lazy loading.
     *
     *
     * @author Patrick Winter, KNIME.com, Zurich, Switzerland
     */
    private class RemoteFileTreeNode extends DefaultMutableTreeNode {

        /**
         * Serial ID.
         */
        private static final long serialVersionUID = 1215339655731965368L;

        private boolean m_loaded;

        private String m_name;

        private boolean m_isDirectory;

        /**
         * Create a tree node to a remote file.
         *
         *
         * @param file The remote file
         * @throws Exception If the file could not be accessed
         */
        public RemoteFileTreeNode(final RemoteFile file) throws Exception {
            super(file);
            m_loaded = false;
            // Get information needed for the node in creation time
            m_name = file.getName();
            m_isDirectory = file.isDirectory();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isLeaf() {
            return !m_isDirectory;
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
            // Create worker to load the children
            RemoteFileTreeNodeWorker worker = new RemoteFileTreeNodeWorker(this);
            // Add worker to list
            m_workers.add(worker);
            // If list does not hold more than this worker, start it
            if (m_workers.size() < 2) {
                // Set loading message and show progress bar
                m_progress.setVisible(true);
                m_info.setText(LOADING);
                // Start worker
                worker.execute();
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return m_name;
        }
    }

}
