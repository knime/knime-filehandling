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
 *   Nov 1, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.copyfiles;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Configuration for the node.
 * 
 * 
 * @author Patrick Winter, KNIME.com, Zurich, Switzerland
 */
class CopyFilesConfiguration {

    private String m_copyormove;

    private String m_sourcecolumn;

    private String m_filenamehandling;

    private String m_targetcolumn;

    private String m_outputdirectory;

    private String m_ifexists;

    /**
     * @return the copyormove
     */
    String getCopyormove() {
        return m_copyormove;
    }

    /**
     * @param copyormove the copyormove to set
     */
    void setCopyormove(final String copyormove) {
        m_copyormove = copyormove;
    }

    /**
     * @return the sourcecolumn
     */
    String getSourcecolumn() {
        return m_sourcecolumn;
    }

    /**
     * @param sourcecolumn the sourcecolumn to set
     */
    void setSourcecolumn(final String sourcecolumn) {
        m_sourcecolumn = sourcecolumn;
    }

    /**
     * @return the filenamehandling
     */
    String getFilenamehandling() {
        return m_filenamehandling;
    }

    /**
     * @param filenamehandling the filenamehandling to set
     */
    void setFilenamehandling(final String filenamehandling) {
        m_filenamehandling = filenamehandling;
    }

    /**
     * @return the targetcolumn
     */
    String getTargetcolumn() {
        return m_targetcolumn;
    }

    /**
     * @param targetcolumn the targetcolumn to set
     */
    void setTargetcolumn(final String targetcolumn) {
        m_targetcolumn = targetcolumn;
    }

    /**
     * @return the outputdirectory
     */
    String getOutputdirectory() {
        return m_outputdirectory;
    }

    /**
     * @param outputdirectory the outputdirectory to set
     */
    void setOutputdirectory(final String outputdirectory) {
        m_outputdirectory = outputdirectory;
    }

    /**
     * @return the ifexists
     */
    String getIfexists() {
        return m_ifexists;
    }

    /**
     * @param ifexists the ifexists to set
     */
    void setIfexists(final String ifexists) {
        m_ifexists = ifexists;
    }

    /**
     * Save the configuration.
     * 
     * 
     * @param settings The <code>NodeSettings</code> to write to
     */
    void save(final NodeSettingsWO settings) {
        settings.addString("copyormove", m_copyormove);
        settings.addString("sourcecolumn", m_sourcecolumn);
        settings.addString("filenamehandling", m_filenamehandling);
        settings.addString("targetcolumn", m_targetcolumn);
        settings.addString("outputdirectory", m_outputdirectory);
        settings.addString("ifexists", m_ifexists);
    }

    /**
     * Load the configuration.
     * 
     * 
     * @param settings The <code>NodeSettings</code> to read from
     */
    void load(final NodeSettingsRO settings) {
        m_copyormove = settings.getString("copyormove", CopyOrMove.COPY.getName());
        m_sourcecolumn = settings.getString("sourcecolumn", "");
        m_filenamehandling = settings.getString("filenamehandling", FilenameHandling.FROMCOLUMN.getName());
        m_targetcolumn = settings.getString("targetcolumn", "");
        m_outputdirectory = settings.getString("outputdirectory", "");
        m_ifexists = settings.getString("ifexists", OverwritePolicy.ABORT.getName());
    }

    /**
     * Load the configuration and check for validity.
     * 
     * 
     * @param settings The <code>NodeSettings</code> to read from
     * @throws InvalidSettingsException If one of the settings is not valid
     */
    void loadAndValidate(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_copyormove = settings.getString("copyormove");
        validate(m_copyormove);
        m_sourcecolumn = settings.getString("sourcecolumn");
        validate(m_sourcecolumn);
        m_filenamehandling = settings.getString("filenamehandling");
        validate(m_filenamehandling);
        m_targetcolumn = settings.getString("targetcolumn");
        if (m_filenamehandling.equals(FilenameHandling.FROMCOLUMN.getName())) {
            validate(m_targetcolumn);
        }
        m_outputdirectory = settings.getString("outputdirectory");
        if (m_filenamehandling.equals(FilenameHandling.SOURCENAME.getName())) {
            validate(m_outputdirectory);
        }
        m_ifexists = settings.getString("ifexists");
        validate(m_ifexists);
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
