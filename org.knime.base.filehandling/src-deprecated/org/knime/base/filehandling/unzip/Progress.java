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
 *   Sep 14, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.unzip;

/**
 * Convenience class for progress tracking.
 *
 *
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 */
@Deprecated
public class Progress {

    private long m_goal;

    private long m_progress;

    /**
     */
    Progress() {
        m_goal = 0;
        m_progress = 0;
    }

    /**
     * @param goal The goal
     */
    Progress(final long goal) {
        m_goal = goal;
        m_progress = 0;
    }

    /**
     * @param amount Amount of progress made
     */
    void advance(final long amount) {
        m_progress += amount;
    }

    /**
     * @return The progress
     */
    long getProgress() {
        return m_progress;
    }

    /**
     * @return The goal
     */
    long getGoal() {
        return m_goal;
    }

    /**
     * Get progress as value between 0 and 1.
     *
     *
     * @return The progress in percent
     */
    double getProgressInPercent() {
        return m_goal == 0 ? 0 : (double)m_progress / m_goal;
    }

}
