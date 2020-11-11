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
 *   2020-10-03 (Vyacheslav Soldatov): created
 */
package org.knime.ext.ftp.filehandling.fs;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.SocketException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPHTTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;

/**
 * Factory of FTP clients.
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
public class FtpClientFactory {
    private final FtpConnectionConfiguration m_configuration;
    private FtpClientFeatures m_features;

    /**
     * @param cfg
     *            connection configuration.
     */
    public FtpClientFactory(final FtpConnectionConfiguration cfg) {
        m_configuration = cfg;
    }

    /**
     * @return new FTP client instance.
     * @throws IOException
     */
    public FtpClient createClient() throws IOException {
        FTPClient ftpClient = createNativeClient();
        if (m_features == null) {
            m_features = FtpClientFeatures.autodetect(ftpClient);
        }
        return new FtpClient(ftpClient, m_features);
    }

    /**
     * @param m_configuration
     *            configuration.
     * @throws IOException
     * @throws SocketException
     */
    private FTPClient createNativeClient() throws IOException {
        final FTPClientConfig ftpConfig = new FTPClientConfig();
        ftpConfig.setServerTimeZoneId(constructServerTimeZoneId());

        // create native FTP client configuration.
        final FTPClient client;
        if (m_configuration.getProxy() != null) {

            if (m_configuration.isUseSsl()) {
                // in given implementation FTPS can't run over HTTP proxy
                throw new IOException("FTPS over HTTP proxy is not supported");
            }

            final ProtectedHostConfiguration proxy = m_configuration.getProxy();
            client = new FTPHTTPClient(proxy.getHost(), proxy.getPort(), proxy.getUser(), proxy.getPassword());
        } else if (m_configuration.isUseSsl()) {

            final FTPSClient ftpsClient = new FtpsClientWithSslSessionReuse();
            ftpsClient.setUseClientMode(true);
            ftpsClient.setDefaultPort(m_configuration.getPort());
            client = ftpsClient;
        } else {
            client = new FTPClient();
        }

        if (m_configuration.isTestMode()) {
            configureTestMode(client);
        }
        client.configure(ftpConfig);

        final int timeOut = m_configuration.getConnectionTimeOut();
        client.setConnectTimeout(timeOut);
        client.setDefaultTimeout(timeOut);

        // connect
        client.connect(m_configuration.getHost(), m_configuration.getPort());

        // setup any after connected
        client.setSoTimeout(timeOut);
        client.setListHiddenFiles(true);
        client.enterLocalPassiveMode();

        if (m_configuration.isUseSsl()) {
            final FTPSClient ftpsClient = (FTPSClient) client;
            // remove data buffer limit
            ftpsClient.execPBSZ(0);
            // set data channel encrypted
            ftpsClient.execPROT("P");
        }

        client.login(m_configuration.getUser(), m_configuration.getPassword());
        if (!FTPReply.isPositiveCompletion(client.getReplyCode())) {
            throw new IOException("Authentication failed: " + client.getReplyString());
        }

        // postconfigure connection
        if (!client.setFileTransferMode(FTP.STREAM_TRANSFER_MODE) || !client.setFileStructure(FTP.FILE_STRUCTURE)
                || !client.setFileType(FTP.BINARY_FILE_TYPE)) {
            throw new IOException("Failed to correct configure client: " + client.getReplyString());
        }

        return client;
    }

    /**
     * @param client
     *            FTP client.
     */
    private void configureTestMode(final FTPClient client) {
        if (m_configuration.isUseSsl()) {
            ((FTPSClient) client).setHostnameVerifier((h, s) -> true); // NOSONAR just for test mode.
        }
        client.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out))); // NOSONAR just
    }

    /**
     * @return time zone ID in form GMT+XX:XX
     */
    private String constructServerTimeZoneId() {
        long serverTimeZoneOffset = Math.abs(m_configuration.getServerTimeZoneOffset());

        long hoursOffset = TimeUnit.MILLISECONDS.toHours(serverTimeZoneOffset);

        StringBuilder sb = new StringBuilder("GMT");
        // add sign
        sb.append(m_configuration.getServerTimeZoneOffset() < 0 ? '-' : '+');
        // add hours
        sb.append(hoursOffset);
        if (sb.length() < 6) {
            sb.insert(4, '0');
        }

        sb.append(':');

        // add minutes
        long minutesOffset = TimeUnit.MILLISECONDS
                .toHours(Math.abs(serverTimeZoneOffset) - TimeUnit.HOURS.toMillis(hoursOffset));
        sb.append(minutesOffset);
        if (sb.length() < 9) {
            sb.insert(7, '0');
        }

        return sb.toString();
    }
}
