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
 *   2020-10-02 (Vyacheslav Soldatov): created
 */
package org.knime.ext.ftp.filehandling.testing;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ftpserver.ConnectionConfig;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.Ftplet;
import org.apache.ftpserver.ftplet.User;
import org.apache.ftpserver.impl.DefaultConnectionConfig;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.ssl.SslConfigurationFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.ConcurrentLoginPermission;
import org.apache.ftpserver.usermanager.impl.TransferRatePermission;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.eclipse.jetty.proxy.ConnectHandler;
import org.eclipse.jetty.proxy.ProxyServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.knime.core.node.NodeLogger;
import org.knime.ext.ftp.filehandling.fs.FtpConnectionConfiguration;
import org.knime.ext.ftp.filehandling.fs.ProtectedHostConfiguration;
import org.knime.filehandling.core.connections.FSFiles;

/**
 * Helper class for embedded FTP server.
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
public class EmbeddedFtpServerContainer {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(EmbeddedFtpServerContainer.class);

    private final Path m_testsHome;
    private final FtpServerFactory m_serverFactory;
    private final FtpConnectionConfiguration m_configuration;
    private final FtpServer m_server;
    private final Server m_proxyServer;

    private final Map<String, Ftplet> m_ftplets = new HashMap<>();

    /**
     * @param ftpCfg
     * @throws IOException
     * @throws FtpException
     *
     */
    public EmbeddedFtpServerContainer(final FtpConnectionConfiguration ftpCfg) throws IOException, FtpException {
        // FTP server
        ftpCfg.setHost("localhost");
        ftpCfg.setPort(getFreePort());

        m_configuration = ftpCfg;

        // create tests working directory
        m_testsHome = Files.createTempDirectory("knime-itests");

        // create server
        m_serverFactory = createServerFactory();
        // set reference to ftplets
        m_serverFactory.setFtplets(m_ftplets);

        // creare server
        m_server = m_serverFactory.createServer();

        // Proxy server
        final ProtectedHostConfiguration proxy = ftpCfg.getProxy();
        if (proxy != null) {
            proxy.setHost("localhost");
            proxy.setPort(getFreePort());

            try {
                m_proxyServer = createProxyServer();
            } catch (Exception ex) {
                throw new IOException("Failed to start proxy server", ex);
            }
        } else {
            m_proxyServer = null;
        }
    }

    private Server createProxyServer() throws Exception {
        final Server server = new Server(m_configuration.getProxy().getPort());

        final ConnectHandler proxy = new ConnectHandler() {
            /**
             * {@inheritDoc}
             */
            @Override
            protected boolean handleAuthentication(final HttpServletRequest request, final HttpServletResponse response,
                    final String address) {
                return handleProxyAuthentication(request);
            }
        };
        server.setHandler(proxy);
        final ServletContextHandler context = new ServletContextHandler(proxy, "/", ServletContextHandler.SESSIONS);
        final ServletHolder proxyServlet = new ServletHolder(ProxyServlet.class);
        context.addServlet(proxyServlet, "/*");

        server.start();
        return server;
    }

    /**
     * @param request
     *            HTTP servlet request.
     * @return true if successfully authenticated.
     */
    protected boolean handleProxyAuthentication(final HttpServletRequest request) {
        final String user = m_configuration.getProxy().getUser();
        if (user == null) {
            // not configured to use authentication.
            return true;
        }

        final String authHeader = request.getHeader("Proxy-Authorization");
        final String encodedUserNamePassword = authHeader.substring(authHeader.indexOf("Basic ") + "Basic ".length());
        final String userNamePassword = new String(Base64.getDecoder().decode(encodedUserNamePassword));

        final int offset = userNamePassword.indexOf(':');
        if (user.equals(userNamePassword.subSequence(0, offset))
                && m_configuration.getProxy().getPassword().equals(userNamePassword.substring(offset + 1))) {
            return true;
        }

        return false;
    }

    private static int getFreePort() throws IOException {
        // get free server socket port
        final int port;
        try (final ServerSocket serverSocket = new ServerSocket(0);) {
            port = serverSocket.getLocalPort();
        }
        return port;
    }

    private FtpServerFactory createServerFactory()
            throws FtpException, IOException {
        // start FTP server
        final FtpServerFactory serverFactory = new FtpServerFactory();

        final ConnectionConfig connectionConfig = new DefaultConnectionConfig(m_configuration.getUser().equals("anonymous"),
                Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, 0);
        serverFactory.setConnectionConfig(connectionConfig);

        // add user manager
        final InMemoryUserManager userManager = new InMemoryUserManager();
        // add default user which will used for client connections
        final User user = createUser(m_testsHome, m_configuration.getUser(), m_configuration.getPassword());
        userManager.save(user);
        serverFactory.setUserManager(userManager);

        // create listener on specified port
        final ListenerFactory factory = new ListenerFactory();
        factory.setPort(m_configuration.getPort());

        // if working director specified, should create it
        final File workDir = (File) serverFactory.getFileSystem().createFileSystemView(user)
                .getFile(m_configuration.getWorkingDirectory()).getPhysicalFile();
        if (!workDir.exists()) {
            workDir.mkdirs();
        }

        // configure SSL
        if (m_configuration.isUseSsl()) {
            final SslConfigurationFactory ssl = new SslConfigurationFactory();
            ssl.setKeystorePassword("changeit");
            ssl.setKeyAlias("ftpd");
            ssl.setKeyPassword("changeit");

            final File keyStoreFile = crateTemporaryKeyStoreFile();
            try {
                ssl.setKeystoreFile(keyStoreFile);
                factory.setSslConfiguration(ssl.createSslConfiguration());
            } finally {
                keyStoreFile.delete();
            }

            factory.setImplicitSsl(false);
        }

        // replace the default listener
        serverFactory.addListener("default", factory.createListener());

        return serverFactory;
    }

    private static File crateTemporaryKeyStoreFile() throws IOException {
        final Path path = Files.createTempFile("junit-keystore-", ".jks");
        try (final InputStream in = EmbeddedFtpServerContainer.class.getResourceAsStream("keystore.jks")) {
            Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
        }
        return path.toFile();
    }

    private static User createUser(final Path testsHome, final String userName, final String password)
            throws IOException {
        final BaseUser user = new BaseUser();
        user.setName(userName);
        user.setPassword(password);

        // create user home directory
        final Path userHome = testsHome.resolve(userName);
        Files.createDirectories(userHome);
        user.setHomeDirectory(userHome.toString());

        // add authorities
        final List<Authority> authorities = new LinkedList<>();
        // add permission to login
        authorities.add(new ConcurrentLoginPermission(Integer.MAX_VALUE, Integer.MAX_VALUE));
        // add permission to write file to user home
        authorities.add(new WritePermission());
        // add permissions to unlimited transfer files
        authorities.add(new TransferRatePermission(Integer.MAX_VALUE, Integer.MAX_VALUE));
        user.setAuthorities(authorities);

        return user;
    }

    /**
     * Starts embedded FTP server and returns connection configuration.
     *
     * @return FTP connection.
     * @throws IOException
     * @throws FtpException
     */
    public FtpConnectionConfiguration startAndGetConnectionConfiguration() throws IOException, FtpException {
        m_server.start();
        return m_configuration;
    }

    /**
     * @return ftplet map.
     */
    public Map<String, Ftplet> getFtplets() {
        return m_ftplets;
    }

    /**
     * Stops embedded FTP server.
     */
    public void stopServer() {
        try {
            m_proxyServer.stop();
        } catch (Exception ex) {
            LOGGER.error("Failed to stop proxy server", ex);
        } finally {
            m_server.stop();
        }
    }

    /**
     * Clears FTP command intercepters.
     */
    public void clearFtplets() {
        m_ftplets.clear();
    }

    /**
     * Clears tests working directory.
     */
    public void clearTestHome() {
        try {
            clearFtplets();
            FSFiles.deleteRecursively(m_testsHome);
        } catch (IOException ex) {
            LOGGER.warn("Failed to clear test home " + m_testsHome, ex);
        }
    }

    /**
     * Convert part from FTP path to real file system path.
     *
     * @param file
     *            FTP file name.
     * @return local file system absolute path.
     * @throws IOException
     */
    public String convertToRealPath(final String file) throws IOException {
        try {
            final User user = m_serverFactory.getUserManager().getUserByName(m_configuration.getUser());
            final File physicalFile = (File) m_serverFactory.getFileSystem().createFileSystemView(user).getFile(file)
                    .getPhysicalFile();
            return physicalFile.getAbsolutePath();
        } catch (FtpException ex) {
            throw new IOException("Failed to convert FTP path to real", ex);
        }
    }
}
