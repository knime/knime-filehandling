/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 * History
 *   Oct 11, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.knime.core.util.proxy.search.GlobalProxySearch;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 */
public final class FilehandlingPlugin extends Plugin {

    /** The plug-in ID. */
    public static final String PLUGIN_ID = "org.knime.base.filehandling";

    // The shared instance
    private static FilehandlingPlugin plugin;

    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
    }

    /**
     * This method is called when the plug-in is stopped.
     *
     * @param context The bundle context.
     * @throws Exception If cause by super class.
     */
    @Override
    public void stop(final BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance.
     *
     * @return The shared instance
     */
    public static FilehandlingPlugin getDefault() {
        return plugin;
    }

    /**
     * Opens an input stream to the file, defining the MIME-Types.
     *
     *
     * @return Input stream to the MIME-Type file
     * @throws IOException If the file is unreadable
     */
    public InputStream getMIMETypeStream() throws IOException {
        Bundle utilBundle = Platform.getBundle("org.knime.core.util");
        URL mimeFile = utilBundle.getResource("META-INF/mime.types");
        return mimeFile.openStream();
    }

    /**
     * No-op, returns {@code null} and will be removed.
     *
     * @return the Eclipse Proxy Service
     * @since 3.7
     * @deprecated use {@link GlobalProxySearch} instead
     */
    @Deprecated(since = "5.3", forRemoval = true)
    public static IProxyService getProxyService() {
        return null;
    }
}
