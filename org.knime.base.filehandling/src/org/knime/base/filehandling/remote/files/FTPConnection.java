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
 * -------------------------------------------------------------------
 */

package org.knime.base.filehandling.remote.files;

import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformation;
import org.knime.core.util.KnimeEncryption;

import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.connectors.HTTPTunnelConnector;

/**
 * Connection over FTP.
 *
 *
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 * @since 2.11
 */
public class FTPConnection extends Connection {

    private final FTPRemoteFile m_ftpRemoteFile;

    /**Constructor for class FTPConnection.
     * @param ftpRemoteFile the {@link FTPRemoteFile}
     */
    public FTPConnection(final FTPRemoteFile ftpRemoteFile) {
        m_ftpRemoteFile = ftpRemoteFile;
    }

    private FTPClient m_client;

    private String m_defaultDir;

    /**
     * {@inheritDoc}
     */
    @Override
    public void open() throws Exception {
        // Create client
        m_client = new FTPClient();
        // Read attributes
        final String host = m_ftpRemoteFile.getURI().getHost();
        final int port = m_ftpRemoteFile.getURI().getPort() != -1 ? m_ftpRemoteFile.getURI().getPort()
                : RemoteFileHandlerRegistry.getDefaultPort(m_ftpRemoteFile.getType());
        String user = m_ftpRemoteFile.getURI().getUserInfo();
        if (user == null) {
            user = "anonymous";
        }
        String password = null;
        if (m_ftpRemoteFile.getConnectionInformation() != null) {
            password = m_ftpRemoteFile.getConnectionInformation().getPassword();
        }
        if (password != null) {
            password = KnimeEncryption.decrypt(password);
        } else {
            password = "";
        }
        ConnectionInformation ftpProxy = m_ftpRemoteFile.getConnectionInformation().getFTPProxy();
        if (ftpProxy != null) {
            HTTPTunnelConnector proxyConnector = new HTTPTunnelConnector(ftpProxy.getHost(), ftpProxy.getPort(),
                ftpProxy.getUser(), ftpProxy.getPassword());
            m_client.setConnector(proxyConnector);
        }
        // Open connection
        m_client.connect(host, port);
        m_client.setPassive(true);
        // Login
        m_client.login(user, password);
        m_client.setType(FTPClient.TYPE_BINARY);
        // Find root directory
        String oldDir;
        do {
            oldDir = m_client.currentDirectory();
            m_client.changeDirectoryUp();
            m_defaultDir = m_client.currentDirectory();
        } while (!m_defaultDir.equals(oldDir));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOpen() {
        boolean open = false;
        if (m_client != null && m_client.isConnected()) {
            open = true;
        }
        return open;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws Exception {
        m_client.logout();
        m_client.disconnect(true);
    }

    /**
     * Return the client of this connection.
     *
     *
     * @return The FTP Client
     */
    public FTPClient getClient() {
        return m_client;
    }

    /**
     * Change working directory to root.
     *
     *
     * @throws Exception If the operation could not be executed
     */
    public void resetWorkingDir() throws Exception {
        m_client.changeDirectory(m_defaultDir);
    }

}