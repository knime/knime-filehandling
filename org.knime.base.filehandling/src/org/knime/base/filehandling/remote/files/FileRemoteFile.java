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
import java.util.Arrays;

import org.apache.commons.io.FilenameUtils;

/**
 * Implementation of the file remote file.
 * 
 * 
 * @author Patrick Winter, University of Konstanz
 */
public class FileRemoteFile extends RemoteFile {

    /**
     * Creates a file remote file for the given URI.
     * 
     * 
     * @param uri The URI
     */
    FileRemoteFile(final URI uri) {
        super(uri, null, null);
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
    public String getType() {
        return "file";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exists() throws Exception {
        return new File(getURI()).exists();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDirectory() throws Exception {
        return new File(getURI()).isDirectory();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void move(final RemoteFile file) throws Exception {
        if (file instanceof FileRemoteFile) {
            FileRemoteFile source = (FileRemoteFile)file;
            boolean success =
                    new File(getURI()).renameTo(new File(source.getURI()));
            if (!success) {
                throw new Exception("Move operation failed");
            }
        } else {
            super.move(file);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream openInputStream() throws Exception {
        return new FileInputStream(new File(getURI()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputStream openOutputStream() throws Exception {
        return new FileOutputStream(new File(getURI()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSize() throws Exception {
        return new File(getURI()).length();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long lastModified() throws Exception {
        return new File(getURI()).lastModified() / 1000;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean delete() throws Exception {
        return deleteRecursively(getURI().getPath());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RemoteFile[] listFiles() throws Exception {
        RemoteFile[] files;
        if (isDirectory()) {
            // Get files in directory
            File[] f = new File(getURI()).listFiles();
            files = new RemoteFile[f.length];
            // Create remote files from local files
            for (int i = 0; i < f.length; i++) {
                files[i] = new FileRemoteFile(f[i].toURI());
            }
        } else {
            // Return 0 files
            files = new RemoteFile[0];
        }
        // Sort files
        Arrays.sort(files);
        return files;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean mkDir() throws Exception {
        return new File(getURI()).mkdir();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RemoteFile getParent() throws Exception {
        String path = getFullName();
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        path = FilenameUtils.getFullPath(path);
        // Build URI
        URI uri = new File(path).toURI();
        // Create remote file and open it
        RemoteFile file = new FileRemoteFile(uri);
        file.open();
        return file;
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
            // Get files in directory
            String[] files = file.list();
            for (int i = 0; i < files.length; i++) {
                // Delete each file recursively
                deleteRecursively(files[i]);
            }
        }
        // Delete this file
        return file.delete();
    }

}
