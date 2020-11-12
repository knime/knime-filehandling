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
 *   2020-10-01 (Vyacheslav Soldatov): created
 */
package org.knime.ext.ftp.filehandling.node;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.node.workflow.ICredentials;
import org.knime.ext.ftp.filehandling.Activator;
import org.knime.ext.ftp.filehandling.fs.FtpConnectionConfiguration;
import org.knime.ext.ftp.filehandling.fs.FtpFSConnection;
import org.knime.ext.ftp.filehandling.fs.FtpFileSystem;
import org.knime.ext.ftp.filehandling.fs.ProtectedHostConfiguration;
import org.knime.filehandling.core.connections.FSConnectionRegistry;
import org.knime.filehandling.core.port.FileSystemPortObject;
import org.knime.filehandling.core.port.FileSystemPortObjectSpec;

/**
 * FTP Connection node.
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
public class FtpConnectionNodeModel extends NodeModel {

    private static final String FILE_SYSTEM_NAME = "FTP";

    private final FtpConnectionSettingsModel m_settings;

    private String m_fsId;

    private FtpFSConnection m_fsConnection;

    /**
     * Creates new instance.
     */
    protected FtpConnectionNodeModel() {
        super(new PortType[0], new PortType[] { FileSystemPortObject.TYPE });
        m_settings = new FtpConnectionSettingsModel();
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        m_fsId = FSConnectionRegistry.getInstance().getKey();
        return new PortObjectSpec[] { createSpec() };
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        m_fsConnection = createConnection(m_settings, getCredentialsProvider());
        FSConnectionRegistry.getInstance().register(m_fsId, m_fsConnection);
        return new PortObject[] { new FileSystemPortObject(createSpec()) };
    }

    /**
     * @param settings
     *            FTP model settings.
     * @param credentialsProvider
     *            credentials provider.
     * @return FTP connection.
     * @throws IOException
     * @throws InvalidSettingsException
     */
    public static FtpFSConnection createConnection(final FtpConnectionSettingsModel settings,
            final CredentialsProvider credentialsProvider) throws IOException, InvalidSettingsException {
        FtpConnectionConfiguration conf = createConfiguration(settings, credentialsProvider::get,
                Activator.getProxyService());
        return new FtpFSConnection(conf);
    }

    /**
     * @param settings
     *            FTP model settings.
     * @param credentialsProvider
     *            credentials provider.
     * @param proxyService
     *            proxy service supplier
     * @return FTP connection configuration.
     * @throws InvalidSettingsException
     */
    public static FtpConnectionConfiguration createConfiguration(final FtpConnectionSettingsModel settings,
            final Function<String, ICredentials> credentialsProvider, final IProxyService proxyService)
            throws InvalidSettingsException {
        final FtpConnectionConfiguration conf = new FtpConnectionConfiguration();
        conf.setHost(settings.getHost());
        conf.setPort(settings.getPort());
        conf.setMaxConnectionPoolSize(settings.getMaxConnectionPoolSize());
        conf.setMinConnectionPoolSize(settings.getMinConnectionPoolSize());
        conf.setCoreConnectionPoolSize(settings.getCoreConnectionPoolSize());
        conf.setMaxIdleTime(TimeUnit.SECONDS.toMillis(settings.getMaxIdleTime()));
        conf.setConnectionTimeOut(settings.getConnectionTimeout());
        conf.setServerTimeZoneOffset(settings.getTimeZoneOffset());
        conf.setUseSsl(settings.isUseSsl());
        conf.setWorkingDirectory(settings.getWorkingDirectory());

        // authentication
        final FtpAuthenticationSettingsModel auth = settings.getAuthenticationSettings();
        switch (auth.getAuthType()) {
        case ANONYMOUS:
            conf.setUser("anonymous");
            conf.setPassword("");
            break;
        case CREDENTIALS:
            ICredentials creds = getCredentials(credentialsProvider, auth.getCredential());
            conf.setUser(creds.getLogin());
            conf.setPassword(creds.getPassword());
            break;
        case USER_PWD:
            conf.setUser(auth.getUserModel().getStringValue());
            conf.setPassword(auth.getPasswordModel().getStringValue());
            break;
        default:
            break;
        }

        // Proxy
        if (settings.isUseProxy()) {
            final ProtectedHostConfiguration proxy = new ProtectedHostConfiguration();
            IProxyData proxyData = proxyService.getProxyData(IProxyData.HTTP_PROXY_TYPE);
            if (proxyData == null) {
                throw new InvalidSettingsException("Eclipse HTTP proxy is not configured");
            }

            proxy.setHost(proxyData.getHost());
            proxy.setPort(proxyData.getPort());
            proxy.setUser(proxyData.getUserId());
            proxy.setPassword(proxyData.getPassword());

            conf.setProxy(proxy);
        }

        return conf;
    }

    private static ICredentials getCredentials(final Function<String, ICredentials> credentialsProvider,
            final String credential) throws InvalidSettingsException {
        final ICredentials creds = credentialsProvider.apply(credential);
        if (creds == null) {
            throw new InvalidSettingsException("Credentials '" + credential + "' not found");
        }
        return creds;
    }

    private FileSystemPortObjectSpec createSpec() {
        return new FileSystemPortObjectSpec(FILE_SYSTEM_NAME, m_fsId,
                FtpFileSystem.createFSLocationSpec(m_settings.getHost()));
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        setWarningMessage("Connection no longer available. Please re-execute the node.");
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // nothing to save
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettingsForModel(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.validateSettings(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadSettingsForModel(settings);
    }

    @Override
    protected void onDispose() {
        // close the file system also when the workflow is closed
        reset();
    }

    @Override
    protected void reset() {
        if (m_fsConnection != null) {
            m_fsConnection.closeInBackground();
            m_fsConnection = null;
        }
        m_fsId = null;
    }
}
