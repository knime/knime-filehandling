/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   Nov 5, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.remote.files;

import java.net.URI;

import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformation;
import org.knime.core.util.KnimeEncryption;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

/**
 * Connection over SSH.
 *
 *
 * @author Patrick Winter, KNIME.com, Zurich, Switzerland
 */
public class SSHConnection extends Connection {

    private URI m_uri;

    private Session m_session;

    private ConnectionInformation m_connectionInformation;

    /**
     * Create a SSH connection to the given URI.
     *
     *
     * @param uri The URI
     * @param connectionInformation Connection information to the given URI
     */
    public SSHConnection(final URI uri, final ConnectionInformation connectionInformation) {
        m_uri = uri;
        m_connectionInformation = connectionInformation;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void open() throws Exception {
        if (m_connectionInformation == null) {
            throw new Exception("Missing the connection information for " + m_uri);
        }
        // Read attributes
        String host = m_uri.getHost();
        int port = m_uri.getPort() >= 0 ? m_uri.getPort() : DefaultPortMap.getMap().get("ssh");
        String user = m_uri.getUserInfo();
        String password = m_connectionInformation.getPassword();
        if (password != null) {
            password = KnimeEncryption.decrypt(password);
        }
        JSch jsch = new JSch();
        // Use keyfile if available
        String keyfile = m_connectionInformation.getKeyfile();
        if (keyfile != null && keyfile.length() != 0) {
            if (password == null) {
                jsch.addIdentity(keyfile);
            } else {
                jsch.addIdentity(keyfile, password);
            }
        }
        // Set known hosts if available
        String knownHosts = m_connectionInformation.getKnownHosts();
        if (knownHosts != null) {
            jsch.setKnownHosts(knownHosts);
        }
        Session session = jsch.getSession(user, host, port);
        session.setPassword(password);
        if (knownHosts == null) {
            session.setConfig("StrictHostKeyChecking", "no");
        }
        session.setTimeout(m_connectionInformation.getTimeout());
        session.connect();
        m_session = session;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOpen() {
        return m_session.isConnected();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws Exception {
        m_session.disconnect();
    }

    /**
     * Returns the Session of this connection.
     *
     *
     * @return The session
     */
    public Session getSession() {
        return m_session;
    }

}
