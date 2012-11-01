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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;

/**
 * 
 * @author Patrick Winter, University of Konstanz
 */
public class ColumnSelection {

    private JPanel m_panel;

    private String m_selection;

    private Class<? extends DataValue>[] m_types;

    private JLabel m_label;

    private JComboBox<Integer> m_combobox;

    private ColumnSelectionRenderer m_renderer;

    /**
     * @param label Label of the column selection
     * @param types Allowed types
     */
    public ColumnSelection(final String label,
            final Class<? extends DataValue>... types) {
        m_types = types;
        m_panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        m_label = new JLabel(label);
        m_combobox = new JComboBox<Integer>(new Integer[0]);
        m_renderer = new ColumnSelectionRenderer();
        m_combobox.setRenderer(m_renderer);
        m_combobox.addActionListener(new ColumnSelectionListener());
        m_panel.add(m_label, gbc);
        ++gbc.gridx;
        m_panel.add(m_combobox, gbc);
    }

    /**
     * @return The component
     */
    public Component getComponent() {
        return m_panel;
    }

    /**
     * @return The selected element
     */
    public String getSelection() {
        return m_selection;
    }

    /**
     * @param enabled If the component should be enabled
     */
    public void setEnabled(final boolean enabled) {
        m_panel.setEnabled(enabled);
        m_label.setEnabled(enabled);
        m_combobox.setEnabled(enabled);
    }

    /**
     * @param selection The element to select
     */
    public void setSelection(final String selection) {
        Integer index = m_renderer.getIndex(selection);
        m_combobox.setSelectedItem(index);
    }

    /**
     * @param spec Specification of the input table
     */
    public void updateColumns(final DataTableSpec spec) {
        List<Integer> ints = new LinkedList<Integer>();
        List<String> strings = new LinkedList<String>();
        List<Icon> icons = new LinkedList<Icon>();
        for (int i = 0; i < spec.getNumColumns(); i++) {
            DataColumnSpec colSpec = spec.getColumnSpec(i);
            DataType type = colSpec.getType();
            for (int j = 0; j < m_types.length; j++) {
                if (type.isCompatible(m_types[j])) {
                    ints.add(ints.size());
                    strings.add(colSpec.getName());
                    icons.add(type.getIcon());
                }
            }
        }
        m_renderer.setContent(strings.toArray(new String[strings.size()]),
                icons.toArray(new Icon[icons.size()]));
        m_combobox.setModel(new JComboBox<Integer>(ints
                .toArray(new Integer[ints.size()])).getModel());
        m_combobox.revalidate();
    }

    private class ColumnSelectionListener implements ActionListener {

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionPerformed(final ActionEvent e) {
            int selected = (Integer)m_combobox.getSelectedItem();
            m_selection = m_renderer.getString(selected);
        }

    }

    private class ColumnSelectionRenderer extends DefaultListCellRenderer {

        private String[] m_strings;

        private Icon[] m_icons;

        private static final long serialVersionUID = -5018178288836179108L;

        public void setContent(final String[] strings, final Icon[] icons) {
            m_strings = strings;
            m_icons = icons;
        }

        public String getString(final int i) {
            return m_strings[i];
        }

        public int getIndex(final String string) {
            int result = -1;
            for (int i = 0; i < m_strings.length; i++) {
                if (m_strings[i].equals(string)) {
                    result = i;
                    break;
                }
            }
            return result;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Component getListCellRendererComponent(final JList<?> list,
                final Object value, final int index, final boolean isSelected,
                final boolean cellHasFocus) {
            JLabel label;
            if (value != null) {
                int selected = (Integer)value;
                label = new JLabel(m_strings[selected]);
                label.setIcon(m_icons[selected]);
            } else {
                label = new JLabel("");
            }
            return label;
        }

    }

}
