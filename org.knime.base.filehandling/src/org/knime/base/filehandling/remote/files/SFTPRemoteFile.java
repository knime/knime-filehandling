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
 *   Nov 5, 2012 (Patrick Winter): created
 */
package org.knime.base.filehandling.remote.files;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import org.apache.commons.io.FilenameUtils;
import org.knime.base.filehandling.remotecredentials.port.RemoteCredentials;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

/**
 * Implementation of the SFTP remote file.
 * 
 * 
 * @author Patrick Winter, University of Konstanz
 */
public class SFTPRemoteFile extends RemoteFile {

    private static ChannelSftp channel = null;

    private String m_path = null;

    private URI m_uri;

    private RemoteCredentials m_credentials;

    /**
     * Creates a SFTP remote file for the given URI.
     * 
     * 
     * @param uri The URI
     * @param credentials Credentials to the given URI
     */
    SFTPRemoteFile(final URI uri, final RemoteCredentials credentials) {
        // Change protocol to general SSH
        try {
            m_uri = new URI(uri.toString().replaceFirst("sftp", "ssh"));
        } catch (URISyntaxException e) {
            // Should not happen, since the syntax remains untouched
        }
        m_credentials = credentials;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean usesConnection() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Connection createConnection() {
        // Use general SSH connection
        return new SSHConnection(m_uri, m_credentials);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URI getURI() {
        return m_uri;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getType() {
        return "sftp";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() throws Exception {
        String name;
        if (isDirectory()) {
            // Remove '/' from path and separate name
            name =
                    FilenameUtils.getName(FilenameUtils
                            .normalizeNoEndSeparator(getPath()));
        } else {
            // Use name from URI
            name = FilenameUtils.getName(m_uri.getPath());
        }
        return FilenameUtils.normalize(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPath() throws Exception {
        openChannel();
        // Use path determined through first run of openChannel()
        String path = m_path;
        // Make sure that the path ends with a '/'
        if (!path.endsWith("/")) {
            path += "/";
        }
        return FilenameUtils.normalize(path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exists() throws Exception {
        // If the file can be found through ls, the file exists
        return getLsEntry() != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDirectory() throws Exception {
        boolean isDirectory = false;
        openChannel();
        // Use path from URI
        String path = FilenameUtils.normalize(m_uri.getPath());
        if (path != null && path.length() > 0) {
            // If path is not missing, try to cd to it
            isDirectory = cd(path);
        } else {
            // If path is missing, interpret this file as root directory, so it
            // is always a directory
            isDirectory = true;
        }
        return isDirectory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void move(final RemoteFile file) throws Exception {
        // If the file is also an SFTP remote file and over the same connection
        // it can be moved
        if (file instanceof SFTPRemoteFile
                && getIdentifier().equals(file.getIdentifier())) {
            SFTPRemoteFile source = (SFTPRemoteFile)file;
            openChannel();
            // Remember if file existed before
            boolean existed = exists();
            // Move file
            channel.rename(source.m_uri.getPath(), m_uri.getPath());
            // Success if target did not exist and now exists and the source
            // does not exist anymore
            boolean success = !existed && exists() && !source.exists();
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
        openChannel();
        String path = m_uri.getPath();
        InputStream stream = channel.get(path);
        // Open stream (null if stream could not be opened)
        if (stream == null) {
            throw new Exception("Path not reachable");
        }
        return stream;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputStream openOutputStream() throws Exception {
        openChannel();
        String path = m_uri.getPath();
        OutputStream stream = channel.put(path);
        // Open stream (null if stream could not be opened)
        if (stream == null) {
            throw new Exception("Path not reachable");
        }
        return stream;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSize() throws Exception {
        // Assume missing
        long size = 0;
        LsEntry entry = getLsEntry();
        if (entry != null) {
            size = entry.getAttrs().getSize();
        }
        return size;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long lastModified() throws Exception {
        // Assume missing
        long time = 0;
        LsEntry entry = getLsEntry();
        if (entry != null) {
            time = entry.getAttrs().getMTime();
        }
        return time;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean delete() throws Exception {
        // Delete can only be true if the file exists
        boolean result = exists();
        openChannel();
        String path = getFullName();
        if (exists()) {
            if (isDirectory()) {
                // Delete inner files first
                RemoteFile[] files = listFiles();
                for (int i = 0; i < files.length; i++) {
                    files[i].delete();
                }
                // Delete this directory
                channel.rmdir(path);
                result = result && !exists();
            } else {
                // Delete this file
                channel.rm(path);
                result = result && !exists();
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public RemoteFile[] listFiles() throws Exception {
        List<RemoteFile> files = new LinkedList<RemoteFile>();
        RemoteFile[] outFiles = new RemoteFile[0];
        if (isDirectory()) {
            try {
                // Get ls entries
                Vector<LsEntry> entries = channel.ls(".");
                // Generate remote file for each entry that is a file
                for (int i = 0; i < entries.size(); i++) {
                    // . and .. will return null after normalization
                    String filename =
                            FilenameUtils.normalize(entries.get(i)
                                    .getFilename());
                    if (filename != null && filename.length() > 0) {
                        // Build URI
                        URI uri =
                                new URI(m_uri.getScheme() + "://"
                                        + m_uri.getAuthority() + getPath()
                                        + filename);
                        // Create remote file and open it
                        RemoteFile file =
                                new SFTPRemoteFile(uri, m_credentials);
                        file.open();
                        // Add remote file to the result list
                        files.add(file);
                    }
                }
                outFiles = files.toArray(new RemoteFile[files.size()]);
            } catch (SftpException e) {
                // Return 0 files
            }
        }
        // Sort results
        Arrays.sort(outFiles);
        return outFiles;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean mkDir() throws Exception {
        boolean existed = exists();
        channel.mkdir(getFullName());
        return !existed && exists();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws Exception {
        if (channel != null) {
            channel.disconnect();
        }
    }

    /**
     * Opens the SFTP channel if it is not already open.
     * 
     * 
     * @throws Exception If the channel could not be opened
     */
    private void openChannel() throws Exception {
        // Check if channel is ready
        if (channel == null) {
            Session session = ((SSHConnection)getConnection()).getSession();
            channel = (ChannelSftp)session.openChannel("sftp");
        }
        // Connect channel
        if (!channel.isConnected()) {
            channel.connect();
        }
        // Check if path is initialized
        if (m_path == null) {
            boolean pathSet = false;
            String path =
                    FilenameUtils.normalizeNoEndSeparator(m_uri.getPath());
            // If URI has path
            if (path != null && path.length() > 0) {
                if (cd(path)) {
                    // Path points to directory
                    m_path = path;
                    pathSet = true;
                } else {
                    // Path points to file
                    path = FilenameUtils.getPath(path);
                    if (cd(path)) {
                        m_path = path;
                        pathSet = true;
                    }
                }
            }
            if (!pathSet) {
                // Use root directory
                String oldDir;
                String newDir;
                // Change directory to parent until the path does not change
                // anymore
                do {
                    oldDir = channel.pwd();
                    cd("..");
                    newDir = channel.pwd();
                } while (!newDir.equals(oldDir));
                m_path = newDir;
            }
        }
        // Change to correct directory
        cd(m_path);
    }

    /**
     * Returns the LsEntry to this file.
     * 
     * 
     * @return LsEntry to this file or null if not existing
     * @throws Exception If the operation could not be executed
     */
    @SuppressWarnings("unchecked")
    private LsEntry getLsEntry() throws Exception {
        // Assume missing
        LsEntry entry = null;
        openChannel();
        // If this is a directory change to parent
        if (isDirectory()) {
            channel.cd("..");
        }
        // Get all entries in working directory
        Vector<LsEntry> entries = channel.ls(".");
        // Check all entries by name
        for (int i = 0; i < entries.size(); i++) {
            LsEntry currentEntry = entries.get(i);
            if (currentEntry.getFilename().equals(getName())) {
                // Entry with the same name is the correct one
                entry = currentEntry;
                break;
            }
        }
        return entry;
    }

    /**
     * Change to another directory.
     * 
     * 
     * @param path Path to the new directory
     * @return true if the cd was successful, false otherwise
     */
    private boolean cd(final String path) {
        boolean result = false;
        try {
            channel.cd(path);
            result = true;
        } catch (SftpException e) {
            // return false if cd was not possible
        }
        return result;
    }

}
