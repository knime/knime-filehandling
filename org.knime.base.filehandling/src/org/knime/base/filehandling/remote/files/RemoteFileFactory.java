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
 *   Nov 2, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.remote.files;

import java.net.URI;

import org.knime.base.filehandling.remotecredentials.port.RemoteCredentials;

/**
 * Factory for remote files.
 * 
 * 
 * @author Patrick Winter, University of Konstanz
 */
public final class RemoteFileFactory {

    private RemoteFileFactory() {
        // Disable default constructor
    }

    /**
     * Creates a remote file for the URI.
     * 
     * 
     * @param uri The URI
     * @param credentials Credentials to the given URI
     * @return Remote file for the given URI
     * @throws Exception If creation of the remote file or opening of its
     *             connection failed
     */
    public static RemoteFile createRemoteFile(final URI uri,
            final RemoteCredentials credentials) throws Exception {
        String scheme = uri.getScheme().toLowerCase();
        if (credentials != null) {
            // Check if the credentials fit to the URI
            credentials.fitsToURI(uri);
        }
        RemoteFile remoteFile = null;
        // Create remote file that fits to the scheme
        if (scheme.equals("file")) {
            remoteFile = new FileRemoteFile(uri);
        } else if (scheme.equals("ftp")) {
            remoteFile = new FTPRemoteFile(uri, credentials);
        } else if (scheme.equals("sftp") || scheme.equals("ssh")) {
            remoteFile = new SFTPRemoteFile(uri, credentials);
        } else if (scheme.equals("http") || scheme.equals("https")) {
            remoteFile = new HTTPRemoteFile(uri, credentials);
        } else if (scheme.equals("scp")) {
            remoteFile = new SCPRemoteFile(uri, credentials);
        }
        if (remoteFile != null) {
            // Open connection of the remote file
            remoteFile.open();
        }
        return remoteFile;
    }

}
