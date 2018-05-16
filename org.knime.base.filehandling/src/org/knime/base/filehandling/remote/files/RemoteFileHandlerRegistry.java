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
 * -------------------------------------------------------------------
 */

package org.knime.base.filehandling.remote.files;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.NodeLogger;


/**
 * {@link RemoteFileHandler} registry that manages the registered {@link RemoteFileHandler}s.
 *
 * @author Tobias Koetter
 * @since 2.11
 */
public final class RemoteFileHandlerRegistry {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(RemoteFileHandlerRegistry.class);

    /**The id of the extension point.*/
    public static final String EXT_POINT_ID = "org.knime.base.filehandling.RemoteFileHandler";

    /**The attribute of the extension point.*/
    public static final String EXT_POINT_ATTR_DF = "RemoteFileHandler";

    private final Map<String, RemoteFileHandler<? extends Connection>> m_handlerCache = new HashMap<>();

    private static volatile RemoteFileHandlerRegistry instance;

    private RemoteFileHandlerRegistry() {
        registerExtensionPoints();
    }

    /**
     * Returns the only instance of this class.
     * @return the only instance
     */
    public static RemoteFileHandlerRegistry getInstance() {
        if (instance == null) {
            synchronized (RemoteFileHandlerRegistry.class) {
                if (instance == null) {
                    instance = new RemoteFileHandlerRegistry();
                }
            }
        }
        return instance;
    }

    /**
     * Registers all extension point implementations.
     */
    private void registerExtensionPoints() {
        try {
            final IExtensionRegistry registry = Platform.getExtensionRegistry();
            final IExtensionPoint point = registry.getExtensionPoint(EXT_POINT_ID);
            if (point == null) {
                LOGGER.error("Invalid extension point: " + EXT_POINT_ID);
                throw new IllegalStateException("ACTIVATION ERROR: --> Invalid extension point: " + EXT_POINT_ID);
            }
            for (final IConfigurationElement elem : point.getConfigurationElements()) {
                final String operator = elem.getAttribute(EXT_POINT_ATTR_DF);
                final String decl = elem.getDeclaringExtension().getUniqueIdentifier();
                if (operator == null || operator.isEmpty()) {
                    LOGGER.error("The extension '" + decl + "' doesn't provide the required attribute '"
                            + EXT_POINT_ATTR_DF + "'");
                    LOGGER.error("Extension " + decl + " ignored.");
                    continue;
                }
                try {
                    final RemoteFileHandler<? extends Connection> handler =
                            (RemoteFileHandler<? extends Connection>)elem.createExecutableExtension(EXT_POINT_ATTR_DF);
                    addRemoteFileHandler(handler);
                } catch (final Throwable t) {
                    LOGGER.error("Problems during initialization of RemoteFileHandler (with id '" + operator + "'.)", t);
                    if (decl != null) {
                        LOGGER.error("Extension " + decl + " ignored.");
                    }
                }
            }
        } catch (final Exception e) {
            LOGGER.error("Exception while registering RemoteFileHandler extensions");
        }
    }

    /**
     * @param protocolName The name of the protocol
     * @return the default port for the given protocol name
     */
    public static int getDefaultPort(final String protocolName) {
        final Protocol protocol = getProtocol(protocolName);
        return protocol != null ? protocol.getPort() : -1;
    }

    /**
     * Get the correspondent protocol to the name.
     * @param protocolName The name of the protocol
     * @return {@link Protocol} to the name
     */
    public static Protocol getProtocol(final String protocolName) {
        return getProtocol(protocolName, getRemoteFileHandler(protocolName));
    }

    /**
     * Get the correspondent protocol to the name.
     * @param protocolName The name of the protocol
     * @param handler the {@link RemoteFileHandler} that shall be used.
     * @return {@link Protocol} to the name
     *
     * @since 3.6
     */
    public static Protocol getProtocol(final String protocolName, final RemoteFileHandler<? extends Connection> handler) {
        if (handler == null) {
            return null;
        }
        final Protocol[] supportedProtocols = handler.getSupportedProtocols();
        for (final Protocol protocol : supportedProtocols) {
            if (protocol.getName().equals(protocolName)) {
                return protocol;
            }
        }
        return null;
    }

    /**
     * @return Array with all protocol names
     */
    public static String[] getAllProtocols() {
        return getInstance().m_handlerCache.keySet().toArray(new String[0]);
    }

    /**
     * @return unmodifiable {@link Collection} with all protocol strings
     */
    public static Collection<String> getProtocols() {
        return Collections.unmodifiableCollection(instance.m_handlerCache.keySet());
    }

    /**
     * @param handler the {@link RemoteFileHandler} to register
     * @throws IllegalArgumentException if a handler for the given protocol exists
     * @see #getInstance()
     */
    public void addRemoteFileHandler(final RemoteFileHandler<? extends Connection> handler)
            throws IllegalArgumentException {
        final Protocol[] protocols = handler.getSupportedProtocols();
        for (final Protocol protocol : protocols) {
            if (containsHandler(protocol.getName())) {
                /* As the remote file handler can be accessed via the connection information this error is rather obsolete. */
                // throw new IllegalArgumentException("Duplicate handler found for protocol:" + protocol.getName());
                return;
            }
            m_handlerCache.put(protocol.getName(), handler);
        }
    }


    /**
     * @param name the name of the {@link RemoteFileHandler}
     * @return <code>true</code> if a handler for the given protocol already exists
     * @see #getInstance()
     */
    public boolean containsHandler(final String name) {
        return m_handlerCache.containsKey(name);
    }

    /**
     * @param protocolName the protocol name to get the file handler for
     * @return the {@link RemoteFileHandler} for the given protocol or <code>null</code> if the protocol is unknown
     */
    public static RemoteFileHandler<? extends Connection> getRemoteFileHandler(final String protocolName) {
        return getInstance().m_handlerCache.get(protocolName);
    }
}
