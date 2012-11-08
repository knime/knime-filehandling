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
 *   Nov 5, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.remote.files;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 * Implementation of the HTTP and HTTPS remote file.
 * 
 * 
 * @author Patrick Winter, University of Konstanz
 */
public class HTTPRemoteFile extends RemoteFile {

    private URI m_uri;

    /**
     * Creates a HTTP remote file for the given URI.
     * 
     * 
     * @param uri The URI
     */
    HTTPRemoteFile(final URI uri) {
        m_uri = uri;
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
    protected String getIdentifier() {
        return buildIdentifier(m_uri);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getType() {
        return "http";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getDefaultPort() {
        // HTTP port:80, HTTPS port:443
        return m_uri.getScheme().equals("https") ? 443 : 80;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exists() throws Exception {
        boolean exists;
        HttpResponse response = getResponse();
        int code = response.getStatusLine().getStatusCode();
        exists = code < 300;
        return exists;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDirectory() throws Exception {
        throw new UnsupportedOperationException(
                unsupportedMessage("isDirectory"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean move(final RemoteFile file) throws Exception {
        throw new UnsupportedOperationException(unsupportedMessage("move"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(final RemoteFile file) throws Exception {
        throw new UnsupportedOperationException(unsupportedMessage("write"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream openInputStream() throws Exception {
        HttpResponse response = getResponse();
        return response.getEntity().getContent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputStream openOutputStream() throws Exception {
        throw new UnsupportedOperationException(
                unsupportedMessage("openOutputStream"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSize() throws Exception {
        // Assume unknown length
        long size = 0;
        HttpResponse response = getResponse();
        long length = response.getEntity().getContentLength();
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
        return m_uri.toURL().openConnection().getLastModified();
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
    public RemoteFile[] listFiles() throws Exception {
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
     * {@inheritDoc}
     */
    @Override
    public void close() throws Exception {
        // Not used
    }

    /**
     * Get the response to this files get request.
     * 
     * 
     * @return The response from the server
     * @throws Exception If communication did not work
     */
    private HttpResponse getResponse() throws Exception {
        // Create request
        DefaultHttpClient client = new DefaultHttpClient();
        if (m_uri.getUserInfo().length() > 0) {
            String password = "password";
            int port = m_uri.getPort();
            if (port < 0) {
                port = getDefaultPort();
            }
            Credentials credentials =
                    new UsernamePasswordCredentials(m_uri.getUserInfo(),
                            password);
            AuthScope scope = new AuthScope(m_uri.getHost(), port);
            client.getCredentialsProvider().setCredentials(scope, credentials);
        }
        HttpGet request = new HttpGet(m_uri);
        // Get response
        HttpResponse response = client.execute(request);
        return response;
    }
}
