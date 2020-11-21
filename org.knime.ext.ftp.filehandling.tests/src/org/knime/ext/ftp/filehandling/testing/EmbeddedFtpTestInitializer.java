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
 *   2020-07-28 (Vyacheslav Soldatov): created
 */
package org.knime.ext.ftp.filehandling.testing;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.apache.ftpserver.ftplet.FtpException;
import org.knime.ext.ftp.filehandling.fs.FtpConnectionConfiguration;
import org.knime.ext.ftp.filehandling.fs.FtpFSConnection;
import org.knime.ext.ftp.filehandling.fs.FtpFileSystem;
import org.knime.ext.ftp.filehandling.fs.FtpPath;
import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.testing.DefaultFSTestInitializer;

/**
 * FTP test initializer. This implementation is based on embedded FTP server.
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
public class EmbeddedFtpTestInitializer extends DefaultFSTestInitializer<FtpPath, FtpFileSystem> {
    private boolean m_testInitialized = false;
    /**
     * FTP file system.
     */
    protected final FtpFileSystem m_fileSystem;

    private final EmbeddedFtpServerContainer m_ftpServerContainer;

    /**
     * @param ftpCfg
     *            FTP configuration.
     *
     * @throws IOException
     * @throws FtpException
     */
    public EmbeddedFtpTestInitializer(final FtpConnectionConfiguration ftpCfg) throws IOException, FtpException {
        this(new EmbeddedFtpServerContainer(ftpCfg));
    }

    @SuppressWarnings("resource")
    private EmbeddedFtpTestInitializer(final EmbeddedFtpServerContainer ftpContainer)
            throws IOException, FtpException {
        super(createConnection(ftpContainer));
        m_ftpServerContainer = ftpContainer;
        m_fileSystem = (FtpFileSystem) getFSConnection().getFileSystem();
    }

    /**
     * @param ftpContainer
     * @return
     * @throws IOException
     * @throws FtpException
     */
    private static FtpFSConnection createConnection(final EmbeddedFtpServerContainer ftpContainer)
            throws IOException, FtpException {
        // starts embedded FTP server and creates client
        FtpConnectionConfiguration config = ftpContainer.startAndGetConnectionConfiguration();
        // extend file system for close embedded FTP server on file system closing.
        FtpFileSystem fs = new FtpFileSystem(config) {
            /**
             * {@inheritDoc}
             */
            @Override
            protected void prepareClose() {
                try {
                    super.prepareClose();
                } finally {
                    // stop embedded FTP server
                    ftpContainer.stopServer();
                }
            }
        };
        return new FtpFSConnection(fs) {
        };
    }

    @Override
    public FtpPath createFileWithContent(final String content, final String... pathComponents) throws IOException {

        final FtpPath ftp = makePath(pathComponents);

        // convert to local path and create file.
        Path path = convertToRealPath(ftp);

        Files.createDirectories(path.getParent());

        try (OutputStream out = Files.newOutputStream(path, StandardOpenOption.WRITE,
                StandardOpenOption.CREATE_NEW)) {
            final byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            out.write(bytes);
            out.flush();
        }

        return ftp;
    }

    @Override
    protected void beforeTestCaseInternal() throws IOException {
        final Path scratchDir = getTestCaseScratchDir();

        if (!m_testInitialized) {
            File file = convertToRealPath(scratchDir.getParent()).toFile();
            if (!file.exists()) {
                file.mkdirs();
            }
            m_testInitialized = true;
        }

        Files.createDirectory(scratchDir);
    }

    @Override
    protected void afterTestCaseInternal() throws IOException {
        m_ftpServerContainer.clearFtplets();
        FSFiles.deleteRecursively(convertToRealPath(getTestCaseScratchDir()));
        m_fileSystem.clearAttributesCache();
    }

    @Override
    public void afterClass() throws IOException {
        if (m_testInitialized) {
            m_ftpServerContainer.clearTestHome();
            m_testInitialized = false;
        }
    }

    private Path convertToRealPath(final Path path) throws IOException {
        return new File(m_ftpServerContainer.convertToRealPath(path.toString())).toPath();
    }
}
