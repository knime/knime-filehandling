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
 *   Oct 16, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.mime;

import java.util.LinkedList;
import java.util.List;

/**
 * Represents a MIME-Type and its registered extensions.
 * 
 * 
 * @author Patrick Winter, University of Konstanz
 */
public class MIMETypeEntry {

    private String m_type;

    private List<String> m_extensions;

    /**
     * @param type Name of this MIME-Type
     */
    public MIMETypeEntry(final String type) {
        m_type = type;
        m_extensions = new LinkedList<String>();
    }

    /**
     * @return The MIME-Types name
     */
    public String getType() {
        return m_type;
    }

    /**
     * @return The extensions of this MIME-Type
     */
    public List<String> getExtensions() {
        return m_extensions;
    }

    /**
     * @param extension Extension to register with this type
     */
    public void addExtension(final String extension) {
        m_extensions.add(extension);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        String result = m_type;
        for (int i = 0; i < m_extensions.size(); i++) {
            result += " " + m_extensions.get(i);
        }
        return result;
    }

}