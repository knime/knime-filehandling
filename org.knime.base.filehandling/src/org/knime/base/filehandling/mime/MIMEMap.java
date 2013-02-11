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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

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
 * @author Patrick Winter, KNIME.com, Zurich, Switzerland
 */
public final class MIMEMap {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(MIMEMap.class);

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
    private static MIMETypeEntry[] getTypesFromExtensions() {
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
        MIMETypeEntry[] entries = new MIMETypeEntry[allElements.size()];
        // Add each element
        for (int i = 0; i < entries.length; i++) {
            // Get MIME-Type
            String type = allElements.get(i).getAttribute("name");
            entries[i] = new MIMETypeEntry(type);
            IConfigurationElement[] children = allElements.get(i).getChildren();
            // Get file extensions
            for (int j = 0; j < children.length; j++) {
                String fileextension =
                        " " + children[j].getAttribute("name").toLowerCase();
                entries[i].addExtension(fileextension);
                LOGGER.debug("Found MIME-Type \"" + type
                        + "\" for file extension \"" + fileextension + "\"");
            }
        }
        return entries;
    }

    /**
     * @return The MIME-Types contained in the <code>mime.types</code> file
     */
    private static MIMETypeEntry[] getTypesFromFile() {
        // (duplicated java code as the MimetypesFileTypeMap class doesn't give
        // us all the information we need).
        List<MIMETypeEntry> entries = new LinkedList<MIMETypeEntry>();
        try {
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(FilehandlingPlugin
                            .getDefault().getMIMETypeStream()));
            String line;
            // Every line is a new MIME-Type
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.startsWith("#")) {
                    // Split on space or tab
                    String[] tokens = line.split("[ \t]+");
                    if (tokens.length > 0) {
                        // Create MIME-Entry (first token is always the name)
                        MIMETypeEntry entry = new MIMETypeEntry(tokens[0]);
                        // All other tokens are extensions to this MIME-Type
                        for (int i = 1; i < tokens.length; i++) {
                            entry.addExtension(tokens[i]);
                        }
                        entries.add(entry);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to parse mime.types config file", e);
            // If file is not readable return nothing
        }
        return entries.toArray(new MIMETypeEntry[entries.size()]);
    }

    /**
     * @return All the registered MIME-Types
     */
    public static MIMETypeEntry[] getAllTypes() {
        MIMETypeEntry[] fromFile = getTypesFromFile();
        MIMETypeEntry[] fromExtension = getTypesFromExtensions();
        // Append fromExtension to fromFile
        MIMETypeEntry[] result =
                Arrays.copyOf(fromFile, fromFile.length + fromExtension.length);
        System.arraycopy(fromExtension, 0, result, fromFile.length,
                fromExtension.length);
        return result;
    }

    /**
     * Initializes the mime map if it has not been initialized before. Should be
     * called before every operation on <code>mimeMap</code>.
     */
    private static void init() {
        if (mimeMap == null) {
            try {
                mimeMap =
                        new MimetypesFileTypeMap(FilehandlingPlugin
                                .getDefault().getMIMETypeStream());
            } catch (IOException e) {
                LOGGER.error("Failed to parse mime.types config file", e);
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
        MIMETypeEntry[] types = getTypesFromExtensions();
        for (int i = 0; i < types.length; i++) {
            mimeMap.addMimeTypes(types[i].toString());
        }
    }

}
