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
 *   2020-10-16 (Vyacheslav Soldatov): created
 */
package org.knime.ext.ftp.filehandling.node;

import org.eclipse.core.net.proxy.IProxyData;

/**
 * Unit test implementation of {@link IProxyData}
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
class ProxyDataImpl implements IProxyData {
    private String m_host;
    private int m_port;
    private String m_user;
    private String m_password;

    /**
     * @param host
     *            proxy host.
     * @param port
     *            proxy port.
     */
    public ProxyDataImpl(final String host, final int port) {
        m_host = host;
        m_port = port;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getType() {
        return IProxyData.HTTP_PROXY_TYPE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHost() {
        return m_host;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setHost(final String host) {
        m_host = host;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPort() {
        return m_port;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setPort(final int port) {
        m_port = port;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUserId() {
        return m_user;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setUserid(final String userid) {
        m_user = userid;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPassword() {
        return m_password;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setPassword(final String password) {
        m_password = password;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRequiresAuthentication() {
        return m_user != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void disable() {
        m_host = null;
        m_port = -1;
        m_user = null;
        m_password = null;
    }
}
