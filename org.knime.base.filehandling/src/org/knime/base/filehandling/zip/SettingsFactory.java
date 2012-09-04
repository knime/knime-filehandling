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

import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * Factory for SettingsModels.
 * 
 * 
 * @author Patrick Winter, University of Konstanz
 */
public final class SettingsFactory {

    private SettingsFactory() {
        // Disables default constructor
    }

    /**
     * Factory method for the URL column.
     * 
     * @return URL column SettingsModel
     */
    public static SettingsModelString createURLColumnSettings() {
        return new SettingsModelString("urlcolumn", "");
    }

    /**
     * Factory method for the target.
     * 
     * @return Target SettingsModel
     */
    public static SettingsModelString createTargetSettings() {
        return new SettingsModelString("target", "");
    }

    /**
     * Factory method for the use prefix setting.
     * 
     * @return Use prefix SettingsModel
     */
    public static SettingsModelBoolean createUsePrefixSettings() {
        return new SettingsModelBoolean("useprefix", false);
    }

    /**
     * Factory method for the prefix.
     * 
     * @return Prefix SettingsModel
     */
    public static SettingsModelString createPrefixSettings() {
        SettingsModelString prefix = new SettingsModelString("prefix", "");
        SettingsModelBoolean useprefix = createUsePrefixSettings();
        prefix.setEnabled(useprefix.getBooleanValue());
        return prefix;
    }

    /**
     * Factory method for the if exists setting.
     * 
     * @return If exists SettingsModel
     */
    public static SettingsModelString createIfExistsSettings() {
        return new SettingsModelString("ifexists", OverwritePolicy.ABORT);
    }

}
