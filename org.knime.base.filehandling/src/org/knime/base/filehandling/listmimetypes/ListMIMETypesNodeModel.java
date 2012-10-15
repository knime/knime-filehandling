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
package org.knime.base.filehandling.listmimetypes;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import javax.activation.MimetypesFileTypeMap;

import org.knime.base.filehandling.mime.MIMEMap;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

import com.sun.activation.registries.MimeTypeEntry;
import com.sun.activation.registries.MimeTypeFile;

/**
 * This is the model implementation of Unzip.
 * 
 * 
 * @author Patrick Winter, University of Konstanz
 */
class ListMIMETypesNodeModel extends NodeModel {

    /**
     * Constructor for the node model.
     */
    protected ListMIMETypesNodeModel() {
        super(0, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        // Create output spec and container
        DataTableSpec outSpec = createOutSpec();
        BufferedDataContainer outContainer = exec.createDataContainer(outSpec);
        // Retrieve mime type entries
        List<MimeTypeEntry> entries = getMimeTypeEntries();
        // Add entries into container
        for (int i = 0; i < entries.size(); i++) {
            DataCell[] cells = new DataCell[2];
            cells[0] = new StringCell(entries.get(i).getFileExtension());
            cells[1] = new StringCell(entries.get(i).getMIMEType());
            outContainer.addRowToTable(new DefaultRow("Row" + i, cells));
        }
        outContainer.close();
        return new BufferedDataTable[]{outContainer.getTable()};
    }

    /**
     * Will build a list of all MIME-Types registered in the
     * <code>MIMEMap</code> (using reflection).
     * 
     * 
     * @return List of all known MIME-Types
     * @throws Exception If reflection has not worked
     */
    @SuppressWarnings("unchecked")
    private List<MimeTypeEntry> getMimeTypeEntries() throws Exception {
        // List of results
        List<MimeTypeEntry> entries = new LinkedList<MimeTypeEntry>();
        // Get map used by MIMEMap
        MimetypesFileTypeMap mimeMap = MIMEMap.getMimeMap();
        // Get the mime type files used by the map
        Field mimeMapField = MimetypesFileTypeMap.class.getDeclaredField("DB");
        mimeMapField.setAccessible(true);
        MimeTypeFile[] db = (MimeTypeFile[])mimeMapField.get(mimeMap);
        // Go through each mime type file
        for (int i = 0; i < db.length; i++) {
            // Get hashtable of the file
            Field typeHashField =
                    MimeTypeFile.class.getDeclaredField("type_hash");
            typeHashField.setAccessible(true);
            Hashtable<Object, Object> typeHash =
                    (Hashtable<Object, Object>)typeHashField.get(db[i]);
            // Go through each hashtable entry
            Enumeration<Object> keys = typeHash.keys();
            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                MimeTypeEntry entry = (MimeTypeEntry)typeHash.get(key);
                // Add mime type entry
                entries.add(entry);
            }
        }
        return entries;
    }

    /**
     * Factory method for the output table spec.
     * 
     * 
     * @return Output table spec
     */
    private DataTableSpec createOutSpec() {
        DataColumnSpec[] columnSpecs = new DataColumnSpec[2];
        columnSpecs[0] =
                new DataColumnSpecCreator("File extension", StringCell.TYPE)
                        .createSpec();
        columnSpecs[1] =
                new DataColumnSpecCreator("MIME-Type", StringCell.TYPE)
                        .createSpec();
        return new DataTableSpec(columnSpecs);
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
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return new DataTableSpec[]{createOutSpec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        // Not used
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // Not used
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // Not used
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
