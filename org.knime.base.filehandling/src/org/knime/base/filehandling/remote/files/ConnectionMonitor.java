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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   Nov 2, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.remote.files;

import java.util.HashMap;
import java.util.Map;

/**
 * Monitors open connections.
 *
 *
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 * @param <C> the {@link Connection}
 */
public final class ConnectionMonitor<C extends Connection> {

    private final Map<String, C> m_connections = new HashMap<>();

    /**
     * Register a connection.
     *
     *
     * @param identifier Identifier for the connection
     * @param connection Connection to register
     */
    public synchronized void registerConnection(final String identifier, final C connection) {
        m_connections.put(identifier, connection);
    }

    /**
     * Find an open connection to the identifier.
     *
     *
     * @param identifier The identifier
     * @return Already opened connection to the identifier or null if not
     *         available
     */
    public synchronized C findConnection(final String identifier) {
        C connection = m_connections.get(identifier);
        // Check if connection is open
        if (connection != null && !connection.isOpen()) {
            try {
                // Try to open connection
                connection.open();
            } catch (final Exception e) {
                // Remove in case of error
                m_connections.remove(identifier);
                connection = null;
            }
        }
        return connection;
    }

    /**
     * Close and remove all connections.
     */
    public synchronized void closeAll() {
        if (m_connections == null || m_connections.isEmpty()) {
            //nothing to close
            return;
        }
        for (final C connection : m_connections.values()) {
            try {
                connection.close();
            } catch (final Exception e) {
                // ignore and close next connection
            }
        }
        m_connections.clear();
    }

}
