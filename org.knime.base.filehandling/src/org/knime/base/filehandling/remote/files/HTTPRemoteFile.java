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
import org.apache.http.client.HttpClient;
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
        // Create request
        HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet(m_uri);
        // Read response
        HttpResponse response = client.execute(request);
        int code = response.getStatusLine().getStatusCode();
        exists = code < 300;
        return exists;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(final RemoteFile file) throws Exception {
        throw new UnsupportedOperationException(
                "Operation not supported by HTTP");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream openInputStream() throws Exception {
        // Create request
        HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet(m_uri);
        // Read response
        HttpResponse response = client.execute(request);
        return response.getEntity().getContent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputStream openOutputStream() throws Exception {
        throw new UnsupportedOperationException(
                "Operation not supported by HTTP");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSize() throws Exception {
        // Assume unknown length
        long size = 0;
        // Create request
        HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet(m_uri);
        // Read response
        HttpResponse response = client.execute(request);
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
        throw new UnsupportedOperationException(
                "Operation not supported by HTTP");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws Exception {
        // Not used
    }

}
