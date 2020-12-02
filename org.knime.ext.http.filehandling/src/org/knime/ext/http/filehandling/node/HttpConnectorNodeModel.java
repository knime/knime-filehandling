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
 *   2020-11-18 (Bjoern Lohrmann): created
 */
package org.knime.ext.http.filehandling.node;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.Function;

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
import org.knime.core.node.workflow.ICredentials;
import org.knime.ext.http.filehandling.fs.HttpConnectionConfig;
import org.knime.ext.http.filehandling.fs.HttpFSConnection;
import org.knime.ext.http.filehandling.fs.HttpFileSystem;
import org.knime.ext.http.filehandling.node.HttpAuthenticationSettings.AuthType;
import org.knime.filehandling.core.connections.FSConnectionRegistry;
import org.knime.filehandling.core.connections.base.auth.UserPasswordAuthProviderSettings;
import org.knime.filehandling.core.port.FileSystemPortObject;
import org.knime.filehandling.core.port.FileSystemPortObjectSpec;

/**
 * HTTP(S) Connector node.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public class HttpConnectorNodeModel extends NodeModel {

    private static final String FILE_SYSTEM_NAME = "HTTP";

    private final HttpConnectorNodeSettings m_settings;

    private String m_fsId;

    private HttpFSConnection m_fsConnection;

    /**
     * Creates new instance.
     */
    protected HttpConnectorNodeModel() {
        super(new PortType[0], new PortType[] { FileSystemPortObject.TYPE });
        m_settings = new HttpConnectorNodeSettings();
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        m_settings.validate();
        m_settings.configureInModel(inSpecs, m -> {
        }, getCredentialsProvider());
        m_fsId = FSConnectionRegistry.getInstance().getKey();
        return new PortObjectSpec[] { createSpec() };
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        final HttpFSConnection fsConnection = createConnection(m_settings, name -> getCredentialsProvider().get(name));
        testConnection(fsConnection);
        m_fsConnection = fsConnection;
        FSConnectionRegistry.getInstance().register(m_fsId, m_fsConnection);
        return new PortObject[] { new FileSystemPortObject(createSpec()) };
    }

    private static void testConnection(final HttpFSConnection fsConnection) throws IOException {
        @SuppressWarnings("resource")
        final HttpFileSystem fs = fsConnection.getFileSystem();
        try {
            Files.readAttributes(fs.getWorkingDirectory(), BasicFileAttributes.class);
        } catch (NoSuchFileException e) { // NOSONAR ignore, all good for now
        }
    }

    private FileSystemPortObjectSpec createSpec() {
        return new FileSystemPortObjectSpec(FILE_SYSTEM_NAME, m_fsId,
                HttpFileSystem.createFSLocationSpec(m_settings.getUrl()));
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

    /**
     * @param settings
     *            The connector node settings.
     * @param credentialsProvider
     *            A credentials provider.
     * @return the {@link HttpFSConnection}
     * @throws IOException
     * @throws InvalidSettingsException
     */
    private static HttpFSConnection createConnection(final HttpConnectorNodeSettings settings,
            final Function<String, ICredentials> credentialsProvider) throws IOException, InvalidSettingsException {

        final HttpConnectionConfig cfg = new HttpConnectionConfig(settings.getUrl());
        cfg.setSslIgnoreHostnameMismatches(settings.sslIgnoreHostnameMismatches());
        cfg.setSslTrustAllCertificates(settings.sslTrustAllCertificates());

        if (settings.getAuthenticationSettings().getAuthType().equals(HttpAuth.BASIC)) {
            cfg.setAuthType(AuthType.BASIC);

            final UserPasswordAuthProviderSettings userPassSettings = settings.getAuthenticationSettings()
                    .getSettingsForAuthType(HttpAuth.BASIC);
            cfg.setUsername(userPassSettings.getUser(credentialsProvider));
            cfg.setPassword(userPassSettings.getPassword(credentialsProvider));
        }

        cfg.setConnectionTimeout(settings.getConnectionTimeout());
        cfg.setReadTimeout(settings.getReadTimeout());
        cfg.setFollowRedirects(settings.followRedirects());

        return new HttpFSConnection(cfg);
    }
}
