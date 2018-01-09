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
 *   Nov 8, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.stringstobinaryobjects;

/**
 * Contains the available encodings.
 * 
 * 
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 */
public final class Encodings {

    private Encodings() {
        // Disable default constructor
    }

    /**
     * Seven-bit ASCII, a.k.a. ISO646-US, a.k.a. the Basic Latin block of the
     * Unicode character set.
     */
    public static final String US_ASCII = "US-ASCII";

    /** ISO Latin Alphabet No. 1, a.k.a. ISO-LATIN-1. */
    public static final String ISO_8859_1 = "ISO-8859-1";

    /** Eight-bit UCS Transformation Format. */
    public static final String UTF_8 = "UTF-8";

    /** Sixteen-bit UCS Transformation Format, big-endian byte order. */
    public static final String UTF_16BE = "UTF-16BE";

    /** Sixteen-bit UCS Transformation Format, little-endian byte order. */
    public static final String UTF_16LE = "UTF-16LE";

    /**
     * Sixteen-bit UCS Transformation Format, byte order identified by an
     * optional byte-order mark.
     */
    public static final String UTF_16 = "UTF-16";

    /**
     * Get all available encodings.
     * 
     * 
     * @return Array with all available encodings
     */
    public static String[] getAllEncodings() {
        return new String[]{US_ASCII, ISO_8859_1, UTF_8, UTF_16BE, UTF_16LE, UTF_16};
    }

}
