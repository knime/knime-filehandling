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
 *   Nov 12, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.remote.connectioninformation.port;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.knime.base.filehandling.remote.files.Protocol;
import org.knime.base.filehandling.remote.files.RemoteFileHandler;
import org.knime.base.filehandling.remote.files.RemoteFileHandlerRegistry;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.KnimeEncryption;

/**
 * Contains the connection information for a connection.
 *
 *
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 */
public class ConnectionInformation implements Serializable {

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

    private String m_knownHosts = null;

    private int m_timeout = 30000;

    private boolean m_useKerberos;

    private ConnectionInformation m_ftpProxy = null;

    private ConnectionInformation m_httpProxy = null;

    private ConnectionInformation m_httpsProxy = null;

    /**
     * Parameterless constructor.
     */
    public ConnectionInformation() {
    }

    /**
     * Constructor to load model settings from {@link ModelContentRO}
     *
     * @param model {@link ModelContentRO} to read model settings from
     * @throws InvalidSettingsException
     * @since 3.3
     */
    protected ConnectionInformation(final ModelContentRO model) throws InvalidSettingsException {
        this.setProtocol(model.getString("protocol"));
        this.setHost(model.getString("host"));
        this.setPort(model.getInt("port"));
        this.setUser(model.getString("user"));
        // saved encrypted as of 3.4.1; see AP-7807, AP-7809, AP-7810
        String pass = model.containsKey("xpassword") ?
            model.getPassword("xpassword", "}l?>mn0am8ty1m<+nf") : model.getString("password");
        this.setPassword(pass);
        this.setKeyfile(model.getString("keyfile"));
        this.setKnownHosts(model.getString("knownhosts"));
        this.setTimeout(model.getInt("timeout", 30000)); // new option in 2.10
        this.setUseKerberos(model.getBoolean("kerberos", false)); //new option in 3.2
        if (model.containsKey("ftpProxy")) {
            setFTPProxy(new ConnectionInformation());
            ModelContentRO proxyModelContent = model.getModelContent("ftpProxy");
            m_ftpProxy.setHost(proxyModelContent.getString("host"));
            m_ftpProxy.setPort(proxyModelContent.getInt("port"));
            m_ftpProxy.setUser(proxyModelContent.getString("user"));
            m_ftpProxy.setPassword(proxyModelContent.getPassword("xpassword", "}l?>mn0am8ty1m<+nf"));
        }
        if (model.containsKey("httpProxy")) {
            setHTTPProxy(new ConnectionInformation());
            ModelContentRO proxyModelContent = model.getModelContent("httpProxy");
            m_httpProxy.setHost(proxyModelContent.getString("host"));
            m_httpProxy.setPort(proxyModelContent.getInt("port"));
            m_httpProxy.setUser(proxyModelContent.getString("user"));
            m_httpProxy.setPassword(proxyModelContent.getPassword("xpassword", "}l?>mn0am8ty1m<+nf"));
        }
        if (model.containsKey("httpsProxy")) {
            setHTTPSProxy(new ConnectionInformation());
            ModelContentRO proxyModelContent = model.getModelContent("httpsProxy");
            m_httpsProxy.setHost(proxyModelContent.getString("host"));
            m_httpsProxy.setPort(proxyModelContent.getInt("port"));
            m_httpsProxy.setUser(proxyModelContent.getString("user"));
            m_httpsProxy.setPassword(proxyModelContent.getPassword("xpassword", "}l?>mn0am8ty1m<+nf"));
        }
    }


    /**
     * Save the connection information in a model content object.
     *
     *
     * @param model The model to save in
     */
    public void save(final ModelContentWO model) {
        model.addString("protocol", m_protocol);
        model.addString("host", m_host);
        model.addInt("port", m_port);
        model.addString("user", m_user);
        model.addPassword("xpassword", "}l?>mn0am8ty1m<+nf", m_password);
        model.addString("keyfile", m_keyfile);
        model.addString("knownhosts", m_knownHosts);
        model.addInt("timeout", m_timeout);
        model.addBoolean("kerberos", m_useKerberos);
        if (m_ftpProxy != null) {
            m_ftpProxy.save(model.addModelContent("ftpProxy"));
        }
        if (m_httpProxy != null) {
            m_httpProxy.save(model.addModelContent("httpProxy"));
        }
        if (m_httpsProxy != null) {
            m_httpsProxy.save(model.addModelContent("httpsProxy"));
        }
    }

