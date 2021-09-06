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
 *  2021-08-10: created (jl)
 */
package org.knime.base.filehandling.binaryobjects.writer;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.node.table.writer.AbstractMultiTableWriterNodeConfig;
import org.knime.filehandling.core.node.table.writer.DefaultMultiTableWriterNodeConfigSerializer;

/**
 * Serializes the keys from the {@link AbstractMultiTableWriterNodeConfig} backwards compatibly into the node settings.
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 */
final class BinaryObjectsToFilesNodeConfigSerializer
    extends DefaultMultiTableWriterNodeConfigSerializer<BinaryObjectsToFilesNodeConfig> {

    private static final String CFG_BINARY_COLUMN_NAME = "binary_object_column";

    private static final String CFG_REMOVE_BINARY_COLUMN_NAME = "remove_binary_object_column";

    private static final String CFG_GENERATE_FILE_NAMES = "generate_file_names";

    // needed to copy all the settings correctly…
    private final SettingsModelString m_binaryColumn;

    private final SettingsModelBoolean m_removeBinaryColumn;

    BinaryObjectsToFilesNodeConfigSerializer() {
        m_binaryColumn = new SettingsModelString(CFG_BINARY_COLUMN_NAME, null);
        m_removeBinaryColumn = new SettingsModelBoolean(CFG_REMOVE_BINARY_COLUMN_NAME, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadInDialog(final BinaryObjectsToFilesNodeConfig config, final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        try {
            loadInModel(config, settings);
        } catch (InvalidSettingsException e) { // NOSONAR: just load the defaults
            config.getSourceColumn().setStringValue(null);
            config.getRemoveSourceColumn().setBooleanValue(false);

            config.setShouldGenerateFilename(true);
            config.getFilenamePattern().setStringValue("File_?.dat"); // only change
            config.getFilenameColumn().setStringValue(null);
            if (config.isCompressionSupported()) {
                config.getCompressFiles().setBooleanValue(false);
            }
        }
    }

    @Override
    public void loadInModel(final BinaryObjectsToFilesNodeConfig config, final NodeSettingsRO settings)
        throws InvalidSettingsException {
        config.getOutputLocation().loadSettingsFrom(settings); // same key

        final var sourceColumn = config.getSourceColumn();
        final var removeSourceColumn = config.getRemoveSourceColumn();
        m_binaryColumn.loadSettingsFrom(settings);
        m_removeBinaryColumn.loadSettingsFrom(settings);
        sourceColumn.setStringValue(m_binaryColumn.getStringValue());
        removeSourceColumn.setBooleanValue(m_removeBinaryColumn.getBooleanValue());
        sourceColumn.setEnabled(m_binaryColumn.isEnabled());
        removeSourceColumn.setEnabled(m_removeBinaryColumn.isEnabled());

        config.setShouldGenerateFilename(settings.getBoolean(CFG_GENERATE_FILE_NAMES));
        config.getFilenamePattern().loadSettingsFrom(settings); // same key
        // the type of the settings model changed so we have to do these shenanigans
        final var filenameColumn = config.getFilenameColumn();
        try {
            filenameColumn.loadSettingsFrom(settings);
        } catch (InvalidSettingsException e) {
            NodeLogger.getLogger(getClass()).debug("No SettingsModelColumnName setting found; trying to load and convert old SettingsModelString", e);
            final var m = new SettingsModelString(config.getFilenameColumnSettingsKey(), "");
            m.loadSettingsFrom(settings);
            filenameColumn.setEnabled(m.isEnabled());
            filenameColumn.setSelection(m.getStringValue(), filenameColumn.useRowID());
        }
        if (config.isCompressionSupported()) {
            try {
                config.getCompressFiles().loadSettingsFrom(settings);
            } catch (InvalidSettingsException e) { // NOSONAR: ignore
                config.getCompressFiles().setBooleanValue(false); // load a default
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveInModel(final BinaryObjectsToFilesNodeConfig config, final NodeSettingsWO settings) {
        config.getOutputLocation().saveSettingsTo(settings); // same key

        final var sourceColumn = config.getSourceColumn();
        final var removeSourceColumn = config.getRemoveSourceColumn();
        m_binaryColumn.setStringValue(sourceColumn.getStringValue());
        m_removeBinaryColumn.setBooleanValue(removeSourceColumn.getBooleanValue());
        m_binaryColumn.setEnabled(sourceColumn.isEnabled());
        m_removeBinaryColumn.setEnabled(removeSourceColumn.isEnabled());
        m_binaryColumn.saveSettingsTo(settings);
        m_removeBinaryColumn.saveSettingsTo(settings);

        settings.addBoolean(CFG_GENERATE_FILE_NAMES, config.shouldGenerateFilename());
        config.getFilenamePattern().saveSettingsTo(settings); // same key
        config.getFilenameColumn().saveSettingsTo(settings); // same key
        if (config.isCompressionSupported()) {
            config.getCompressFiles().saveSettingsTo(settings);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validate(final BinaryObjectsToFilesNodeConfig config, final NodeSettingsRO settings)
        throws InvalidSettingsException {
        config.getOutputLocation().validateSettings(settings);
        m_binaryColumn.validateSettings(settings);
        m_removeBinaryColumn.validateSettings(settings);

        settings.getBoolean(CFG_GENERATE_FILE_NAMES);
        config.getFilenamePattern().validateSettings(settings);
        // the type of the settings model changed so we have to do these shenanigans
        try {
            config.getFilenameColumn().validateSettings(settings);
        } catch (InvalidSettingsException e) {
            NodeLogger.getLogger(getClass()).debug("No SettingsModelColumnName setting found; trying to validate old SettingsModelString", e);
            final var m = new SettingsModelString(config.getFilenameColumnSettingsKey(), "");
            m.validateSettings(settings);
        }
        if (config.isCompressionSupported()) {
            try {
                config.getCompressFiles().validateSettings(settings);
            } catch (final InvalidSettingsException e) { // NOSONAR: ignore
                // ignore
            }
        }
    }

}
