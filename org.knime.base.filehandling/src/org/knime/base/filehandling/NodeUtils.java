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
 *   Oct 17, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling;

import java.awt.GridBagConstraints;
import java.awt.Insets;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;

/**
 * Utility class for standard node implementation.
 * 
 * 
 * @author Patrick Winter, KNIME.com, Zurich, Switzerland
 */
public final class NodeUtils {

    private NodeUtils() {
        // Disable default constructor
    }

    /**
     * Checks if a column selection is set correctly. Will throw an exception if
     * not.
     * 
     * 
     * @param inSpec Specification of the input table
     * @param selectionName Name of the column setting (will be used for the
     *            error messages)
     * @param selectedColumn Name of the selected column
     * @param types Accepted column types, if none are given every type will be
     *            accepted
     * @throws InvalidSettingsException If the column is not set correctly
     */
    public static void checkColumnSelection(final DataTableSpec inSpec, final String selectionName,
            final String selectedColumn, final Class<? extends DataValue>... types) throws InvalidSettingsException {
        // Does the column setting reference to an existing column?
        int columnIndex = inSpec.findColumnIndex(selectedColumn);
        if (columnIndex < 0) {
            throw new InvalidSettingsException(selectionName + " column not set");
        }
        // Check types if available
        if (types.length > 0) {
            // Is the column setting referencing to a column of a correct type?
            DataType type = inSpec.getColumnSpec(columnIndex).getType();
            boolean typeOk = false;
            // Check each accepted type
            for (int i = 0; i < types.length; i++) {
                if (type.isCompatible(types[i])) {
                    typeOk = true;
                    break;
                }
            }
            // Has a correct type been found?
            if (!typeOk) {
                throw new InvalidSettingsException(selectionName + " column not set");
            }
        }
    }

    /**
     * Reset the grid bag constraints to useful defaults.
     * 
     * 
     * The defaults are all insets to 5, anchor northwest, fill both, x and y 0
     * and x and y weight 0.
     * 
     * @param gbc The constraints object.
     */
    public static void resetGBC(final GridBagConstraints gbc) {
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridx = 0;
        gbc.gridy = 0;
    }

}
