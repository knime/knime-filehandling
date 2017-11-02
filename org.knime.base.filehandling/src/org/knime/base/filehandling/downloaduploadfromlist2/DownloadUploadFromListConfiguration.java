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
package org.knime.base.filehandling.downloaduploadfromlist2;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Configuration for the node.
 * 
 * 
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 */
class DownloadUploadFromListConfiguration {

    private String m_source;

    private String m_target;

    private String m_overwritePolicy;

    private boolean m_abortonfail;

    /**
     * @return the source
     */
    public String getSource() {
        return m_source;
    }

    /**
     * @param source the source to set
     */
    public void setSource(final String source) {
        m_source = source;
    }

    /**
     * @return the target
     */
    public String getTarget() {
        return m_target;
    }

    /**
     * @param target the target to set
     */
    public void setTarget(final String target) {
        m_target = target;
    }

    /**
     * @return the overwritePolicy
     */
    public String getOverwritePolicy() {
        return m_overwritePolicy;
    }

    /**
     * @param overwritePolicy the overwritePolicy to set
     */
    public void setOverwritePolicy(final String overwritePolicy) {
        m_overwritePolicy = overwritePolicy;
    }

    /**
     * @return the abortonfail
     */
    public boolean getAbortonfail() {
        return m_abortonfail;
    }

    /**
     * @param abortonfail the abortonfail to set
     */
    public void setAbortonfail(final boolean abortonfail) {
        m_abortonfail = abortonfail;
    }

    /**
     * Save the configuration.
     * 
     * 
     * @param settings The <code>NodeSettings</code> to write to
     */
    void save(final NodeSettingsWO settings) {
        settings.addString("source", m_source);
        settings.addString("target", m_target);
        settings.addString("overwritepolicy", m_overwritePolicy);
        settings.addBoolean("abortonfail", m_abortonfail);
    }

    /**
     * Load the configuration.
     * 
     * 
     * @param settings The <code>NodeSettings</code> to read from
     */
    void load(final NodeSettingsRO settings) {
        m_source = settings.getString("source", "");
        m_target = settings.getString("target", "");
        m_overwritePolicy = settings.getString("overwritepolicy", OverwritePolicy.OVERWRITE.getName());
        m_abortonfail = settings.getBoolean("abortonfail", true);
    }

    /**
     * Load the configuration and check for validity.
     * 
     * 
     * @param settings The <code>NodeSettings</code> to read from
     * @throws InvalidSettingsException If one of the settings is not valid
     */
    void loadAndValidate(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_source = settings.getString("source");
        validate(m_source);
        m_target = settings.getString("target");
        validate(m_target);
        m_overwritePolicy = settings.getString("overwritepolicy");
        validate(m_overwritePolicy);
        m_abortonfail = settings.getBoolean("abortonfail", true);
    }

    /**
     * Checks if the string is not null or empty.
     * 
     * 
     * @param string The string to check
     * @throws InvalidSettingsException If the string is null or empty
     */
    private void validate(final String string) throws InvalidSettingsException {
        if (string == null || string.length() == 0) {
            throw new InvalidSettingsException("Invalid setting");
        }
    }

}
