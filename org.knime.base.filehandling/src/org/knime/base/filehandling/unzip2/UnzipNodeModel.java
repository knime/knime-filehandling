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
 *   Sep 5, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.unzip2;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FilenameUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.uri.URIContent;
import org.knime.core.data.uri.URIDataCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * This is the model implementation.
 * 
 * 
 * @author Patrick Winter, KNIME.com, Zurich, Switzerland
 */
class UnzipNodeModel extends NodeModel {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(UnzipNodeModel.class);

    private SettingsModelString m_source;

    private SettingsModelString m_targetdirectory;

    private SettingsModelString m_output;

    private SettingsModelString m_ifexists;

    /**
     * Constructor for the node model.
     */
    protected UnzipNodeModel() {
        super(0, 1);
        m_source = SettingsFactory.createSourceSettings();
        m_targetdirectory = SettingsFactory.createTargetDirectorySettings();
        m_output = SettingsFactory.createOutputSettings();
        m_ifexists = SettingsFactory.createIfExistsSettings();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception {
        // Create output spec and container
        DataTableSpec outSpec = createOutSpec();
        BufferedDataContainer outContainer = exec.createDataContainer(outSpec);
        // Get sourcefile
        File source = new File(textToPath(m_source.getStringValue()));
        // Extract files from archive
        unarchive(source, outContainer, exec);
        outContainer.close();
        return new BufferedDataTable[]{outContainer.getTable()};
    }

    /**
     * Extracts the files from the source file into the configured directory.
     * 
     * @param source Source archive containing the files.
     * @param outContainer Container that will be filled with the URIs/locations
     *            of the extracted files
     * @param exec Execution context to check for cancellation
     * @throws Exception If an error occurs
     */
    private void unarchive(final File source, final BufferedDataContainer outContainer, final ExecutionContext exec)
            throws Exception {
        int rowID = 0;
        boolean abort = m_ifexists.getStringValue().equals(OverwritePolicy.ABORT.getName());
        File targetDirectory = new File(m_targetdirectory.getStringValue());
        // Create input stream
        // Autodetection of type (needs buffered stream)
        InputStream in = new BufferedInputStream(openUncompressStream(source));
        ArchiveInputStream input = new ArchiveStreamFactory().createArchiveInputStream(in);
        ArchiveEntry entry;
        // Process each archive entry
        while ((entry = input.getNextEntry()) != null) {
            exec.checkCanceled();
            // Create target file for this entry
            File target = new File(targetDirectory, entry.getName());
            if (entry.isDirectory()) {
                // Create directory if not there
                // Only important if an empty directory is inside the archive
                target.mkdirs();
            } else {
                // If target exists either throw exception or inform user of
                // replacement
                if (target.exists()) {
                    if (abort) {
                        throw new Exception("File \"" + target.getAbsolutePath() + "\" exists, overwrite policy: \""
                                + m_ifexists.getStringValue() + "\"");
                    } else {
                        LOGGER.info("Replacing file " + target.getAbsolutePath());
                    }
                }
                // Create dirs if necessary
                target.mkdirs();
                // Delete old file if there is one and create new one
                target.delete();
                target.createNewFile();
                OutputStream out = new FileOutputStream(target);
                // Copy content of current entry to target file
                IOUtils.copy(input, out);
                out.close();
                // Create row with path or URI
                DataCell cell = null;
                String outputSelection = m_output.getStringValue();
                if (outputSelection.equals(OutputSelection.LOCATION.getName())) {
                    cell = new StringCell(target.getAbsolutePath());
                }
                if (outputSelection.equals(OutputSelection.URI.getName())) {
                    URI uri = target.getAbsoluteFile().toURI();
                    String extension = FilenameUtils.getExtension(uri.getPath());
                    URIContent content = new URIContent(uri, extension);
                    cell = new URIDataCell(content);
                }
                outContainer.addRowToTable(new DefaultRow("Row" + rowID, cell));
                rowID++;
            }
        }
        input.close();
    }

    /**
     * Uncompresses the given file.
     * 
     * @param source The potentially compressed source file
     * @return Uncompressed version of the source, or source if it was not
     *         compressed
     * @throws Exception If IOException occurred
     */
    private InputStream openUncompressStream(final File source) throws Exception {
        InputStream uncompressStream;
        // Buffered stream needet for autodetection of type
        InputStream in = new BufferedInputStream(new FileInputStream(source));
        try {
            // Try to create a compressor for the source, throws exception if
            // source is not compressed
            uncompressStream = new CompressorStreamFactory().createCompressorInputStream(in);
        } catch (CompressorException e) {
            // Source is not compressed
            uncompressStream = in;
        }
        return uncompressStream;
    }

    /**
     * Tries to create a path from the passed string.
     * 
     * @param text the string to transform into a path
     * @return Path if entered value could be properly transformed
     */
    private static String textToPath(final String text) {
        String path;
        try {
            URL url = new URL(text);
            try {
                URI uri = url.toURI();
                path = uri.getPath();
            } catch (URISyntaxException e) {
                path = url.getPath();
            }
        } catch (MalformedURLException e) {
            // see if they specified a file without giving the protocol
            File tmp = new File(text);

            // if that blows off we let the exception go up the stack.
            path = tmp.getAbsolutePath();
        }
        return path;
    }

    /**
     * Factory method for the output table spec.
     * 
     * 
     * @return Output table spec
     */
    private DataTableSpec createOutSpec() {
        DataColumnSpec columnSpec = null;
        String output = m_output.getStringValue();
        if (output.equals(OutputSelection.LOCATION.getName())) {
            columnSpec = new DataColumnSpecCreator("Location", StringCell.TYPE).createSpec();
        }
        if (output.equals(OutputSelection.URI.getName())) {
            columnSpec = new DataColumnSpecCreator("URI", URIDataCell.TYPE).createSpec();
        }
        return new DataTableSpec(columnSpec);
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
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        // Is the source set?
        if (m_source.getStringValue().equals("")) {
            throw new InvalidSettingsException("Source not set");
        }
        // Is the target set?
        if (m_targetdirectory.getStringValue().equals("")) {
            throw new InvalidSettingsException("Target directory not set");
        }
        // Does the target directory exist?
        File targetdirectory = new File(m_targetdirectory.getStringValue());
        if (!targetdirectory.isDirectory()) {
            throw new InvalidSettingsException("Target directory does not exist");
        }
        return new DataTableSpec[]{createOutSpec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_source.saveSettingsTo(settings);
        m_targetdirectory.saveSettingsTo(settings);
        m_output.saveSettingsTo(settings);
        m_ifexists.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_source.loadSettingsFrom(settings);
        m_targetdirectory.loadSettingsFrom(settings);
        m_output.loadSettingsFrom(settings);
        m_ifexists.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_source.validateSettings(settings);
        m_targetdirectory.validateSettings(settings);
        m_output.validateSettings(settings);
        m_ifexists.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // Not used
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // Not used
    }

}