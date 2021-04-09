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
 *   2020-08-28 (soldatov): created
 */
package org.knime.ext.ssh.filehandling.fs;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.Set;

import org.apache.sshd.common.util.GenericUtils;
import org.apache.sshd.sftp.client.SftpClient;

/**
 * Implementation of {@link PosixFileAttributeView}
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
public class SshFileAttributeView implements PosixFileAttributeView {
    private final PosixFileAttributeView m_view;
    private final Path m_path;

    /**
     * @param path
     *            file path.
     * @param view
     *            file attributes view.
     */
    public SshFileAttributeView(final Path path, final PosixFileAttributeView view) {
        m_path = path;
        m_view = view;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PosixFileAttributes readAttributes() throws IOException {
        return m_view.readAttributes();
    }

    @Override
    public void setTimes(final FileTime lastModifiedTime, final FileTime lastAccessTime, final FileTime createTime)
            throws IOException {
        SftpClient.Attributes attrs = new SftpClient.Attributes();

        if (lastModifiedTime != null) {
            // last modified time and last access time should be
            // processed commonly. It is specific of Mine SFTP implementation
            attrs.modifyTime(lastModifiedTime);
            attrs.accessTime(lastModifiedTime);
        }
        if (lastAccessTime != null) {
            // last modified time and last access time should be
            // processed commonly. It is specific of Mine SFTP implementation
            attrs.modifyTime(lastAccessTime);
            attrs.accessTime(lastAccessTime);
        }
        if (createTime != null) {
            attrs.createTime(createTime);
        }

        if (!GenericUtils.isEmpty(attrs.getFlags())) {
            provider().writeRemoteAttributes(m_path, attrs);
        }
    }

    @Override
    public void setPermissions(final Set<PosixFilePermission> perms) throws IOException {
        setAttribute("permissions", perms);
    }

    @Override
    public void setGroup(final GroupPrincipal group) throws IOException {
        setAttribute("group", group);
    }

    @Override
    public UserPrincipal getOwner() throws IOException {
        return readAttributes().owner();
    }

    @Override
    public void setOwner(final UserPrincipal owner) throws IOException {
        setAttribute("owner", owner);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String name() {
        return m_view.name();
    }

    private void setAttribute(final String attribute, final Object value) throws IOException {
        provider().setAttribute(m_path, attribute, value);
    }

    @SuppressWarnings("resource")
    private SshFileSystemProvider provider() {
        return (SshFileSystemProvider) m_path.getFileSystem().provider();
    }
}
