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
 *
 * History
 *   Sep 21, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.filemetainfo2;

import org.knime.core.data.DataType;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;

/**
 * Enums for attributes.
 *
 *
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 */
enum Attributes {

    /**
     * Is the file a directory?
     */
    DIRECTORY(0, "Directory", BooleanCell.TYPE),

    /**
     * Is the file hidden?
     */
    HIDDEN(1, "Hidden", BooleanCell.TYPE),

    /**
     * Size of the file in bytes.
     */
    SIZE(2, "Size", LongCell.TYPE),

    /**
     * Size of the file in bytes, in human readable form.
     */
    HUMANSIZE(3, "Size (human readable)", StringCell.TYPE),

    /**
     * Last time the file was modified.
     */
    MODIFIED(4, "Last modified", LocalDateTimeCellFactory.TYPE),

    /**
     * Read, write and execute permissions for the file.
     */
    PERMISSIONS(5, "Permissions", StringCell.TYPE),

    /**
     * If the file exists.
     */
    EXISTS(6, "Exists", BooleanCell.TYPE);


    private final int m_position;

    private final String m_name;

    private final DataType m_type;

    /**
     * @param position Position of this attribute
     * @param name Name of this attribute
     * @param type Type of this attribute
     */
    Attributes(final int position, final String name, final DataType type) {
        m_position = position;
        m_name = name;
        m_type = type;
    }

    /**
     * @return Position of this attribute
     */
    int getPosition() {
        return m_position;
    }

    /**
     * @return Name of this attribute
     */
    String getName() {
        return m_name;
    }

    /**
     * @return Type of this attribute
     */
    DataType getType() {
        return m_type;
    }

    /**
     * @return Array of all attributes
     */
    static Attributes[] getAllAttributes() {
        return new Attributes[]{DIRECTORY, HIDDEN, SIZE, HUMANSIZE, MODIFIED, PERMISSIONS, EXISTS};
    }

}
