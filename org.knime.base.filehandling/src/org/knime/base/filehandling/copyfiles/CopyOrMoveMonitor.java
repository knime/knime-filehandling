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
 *   Oct 10, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.copyfiles;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Monitors what files have been touched and provides a rollback.
 * 
 * 
 * @author Patrick Winter, KNIME.com, Zurich, Switzerland
 */
class CopyOrMoveMonitor {

    private String m_action;

    private Set<String> m_touchedFiles = new HashSet<String>();

    private List<String> m_sources = new LinkedList<String>();

    private List<String> m_targets = new LinkedList<String>();

    /**
     * @param action Copy or move setting
     */
    CopyOrMoveMonitor(final String action) {
        m_action = action;
    }

    /**
     * Register files as processed.
     * 
     * 
     * @param source Path of the source file
     * @param target Path of the target file
     */
    void registerFiles(final String source, final String target) {
        m_sources.add(source);
        m_targets.add(target);
        m_touchedFiles.add(source);
        m_touchedFiles.add(target);
    }

    /**
     * Check if a file has been touched before.
     * 
     * 
     * @param file The file that should be checked
     * @return true if the file has not been touched before, false otherwise
     */
    boolean isNewFile(final String file) {
        return !m_touchedFiles.contains(file);
    }

    /**
     * Rollback changes that were made to the file system.
     * 
     * 
     * This method will rollback all changes that were made to the file system.
     * If the action was copy, then all the registered targets will be deleted.
     * If the action was move, then all the registered targets will be moved to
     * there source again.
     */
    void rollback() {
        String copy = CopyOrMove.COPY.getName();
        String move = CopyOrMove.MOVE.getName();
        String[] targets = m_targets.toArray(new String[m_targets.size()]);
        if (m_action.equals(copy)) {
            // Delete all targets
            for (int i = 0; i < targets.length; i++) {
                try {
                    File file = new File(targets[i]);
                    file.delete();
                } catch (Exception e) {
                    // If one file fails, the others should still be deleted
                }
            }
        } else if (m_action.equals(move)) {
            String[] sources = m_sources.toArray(new String[m_sources.size()]);
            // Move all targets to there original source
            for (int i = 0; i < sources.length; i++) {
                try {
                    File source = new File(sources[i]);
                    File target = new File(targets[i]);
                    target.renameTo(source);
                } catch (Exception e) {
                    // If one file fails, the others should still be moved
                }
            }
        }
    }

}
