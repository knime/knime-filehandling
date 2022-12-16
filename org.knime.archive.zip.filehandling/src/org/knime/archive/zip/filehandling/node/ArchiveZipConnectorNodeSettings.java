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
 *   2022-04-27 (Dragan Keselj): created
 */
package org.knime.archive.zip.filehandling.node;

import java.io.Closeable;
import java.io.IOException;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.knime.archive.zip.filehandling.fs.ArchiveZipFSConnectionConfig;
import org.knime.archive.zip.filehandling.fs.ArchiveZipFileSystem;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.defaultnodesettings.EnumConfig;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;

/**
 * Settings for the ZIP Archive Connector.
 *
 * @author Dragan Keselj, KNIME GmbH
 */
class ArchiveZipConnectorNodeSettings {

    private static final String KEY_WORKING_DIRECTORY = "workingDirectory";
    private static final String KEY_FILE = "file";
    private static final String KEY_ENCODING = "encoding";

    private static final String[] FILE_EXTENSIONS = { ".zip", ".jar" };
    private static final String DEFAULT_ENCODING = "CP437";

    private static final String CFG_USE_DEFAULT_ENCODING = "useDefaultEncoding";

    private NodeCreationConfiguration m_nodeCreationConfig;

    private final SettingsModelString m_workingDirectory;
    private final SettingsModelReaderFileChooser m_file;

    private final SettingsModelBoolean m_useDefaultEncodingModel;
    private final SettingsModelString m_encoding;

    /**
     * @param cfg
     *            node creation configuration.
     */
    public ArchiveZipConnectorNodeSettings(final NodeCreationConfiguration cfg) {
        m_nodeCreationConfig = cfg;

        m_file = new SettingsModelReaderFileChooser( //
                KEY_FILE, //
                cfg.getPortConfig().orElseThrow(() -> new IllegalStateException("port creation config is absent")), //
                ArchiveZipConnectorNodeFactory.FS_CONNECT_GRP_ID, EnumConfig.create(FilterMode.FILE), FILE_EXTENSIONS);
        m_workingDirectory = new SettingsModelString(KEY_WORKING_DIRECTORY, ArchiveZipFileSystem.SEPARATOR);

        m_useDefaultEncodingModel = new SettingsModelBoolean(CFG_USE_DEFAULT_ENCODING, true);
        m_encoding = new SettingsModelString(KEY_ENCODING, DEFAULT_ENCODING);
    }

    private void save(final NodeSettingsWO settings) {
        m_file.saveSettingsTo(settings);
        m_workingDirectory.saveSettingsTo(settings);
        m_useDefaultEncodingModel.saveSettingsTo(settings);
        m_encoding.saveSettingsTo(settings);
    }

    /**
     * Saves settings to the given {@link NodeSettingsWO} (to be called by the node
     * dialog).
     *
     * @param settings
     */
    public void saveForDialog(final NodeSettingsWO settings) {
        save(settings);
    }

    /**
     * Saves settings to the given {@link NodeSettingsWO} (to be called by the node
     * model).
     *
     * @param settings
     */
    public void saveForModel(final NodeSettingsWO settings) {
        save(settings);
    }

    private void load(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_file.loadSettingsFrom(settings);
        m_workingDirectory.loadSettingsFrom(settings);
        if (settings.containsKey(KEY_ENCODING)) {
            m_encoding.loadSettingsFrom(settings);
            //for backward-compatibility
            m_useDefaultEncodingModel.setBooleanValue(false);
        }
        //added to AP 4.7.0
        if (settings.containsKey(CFG_USE_DEFAULT_ENCODING)) {
            m_useDefaultEncodingModel.loadSettingsFrom(settings);
        }
    }

    /**
     * Loads settings from the given {@link NodeSettingsRO} (to be called by the
     * node dialog).
     *
     * @param settings
     * @throws NotConfigurableException
     */
    public void loadForDialog(final NodeSettingsRO settings) throws NotConfigurableException {
        try {
            load(settings);
        } catch (InvalidSettingsException ex) {
            throw new NotConfigurableException(ex.getMessage(), ex);
        }
    }

