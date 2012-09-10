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
 *   Sep 3, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.zip;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * This is the model implementation of Zip.
 * 
 * 
 * @author Patrick Winter, University of Konstanz
 */
class ZipNodeModel extends NodeModel {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(ZipNodeModel.class);

    private SettingsModelString m_urlcolumn;

    private SettingsModelString m_target;

    private SettingsModelString m_pathhandling;

    private SettingsModelString m_prefix;

    private SettingsModelString m_ifexists;

    private SettingsModelIntegerBounded m_compressionlevel;

    private long m_processedSize;

    private long m_size;

    private Set<String> m_newfiles;

    /**
     * Constructor for the node model.
     */
    protected ZipNodeModel() {
        super(1, 0);
        m_urlcolumn = SettingsFactory.createURLColumnSettings();
        m_target = SettingsFactory.createTargetSettings();
        m_pathhandling = SettingsFactory.createPathHandlingSettings();
        m_prefix = SettingsFactory.createPrefixSettings(m_pathhandling);
        m_ifexists = SettingsFactory.createIfExistsSettings();
        m_compressionlevel = SettingsFactory.createCompressionLevelSettings();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        List<String> entries = new LinkedList<String>();
        String column = m_urlcolumn.getStringValue();
        String target = m_target.getStringValue();
        String ifExists = m_ifexists.getStringValue();
        File targetFile = new File(target);
        File oldFile = new File(target + ".old");
        // Abort if zip file exists and policy is abort
        if (ifExists.equals(OverwritePolicy.ABORT.getName())
                && targetFile.exists()) {
            throw new RuntimeException("File \"" + targetFile.getAbsolutePath()
                    + "\" exists, overwrite policy: \"" + ifExists + "\"");
        }
        // Move old zip file if policy is overwrite
        if (ifExists.equals(OverwritePolicy.OVERWRITE.getName())) {
            if (targetFile.renameTo(oldFile)) {
                LOGGER.info("Replacing existing zip file \""
                        + targetFile.getAbsolutePath() + "\"");
            }
        }
        // Read filenames from table
        int index = inData[0].getDataTableSpec().findColumnIndex(column);
        for (DataRow row : inData[0]) {
            if (!row.getCell(index).isMissing()) {
                entries.add(row.getCell(index).toString());
            }
        }
        String[] filenames = entries.toArray(new String[entries.size()]);
        // Write files to zip file
        writeToZip(filenames, target, exec);
        return new BufferedDataTable[0];
    }

