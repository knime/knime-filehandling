/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 */
package org.knime.base.filehandling.remote.dialog;

import static java.util.Objects.requireNonNull;

import java.awt.Component;
import java.util.Objects;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.Spring;
import javax.swing.SpringLayout;
import javax.swing.SpringLayout.Constraints;

import org.apache.commons.lang.StringUtils;
import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformation;
import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformationPortObjectSpec;
import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Dialog component to choose a file from a remote location.
 *
 * @author Noemi Balassa
 * @since 4.0
 */
public class DialogComponentRemoteFileChooser extends DialogComponent {

    private final String m_label;

    private final int m_portIndex;

    private final RemoteFileChooserPanel m_remoteFileChooserPanel;

    /**
     * Constructs a {@link DialogComponentRemoteFileChooser} object.
     *
     * @param model the string model for storing the current selection.
     * @param label the label of the file chooser to show on the border, if enabled, and in messages.
     * @param border the indicator of whether to show the border.
     * @param portIndex the index of the port to check for connection information specification. A negative value
     *            indicates not to try getting the specification from any of the input ports, which always disables
     *            browsing.
     * @param historyId the identifier for the persistent history.
     * @param selectionMode the mode allowing the selection of a file, directory, or both.
     * @param flowVariableModel the model for the flow variable button.
     * @throws IllegalArgumentException if {@code label} is <em>blank</em>.
     * @see RemoteFileChooser#SELECT_FILE
     * @see RemoteFileChooser#SELECT_DIR
     * @see RemoteFileChooser#SELECT_FILE_OR_DIR
     */
    public DialogComponentRemoteFileChooser(final SettingsModelString model, final String label, final boolean border,
        final int portIndex, final String historyId, final int selectionMode,
        final FlowVariableModel flowVariableModel) {
        super(model);
        if (StringUtils.isBlank(requireNonNull(label, "label"))) {
            throw new IllegalArgumentException("label must not be blank.");
        }
        m_label = label;
        m_portIndex = portIndex;
        final JPanel panel = getComponentPanel();
        final SpringLayout layout = new SpringLayout();
        panel.setLayout(layout);
        m_remoteFileChooserPanel =
            new RemoteFileChooserPanel(panel, label, border, historyId, selectionMode, flowVariableModel, null);
        final JPanel fileChooserPanel = m_remoteFileChooserPanel.getPanel();
        panel.add(fileChooserPanel);
        // "Simply" bind all sides of the two panels
        final Constraints panelConstraints = layout.getConstraints(panel);
        final Constraints fileChooserPanelConstraints = layout.getConstraints(fileChooserPanel);
        final Spring padding = Spring.constant(0);
        fileChooserPanelConstraints.setX(padding);
        fileChooserPanelConstraints.setY(padding);
        panelConstraints.setConstraint(SpringLayout.EAST, fileChooserPanelConstraints.getConstraint(SpringLayout.EAST));
        panelConstraints.setConstraint(SpringLayout.SOUTH,
            fileChooserPanelConstraints.getConstraint(SpringLayout.SOUTH));
    }

    @Override
    public void setToolTipText(final String text) {
        // Relies on that the first component is the "selection" combo box.
        final JPanel panel = m_remoteFileChooserPanel.getPanel();
        if (panel.getComponentCount() > 0) {
            final Component firstComponent = panel.getComponent(0);
            if (firstComponent instanceof JComponent) {
                ((JComponent)firstComponent).setToolTipText(text);
            }
        }
    }

    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs) throws NotConfigurableException {
    }

    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_remoteFileChooserPanel.setEnabled(enabled);
    }

    @Override
    protected void updateComponent() {
        final SettingsModelString model = (SettingsModelString)getModel();
        ConnectionInformation connectionInformation = null;
        if (m_portIndex >= 0) {
            final PortObjectSpec[] portObjectSpecs = getLastTableSpecs();
            if (portObjectSpecs != null && portObjectSpecs.length > m_portIndex) {
                final PortObjectSpec portObjectSpec = portObjectSpecs[m_portIndex];
                if (portObjectSpec instanceof ConnectionInformationPortObjectSpec) {
                    connectionInformation =
                        ((ConnectionInformationPortObjectSpec)portObjectSpec).getConnectionInformation();
                }
            }
        }
        m_remoteFileChooserPanel.setConnectionInformation(connectionInformation);
        final String selection = model.getStringValue();
        if (!Objects.equals(selection, m_remoteFileChooserPanel.getSelection())) {
            m_remoteFileChooserPanel.setSelection(selection);
        }
        setEnabledComponents(model.isEnabled());
    }

    @Override
    protected void validateSettingsBeforeSave() throws InvalidSettingsException {
        final String selection = m_remoteFileChooserPanel.getSelection();
        if (StringUtils.isBlank(selection)) {
            throw new InvalidSettingsException(m_label + " is required.");
        }
        ((SettingsModelString)getModel()).setStringValue(selection);
    }

}
