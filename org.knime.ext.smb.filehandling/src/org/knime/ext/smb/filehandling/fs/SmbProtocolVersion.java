/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   2022-08-21 (Alexander Bondaletov): created
 */
package org.knime.ext.smb.filehandling.fs;

import java.util.Optional;
import java.util.stream.Stream;

import org.knime.core.node.InvalidSettingsException;

import com.hierynomus.mssmb2.SMB2Dialect;

/**
 * SMP Protocol version options.
 *
 * @author Alexander Bondaletov
 */
public enum SmbProtocolVersion {
    /**
     * Automatic mode (version is not specified).
     */
    AUTO("auto", "Auto"),
    /**
     * Version 2.0.2
     */
    V_2_0_2("2.0.2", "2.0.2", SMB2Dialect.SMB_2_0_2),
    /**
     * Version 2.1
     */
    V_2_1("2.1", "2.1", SMB2Dialect.SMB_2_1),
    /**
     * The highest 2.x version supported by both client and server.
     */
    V_2_X("2.x", "2.x (2.1, 2.0.2)", SMB2Dialect.SMB_2_1, SMB2Dialect.SMB_2_0_2),
    /**
     * Version 3.0
     */
    V_3_0("3.0", "3.0", SMB2Dialect.SMB_3_0),
    /**
     * Version 3.0.2
     */
    V_3_0_2("3.0.2", "3.0.2", SMB2Dialect.SMB_3_0_2),
    /**
     * Version 3.1.1
     */
    V_3_1_1("3.1.1", "3.1.1", SMB2Dialect.SMB_3_1_1),
    /**
     * The highest 2.x version supported by both client and server.
     */
    V_3_X("3.x", "3.x (3.1.1, 3.0.2, 3.0)", SMB2Dialect.SMB_3_1_1, SMB2Dialect.SMB_3_0_2, SMB2Dialect.SMB_3_0);

    private final String m_key;
    private final String m_title;
    private final SMB2Dialect[] m_dialects;

    private SmbProtocolVersion(final String key, final String title, final SMB2Dialect... dialect) {
        m_key = key;
        m_title = title;
        m_dialects = dialect;
    }

    /**
     * @return the key
     */
    public String getKey() {
        return m_key;
    }

    /**
     * @return the optional with the dialect object
     */
    public Optional<SMB2Dialect[]> getDialect() {
        if (m_dialects == null) {
            return Optional.empty();
        } else {
            return Optional.of(m_dialects);
        }
    }

    @Override
    public String toString() {
        return m_title;
    }

    /**
     * @param key
     *            The key.
     * @return {@link SmbProtocolVersion} corresponding to a given key.
     * @throws InvalidSettingsException
     *             In case the object with a given key is not found.
     */
    public static SmbProtocolVersion fromKey(final String key) throws InvalidSettingsException {
        return Stream.of(values())//
                .filter(v -> v.m_key.equals(key))//
                .findAny()//
                .orElseThrow(() -> new InvalidSettingsException("Invalid protocol version: " + key));
    }
}
