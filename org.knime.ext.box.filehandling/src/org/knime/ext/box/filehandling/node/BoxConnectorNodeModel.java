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
 *   2023-02-15 (Alexander Bondaletov): created
 */
package org.knime.ext.box.filehandling.node;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;

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
import org.knime.credentials.base.CredentialPortObject;
import org.knime.credentials.base.CredentialPortObjectSpec;
import org.knime.credentials.base.oauth.api.AccessTokenCredential;
import org.knime.ext.box.filehandling.fs.BoxFSConnection;
import org.knime.ext.box.filehandling.fs.BoxFSConnectionConfig;
import org.knime.filehandling.core.connections.FSConnectionRegistry;
import org.knime.filehandling.core.defaultnodesettings.status.NodeModelStatusConsumer;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage.MessageType;
import org.knime.filehandling.core.port.FileSystemPortObject;
import org.knime.filehandling.core.port.FileSystemPortObjectSpec;

/**
 * Node model for the Box Connector
 *
 * @author Alexander Bondaletov, Redfield SE
 */
public class BoxConnectorNodeModel extends NodeModel {
    private static final String FILE_SYSTEM_NAME = "Box";

    private final NodeModelStatusConsumer m_statusConsumer = new NodeModelStatusConsumer(
            EnumSet.of(MessageType.ERROR, MessageType.WARNING));

    private String m_fsId;
    private BoxFSConnection m_fsConnection;
    private final BoxConnectorSettings m_settings;

    /**
     * Creates new instance
     */
    protected BoxConnectorNodeModel() {
        super(new PortType[] { CredentialPortObject.TYPE }, new PortType[] { FileSystemPortObject.TYPE });
        m_settings = new BoxConnectorSettings();
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        m_statusConsumer.setWarningsIfRequired(this::setWarningMessage);
        m_settings.validate();

        if (inSpecs[0] != null) {
            final var optType = ((CredentialPortObjectSpec) inSpecs[0]).getCredentialType();
            if (optType.isPresent() && optType.get() != AccessTokenCredential.TYPE) {
                throw new InvalidSettingsException(String.format("Provided credential (via input port) is of type '%s', but should be '%s'",//
                        optType.get().getName(),//
                        AccessTokenCredential.TYPE.getName()));
            }
        }

        m_fsId = FSConnectionRegistry.getInstance().getKey();
        return new PortObjectSpec[] { createSpec() };
    }

    private FileSystemPortObjectSpec createSpec() {
        return new FileSystemPortObjectSpec(FILE_SYSTEM_NAME, //
                m_fsId, //
                BoxFSConnectionConfig.createFSLocationSpec());
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        var credential = ((CredentialPortObject) inObjects[0]).getCredential(AccessTokenCredential.class)
                .orElseThrow(() -> new InvalidSettingsException(
                        "Credential is not available. Please re-execute the authenticator node"));
        var token = credential.getAccessToken();

        var config = m_settings.createFSConnectionConfig(token);
        m_fsConnection = new BoxFSConnection(config);

        FSConnectionRegistry.getInstance().register(m_fsId, m_fsConnection);

        return new PortObject[] { new FileSystemPortObject(createSpec()) };
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        setWarningMessage("Connection no longer available. Please re-execute the node.");
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // noting to save
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveForModel(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.validateSettings(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadForModel(settings);
    }

    @Override
    protected void onDispose() {
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
