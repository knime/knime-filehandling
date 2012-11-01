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
 *   Oct 31, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.dialogcomponents;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

/**
 * Custom implementation of a button group.
 * 
 * 
 * @author Patrick Winter, University of Konstanz
 */
public class CustomButtonGroup {

    private JPanel m_panel;

    private ButtonGroup m_group;

    private JRadioButton[] m_buttons;

    /**
     * @param label Label of the button group
     * @param border If the button group is surrounded by a border
     * @param options The available options
     */
    public CustomButtonGroup(final String label, final boolean border,
            final String... options) {
        m_panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.gridx = 0;
        gbc.gridy = 0;
        m_group = new ButtonGroup();
        m_buttons = new JRadioButton[options.length];
        for (int i = 0; i < m_buttons.length; i++) {
            m_buttons[i] = new JRadioButton(options[i]);
            m_buttons[i].setActionCommand(options[i]);
            m_group.add(m_buttons[i]);
            m_panel.add(m_buttons[i], gbc);
            ++gbc.gridx;
        }
        if (border) {
            m_panel.setBorder(new TitledBorder(new EtchedBorder(), label));
        } else if (!label.equals("")) {
            m_panel.setBorder(new TitledBorder(label));
        }
    }

    /**
     * @return The component
     */
    public Component getComponent() {
        return m_panel;
    }

    /**
     * @param selection The selected option
     */
    public void setSelection(final String selection) {
        for (int i = 0; i < m_buttons.length; i++) {
            if (selection.equals(m_buttons[i].getActionCommand())) {
                m_group.setSelected(m_buttons[i].getModel(), true);
                break;
            }
        }
    }

    /**
     * @return The selected option
     */
    public String getSelection() {
        return m_group.getSelection().getActionCommand();
    }

    /**
     * @param enabled If the component should be enabled
     */
    public void setEnabled(final boolean enabled) {
        m_panel.setEnabled(enabled);
        for (int i = 0; i < m_buttons.length; i++) {
            m_buttons[i].setEnabled(enabled);
        }
    }

}
