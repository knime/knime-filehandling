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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import org.knime.base.filehandling.remotecredentials.port.RemoteCredentials;

/**
 * Implementation of the file remote file.
 * 
 * 
 * @author Patrick Winter, University of Konstanz
 */
public class FileRemoteFile extends RemoteFile {

    private URI m_uri;

    /**
     * Creates a file remote file for the given URI.
     * 
     * 
     * @param uri The URI
     * @param credentials Credentials to the given URI
     */
    FileRemoteFile(final URI uri, final RemoteCredentials credentials) {
        m_uri = uri;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean usesConnection() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Connection createConnection() {
        // Does not use a connection
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getIdentifier() {
        return buildIdentifier(m_uri);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getType() {
        return "file";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exists() throws Exception {
        return new File(m_uri).exists();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDirectory() throws Exception {
        return new File(m_uri).isDirectory();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean move(final RemoteFile file) throws Exception {
        boolean success;
        if (file instanceof FileRemoteFile) {
            FileRemoteFile source = (FileRemoteFile)file;
            success = new File(m_uri).renameTo(new File(source.m_uri));
        } else {
            write(file);
            success = file.delete();
        }
        return success;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(final RemoteFile file) throws Exception {
        byte[] buffer = new byte[1024];
        InputStream in = file.openInputStream();
        OutputStream out = openOutputStream();
        int length;
        while (((length = in.read(buffer)) > 0)) {
            out.write(buffer, 0, length);
        }
        in.close();
        out.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream openInputStream() throws Exception {
        return new FileInputStream(new File(m_uri));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputStream openOutputStream() throws Exception {
        return new FileOutputStream(new File(m_uri));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSize() throws Exception {
        return new File(m_uri).length();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long lastModified() throws Exception {
        return new File(m_uri).lastModified();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean delete() throws Exception {
        return deleteRecursively(m_uri.getPath());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RemoteFile[] listFiles() throws Exception {
        RemoteFile[] files;
        if (isDirectory()) {
            File[] f = new File(m_uri).listFiles();
            files = new RemoteFile[f.length];
            for (int i = 0; i < f.length; i++) {
                files[i] = new FileRemoteFile(f[i].toURI(), null);
            }
        } else {
            files = new RemoteFile[0];
        }
        return files;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean mkDir() throws Exception {
        return new File(m_uri).mkdir();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws Exception {
        // Not used
    }

    /**
     * Deletes files and directories recursively.
     * 
     * 
     * @param path Path to the file or directory
     * @return true if deletion was successful, false otherwise
     */
    private boolean deleteRecursively(final String path) {
        File file = new File(path);
        if (file.isDirectory()) {
            String[] files = file.list();
            for (int i = 0; i < files.length; i++) {
                deleteRecursively(files[i]);
            }
        }
        return file.delete();
    }

}