    /**
     * Create a connection information object loaded from the content object.
     *
     *
     * @param model The model to read from
     * @return The created <code>ConnectionInformation</code> object
     * @throws InvalidSettingsException If the model contains invalid
     *             information.
     * @noreference Not to be called by client
     */
    public static ConnectionInformation load(final ModelContentRO model) throws InvalidSettingsException {
        return new ConnectionInformation(model);
    }

    /**
     * Checks if this connection information object fits to the URI.
     *
     *
     * @param uri The URI to check against
     * @throws Exception If something is incompatible
     */
    public void fitsToURI(final URI uri) throws Exception {
        final RemoteFileHandler<?> fileHandler =
                RemoteFileHandlerRegistry.getRemoteFileHandler(getProtocol());
        if (fileHandler == null) {
            throw new Exception("No file handler found for protocol: " + getProtocol());
        }
        final String scheme = uri.getScheme().toLowerCase();
        final Protocol[] protocols = fileHandler.getSupportedProtocols();
        boolean supportedProtocol = false;
        for (final Protocol protocol : protocols) {
            if (protocol.getName().equals(scheme)) {
                supportedProtocol = true;
                break;
            }
        }
        if (!supportedProtocol) {
            throw new Exception("Protocol \"" + scheme + "\" incompatible with connection information protocol "
                    + getProtocol());
        }
        // Host
        final String uriHost = uri.getHost();
        if (uriHost == null) {
            throw new Exception("No host in URI " + uri);
        }
        if (!uriHost.equalsIgnoreCase(m_host)) {
            throw new Exception("Host incompatible. URI host: " + uriHost
                                + " connection information host " + getHost());
        }
        // Port
        int port = uri.getPort();
        // If port is invalid use default port
        port = port < 0 ? RemoteFileHandlerRegistry.getDefaultPort(scheme) : port;
        if (port != m_port) {
            throw new Exception("Port incompatible");
        }
        // User
        final String user = uri.getUserInfo();
        // User might not be used
        if (!m_useKerberos && StringUtils.isNotEmpty(user) && StringUtils.isNotEmpty(m_user) && !user.equals(m_user)) {
            throw new Exception("User incompatible");
        }
    }

    /**
     * Create the corresponding uri to this connection information.
     *
     *
     * @return URI to this connection information
     */
    public URI toURI() {
        URI uri = null;
        try {
            uri = new URI(m_protocol, m_user, m_host, m_port, null, null, null);
        } catch (final URISyntaxException e) {
            // Should not happen
            NodeLogger.getLogger(getClass()).coding(e.getMessage(), e);
        }
        return uri;
    }

    /**
     * Set the protocol.
     *
     *
     * Will convert the protocol to lower case.
     *
     * @param protocol the protocol to set
     */
    public void setProtocol(final String protocol) {
        m_protocol = protocol.toLowerCase();
//        // Change sftp and scp to ssh
//        if (m_protocol.equals("sftp")) {
//            m_protocol = m_protocol.replace("sftp", "ssh");
//        } else if (m_protocol.equals("scp")) {
//            m_protocol = m_protocol.replace("scp", "ssh");
//        }
    }

    /**
     * Set the host.
     *
     *
     * Will convert the host to lower case.
     *
     * @param host the host to set
     */
    public void setHost(final String host) {
        m_host = host.toLowerCase();
    }

    /**
     * Set the port.
     *
     *
     * @param port the port to set
     */
    public void setPort(final int port) {
        m_port = port;
    }

    /**
     * Set the user.
     *
     *
     * User may be null to disable user authentication.
     *
     * @param user the user to set
     */
    public void setUser(final String user) {
        m_user = user;
    }

    /**
     * Set the password. The password must be encrypted by the {@link KnimeEncryption} class.
     *
     * Password may be <code>null</code> to disable authentication via password.
     *
     * @param password the encrypted password
     */
    public void setPassword(final String password) {
        m_password = password;
    }

    /**
     * Set the keyfile.
     *
     *
     * Keyfile may be null to disable authentication via keyfile.
     *
     * @param keyfile the keyfile to set
     */
    public void setKeyfile(final String keyfile) {
        m_keyfile = keyfile;
    }

    /**
     * Set the known hosts file.
     *
     *
     * Known hosts may be null to disable use.
     *
     * @param knownHosts the known hosts file to set
     */
    public void setKnownHosts(final String knownHosts) {
        m_knownHosts = knownHosts;
    }

