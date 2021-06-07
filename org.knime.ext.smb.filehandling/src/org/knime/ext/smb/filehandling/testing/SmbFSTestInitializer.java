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
 *   2021-03-07 (Alexander Bondaletov): created
 */
package org.knime.ext.smb.filehandling.testing;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.LinkedList;

import org.knime.ext.smb.filehandling.fs.SmbFileSystem;
import org.knime.ext.smb.filehandling.fs.SmbPath;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.testing.DefaultFSTestInitializer;
import org.knime.filehandling.core.testing.FSTestInitializer;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msfscc.FileAttributes;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2CreateOptions;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.share.File;

/**
 * {@link FSTestInitializer} for SMB file system.
 *
 * @author Alexander Bondaletov
 */
class SmbFSTestInitializer extends DefaultFSTestInitializer<SmbPath, SmbFileSystem> {

    private DiskShare m_client;

    /**
     * @param fsConnection
     *            FS connection.
     */
    protected SmbFSTestInitializer(final FSConnection fsConnection) {
        super(fsConnection);

        m_client = getFileSystem().getClient();
    }

    @Override
    public SmbPath createFileWithContent(final String content, final String... pathComponents) throws IOException {
        SmbPath path = makePath(pathComponents);
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

        ensureDirectoryExists((SmbPath) path.getParent());

        try (File file = m_client.openFile(path.getSmbjPath(), EnumSet.of(AccessMask.GENERIC_WRITE),
                EnumSet.of(FileAttributes.FILE_ATTRIBUTE_NORMAL), EnumSet.of(SMB2ShareAccess.FILE_SHARE_WRITE),
                SMB2CreateDisposition.FILE_OVERWRITE_IF, EnumSet.noneOf(SMB2CreateOptions.class))) {
            try (OutputStream out = file.getOutputStream()) {
                out.write(bytes);
            }
        }
        return path;
    }

    @Override
    protected void beforeTestCaseInternal() throws IOException {
        ensureDirectoryExists(getTestCaseScratchDir());
    }

    private void ensureDirectoryExists(SmbPath dir) {
        final LinkedList<String> paths = new LinkedList<>();

        while (!dir.isRoot() && !m_client.folderExists(dir.getSmbjPath())) {
            paths.add(dir.getSmbjPath());
            dir = (SmbPath) dir.getParent();
        }

        while (!paths.isEmpty()) {
            m_client.mkdir(paths.removeLast());
        }
    }

    @Override
    protected void afterTestCaseInternal() throws IOException {
        SmbPath dir = getTestCaseScratchDir();
        m_client.rmdir(dir.getSmbjPath(), true);
    }

    @Override
    public void afterClass() throws IOException {
        SmbPath dir = getFileSystem().getWorkingDirectory();
        m_client.rmdir(dir.getSmbjPath(), true);
    }
}
