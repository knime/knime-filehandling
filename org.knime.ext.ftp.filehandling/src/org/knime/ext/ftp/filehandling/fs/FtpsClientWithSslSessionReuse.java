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
 *   2020-10-13 (Vyacheslav Soldatov): created
 */
package org.knime.ext.ftp.filehandling.fs;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.Locale;

import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.SSLSocket;

import org.apache.commons.net.ftp.FTPSClient;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.CheckUtils;

/**
 * This is hacking style implementation of session reuse FTPS client. </br>
 *
 * To quote <a href=
 * "https://eng.wealthfront.com/2016/06/10/connecting-to-an-ftps-server-with-ssl-session-reuse-in-java-7-and-8/">Connecting
 * to an FTPS Server with SSL Session Reuse in Java 7 and 8</a>:
 *
 * "...vsftpd (and most other FTPS servers) requires SSL session reuse between
 * the control and data connections as a security measure: essentially, the
 * server requires that the SSL session used for data transfer is the same as
 * that used for the connection to the command port (port 21). This ensures that
 * the party that initially authenticated is the same as the party sending or
 * retrieving data, thereby preventing someone from hijacking a data connection
 * after authentication in a classic man-in-the-middle attack"
 *
 * There is also an unresolved Jira issue for SSL session reuse in the Apache
 * Commons Net project: https://issues.apache.org/jira/browse/NET-408
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
class FtpsClientWithSslSessionReuse extends FTPSClient {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(FtpsClientWithSslSessionReuse.class);

    private final SslSessionReuseFailureListener m_sessionReuseFailureListener;

    interface SslSessionReuseFailureListener {
        void notifyFailure();
    }

    /**
     * Default constructor.
     *
     * @param failureListener
     *            Listener that gets notified when SSL session reuse has failed.
     */
    public FtpsClientWithSslSessionReuse(final SslSessionReuseFailureListener failureListener) {
        super("TLS", false);
        CheckUtils.checkArgumentNotNull(failureListener, "SslSessionReuseFailureListener must not be null");
        m_sessionReuseFailureListener = failureListener;

        // By default, FTPSClient does not check the hostname against the server
        // certificate. This sets the Apache HttpClient hostname verifier which
        // seems like a solid choice.
        setHostnameVerifier(new DefaultHostnameVerifier());
    }

    /**
     * copied and adapted from https://eng.wealthfront.com/2016/06/10/
     * connecting-to-an-ftps-server-with-ssl-session-reuse-in-java-7-and-8/
     */
    @Override
    protected void _prepareDataSocket_(final Socket socket) throws IOException {
        if (!(socket instanceof SSLSocket)) {
            return;
        }

        try {
            final SSLSession sslSession = ((SSLSocket) _socket_).getSession();
            if (!sslSession.isValid()) {
                // Some servers do not support SSL session reuse. In this case, the SSLSession
                // will have been invalidated prior to invoking this method and we cannot
                // reuse it (see AP-16122)
                m_sessionReuseFailureListener.notifyFailure();
                super._prepareDataSocket_(socket);
                return;
            }

            final SSLSessionContext sslSessionContext = sslSession.getSessionContext();

            // get session cache
            final Field sessionHostPortCache = sslSessionContext.getClass().getDeclaredField("sessionHostPortCache");
            sessionHostPortCache.setAccessible(true);
            final Object cache = sessionHostPortCache.get(sslSessionContext);

            // get put method of session cache
            final Method put = cache.getClass().getDeclaredMethod("put", Object.class, Object.class);
            put.setAccessible(true);

            // calling of getHost method returns the host like it is used
            // by SSL session context
            final Method getHost = socket.getClass().getDeclaredMethod("getHost");
            getHost.setAccessible(true);

            final Object host = getHost.invoke(socket);
            // cache key is just 'host:port'
            final String key = String.format("%s:%s", host, String.valueOf(socket.getPort())).toLowerCase(Locale.ROOT);

            // put existing session to cache of given context.
            put.invoke(cache, key, sslSession);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException
                | NoSuchMethodException | InvocationTargetException ex) {
            LOGGER.error(
                    "The SSL session reuse feature is not working on this Java version. Default method will be called instead.",
                    ex);
            super._prepareDataSocket_(socket);
        }
    }
}