    /**
     * Sets the timeout for the connection.
     *
     * @param timeout the timeout in milliseconds
     */
    public void setTimeout(final int timeout) {
        m_timeout = timeout;
    }

    /**
     * @param useKerberos <code>true</code> if Kerberos should be used
     * @since 3.2
     */
    public void setUseKerberos(final boolean useKerberos) {
        m_useKerberos = useKerberos;
    }

    /**
     * Get the protocol.
     *
     *
     * @return the protocol
     */
    public String getProtocol() {
        return m_protocol;
    }

    /**
     * Get the host.
     *
     *
     * @return the host
     */
    public String getHost() {
        return m_host;
    }

    /**
     * Get the port.
     *
     *
     * @return the port
     */
    public int getPort() {
        return m_port;
    }

    /**
     * Get the user.
     *
     *
     * @return the user
     */
    public String getUser() {
        return m_user;
    }

    /**
     * Get the encrypted password. Use {@link KnimeEncryption} to decrypt the password.
     *
     * @return the encrypted password
     */
    public String getPassword() {
        return m_password;
    }

    /**
     * Get the keyfile.
     *
     *
     * @return the keyfile
     */
    public String getKeyfile() {
        return m_keyfile;
    }

    /**
     * Get the known hosts.
     *
     *
     * @return the known hosts
     */
    public String getKnownHosts() {
        return m_knownHosts;
    }

    /**
     * Returns the timeout for the connection.
     *
     * @return the timeout in milliseconds
     */
    public int getTimeout() {
        return m_timeout;
    }

    /**
     * @return <code>true</code> if Kerberos should be used
     * @since 3.2
     */
    public boolean useKerberos() {
        return m_useKerberos;
    }

    /**
     * @param proxyInfo containing the necessary information to connect to an ftp-proxy
     * @since 3.5
     */
    public void setFTPProxy(final ConnectionInformation proxyInfo) {
        m_ftpProxy = proxyInfo;
    }

    /**
     * @return the ftp-proxy configured for this connection. {@code null} if non configured.
     * @since 3.5
     */
    public ConnectionInformation getFTPProxy() {
        return m_ftpProxy;
    }

    /**
     * @param proxyInfo containing the necessary information to connect to an http-proxy
     * @since 3.6
     */
    public void setHTTPProxy(final ConnectionInformation proxyInfo) {
        m_httpProxy = proxyInfo;
    }

    /**
     * @return the http-proxy configured for this connection. {@code null} if non configured.
     * @since 3.6
     */
    public ConnectionInformation getHTTPProxy() {
        return m_httpProxy;
    }

    /**
     * @param proxyInfo containing the necessary information to connect to an https-proxy
     * @since 3.6
     */
    public void setHTTPSProxy(final ConnectionInformation proxyInfo) {
        m_httpsProxy = proxyInfo;
    }

    /**
     * @return the https-proxy configured for this connection. {@code null} if non configured.
     * @since 3.6
     */
    public ConnectionInformation getHTTPSProxy() {
        return m_httpsProxy;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final HashCodeBuilder hcb = new HashCodeBuilder();
        hcb.append(m_protocol);
        hcb.append(m_host);
        hcb.append(m_port);
        hcb.append(m_user);
        hcb.append(m_password);
        hcb.append(m_keyfile);
        hcb.append(m_knownHosts);
        hcb.append(m_useKerberos);
        return hcb.hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof ConnectionInformation)) {
            return false;
        }
        final ConnectionInformation ci = (ConnectionInformation)obj;
        final EqualsBuilder eqBuilder = new EqualsBuilder();
        eqBuilder.append(m_protocol, ci.m_protocol);
        eqBuilder.append(m_host, ci.m_host);
        eqBuilder.append(m_port, ci.m_port);
        eqBuilder.append(m_user, ci.m_user);
        eqBuilder.append(m_password, ci.m_password);
        eqBuilder.append(m_keyfile, ci.m_keyfile);
        eqBuilder.append(m_knownHosts, ci.m_knownHosts);
        eqBuilder.append(m_useKerberos, ci.m_useKerberos);
        return eqBuilder.isEquals();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return toURI().toString();
    }

    /**
     * Creates a remote file.
     *
     * @return Remote file for the connection.
     *
     * @since 3.6
     */
    public RemoteFileHandler<?> getRemoteFileHandler() {
        return RemoteFileHandlerRegistry.getRemoteFileHandler(m_protocol);
    }

}
