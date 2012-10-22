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
 *   Oct 18, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.remotecopy.connections;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.net.ftp.FTPClient;
import org.knime.core.node.NodeLogger;

import com.jcraft.jsch.Session;

/**
 * Monitors the opened connections.
 * 
 * 
 * @author Patrick Winter, University of Konstanz
 */
public class ConnectionMonitor {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(ConnectionMonitor.class);

    private Map<String, Object> m_connections;

    /**
     * Create a new monitor.
     */
    public ConnectionMonitor() {
        m_connections = new HashMap<String, Object>();
    }

    /**
     * Get a connection for the given URI.
     * 
     * 
     * @param uri URI with host information
     * @return Corresponding connection if it exists or null if not.
     */
    public Object getConnection(final URI uri) {
        LOGGER.info("Used connection: " + hostFromURI(uri));
        return m_connections.get(hostFromURI(uri));
    }

    /**
     * Registers the given connection (overwriting an old one).
     * 
     * 
     * @param uri URI with host information
     * @param connection Open connection
     */
    public void registerConnection(final URI uri, final Object connection) {
        m_connections.put(hostFromURI(uri), connection);
        LOGGER.info("Opened connection: " + hostFromURI(uri));
    }

    /**
     * Closes all known connections.
     */
    public void closeConnections() {
        Set<String> keys = m_connections.keySet();
        for (String key : keys) {
            Object connection = m_connections.get(key);
            if (connection instanceof FTPClient) {
                FTPClient client = (FTPClient)connection;
                try {
                    client.logout();
                    client.disconnect();
                    LOGGER.info("Closed connection: " + key);
                } catch (IOException e) {
                    // ignore
                }
            }
            if (connection instanceof Session) {
                Session session = (Session)connection;
                session.disconnect();
            }
        }
    }

    /**
     * Creates an identifier for the host using the URI.
     * 
     * 
     * @param uri URI with host information
     * @return Identifier for the connection to the host.
     */
    private String hostFromURI(final URI uri) {
        return uri.getScheme() + "://" + uri.getAuthority();
    }

}
