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
import java.util.Vector;

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

    private URI m_uri;

    private RemoteCredentials m_credentials;

    private ChannelSftp m_channel;

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
            // should not happen
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
    protected String getIdentifier() {
        return buildIdentifier(m_uri);
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
        LsEntry entry = getLsEntry();
        return entry.getFilename();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFullName() throws Exception {
        return getName() + "/" + getPath();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPath() throws Exception {
        // TODO get correct path
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exists() throws Exception {
        return getLsEntry() != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDirectory() throws Exception {
        boolean isDirectory = false;
        LsEntry entry = getLsEntry();
        if (entry != null) {
            isDirectory = entry.getAttrs().isDir();
        }
        return isDirectory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean move(final RemoteFile file) throws Exception {
        boolean success;
        if (file instanceof SFTPRemoteFile
                && getIdentifier().equals(file.getIdentifier())) {
            SFTPRemoteFile source = (SFTPRemoteFile)file;
            openChannel();
            boolean existed = exists();
            m_channel.rename(source.m_uri.getPath(), m_uri.getPath());
            success = !existed && exists() && !source.exists();
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
        openChannel();
        String path = m_uri.getPath();
        InputStream stream = m_channel.get(path);
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
        OutputStream stream = m_channel.put(path);
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
        boolean result = true;
        openChannel();
        String path = m_uri.getPath();
        try {
            if (isDirectory()) {
                m_channel.rmdir(path);
            } else {
                m_channel.rm(path);
            }
        } catch (SftpException e) {
            int code = Integer.parseInt(e.toString().split(":")[0]);
            if (code == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                result = false;
            } else {
                throw e;
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
        RemoteFile[] files;
        if (isDirectory()) {
            Vector<LsEntry> entries = m_channel.ls(m_uri.getPath());
            files = new RemoteFile[entries.size()];
            for (int i = 0; i < entries.size(); i++) {
                URI uri =
                        new URI(m_uri.getScheme() + "://"
                                + m_uri.getAuthority()
                                + entries.get(i).getFilename());
                files[i] = new SFTPRemoteFile(uri, m_credentials);
            }
        } else {
            files = new RemoteFile[0];
        }
        Arrays.sort(files);
        return files;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean mkDir() throws Exception {
        boolean existed = exists();
        m_channel.mkdir(m_uri.getPath());
        return !existed && exists();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws Exception {
        if (m_channel != null) {
            m_channel.disconnect();
        }
    }

    /**
     * Opens the SFTP channel if it is not already open.
     * 
     * 
     * @throws Exception If the channel could not be opened
     */
    private void openChannel() throws Exception {
        if (m_channel == null || !m_channel.isConnected()) {
            Session session = ((SSHConnection)getConnection()).getSession();
            m_channel = (ChannelSftp)session.openChannel("sftp");
            m_channel.connect();
        }
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
        LsEntry entry = null;
        openChannel();
        String path = m_uri.getPath();
        Vector<LsEntry> entries = m_channel.ls(path);
        for (int i = 0; i < entries.size(); i++) {
            LsEntry currentEntry = entries.get(i);
            if (currentEntry.getFilename().equals(path)) {
                entry = currentEntry;
                break;
            }
        }
        return entry;
    }

}
