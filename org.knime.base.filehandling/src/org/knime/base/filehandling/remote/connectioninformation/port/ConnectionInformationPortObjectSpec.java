/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 *   Nov 13, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.remote.connectioninformation.port;

import javax.swing.JComponent;

import org.apache.commons.lang.ObjectUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.port.AbstractSimplePortObjectSpec;

/**
 * Spec for the connection information port object.
 * 
 * 
 * @author Patrick Winter, KNIME.com, Zurich, Switzerland
 */
public class ConnectionInformationPortObjectSpec extends AbstractSimplePortObjectSpec {

    private ConnectionInformation m_connectionInformation;

    /**
     * Create default port object spec without connection information.
     */
    public ConnectionInformationPortObjectSpec() {
        m_connectionInformation = null;
    }

    /**
     * Create specs that contain connection information.
     * 
     * 
     * @param connectionInformation The content of this port object
     */
    public ConnectionInformationPortObjectSpec(final ConnectionInformation connectionInformation) {
        if (connectionInformation == null) {
            throw new NullPointerException("List argument must not be null");
        }
        m_connectionInformation = connectionInformation;
    }

    /**
     * Return the connection information contained by this port object spec.
     * 
     * 
     * @return The content of this port object
     */
    public ConnectionInformation getConnectionInformation() {
        return m_connectionInformation;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JComponent[] getViews() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object ospec) {
        if (ospec == this) {
            return true;
        }
        if (!(ospec instanceof ConnectionInformationPortObjectSpec)) {
            return false;
        }
        ConnectionInformationPortObjectSpec oCIPOS = (ConnectionInformationPortObjectSpec)ospec;
        return ObjectUtils.equals(m_connectionInformation, oCIPOS.m_connectionInformation);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return m_connectionInformation == null ? 0 : m_connectionInformation.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void save(final ModelContentWO model) {
        m_connectionInformation.save(model);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void load(final ModelContentRO model) throws InvalidSettingsException {
        m_connectionInformation = ConnectionInformation.load(model);
    }

}
