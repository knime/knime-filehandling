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
package org.knime.base.filehandling.remotecopy.datasource;

import java.net.URI;

import org.knime.base.filehandling.remotecopy.connections.ConnectionMonitor;

/**
 * Factory class for data source construction.
 * 
 * 
 * @author Patrick Winter, University of Konstanz
 */
public final class DataSourceFactory {

    private DataSourceFactory() {
        // Disable the default constructor
    }

    /**
     * Factory method for data source construction.
     * 
     * 
     * Will determine what source is used by the scheme of the URI.
     * 
     * @param uri The URI that will be used by the data source
     * @param monitor Monitor for connection reuse
     * @return Data source for the URI
     * @throws Exception If construction was not possible
     */
    public static DataSource getSource(final URI uri,
            final ConnectionMonitor monitor) throws Exception {
        String scheme = uri.getScheme();
        DataSource source = null;
        if (scheme.equals("file")) {
            source = new FileDataSource(uri);
        }
        if (scheme.equals("ftp")) {
            source = new FTPDataSource(uri, monitor);
        }
        if (scheme.equals("sftp")) {
            source = new SFTPDataSource(uri, monitor);
        }
        if (scheme.equals("scp")) {
            source = new SCPDataSource(uri, monitor);
        }
        if (source == null) {
            source = new DefaultDataSource(uri);
        }
        return source;
    }

}
