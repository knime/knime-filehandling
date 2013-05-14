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

import org.apache.commons.io.FilenameUtils;
import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformation;
import org.knime.core.node.ExecutionContext;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

/**
 * Implementation of the SFTP remote file.
 * 
 * 
 * @author Patrick Winter, KNIME.com, Zurich, Switzerland
 */
public class SFTPRemoteFile extends RemoteFile {

    private static ChannelSftp channel = null;

    private String m_path = null;

    private String m_nameCache = null;

    private String m_pathCache = null;

    private Boolean m_existsCache = null;

    private Boolean m_isdirCache = null;

    private LsEntry m_entryCache = null;

    private Long m_sizeCache = null;

    private Long m_modifiedCache = null;

    private void resetCache() {
        m_pathCache = null;
        m_nameCache = null;
        m_existsCache = null;
        m_isdirCache = null;
        m_entryCache = null;
        m_sizeCache = null;
        m_modifiedCache = null;
        m_path = null;
    }

    /**
     * Creates a SFTP remote file for the given URI.
     * 
     * 
     * @param uri The URI
     * @param connectionInformation Connection information to the given URI
     * @param connectionMonitor Monitor for the connection
     */
    SFTPRemoteFile(final URI uri, final ConnectionInformation connectionInformation,
            final ConnectionMonitor connectionMonitor) {
        super(uri, connectionInformation, connectionMonitor);
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
        return new SSHConnection(getURI(), getConnectionInformation());
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
        if (m_nameCache == null) {
            String name;
            if (isDirectory()) {
                // Remove '/' from path and separate name
                String path = getPath();
                path = path.substring(0, path.length() - 1);
                name = FilenameUtils.getName(path);
            } else {
                // Use name from URI
                name = FilenameUtils.getName(getURI().getPath());
            }
            m_nameCache = name;
        }
        return m_nameCache;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPath() throws Exception {
        if (m_pathCache == null) {
            openChannel();
            String path = getURI().getPath();
            // If path is empty use working directory
            if (path == null || path.length() == 0) {
                // Use path determined through first run of openChannel()
                path = m_path;
            }
            boolean changed = cd(path);
            // If directory has not changed the path pointed to a file
            if (!changed) {
                path = FilenameUtils.getFullPath(path);
            }
            // Make sure that the path ends with a '/'
            if (!path.endsWith("/")) {
                path += "/";
            }
            // Make sure that the path starts with a '/'
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
            m_pathCache = path;
        }
        return m_pathCache;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exists() throws Exception {
        if (m_existsCache == null) {
            // In case of the root directory there is no ls entry available but
            // isDirectory returns true
            m_existsCache = getLsEntry() != null || isDirectory();
        }
        return m_existsCache;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDirectory() throws Exception {
        if (m_isdirCache == null) {
            boolean isDirectory = false;
            openChannel();
            // Use path from URI
            String path = getURI().getPath();
            if (path != null && path.length() > 0) {
                // If path is not missing, try to cd to it
                isDirectory = cd(path);
            } else {
                // If path is missing, interpret this file as root directory, so
                // it
                // is always a directory
                isDirectory = true;
            }
            m_isdirCache = isDirectory;
        }
        return m_isdirCache;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void move(final RemoteFile file, final ExecutionContext exec) throws Exception {
        // If the file is also an SFTP remote file and over the same connection
        // it can be moved
        if (file instanceof SFTPRemoteFile && getIdentifier().equals(file.getIdentifier())) {
            SFTPRemoteFile source = (SFTPRemoteFile)file;
            openChannel();
            // Remember if file existed before
            boolean existed = exists();
            // Move file
            channel.rename(source.getURI().getPath(), getURI().getPath());
            resetCache();
            // Success if target did not exist and now exists and the source
            // does not exist anymore
            boolean success = !existed && exists() && !source.exists();
            if (!success) {
                throw new Exception("Move operation failed");
            }
        } else {
            super.move(file, exec);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream openInputStream() throws Exception {
        openChannel();
        String path = getURI().getPath();
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
        String path = getURI().getPath();
        OutputStream stream = channel.put(path);
        // Open stream (null if stream could not be opened)
        if (stream == null) {
            throw new Exception("Path not reachable");
        }
        resetCache();
        return stream;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSize() throws Exception {
        if (m_sizeCache == null) {
            // Assume missing
            long size = 0;
            LsEntry entry = getLsEntry();
            if (entry != null) {
                size = entry.getAttrs().getSize();
            }
            m_sizeCache = size;
        }
        return m_sizeCache;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long lastModified() throws Exception {
        if (m_modifiedCache == null) {
            // Assume missing
            long time = 0;
            LsEntry entry = getLsEntry();
            if (entry != null) {
                time = entry.getAttrs().getMTime();
            }
            m_modifiedCache = time;
        }
        return m_modifiedCache;
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
                resetCache();
                result = result && !exists();
            } else {
                // Delete this file
                channel.rm(path);
                resetCache();
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
        openChannel();
        if (isDirectory()) {
            try {
                cd(m_path);
                // Get ls entries
                List<LsEntry> entries = channel.ls(".");
                // Generate remote file for each entry that is a file
                for (int i = 0; i < entries.size(); i++) {
                    // . and .. will return null after normalization
                    String filename = entries.get(i).getFilename();
                    if (filename.equals(".") || filename.equals("..")) {
                        filename = "";
                    }
                    if (filename != null && filename.length() > 0) {
                        try {
                            // Build URI
                            URI uri =
                                    new URI(getURI().getScheme() + "://" + getURI().getAuthority() + getPath()
                                            + filename);
                            // Create remote file and open it
                            RemoteFile file =
                                    new SFTPRemoteFile(uri, getConnectionInformation(), getConnectionMonitor());
                            file.open();
                            // Add remote file to the result list
                            files.add(file);
                        } catch (URISyntaxException e) {
                            // ignore files that are not representable
                        }
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
        boolean result = false;
        try {
            channel.mkdir(getFullName());
            resetCache();
            result = true;
        } catch (Exception e) {
            // result stays false
        }
        return result;
    }

    /**
     * Opens the SFTP channel if it is not already open.
     * 
     * 
     * @throws Exception If the channel could not be opened
     */
    private void openChannel() throws Exception {
        // Check if channel is ready
        if (channel == null || !channel.getSession().isConnected()) {
            Session session = ((SSHConnection)getConnection()).getSession();
            if (!session.isConnected()) {
                session.connect();
            }
            channel = (ChannelSftp)session.openChannel("sftp");
        }
        // Connect channel
        if (!channel.isConnected()) {
            channel.connect();
        }
        // Check if path is initialized
        if (m_path == null) {
            boolean pathSet = false;
            String path = getURI().getPath();
            // If URI has path
            if (path != null && path.length() > 0) {
                if (cd(path)) {
                    // Path points to directory
                    m_path = path;
                    pathSet = true;
                } else {
                    // Path points to file
                    path = FilenameUtils.getFullPath(path);
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
        if (m_entryCache == null) {
            // Assume missing
            LsEntry entry = null;
            openChannel();
            // If this is a directory change to parent
            if (isDirectory()) {
                channel.cd("..");
            }
            // Get all entries in working directory
            List<LsEntry> entries = channel.ls(".");
            // Check all entries by name
            for (int i = 0; i < entries.size(); i++) {
                LsEntry currentEntry = entries.get(i);
                if (currentEntry.getFilename().equals(getName())) {
                    // Entry with the same name is the correct one
                    entry = currentEntry;
                    break;
                }
            }
            m_entryCache = entry;
        }
        return m_entryCache;
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
