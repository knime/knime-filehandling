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
 *   Sep 5, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.remote.connectioninformation.node;

import java.io.File;
import java.io.IOException;

import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformation;
import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformationPortObject;
import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformationPortObjectSpec;
import org.knime.base.filehandling.remote.files.ConnectionMonitor;
import org.knime.base.filehandling.remote.files.RemoteFileFactory;
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

/**
 * This is the model implementation.
 * 
 * 
 * @author Patrick Winter, University of Konstanz
 */
public class ConnectionInformationNodeModel extends NodeModel {

    private Protocol m_protocol;

    private ConnectionInformationConfiguration m_configuration;

    /**
     * Constructor for the node model.
     * 
     * @param protocol The protocol of this connection information model
     */
    public ConnectionInformationNodeModel(final Protocol protocol) {
        super(new PortType[]{},
                new PortType[]{ConnectionInformationPortObject.TYPE});
        m_protocol = protocol;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects,
            final ExecutionContext exec) throws Exception {
        // Create connection information object
        ConnectionInformation connectionInformation =
                new ConnectionInformation();
        // Put settings into object
        connectionInformation.setProtocol(m_protocol.getName());
        connectionInformation.setHost(m_configuration.getHost());
        connectionInformation.setPort(m_configuration.getPort());
        if (!m_configuration.getAuthenticationmethod().equals(
                AuthenticationMethod.NONE.getName())) {
            connectionInformation.setUser(m_configuration.getUser());
            connectionInformation.setPassword(m_configuration.getPassword());
        }
        if (m_protocol.hasKeyfileSupport()
                && m_configuration.getAuthenticationmethod().equals(
                        AuthenticationMethod.KEYFILE.getName())) {
            connectionInformation.setKeyfile(m_configuration.getKeyfile());
        }
        if (m_protocol.hasCertificateSupport()
                && m_configuration.getUsecertificate()) {
            connectionInformation.setCertificate(m_configuration
                    .getCertificate());
        }
        // Test connection
        if (m_configuration.getTestconnection()) {
            try {
                RemoteFileFactory.createRemoteFile(
                        connectionInformation.toURI(), connectionInformation);
                ConnectionMonitor.closeAll();
            } catch (Exception e) {
                throw new Exception("Connection to "
                        + connectionInformation.toURI() + " failed");
            }
        }
        // Return port object with connection information
        return new PortObject[]{new ConnectionInformationPortObject(
                connectionInformation)};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // Not used
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        // Return empty port object spec
        return new PortObjectSpec[]{new ConnectionInformationPortObjectSpec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_configuration != null) {
            m_configuration.save(settings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        ConnectionInformationConfiguration config =
                new ConnectionInformationConfiguration(m_protocol);
        config.loadAndValidate(settings);
        m_configuration = config;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        new ConnectionInformationConfiguration(m_protocol)
                .loadAndValidate(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // Not used
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // Not used
    }

}
