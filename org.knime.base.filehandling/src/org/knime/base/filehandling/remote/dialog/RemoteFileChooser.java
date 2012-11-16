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

import java.awt.Component;
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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.knime.base.filehandling.remote.files.ConnectionMonitor;
import org.knime.base.filehandling.remote.files.RemoteFile;
import org.knime.base.filehandling.remote.files.RemoteFileFactory;
import org.knime.base.filehandling.remotecredentials.port.RemoteCredentials;

/**
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

    private JDialog m_dialog;

    private JTree m_tree;

    private int m_selectionType;

    private String m_selectedFile;

    /**
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
     * @return the selectedFile
     */
    public String getSelectedFile() {
        return m_selectedFile;
    }

    /**
     * @param parent Parent of this dialog
     */
    public void open(final Frame parent) {
        RemoteFile root = null;
        try {
            root = RemoteFileFactory.createRemoteFile(m_uri, m_credentials);
        } catch (Exception e) {
            // ignore
        }
        JPanel panel = initPanel(root);
        m_dialog = new JDialog(parent);
        m_dialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        resetGBC(gbc);
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        m_dialog.add(panel, gbc);
        m_dialog.setTitle("Files on server");
        m_dialog.pack();
        m_dialog.setModalityType(Dialog.DEFAULT_MODALITY_TYPE);
        m_dialog.setSize(400, 600);
        m_dialog.setVisible(true);
        m_dialog.dispose();
        ConnectionMonitor.closeAll();
    }

    private JPanel initPanel(final RemoteFile root) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        // Buttons
        resetGBC(gbc);
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
        resetGBC(gbc);
        gbc.weightx = 1;
        gbc.weighty = 1;
        try {
            RemoteFileTreeNode rootNode = new RemoteFileTreeNode(root);
            m_tree = new JTree(rootNode);
            m_tree.getSelectionModel().setSelectionMode(
                    TreeSelectionModel.SINGLE_TREE_SELECTION);
            m_tree.setCellRenderer(new RemoteFileTreeCellRenderer());
            panel.add(new JScrollPane(m_tree), gbc);
        } catch (Exception e) {
            // ignore
        }
        gbc.weighty = 0;
        gbc.gridy++;
        panel.add(buttonPanel, gbc);
        return panel;
    }

    private void resetGBC(final GridBagConstraints gbc) {
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridx = 0;
        gbc.gridy = 0;
    }

    private class ButtonListener implements ActionListener {

        @Override
        public void actionPerformed(final ActionEvent e) {
            String action = e.getActionCommand();
            if (action.equals("ok")) {
                TreePath[] paths =
                        m_tree.getSelectionModel().getSelectionPaths();
                if (paths.length > 0) {
                    RemoteFile file =
                            (RemoteFile)((RemoteFileTreeNode)paths[0]
                                    .getLastPathComponent()).getUserObject();
                    try {
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
                        if (typeOk) {
                            saveAndQuit(file);
                        }
                    } catch (Exception ex) {
                        // do not save or close
                    }
                }
            } else if (action.equals("cancel")) {
                m_dialog.dispose();
                ConnectionMonitor.closeAll();
            }
        }

        private void saveAndQuit(final RemoteFile file) {
            try {
                m_selectedFile = file.getFullName();
                m_dialog.dispose();
                ConnectionMonitor.closeAll();
            } catch (Exception e) {
                // do not close in case of exception
            }
        }
    }

    private class RemoteFileTreeNode extends DefaultMutableTreeNode {

        /**
         * 
         */
        private static final long serialVersionUID = 1215339655731965368L;

        private boolean m_loaded;

        /**
         * @param object The user object
         * 
         */
        public RemoteFileTreeNode(final Object object) {
            super(object);
            m_loaded = false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isLeaf() {
            boolean result = true;
            try {
                result = !((RemoteFile)getUserObject()).isDirectory();
            } catch (Exception e) {
                // ignore
            }
            return result;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getChildCount() {
            if (!m_loaded) {
                loadChildren();
            }
            return super.getChildCount();
        }

        private void loadChildren() {
            m_loaded = true;
            try {
                RemoteFile[] files = ((RemoteFile)getUserObject()).listFiles();
                for (int i = 0; i < files.length; i++) {
                    add(new RemoteFileTreeNode(files[i]));
                }
            } catch (Exception e) {
                // ignore
            }
        }
    }

    private class RemoteFileTreeCellRenderer extends DefaultTreeCellRenderer {

        /**
         * 
         */
        private static final long serialVersionUID = 1891399815069956785L;

        /**
         * {@inheritDoc}
         */
        @Override
        public Component getTreeCellRendererComponent(final JTree tree,
                final Object value, final boolean sel, final boolean expanded,
                final boolean leaf, final int row, final boolean hasFocus1) {
            JLabel renderer =
                    (JLabel)super.getTreeCellRendererComponent(tree, value,
                            sel, expanded, leaf, row, hasFocus1);
            RemoteFile file =
                    (RemoteFile)((DefaultMutableTreeNode)value).getUserObject();
            try {
                if (file.isDirectory()) {
                    if (expanded) {
                        renderer.setIcon(openIcon);
                    } else {
                        renderer.setIcon(closedIcon);
                    }
                }
            } catch (Exception e) {
                // ignore
            }
            return renderer;
        }

    }

}
