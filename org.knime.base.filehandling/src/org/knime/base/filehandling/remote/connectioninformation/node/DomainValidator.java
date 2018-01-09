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
 *   Feb 1, 2013 (Patrick Winter): created
 */
package org.knime.base.filehandling.remote.connectioninformation.node;

import java.util.regex.Pattern;

/**
 * Utility class to check domains.
 * 
 * 
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 */
final class DomainValidator {

    private DomainValidator() {
        // Disable default constructor
    }

    // Define valid outer characters
    // alphanumeric
    // Examples: 0; a; B; u; z
    private static final String OUTER = "[a-zA-Z0-9]";

    // Define valid inner characters
    // outer and '-'
    // Examples: 0; a; B; -
    private static final String INNER = OUTER + "|[\\-]";

    // Define valid label
    // single outer or outer then 0-* inner then outer
    // Examples: a; aa; a-a; a--a; a-a-a; uni-konstanz
    private static final String LABEL = OUTER + "|" + OUTER + INNER + "*" + OUTER;

    // Define multiple labels
    // label '.' label...
    // Examples: a.a; a.a-a; www.uni-konstanz.de
    private static final String MULTILABEL = LABEL + "([.]" + LABEL + ")*";

    private static Pattern pattern = Pattern.compile(MULTILABEL);

    /**
     * Checks if a string is a valid domain.
     * 
     * 
     * @param string The string to check
     * @return true if the string is a valid domain, false otherwise
     */
    public static boolean isValidDomain(final String string) {
        return pattern.matcher(string).matches();
    }

}
