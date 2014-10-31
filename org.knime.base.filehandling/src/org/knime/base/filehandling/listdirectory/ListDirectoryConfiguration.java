/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   Nov 9, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.listdirectory;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Configuration for the node.
 *
 *
 * @author Patrick Winter, KNIME.com, Zurich, Switzerland
 */
class ListDirectoryConfiguration {

    private String m_directory;

    private boolean m_recursive = true;

    /**
     * @return the directory
     */
    public String getDirectory() {
        return m_directory;
    }

    /**
     * @param directory the directory to set
     */
    public void setDirectory(final String directory) {
        m_directory = directory;
    }

    /**
     * @return the recursive
     */
    public boolean getRecursive() {
        return m_recursive;
    }

    /**
     * @param recursive the recursive to set
     */
    public void setRecursive(final boolean recursive) {
        m_recursive = recursive;
    }

    /**
     * Save the configuration.
     *
     *
     * @param settings The <code>NodeSettings</code> to write to
     */
    void save(final NodeSettingsWO settings) {
        settings.addString("directory", m_directory);
        settings.addBoolean("recursive", m_recursive);
    }

    /**
     * Load the configuration.
     *
     *
     * @param settings The <code>NodeSettings</code> to read from
     */
    void load(final NodeSettingsRO settings) {
        m_directory = settings.getString("directory", "");
        m_recursive = settings.getBoolean("recursive", true);
    }

    /**
     * Load the configuration and check for validity.
     *
     *
     * @param settings The <code>NodeSettings</code> to read from
     * @throws InvalidSettingsException If one of the settings is not valid
     */
    void loadAndValidate(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_directory = settings.getString("directory");
        m_recursive = settings.getBoolean("recursive", true);
    }

    /**
     * Checks if the setting is not null or empty.
     *
     *
     * @param name The name that will be displayed in case of error
     * @param setting The setting to check
     * @throws InvalidSettingsException If the string is null or empty
     */
    public void validate(final String name, final String setting) throws InvalidSettingsException {
        if (setting == null || setting.length() == 0) {
            throw new InvalidSettingsException(name + " missing");
        }
    }

}
