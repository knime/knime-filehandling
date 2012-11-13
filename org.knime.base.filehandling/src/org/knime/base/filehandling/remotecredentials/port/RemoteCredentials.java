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
 *   Nov 12, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.remotecredentials.port;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;

/**
 * Contains the credentials for a connection.
 * 
 * 
 * @author Patrick Winter, University of Konstanz
 */
public class RemoteCredentials implements Serializable {

    /**
     * Serial id.
     */
    private static final long serialVersionUID = -618632550017543955L;

    private String m_protocol = null;

    private String m_host = null;

    private int m_port = -1;

    private String m_user = null;

    private String m_password = null;

    private String m_keyfile = null;

    private String m_certificate = null;

    /**
     * @param model The model to save in
     */
    public void save(final ModelContentWO model) {
        model.addString("protocol", m_protocol);
        model.addString("host", m_host);
        model.addInt("port", m_port);
        model.addString("user", m_user);
        model.addString("password", m_password);
        model.addString("keyfile", m_keyfile);
        model.addString("certificate", m_certificate);
    }

    /**
     * @param model The model to read from
     * @return RemoteCredentials object
     * @throws InvalidSettingsException If the model contains invalid
     *             information.
     * @noreference Not to be called by client.
     */
    public static RemoteCredentials load(final ModelContentRO model)
            throws InvalidSettingsException {
        RemoteCredentials credentials = new RemoteCredentials();
        credentials.setProtocol(model.getString("protocol"));
        credentials.setHost(model.getString("host"));
        credentials.setPort(model.getInt("port"));
        credentials.setUser(model.getString("user"));
        credentials.setPassword(model.getString("password"));
        credentials.setKeyfile(model.getString("keyfile"));
        credentials.setCertificate(model.getString("certificate"));
        return credentials;
    }

    /**
     * @param output The output to save in
     * @throws IOException ...
     * @noreference Not to be called by client.
     */
    public void save(final DataOutput output) throws IOException {
        output.writeUTF(m_protocol);
        output.writeUTF(m_host);
        output.writeInt(m_port);
        output.writeUTF(m_user);
        output.writeUTF(m_password);
        output.writeUTF(m_keyfile);
        output.writeUTF(m_certificate);
    }

    /**
     * Load from stream (e.g. inline data stream).
     * 
     * @param input ...
     * @return ...
     * @throws IOException ...
     * @noreference Not to be called by client.
     */
    public static RemoteCredentials load(final DataInput input)
            throws IOException {
        RemoteCredentials credentials = new RemoteCredentials();
        credentials.setProtocol(input.readUTF());
        credentials.setHost(input.readUTF());
        credentials.setPort(input.readInt());
        credentials.setUser(input.readUTF());
        credentials.setPassword(input.readUTF());
        credentials.setKeyfile(input.readUTF());
        credentials.setCertificate(input.readUTF());
        return credentials;
    }

    /**
     * Checks if this credentials object fits to the URI.
     * 
     * 
     * @param uri The URI
     * @throws Exception If something is incompatible
     */
    public void fitsToURI(final URI uri) throws Exception {
        // Scheme
        String scheme = uri.getScheme().toLowerCase();
        if (scheme.equals("sftp")) {
            scheme = scheme.replace("sftp", "ssh");
        } else if (scheme.equals("scp")) {
            scheme = scheme.replace("scp", "ssh");
        }
        if (!scheme.equals(m_protocol)) {
            throw new Exception("Protocol incompatible");
        }
        // Host
        if (!uri.getHost().toLowerCase().equals(m_host.toLowerCase())) {
            throw new Exception("Host incompatible");
        }
        // Port
        int port = uri.getPort();
        // TODO port = port<0 ? defaultPort(scheme) : port
        if (port != m_port) {
            throw new Exception("Port incompatible");
        }
        // User
        String user = uri.getUserInfo().toLowerCase();
        if (user != null && !user.equals(m_user.toLowerCase())) {
            throw new Exception("User incompatible");
        }
    }

    /**
     * @param protocol the protocol to set
     */
    public void setProtocol(final String protocol) {
        m_protocol = protocol;
    }

    /**
     * @param host the host to set
     */
    public void setHost(final String host) {
        m_host = host;
    }

    /**
     * @param port the port to set
     */
    public void setPort(final int port) {
        m_port = port;
    }

    /**
     * @param user the user to set
     */
    public void setUser(final String user) {
        m_user = user;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(final String password) {
        m_password = password;
    }

    /**
     * @param keyfile the keyfile to set
     */
    public void setKeyfile(final String keyfile) {
        m_keyfile = keyfile;
    }

    /**
     * @param certificate the certificate to set
     */
    public void setCertificate(final String certificate) {
        m_certificate = certificate;
    }

    /**
     * @return the protocol
     */
    public String getProtocol() {
        return m_protocol;
    }

    /**
     * @return the host
     */
    public String getHost() {
        return m_host;
    }

    /**
     * @return the port
     */
    public int getPort() {
        return m_port;
    }

    /**
     * @return the user
     */
    public String getUser() {
        return m_user;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return m_password;
    }

    /**
     * @return the keyfile
     */
    public String getKeyfile() {
        return m_keyfile;
    }

    /**
     * @return the certificate
     */
    public String getCertificate() {
        return m_certificate;
    }

}
