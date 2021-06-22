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
 *   2020-10-04 (Vyacheslav Soldatov): created
 */
package org.knime.ext.ftp.filehandling.fs;

import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.net.ftp.FTPFile;
import org.knime.filehandling.core.connections.base.attributes.BaseFileAttributes;
import org.knime.filehandling.core.connections.base.attributes.BasePrincipal;

/**
 * FTP implementation of the {@link BaseFileAttributes}.
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
public class FtpFileAttributes extends BaseFileAttributes {
    private final FTPFile m_file;

    /**
     * @param path
     *            file path.
     * @param file
     *            Google file info.
     */
    public FtpFileAttributes(final FtpPath path, final FTPFile file) {
        super(file.isFile(), path, toTime(file.getTimestamp()), toTime(file.getTimestamp()),
                toTime(file.getTimestamp()), file.getSize(),
                file.isSymbolicLink(), !file.isFile() && !file.isDirectory(), toPrincipal(file.getUser()),
                toPrincipal(file.getGroup()), getPermissionSet(file));
        m_file = file;
    }

    /**
     * @param ftpFile
     * @return set of POSIX permissions.
     */
    private static Set<PosixFilePermission> getPermissionSet(final FTPFile ftpFile) {
        HashSet<PosixFilePermission> permissions = new HashSet<>();
        addPermissionIfSet(ftpFile, FTPFile.USER_ACCESS, FTPFile.READ_PERMISSION, PosixFilePermission.OWNER_READ,
                permissions);
        addPermissionIfSet(ftpFile, FTPFile.USER_ACCESS, FTPFile.WRITE_PERMISSION, PosixFilePermission.OWNER_WRITE,
                permissions);
        addPermissionIfSet(ftpFile, FTPFile.USER_ACCESS, FTPFile.EXECUTE_PERMISSION, PosixFilePermission.OWNER_EXECUTE,
                permissions);
        addPermissionIfSet(ftpFile, FTPFile.GROUP_ACCESS, FTPFile.READ_PERMISSION, PosixFilePermission.GROUP_READ,
                permissions);
        addPermissionIfSet(ftpFile, FTPFile.GROUP_ACCESS, FTPFile.WRITE_PERMISSION, PosixFilePermission.GROUP_WRITE,
                permissions);
        addPermissionIfSet(ftpFile, FTPFile.GROUP_ACCESS, FTPFile.EXECUTE_PERMISSION, PosixFilePermission.GROUP_EXECUTE,
                permissions);
        addPermissionIfSet(ftpFile, FTPFile.WORLD_ACCESS, FTPFile.READ_PERMISSION,
                PosixFilePermission.OTHERS_READ, // NOSONAR just read from server
                permissions);
        addPermissionIfSet(ftpFile, FTPFile.WORLD_ACCESS, FTPFile.WRITE_PERMISSION,
                PosixFilePermission.OTHERS_WRITE, // NOSONAR just read from server
                permissions);
        addPermissionIfSet(ftpFile, FTPFile.WORLD_ACCESS, FTPFile.EXECUTE_PERMISSION,
                PosixFilePermission.OTHERS_EXECUTE, // NOSONAR just read from server
                permissions);
        return permissions;
    }

    private static void addPermissionIfSet(final FTPFile file, final int access, final int permission,
            final PosixFilePermission value, final Set<PosixFilePermission> permissions) {
        if (file.hasPermission(access, permission)) {
            permissions.add(value);
        }
    }

    private static GroupPrincipal toPrincipal(final String name) {
        return name == null ? null : new BasePrincipal(name);
    }

    /**
     * @return the file or null if is not a file or directory.
     */
    public FTPFile getMetadata() {
        return m_file;
    }

    private static FileTime toTime(final Calendar timestamp) {
        return timestamp == null ? FileTime.fromMillis(0) : FileTime.fromMillis(timestamp.getTimeInMillis());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FtpPath fileKey() {
        return (FtpPath) super.fileKey();
    }
}