    /**
     * Loads settings from the given {@link NodeSettingsRO} (to be called by the
     * node model).
     *
     * @param settings
     * @throws InvalidSettingsException
     */
    public void loadForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        load(settings);
    }

    /**
     * Forwards the given {@link PortObjectSpec} and status message consumer to the
     * file chooser settings models to they can configure themselves properly.
     *
     * @param inSpecs
     *            input specifications.
     * @param statusConsumer
     *            status consumer.
     * @throws InvalidSettingsException
     */
    public void configureInModel(final PortObjectSpec[] inSpecs, final Consumer<StatusMessage> statusConsumer)
            throws InvalidSettingsException {
        m_file.configureInModel(inSpecs, statusConsumer);
    }

    /**
     * Validates the settings in the given {@link NodeSettingsRO}.
     *
     * @param settings
     * @throws InvalidSettingsException
     */
    public void validate(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_file.validateSettings(settings);
        m_workingDirectory.validateSettings(settings);
        if (settings.containsKey(CFG_USE_DEFAULT_ENCODING)) {
            m_useDefaultEncodingModel.validateSettings(settings);
        }
        if (settings.containsKey(KEY_ENCODING)) {
            m_encoding.validateSettings(settings);
        }

        final var temp = new ArchiveZipConnectorNodeSettings(m_nodeCreationConfig);
        temp.loadForModel(settings);
        temp.validate();
    }

    /**
     * Validates the current settings.
     *
     * @throws InvalidSettingsException
     */
    public void validate() throws InvalidSettingsException {
        if (StringUtils.isBlank(m_file.getLocation().getPath())) {
            throw new InvalidSettingsException("File must be specified.");
        }

        if (StringUtils.isBlank(getWorkingDirectory())) {
            throw new InvalidSettingsException("Working directory must be specified.");
        }
    }

    /**
     * @return working directory.
     */
    String getWorkingDirectory() {
        return m_workingDirectory.getStringValue();
    }

    /**
     * @return the settings model for the working directory.
     */
    SettingsModelString getWorkingDirectoryModel() {
        return m_workingDirectory;
    }

    /**
     * @return file location model.
     */
    SettingsModelReaderFileChooser getFileModel() {
        return m_file;
    }

    /**
     * @return known hosts file location.
     */
    FSLocation getFile() {
        return m_file.getLocation();
    }

    /**
     * Returns whether or not to use a default encoding (which is
     * currently UTF-8).
     *
     * @return the use default encoding model
     */
    SettingsModelBoolean getUseDefaultEncodingModel() {
        return m_useDefaultEncodingModel;
    }

    /**
     * Returns encoding used in unpacking the zip archive.
     *
     * @return encoding
     */
    String getEncoding() {
        return m_encoding.getStringValue();
    }

    /**
     * Sets encoding used in unpacking the zip archive.
     * @param encoding
     */
    public void setEncoding(final String encoding) {
        m_encoding.setStringValue(encoding);
    }

    /**
     * @param location
     *            location to test.
     * @return true if the location is NULL location in fact.
     */
    static boolean isEmptyLocation(final FSLocation location) {
        return location == null || StringUtils.isBlank(location.getPath());
    }

    /**
     * Convert settings to a {@link ArchiveZipFSConnectionConfig} instance.
     *
     * @param m_statusConsumer
     *
     * @param m_statusConsumer
     * @throws InvalidSettingsException
     * @throws IOException
     */
    @SuppressWarnings("resource")
    ArchiveZipFSConnectionConfig createFSConnectionConfig(final Consumer<StatusMessage> statusConsumer)
            throws InvalidSettingsException {
        final var cfg = new ArchiveZipFSConnectionConfig(getWorkingDirectory());
        final var pathAccessor = m_file.createReadPathAccessor();
        try {
            cfg.setCloseable(pathAccessor);
            final FSPath zipFilePath = pathAccessor.getRootPath(statusConsumer);
            cfg.setZipFilePath(zipFilePath);
            cfg.setUseDefaultEncoding(getUseDefaultEncodingModel().getBooleanValue());
            cfg.setEncoding(getEncoding());
        } catch (Exception e) { //NOSONAR
            closeQuietly(pathAccessor);
            throw new InvalidSettingsException("Unable to get the zip file path: " + e.getMessage(), e);
        }
        return cfg;
    }

    private static void closeQuietly(final Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException e) { //NOSONAR
            // quietly ignored
        }
    }
}
