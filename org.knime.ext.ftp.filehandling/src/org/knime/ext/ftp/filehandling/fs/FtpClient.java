/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   2020-10-03 (Vyacheslav Soldatov): created
 */
package org.knime.ext.ftp.filehandling.fs;

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.NoSuchFileException;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

/**
 * Facade class for {@link FTPClient}
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
public class FtpClient {
    private final FTPClient m_client;
    private final FtpClientFeatures m_features;

    /**
     * @param client
     *            FTP client.
     * @param features
     *            client features.
     */
    public FtpClient(final FTPClient client, final FtpClientFeatures features) {
        m_client = client;
        m_features = features;
    }

    /**
     * closes client.
     */
    public void close() {
        try {
            try {
                m_client.logout();
            } finally {
                m_client.disconnect();
            }
        } catch (IOException ex) { // NOSONAR. It is not important what is answered the server
            // if connection closing
        }
    }

    /**
     * @param path
     *            path to file to get information.
     * @return file metadata.
     * @throws IOException
     */
    public FTPFile getFileInfo(final FtpPath path) throws IOException {
        if (m_features.ismListSupported()) {
            return mlist(path.toString());
        }

        // modern MLST feature is not implemented on server side
        // need to list a parent folder and select the file from list of files
        if (path.isRoot()) {
            // create synthetic root folder
            FTPFile root = new FTPFile();
            root.setName(null);
            root.setType(FTPFile.DIRECTORY_TYPE);
            return root;
        }

        final String fileName = path.getFileName().toString();
        // list parent folder and select file with given name.
        for (FTPFile file : listFiles(path.getParent().toString())) {
            if (fileName.equals(file.getName())) {
                return file;
            }
        }

        throw new NoSuchFileException(path.toString());
    }

    private FTPFile mlist(final String path) throws IOException {
        FTPFile file = m_client.mlistFile(path);
        if (file == null) {
            throw new NoSuchFileException(path);
        }
        return file;
    }

    /**
     * @param path
     *            directory path to create.
     * @throws IOException
     */
    public void mkdir(final String path) throws IOException {
        m_client.mkd(path);
        checkPositiveResponse();
    }

    /**
     * @param dir
     *            directory to list.
     * @return array of FTP files.
     * @throws IOException
     */
    public FTPFile[] listFiles(final String dir) throws IOException {
        return m_client.listFiles(dir,
                f -> f != null && !".".equals(f.getName()) && !"..".equals(f.getName()));
    }

    /**
     * @param path
     *            file path.
     * @throws IOException
     */
    public void deleteFile(final String path) throws IOException {
        m_client.deleteFile(path);
        checkPositiveResponse();
    }

    /**
     * @param path
     *            file path.
     * @throws IOException
     */
    public void deleteDirectory(final String path) throws IOException {
        m_client.removeDirectory(path);
        checkPositiveResponse();
    }

    /**
     * @param path
     *            file path.
     * @param in
     *            file content.
     * @throws IOException
     */
    public void createFile(final String path, final InputStream in) throws IOException {
        m_client.storeFile(path, in);
    }

    /**
     * @param file
     *            file
     * @return file output stream.
     * @throws IOException
     */
    @SuppressWarnings("resource")
    public OutputStream openForRewrite(final String file) throws IOException {
        OutputStream stream = m_client.storeFileStream(file);
        if (stream == null) {
            throw new IOException(m_client.getReplyString().trim());
        }
        return wrapToCompletePendingCommand(stream);
    }

    /**
     * @param file
     *            file
     * @return file output stream.
     * @throws IOException
     */
    @SuppressWarnings("resource")
    public OutputStream openForAppend(final String file) throws IOException {
        OutputStream stream = m_client.appendFileStream(file);
        if (stream == null) {
            throw new IOException(m_client.getReplyString().trim());
        }
        return wrapToCompletePendingCommand(stream);
    }

    private OutputStream wrapToCompletePendingCommand(final OutputStream stream) {
        return new FilterOutputStream(stream) {
            /**
             * {@inheritDoc}
             */
            @Override
            public void close() throws IOException {
                try {
                    super.close();
                } finally {
                    completePendingCommand();
                }
            }
        };
    }

    void completePendingCommand() throws IOException {
        if (!m_client.completePendingCommand() && m_client.getReplyCode() != 426) {
            // reply code 426 means that the TCP connection was established but then broken
            // by the client, which happens if an input stream is closed before end-of-file.
            throw new IOException(m_client.getReplyString().trim());
        }
    }

    /**
     * @param path
     *            file path.
     * @param out
     *            local output stream.
     * @throws IOException
     */
    public void getFileContent(final String path, final OutputStream out) throws IOException {
        m_client.retrieveFile(path, out);
    }

    /**
     * @param path
     *            file path.
     * @return file content as stream.
     * @throws IOException
     */
    @SuppressWarnings("resource")
    public InputStream getFileContentAsStream(final String path) throws IOException {
        final InputStream stream = m_client.retrieveFileStream(path);
        if (stream == null) {
            throw new IOException(m_client.getReplyString().trim());
        }

        return new FilterInputStream(stream) {
            /**
             * {@inheritDoc}
             */
            @Override
            public void close() throws IOException {
                try {
                    super.close();
                } finally {
                    completePendingCommand();
                }
            }
        };
    }

    private void checkPositiveResponse() throws IOException {
        if (!isPositiveResponse()) {
            throw new IOException(m_client.getReplyString().trim());
        }
    }

    private boolean isPositiveResponse() {
        return FTPReply.isPositiveCompletion(m_client.getReplyCode());
    }

    /**
     * @param client
     *            FTP client.
     * @throws IOException
     *             I/O exception with details if not positive response.
     */
    public static void checkPositiveResponse(final FTPClient client) throws IOException {
        if (!FTPReply.isPositiveCompletion(client.getReplyCode())) {
            throw new IOException(client.getReplyString().trim());
        }
    }

    /**
     * @param from
     *            from path.
     * @param to
     *            path.
     * @throws IOException
     */
    public void rename(final String from, final String to) throws IOException {
        if (!m_client.rename(from, to)) {
            throw new IOException(m_client.getReplyString().trim());
        }
    }
}
