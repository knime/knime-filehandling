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
 *   Oct 11, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.mime;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.activation.MimetypesFileTypeMap;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.knime.base.filehandling.FilehandlingPlugin;
import org.knime.core.node.NodeLogger;

/**
 * Utility class for a singleton <code>MimetypesFileTypeMap</code>.
 * 
 * 
 * @author Patrick Winter, University of Konstanz
 */
public final class MIMEMap {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(MIMEMap.class);

    /**
     * Path to the resource folder of the project.
     */
    private static final String RESOURCEPATH = FilehandlingPlugin.getDefault()
            .getPluginRootPath()
            + File.separator
            + "resources"
            + File.separator;

    private static final String EXTENSIONPOINT_ID =
            "org.knime.base.filehandling.mimetypes";

    private static MimetypesFileTypeMap mimeMap = null;

    private MIMEMap() {
        // Disable default constructor
    }

    /**
     * @param fileextension The file extension to search for
     * @return MIME-Type for the given file extension
     */
    public static String getMIMEType(final String fileextension) {
        init();
        return mimeMap.getContentType("." + fileextension.toLowerCase());
    }

    /**
     * Searches for all MIME-Types registered through the extension point.
     * 
     * 
     * @return MIME-Types in mime.types format
     */
    public static String[] getTypesFromExtensions() {
        // Get extensions
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint(EXTENSIONPOINT_ID);
        IExtension[] extensions = point.getExtensions();
        // Add all configuration elements to one list
        ArrayList<IConfigurationElement> allElements =
                new ArrayList<IConfigurationElement>();
        for (IExtension ext : extensions) {
            IConfigurationElement[] elements = ext.getConfigurationElements();
            allElements.addAll(Arrays.asList(elements));
        }
        String[] types = new String[allElements.size()];
        // Add each element
        for (int i = 0; i < allElements.size(); i++) {
            // Get MIME-Type
            String type = allElements.get(i).getAttribute("name");
            String fileextensions = "";
            IConfigurationElement[] children = allElements.get(i).getChildren();
            // Get file extensions
            for (int j = 0; j < children.length; j++) {
                fileextensions +=
                        " " + children[j].getAttribute("name").toLowerCase();
            }
            types[i] = type + fileextensions;
            LOGGER.debug("Found MIME-Type \"" + type
                    + "\" for file extensions \""
                    + fileextensions.replaceFirst(" ", "") + "\"");
        }
        return types;
    }
    
    /**
     * @return The used <code>MimetypesFileTypeMap</code>
     */
    public static MimetypesFileTypeMap getMimeMap() {
        init();
        return mimeMap;
    }

    /**
     * Initializes the mime map if it has not been initialized before. Should be
     * could before every operation on <code>mimeMap</code>.
     */
    private static void init() {
        if (mimeMap == null) {
            try {
                mimeMap =
                        new MimetypesFileTypeMap(new FileInputStream(new File(
                                RESOURCEPATH, "mime.types")));
            } catch (FileNotFoundException e) {
                // If the file is not readable use default MIME-Types
                mimeMap = new MimetypesFileTypeMap();
            }
        }
        // Add MIME-Types defined by other plugins
        addFromExtensions();
    }

    /**
     * Adds MIME-Types added through the extension point into the mime map.
     */
    private static void addFromExtensions() {
        String[] types = getTypesFromExtensions();
        for (int i = 0; i < types.length; i++) {
            mimeMap.addMimeTypes(types[i]);
        }
    }

}
