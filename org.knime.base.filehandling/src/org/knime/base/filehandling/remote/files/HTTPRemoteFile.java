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
 *   Nov 5, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.remote.files;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformation;
import org.knime.core.node.ExecutionContext;
import org.knime.core.util.KnimeEncryption;

/**
 * Implementation of the HTTP and HTTPS remote file.
 *
 *
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 */
public class HTTPRemoteFile extends RemoteFile<Connection> {

    /**
     * Creates a HTTP remote file for the given URI.
     *
     *
     * @param uri The URI
     * @param connectionInformation Connection information to the given URI
     */
    HTTPRemoteFile(final URI uri, final ConnectionInformation connectionInformation) {
        super(uri, connectionInformation, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean usesConnection() {
        // HTTP, by design, builds a new connection for every request
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Connection createConnection() {
        // Does not use a persistent connection
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getType() {
        return getURI().getScheme();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exists() throws Exception {
        boolean exists;
        final HttpResponse response = getResponse();
        final int code = response.getStatusLine().getStatusCode();
        // Status codes above 300 indicate missing resources
        exists = code < 300;
        return exists;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDirectory() throws Exception {
        // HTTP resource does always point to a file
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void move(final RemoteFile<Connection> file, final ExecutionContext exec) throws Exception {
        throw new UnsupportedOperationException(unsupportedMessage("move"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(final RemoteFile<Connection> file, final ExecutionContext exec) throws Exception {
        throw new UnsupportedOperationException(unsupportedMessage("write"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream openInputStream() throws Exception {
        final HttpResponse response = getResponse();
        return response.getEntity().getContent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputStream openOutputStream() throws Exception {
        throw new UnsupportedOperationException(unsupportedMessage("openOutputStream"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSize() throws Exception {
        // Assume unknown length
        long size = 0;
        final HttpResponse response = getResponse();
        final long length = response.getEntity().getContentLength();
        if (length > 0) {
            size = length;
        }
        return size;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long lastModified() throws Exception {
    	// convert from milliseconds to seconds
        return getURI().toURL().openConnection().getLastModified() / 1000;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean delete() throws Exception {
        throw new UnsupportedOperationException(unsupportedMessage("delete"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RemoteFile<Connection>[] listFiles() throws Exception {
        throw new UnsupportedOperationException(unsupportedMessage("listFiles"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean mkDir() throws Exception {
        throw new UnsupportedOperationException(unsupportedMessage("mkDir"));
    }

    /**
     * Get the response to this files get request.
     *
     *
     * @return The response from the server
     * @throws Exception If communication did not work
     */
    private HttpResponse getResponse() throws Exception {
        Builder requestBuilder = RequestConfig.custom();
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        ConnectionInformation connInfo = getConnectionInformation();
        if (connInfo != null) {
            requestBuilder.setConnectTimeout(connInfo.getTimeout());
            requestBuilder.setSocketTimeout(connInfo.getTimeout());
            String protocol = connInfo.getProtocol();
            if ("http".equals(protocol) && connInfo.getHTTPProxy() != null) {
                configureProxy(connInfo.getHTTPProxy(), requestBuilder, credentialsProvider);
            } else if ("https".equals(protocol) && connInfo.getHTTPSProxy() != null) {
                configureProxy(connInfo.getHTTPSProxy(), requestBuilder, credentialsProvider);
            }
        }

        // Create request
        HttpClientBuilder clientBuilder = HttpClientBuilder.create();

        final HttpGet request;
        // If user info is given in the URI use HTTP basic authentication
        if (connInfo != null && getURI().getUserInfo() != null && getURI().getUserInfo().length() > 0) {
            // Decrypt password from the connection information
            final String password = KnimeEncryption.decrypt(connInfo.getPassword());
            // Get port (replacing it with the default port if necessary)
            int port = getURI().getPort();
            if (port < 0) {
                port = RemoteFileHandlerRegistry.getDefaultPort(getType());
            }
            final Credentials credentials = new UsernamePasswordCredentials(getURI().getUserInfo(), password);
            credentialsProvider.setCredentials(new AuthScope(getURI().getHost(), port), credentials);
            clientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            request = new HttpGet(getURI());
            request.addHeader(new BasicScheme().authenticate(credentials, request, null));
        } else {
            request = new HttpGet(getURI());
        }
        request.setConfig(requestBuilder.build());
        // Return response
        return clientBuilder.build().execute(request);
    }

    private static final void configureProxy(final ConnectionInformation proxy, final Builder requestBuilder,
        final CredentialsProvider credentialsProvider) {
        HttpHost proxyHost = new HttpHost(proxy.getHost(), proxy.getPort());
        requestBuilder.setProxy(proxyHost);
        if (proxy.getUser() != null) {
            credentialsProvider.setCredentials(new AuthScope(proxy.getHost(), proxy.getPort()),
                new UsernamePasswordCredentials(proxy.getUser(), proxy.getPassword()));
        }
    }
}
