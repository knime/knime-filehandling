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
 *   2020-11-20 (Bjoern Lohrmann, KNIME GmbH): created
 */
package org.knime.ext.http.filehandling.fs;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.http.HTTPConduitFactory;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.knime.core.util.ThreadLocalHTTPAuthenticator;
import org.knime.core.util.ThreadLocalHTTPAuthenticator.AuthenticationCloseable;
import org.knime.ext.http.filehandling.fs.HttpFSConnectionConfig.Auth;
import org.knime.filehandling.core.connections.base.attributes.BaseFileAttributes;
import org.knime.filehandling.core.defaultnodesettings.ExceptionUtil;

/**
 * Class that allows to make HTTP requests. Instances of this are heavy-weight
 * objects, whose initialization and disposal may be expensive. Client instances
 * must be properly closed before being disposed to avoid leaking resources.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
final class HttpClient {

    private static final int MAX_RETRANSITS = 4;

    private final HttpFSConnectionConfig m_config;

    private final Client m_client;

    private HttpClient(final HttpFSConnectionConfig config, final Client client) {
        m_config = config;
        m_client = client;
    }

    private Builder createInvocationBuilder(final String url) {
        WebTarget target = m_client.target(url);

        // Support relative redirects too, see
        // https://tools.ietf.org/html/rfc7231#section-3.1.4.2
        target = target.property("http.redirect.relative.uri", true);

        final Builder request = target.request();

        // make sure that the AsyncHTTPConduitFactory is *not* used
        final Bus bus = BusFactory.getThreadDefaultBus();
        bus.setExtension(null, HTTPConduitFactory.class);

        if (m_config.getAuthType() == Auth.BASIC) {
            setBasicAuthentication(request);
        }

        HTTPClientPolicy clientPolicy = WebClient.getConfig(request).getHttpConduit().getClient();

        if (!clientPolicy.isSetAutoRedirect()) {
            clientPolicy.setAutoRedirect(m_config.isFollowRedirects());
        }

        if (!clientPolicy.isSetMaxRetransmits()) {
            clientPolicy.setMaxRetransmits(MAX_RETRANSITS);
        }

        return request;
    }

    private void setBasicAuthentication(final Builder request) {
        final byte[] toEncode = (m_config.getUsername() + ":" + m_config.getPassword())
                .getBytes(StandardCharsets.UTF_8);
        request.header("Authorization", "Basic " + Base64Utility.encode(toEncode));
    }

    /**
     * Issues a HTTP Get request to the given URL.
     *
     * @param path
     *            The {@link HttpPath} to retrieve.
     * @return an input stream for the response body.
     * @throws IOException
     */
    @SuppressWarnings("resource")
    InputStream getAsInputStream(final HttpPath path) throws IOException {
        final String url = path.getRequestUrl();
        final Response response = invoke(createInvocationBuilder(url).buildGet());

        if (response.getStatus() >= 200 && response.getStatus() < 300) {
            return new FilterInputStream(response.readEntity(InputStream.class)) {
                @Override
                public void close() throws IOException {
                    try {
                        super.close();
                    } finally {
                        response.close();
                    }
                }
            };
        } else {
            response.close();
            throw mapToException(response, path);
        }
    }

    public BasicFileAttributes headAsFileAttributes(final HttpPath path) throws IOException {
        final String url = path.getRequestUrl();

        try (final Response response = invoke(createInvocationBuilder(url).buildGet())) {
            if (response.getStatus() >= 200 && response.getStatus() < 300) {
                final Date lastModified = Optional.ofNullable(response.getLastModified())
                        .orElse(Date.from(Instant.ofEpochMilli(0)));
                final long size = (response.getLength() != -1) ? response.getLength() : 0;

                return new BaseFileAttributes(true, //
                        path, //
                        FileTime.fromMillis(lastModified.getTime()), //
                        FileTime.fromMillis(lastModified.getTime()), //
                        FileTime.fromMillis(lastModified.getTime()), //
                        size, //
                        false, //
                        false, //
                        null, //
                        null, //
                        null);
            } else {
                throw mapToException(response, path);
            }
        }
    }

    private static IOException mapToException(final Response response, final HttpPath path) {
        final IOException toReturn;

        switch (Status.fromStatusCode(response.getStatus())) {
        case UNAUTHORIZED:
            toReturn = mapUnauthorizedResponse(response);
            break;
        case FORBIDDEN:
            toReturn = new AccessDeniedException(path.toString());
            break;
        case NOT_FOUND:
            toReturn = new NoSuchFileException(path.toString());
            break;
        default:
            toReturn = new IOException(String.format("Error, webserver returned HTTP %d (%s).", response.getStatus(),
                    response.getStatusInfo().getReasonPhrase()));
        }

        return toReturn;
    }

    private static IOException mapUnauthorizedResponse(final Response response) {
        final IOException toReturn;
        final String[] supportedAuthTypes = getSupportedAuthTypes(response);
        if (supportedAuthTypes.length > 0) {
            toReturn = new AccessDeniedException(
                    "Authentication required/failed. Server supports the following authentication types: "
                    + String.join(", ", supportedAuthTypes));
        } else {
            toReturn = new AccessDeniedException("Authentication failed.");
        }
        return toReturn;
    }

    private static String[] getSupportedAuthTypes(final Response response) {
        final List<Object> authSchemes = response.getHeaders().get("WWW-Authenticate");
        if (authSchemes == null) {
            return new String[0];
        } else {
            return authSchemes.stream() //
                    .map(o -> (String) o) //
                    .map(h -> h.split(" ")[0]) //
                    .toArray(String[]::new);
        }
    }

    private Response invoke(final Invocation invocation) throws IOException {
        try (AuthenticationCloseable c = ThreadLocalHTTPAuthenticator.suppressAuthenticationPopups()) {
            final Future<Response> responseFuture = invocation.submit(Response.class);
            try {
                return responseFuture.get(m_config.getReadTimeout().getSeconds(), TimeUnit.SECONDS);
            } catch (ExecutionException e) { // NOSONAR we are rethrowing the cause (the ExecutionException itself is
                                             // uninteresting)
                String errorMsg = null;
                final Throwable t = ExceptionUtil.getDeepestError(e);

                if (t instanceof ConnectException) {
                    errorMsg = "Unable to connect: Probably the host and/or port are incorrect.";
                } else if (t instanceof UnknownHostException) {
                    errorMsg = "Unable to connect: The host is unkown.";
                } else if (t instanceof SocketTimeoutException) {
                    errorMsg = "Unable to connect: Connection timed out.";
                }

                if (errorMsg == null) {
                    throw ExceptionUtil.wrapAsIOException(t); // NOSONAR we are rethrowing the cause
                } else {
                    throw new IOException(errorMsg, t);
                }
            } catch (TimeoutException e) {
                throw ExceptionUtil.wrapAsIOException(e);
            } catch (InterruptedException e) { // NOSONAR rethrown as InterruptedIOException
                responseFuture.cancel(true);
                throw (IOException) new InterruptedIOException().initCause(e);
            }
        }
    }

    /**
     * Creates a new {@link HttpClient} instance.
     *
     * @param cfg
     *            The HTTP Connection config
     * @return The client to be used for the request.
     * @throws IOException
     */
    static HttpClient create(final HttpFSConnectionConfig cfg) throws IOException {
        final ClientBuilder clientBuilder = ClientBuilder.newBuilder();
        if (cfg.isSslTrustAllCertificates()) {
            try {
                clientBuilder.getConfiguration();
                final SSLContext context = SSLContext.getInstance("TLS");
                context.init(null, null, null);
                clientBuilder.sslContext(context);
            } catch (final NoSuchAlgorithmException | KeyManagementException e) {
                throw ExceptionUtil.wrapAsIOException(e);
            }
        }

        if (cfg.sslIgnoreHostnameMismatches()) {
            clientBuilder.hostnameVerifier((hostName, session) -> true); // NOSONAR user is supposed to control hostname
                                                                         // verification
        }

        clientBuilder.property(Message.CONNECTION_TIMEOUT, cfg.getConnectionTimeout().toMillis());
        clientBuilder.property(Message.RECEIVE_TIMEOUT, cfg.getReadTimeout().toMillis());
        return new HttpClient(cfg, clientBuilder.build());
    }

    /**
     * Closes this client and releases all resources held.
     */
    void close() {
        m_client.close();
    }
}
