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
package org.knime.base.filehandling.binaryobjectstofiles;

import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * Factory for SettingsModels.
 * 
 * 
 * @author Patrick Winter, University of Konstanz
 */
final class SettingsFactory {

    private SettingsFactory() {
        // Disables default constructor
    }

    /**
     * Factory method for the binary object column setting.
     * 
     * 
     * @return Binary object column <code>SettingsModel</code>
     */
    static SettingsModelString createBinaryObjectColumnSettings() {
        return new SettingsModelString("binaryobjectcolumn", "");
    }

    /**
     * Factory method for the filename handling setting.
     * 
     * 
     * @return Filename handling <code>SettingsModel</code>
     */
    static SettingsModelString createFilenameHandlingSettings() {
        return new SettingsModelString("filenamehandling",
                FilenameHandling.GENERATE.getName());
    }

    /**
     * Factory method for the target column setting.
     * 
     * 
     * @param filenamehandling <code>SettingsModel</code> for the filename
     *            handling setting
     * 
     * @return Target column <code>SettingsModel</code>
     */
    static SettingsModelString createTargetColumnSettings(
            final SettingsModelString filenamehandling) {
        SettingsModelString namecolumn =
                new SettingsModelString("targetcolumn", "");
        String handling = filenamehandling.getStringValue();
        namecolumn.setEnabled(handling.equals(FilenameHandling.FROMCOLUMN
                .getName()));
        return namecolumn;
    }

    /**
     * Factory method for the output directory setting.
     * 
     * 
     * @param filenamehandling <code>SettingsModel</code> for the filename
     *            handling setting
     * 
     * @return Output directory <code>SettingsModel</code>
     */
    static SettingsModelString createOutputDirectorySettings(
            final SettingsModelString filenamehandling) {
        SettingsModelString directory =
                new SettingsModelString("outputdirectory", "");
        String handling = filenamehandling.getStringValue();
        directory.setEnabled(handling.equals(FilenameHandling.GENERATE
                .getName()));
        return directory;
    }

    /**
     * Factory method for the name pattern setting.
     * 
     * 
     * @param filenamehandling <code>SettingsModel</code> for the filename
     *            handling setting
     * 
     * @return Name pattern <code>SettingsModel</code>
     */
    static SettingsModelString createNamePatternSettings(
            final SettingsModelString filenamehandling) {
        SettingsModelString namepattern =
                new SettingsModelString("namepattern", "file_?.dat");
        String handling = filenamehandling.getStringValue();
        namepattern.setEnabled(handling.equals(FilenameHandling.GENERATE
                .getName()));
        return namepattern;
    }

    /**
     * Factory method for the remove binary object column setting.
     * 
     * 
     * @return Remove binary object <code>SettingsModel</code>
     */
    static SettingsModelBoolean createRemoveBinaryObjectColumnSettings() {
        return new SettingsModelBoolean("removebinaryobjectcolumn", true);
    }

    /**
     * Factory method for the if exists setting.
     * 
     * 
     * @return If exists <code>SettingsModel</code>
     */
    static SettingsModelString createIfExistsSettings() {
        return new SettingsModelString("ifexists",
                OverwritePolicy.ABORT.getName());
    }

}
