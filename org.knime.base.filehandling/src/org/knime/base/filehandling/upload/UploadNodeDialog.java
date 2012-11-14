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

import java.awt.Container;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.knime.base.filehandling.remote.dialog.RemoteFileChooser;
import org.knime.base.filehandling.remotecredentials.port.RemoteCredentials;
import org.knime.base.filehandling.remotecredentials.port.RemoteCredentialsPortObjectSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

/**
 * <code>NodeDialog</code> for the node.
 * 
 * 
 * @author Patrick Winter, University of Konstanz
 */
public class UploadNodeDialog extends NodeDialogPane {

    private RemoteCredentials m_credentials;

    private JButton m_target;

    /**
     * New pane for configuring the node dialog.
     */
    public UploadNodeDialog() {
        m_target = new JButton("Browse");
        m_target.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                // Open dialog
                Frame frame = null;
                Container container = getPanel().getParent();
                while (container != null) {
                    if (container instanceof Frame) {
                        frame = (Frame)container;
                        break;
                    }
                    container = container.getParent();
                }
                RemoteFileChooser dialog =
                        new RemoteFileChooser(m_credentials.toURI(),
                                m_credentials, RemoteFileChooser.SELECT_DIR);
                dialog.open(frame);
            }
        });
        addTab("Options", initLayout());
    }

    private JPanel initLayout() {
        JPanel panel = new JPanel();
        panel.add(m_target);
        return panel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        RemoteCredentialsPortObjectSpec object =
                (RemoteCredentialsPortObjectSpec)specs[0];
        m_credentials = object.getCredentials();
        if (m_credentials == null) {
            throw new NotConfigurableException("No credentials available");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        //
    }
}