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
 * ------------------------------------------------------------------------
 * 
 * History
 *   Sep 5, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.extracturiinfo;

import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * Factory for SettingsModels.
 * 
 * 
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 */
final class SettingsFactory {

    private SettingsFactory() {
        // Disables default constructor
    }

    /**
     * Factory method for the column selection setting.
     * 
     * 
     * @return Column selection <code>SettingsModel</code>
     */
    static SettingsModelString createColumnSelectionSettings() {
        return new SettingsModelString("columnselection", "");
    }

    /**
     * Factory method for the authority setting.
     * 
     * 
     * @return Authority <code>SettingsModel</code>
     */
    static SettingsModelBoolean createAuthoritySettings() {
        return new SettingsModelBoolean("authority", false);
    }

    /**
     * Factory method for the fragment setting.
     * 
     * 
     * @return Fragment <code>SettingsModel</code>
     */
    static SettingsModelBoolean createFragmentSettings() {
        return new SettingsModelBoolean("fragment", false);
    }

    /**
     * Factory method for the host setting.
     * 
     * 
     * @return Host <code>SettingsModel</code>
     */
    static SettingsModelBoolean createHostSettings() {
        return new SettingsModelBoolean("Host", false);
    }

    /**
     * Factory method for the path setting.
     * 
     * 
     * @return Path <code>SettingsModel</code>
     */
    static SettingsModelBoolean createPathSettings() {
        return new SettingsModelBoolean("path", false);
    }

    /**
     * Factory method for the Port setting.
     * 
     * 
     * @return Port <code>SettingsModel</code>
     */
    static SettingsModelBoolean createPortSettings() {
        return new SettingsModelBoolean("port", false);
    }

    /**
     * Factory method for the query setting.
     * 
     * 
     * @return Query <code>SettingsModel</code>
     */
    static SettingsModelBoolean createQuerySettings() {
        return new SettingsModelBoolean("query", false);
    }

    /**
     * Factory method for the scheme setting.
     * 
     * 
     * @return Scheme <code>SettingsModel</code>
     */
    static SettingsModelBoolean createSchemeSettings() {
        return new SettingsModelBoolean("scheme", false);
    }

    /**
     * Factory method for the user info setting.
     * 
     * 
     * @return User info <code>SettingsModel</code>
     */
    static SettingsModelBoolean createUserInfoSettings() {
        return new SettingsModelBoolean("userinfo", false);
    }

}
