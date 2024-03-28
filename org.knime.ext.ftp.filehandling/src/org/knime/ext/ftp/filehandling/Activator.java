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
 *   2020-10-15 (Vyacheslav Soldatov): created
 */
package org.knime.ext.ftp.filehandling;

import org.eclipse.core.net.proxy.IProxyService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
public class Activator implements BundleActivator {

    private static BundleContext bundleContext = null;
    private static ServiceReference<IProxyService> proxyServiceRef;
    private static IProxyService proxyService;

    @Override
    public void start(final BundleContext context) throws Exception {
        synchronized (Activator.class) {
            bundleContext = context;
        }
        setSystemTLSProperty();
    }

    private static void setSystemTLSProperty() {
        try {
            // according to https://issues.apache.org/jira/browse/NET-408 discussion
            // for JDK 8u161 or higher in order to support FTPS session reuse
            // following property must be disabled
            System.setProperty("jdk.tls.useExtendedMasterSecret", String.valueOf(false));

        } catch (SecurityException ex) { // NOSONAR can be ignored
        }
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        synchronized (Activator.class) {
            if (proxyServiceRef != null) {
                context.ungetService(proxyServiceRef);
                proxyServiceRef = null;
            }
        }
    }

    /**
     * Calling this method will initialize the {@link IProxyService}.
     *
     * @return Proxy service.
     */
    public static synchronized IProxyService getProxyService() {
        if (proxyServiceRef == null) {
            // using the proxy service class object here initializes the service indirectly
            // by loading the class, and subsequently, its plugin activator being called
            proxyServiceRef = bundleContext.getServiceReference(IProxyService.class);
            proxyService = bundleContext.getService(proxyServiceRef);
        }

        return proxyService;
    }
}
