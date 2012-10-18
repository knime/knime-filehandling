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
 *   Oct 17, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.remotecopy;

import java.net.URI;

import org.knime.base.filehandling.remotecopy.datasink.DataSink;
import org.knime.base.filehandling.remotecopy.datasink.DataSinkFactory;
import org.knime.base.filehandling.remotecopy.datasource.DataSource;
import org.knime.base.filehandling.remotecopy.datasource.DataSourceFactory;
import org.knime.core.node.ExecutionContext;

/**
 * Copies files from one resource to another.
 * 
 * 
 * @author Patrick Winter, University of Konstanz
 */
public final class Copier {

    private Copier() {
        // Disable default constructor
    }

    /**
     * Copies the resource from the source URI to the target URI.
     * 
     * 
     * @param sourceURI URI that points to the source resource
     * @param targetURI URI that points to the target resource
     * @param exec Execution context to check for cancellation
     * @throws Exception If one of the resources is not reachable or the target
     *             is not writable
     */
    public static void copy(final URI sourceURI, final URI targetURI,
            final ExecutionContext exec) throws Exception {
        try {
            // Create fitting data source and data sink
            DataSource source = DataSourceFactory.getSource(sourceURI);
            DataSink target = DataSinkFactory.getSink(targetURI);
            byte[] buffer = new byte[1024];
            int length;
            // Copy bytes
            while ((length = source.read(buffer)) > 0) {
                exec.checkCanceled();
                target.write(buffer, length);
            }
            // Close
            source.close();
            target.close();
        } catch (Exception e) {
            // Print debug information
            e.printStackTrace();
            throw e;
        }
    }

}
