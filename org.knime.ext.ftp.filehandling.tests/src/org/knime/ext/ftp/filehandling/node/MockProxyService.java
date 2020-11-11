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
 *   2020-10-16 (soldatov): created
 */
package org.knime.ext.ftp.filehandling.node;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.net.proxy.IProxyChangeListener;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.CoreException;

/**
 * Unit test implementation of {@link IProxyService}
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
public class MockProxyService implements IProxyService {
    private boolean m_enabled;
    private final Map<String, IProxyData> m_proxies = new HashMap<>();

    /**
     * Default constructor.
     */
    public MockProxyService() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setProxiesEnabled(final boolean enabled) {
        m_enabled = enabled;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isProxiesEnabled() {
        return m_enabled;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasSystemProxies() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSystemProxiesEnabled(final boolean enabled) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSystemProxiesEnabled() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IProxyData[] getProxyData() {
        Collection<IProxyData> proxies = m_proxies.values();
        return proxies.toArray(new IProxyData[proxies.size()]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IProxyData[] select(final URI uri) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated
     */
    @Deprecated
    @Override
    public IProxyData[] getProxyDataForHost(final String host) {
        throw new UnsupportedOperationException("Deprecated");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IProxyData getProxyData(final String type) {
        return m_proxies.get(type);
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated
     */
    @Deprecated
    @Override
    public IProxyData getProxyDataForHost(final String host, final String type) {
        throw new UnsupportedOperationException("Deprecated");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setProxyData(final IProxyData[] proxies) throws CoreException {
        for (IProxyData data : proxies) {
            m_proxies.put(data.getType(), data);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getNonProxiedHosts() {
        return new String[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setNonProxiedHosts(final String[] hosts) throws CoreException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addProxyChangeListener(final IProxyChangeListener listener) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeProxyChangeListener(final IProxyChangeListener listener) {
    }

    /**
     * Clears the service.
     */
    public void clear() {
        m_proxies.clear();
    }
}