    /**
     * Puts all the given files into a zip file.
     * 
     * 
     * Behavior is controlled by the overwrite policy in the if existing
     * setting.
     * 
     * @param filenames Path to the files
     * @param target Path to the zip file
     * @param exec Execution context for <code>checkCanceled()</code> and
     *            <code>setProgress()</code>
     * @throws Exception When abort condition is met or user canceled
     */
    private void writeToZip(final String[] filenames, final String target,
            final ExecutionContext exec) throws Exception {
        m_processedSize = 0;
        m_size = 0;
        m_newfiles = new HashSet<String>();
        ZipOutputStream zout = null;
        File newFile = new File(target);
        File oldFile = new File(target + ".old");
        boolean fileExists = newFile.exists();
        // Create files for each filename
        File[] files = new File[filenames.length];
        for (int i = 0; i < files.length; i++) {
            files[i] = new File(filenames[i]);
        }
        try {
            // Resolve directories
            files = resolveDirectories(files);
            // Rename existing file
            if (fileExists) {
                oldFile.delete();
                newFile.renameTo(oldFile);
            }
            newFile.getParentFile().mkdirs();
            zout = new ZipOutputStream(new FileOutputStream(newFile));
            zout.setLevel(m_compressionlevel.getIntValue());
            // Calculate size of files
            m_size = sizeOfFiles(files);
            // Copy existing files into new zip file
            if (fileExists) {
                for (int i = 0; i < files.length; i++) {
                    m_newfiles.add(getName(files[i]));
                }
                // Add size of files in the old zip file
                m_size += checkFilesInZip(oldFile, exec);
                addOldFiles(oldFile, zout, exec);
            }
            // Add new files to zip file
            for (int i = 0; i < files.length; i++) {
                addFile(files[i], zout, exec);
            }
            zout.close();
            // Remove old file
            oldFile.delete();
        } catch (Exception e) {
            // Close all streams and restore old file if node got aborted
            if (zout != null) {
                try {
                    zout.close();
                } catch (Exception e2) {
                    // Occurs when the zip file has no entry
                }
            }
            // Remove unfinished new file and restore old file
            newFile.delete();
            oldFile.renameTo(newFile);
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Adds files from an old zip file into the zip stream.
     * 
     * 
     * Behavior is controlled by the overwrite policy in the if existing
     * setting.
     * 
     * @param oldFile Zip file that contains the files to copy
     * @param zout Zip stream where the files get added
     * @param files Name of the files that will later be added
     * @param size Total size of all files that go into the zip
     * @param exec Execution context for <code>checkCanceled()</code> and
     *            <code>setProgress()</code>
     * @throws Exception When abort condition is met or user canceled
     */
    private void addOldFiles(final File oldFile, final ZipOutputStream zout,
            final ExecutionContext exec) throws Exception {
        FileInputStream in = new FileInputStream(oldFile);
        ZipInputStream zin = new ZipInputStream(in);
        try {
            byte[] buffer = new byte[1024];
            ZipEntry entry = zin.getNextEntry();
            while (entry != null) {
                String name = entry.getName();
                boolean notInFiles = true;
                // Check if new files contain a file by the same name
                if (m_newfiles.contains(name)) {
                    exec.checkCanceled();
                    notInFiles = false;
                }
                if (notInFiles) {
                    // Add file to new zip file
                    zout.putNextEntry(new ZipEntry(name));
                    int length;
                    while ((length = zin.read(buffer)) > 0) {
                        exec.checkCanceled();
                        exec.setProgress((double)m_processedSize / m_size);
                        zout.write(buffer, 0, length);
                        m_processedSize += length;
                    }
                } else {
                    LOGGER.info("Replacing existing file \"" + name + "\"");
                }
                entry = zin.getNextEntry();
            }
        } finally {
            in.close();
            zin.close();
        }
    }

    /**
     * Calculates the size off all files in a zip file.
     * 
     * 
     * @param file The zip file
     * @param exec Execution context for <code>checkCanceled()</code>
     * @return Uncompressed size of files in the zip file
     * @throws Exception If the file can not be read or user canceled
     */
    private long checkFilesInZip(final File file, final ExecutionContext exec)
            throws Exception {
        String ifExists = m_ifexists.getStringValue();
        long size = 0;
        FileInputStream in = null;
        ZipInputStream zin = null;
        try {
            in = new FileInputStream(file);
            zin = new ZipInputStream(in);
            ZipEntry entry = zin.getNextEntry();
            while (entry != null) {
                exec.checkCanceled();
                if (!m_newfiles.contains(entry.getName())) {
                    size += entry.getSize();
                } else if (ifExists.equals(OverwritePolicy.APPEND_ABORT
                        .getName())) {
                    throw new IOException("File \"" + entry.getName()
                            + "\" exists in zip file, overwrite"
                            + " policy: \"" + ifExists + "\"");
                }
                entry = zin.getNextEntry();
            }
        } finally {
            if (in != null) {
                in.close();
            }
            if (zin != null) {
                zin.close();
            }
        }
        return size;
    }

    /**
     * Calculate the total size of the given files.
     * 
     * 
     * Calculate the sum of all the files sizes. Goes recursively through
     * directories.
     * 
     * @param files The files for the calculation
     * @return Size of all the files
     */
    private long sizeOfFiles(final File[] files) {
        long size = 0;
        for (int i = 0; i < files.length; i++) {
            size += files[i].length();
        }
        return size;
    }

    /**
     * Adds the given file into the zip stream.
     * 
     * 
     * @param file The file to add
     * @param zout The zip stream where the file will be added
     * @param exec Execution context for <code>checkCanceled()</code> and
     *            <code>setProgress()</code>
     * @throws Exception If the file can not be read or user canceled
     */
    private void addFile(final File file, final ZipOutputStream zout,
            final ExecutionContext exec) throws Exception {
        FileInputStream in = null;
        try {
            byte[] buffer = new byte[1024];
            exec.setProgress((double)m_processedSize / m_size);
            in = new FileInputStream(file);
            String filename = getName(file);
            zout.putNextEntry(new ZipEntry(filename));
            int length;
            while ((length = in.read(buffer)) > 0) {
                exec.checkCanceled();
                exec.setProgress((double)m_processedSize / m_size);
                zout.write(buffer, 0, length);
                m_processedSize += length;
            }
            zout.closeEntry();
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    /**
     * Returns the correct file name according to the path handling policy.
     * 
     * 
     * @param file File for the name
     * @return Name of the given file with cut path
     */
    private String getName(final File file) {
        String name = file.getAbsolutePath();
        String pathhandling = m_pathhandling.getStringValue();
        String prefix = m_prefix.getStringValue();
        if (pathhandling.equals(PathHandling.ONLY_FILENAME.getName())) {
            name = file.getName();
        }
        if (pathhandling.equals(PathHandling.TRUNCATE_PREFIX.getName())) {
            name = name.replaceFirst(prefix, "");
        }
        return name;
    }

    /**
     * Replaces directories in the given file array by all contained files.
     * 
     * 
     * @param files Array of files that potentially contains directories
     * @return List of all files with directories resolved
     */
    private File[] resolveDirectories(final File[] files) {
        List<File> allFiles = new LinkedList<File>();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                File[] innerFiles = resolveDirectories(files[i].listFiles());
                for (int j = 0; j < innerFiles.length; j++) {
                    allFiles.add(innerFiles[j]);
                }
            } else {
                allFiles.add(files[i]);
            }
        }
        return allFiles.toArray(new File[allFiles.size()]);
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
        if (m_urlcolumn.getStringValue().equals("")) {
            throw new InvalidSettingsException("URL column not set");
        }
        int columnIndex =
                inSpecs[0].findColumnIndex(m_urlcolumn.getStringValue());
        if (columnIndex < 0) {
            throw new InvalidSettingsException("URL column not set");
        }
        if (m_target.getStringValue().equals("")) {
            throw new InvalidSettingsException("Target not set");
        }
        if (m_pathhandling.getStringValue().equals(
                PathHandling.TRUNCATE_PREFIX.getName())
                && !new File(m_prefix.getStringValue()).exists()) {
            throw new InvalidSettingsException(
                    "Prefix directory does not exist");
        }
        return new DataTableSpec[]{};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_urlcolumn.saveSettingsTo(settings);
        m_target.saveSettingsTo(settings);
        m_ifexists.saveSettingsTo(settings);
        m_prefix.saveSettingsTo(settings);
        m_pathhandling.saveSettingsTo(settings);
        m_compressionlevel.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_urlcolumn.loadSettingsFrom(settings);
        m_target.loadSettingsFrom(settings);
        m_ifexists.loadSettingsFrom(settings);
        m_prefix.loadSettingsFrom(settings);
        m_pathhandling.loadSettingsFrom(settings);
        m_compressionlevel.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_urlcolumn.validateSettings(settings);
        m_target.validateSettings(settings);
        m_ifexists.validateSettings(settings);
        m_prefix.validateSettings(settings);
        m_pathhandling.validateSettings(settings);
        m_compressionlevel.validateSettings(settings);
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
