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
 *   2020-08-13 (Vyacheslav Soldatov): created
 */
package org.knime.ext.ssh.filehandling.fs;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.sshd.client.subsystem.sftp.SftpClient;
import org.apache.sshd.client.subsystem.sftp.SftpClient.Attributes;
import org.apache.sshd.client.subsystem.sftp.SftpClient.OpenMode;
import org.apache.sshd.client.subsystem.sftp.fs.SftpFileSystemProvider;
import org.apache.sshd.client.subsystem.sftp.impl.SftpRemotePathChannel;
import org.apache.sshd.common.SshException;
import org.apache.sshd.common.subsystem.sftp.SftpConstants;
import org.apache.sshd.common.subsystem.sftp.SftpException;
import org.apache.sshd.common.util.GenericUtils;
import org.apache.sshd.common.util.ValidateUtils;
import org.apache.sshd.common.util.io.IoUtils;

/**
 * This class contains methods for access to native NAME SFTP implementation.
 * Most of code has copied from {@link SftpFileSystemProvider}
 *
 * @author @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
public final class NativeSftpProviderUtils {
    private NativeSftpProviderUtils() {
    }

    @SuppressWarnings("resource")
    static SeekableByteChannel newByteChannelInternal(final ConnectionResource resource, final SshPath path,
            final Set<? extends OpenOption> options, @SuppressWarnings("unused") final FileAttribute<?>... attrs)
            throws IOException {

        Collection<OpenMode> modes = OpenMode.fromOpenOptions(options);
        if (modes.isEmpty()) {
            modes = EnumSet.of(OpenMode.Read, OpenMode.Write);
        }

        // TODO: process file attributes
        SftpRemotePathChannel nativeChannel = new SftpRemotePathChannel(path.toSftpString(), resource.getClient(), true,
                modes);
        return new SshSeekableByteChannel(resource, nativeChannel);
    }

    @SuppressWarnings("resource")
    static Iterator<SshPath> createPathIteratorImpl(final ConnectionResource resource, final SshPath dir,
            final Filter<? super Path> filter) throws IOException {
        final SftpClient sftpClient = resource.getClient();
        // FIXME directory stream is not really closed
        return new SshPathIterator(dir, sftpClient.readDir(dir.toSftpString()).iterator(), filter);
    }

    static Void createDirectoryInternal(final SftpClient sftp, final SshPath dir, final FileAttribute<?>... attrs)
            throws IOException {

        try {
            sftp.mkdir(dir.toString());
        } catch (SftpException e) {
            int sftpStatus = e.getStatus();
            if ((sftp.getVersion() == SftpConstants.SFTP_V3) && (sftpStatus == SftpConstants.SSH_FX_FAILURE)) {
                try {
                    Attributes attributes = sftp.stat(dir.toString());
                    if (attributes != null) {
                        throw new FileAlreadyExistsException(dir.toString());
                    }
                } catch (SshException e2) {
                    e.addSuppressed(e2);
                }
            }
            if (sftpStatus == SftpConstants.SSH_FX_FILE_ALREADY_EXISTS) {
                throw new FileAlreadyExistsException(dir.toString());
            }
            throw e;
        }
        for (FileAttribute<?> attr : attrs) {
            setAttribute(sftp, dir, attr.name(), attr.value());
        }

        return null;
    }

    static SftpClient.Attributes readRemoteAttributes(final SftpClient client,
            final SshPath path,
            final LinkOption... options) throws IOException {
        try {
            SftpClient.Attributes attrs;
            if (IoUtils.followLinks(options)) {
                attrs = client.stat(path.toString());
            } else {
                attrs = client.lstat(path.toString());
            }
            return attrs;
        } catch (SftpException e) {
            if (e.getStatus() == SftpConstants.SSH_FX_NO_SUCH_FILE) {
                throw new NoSuchFileException(path.toString());
            }
            throw e;
        }
    }

    static Void setAttribute(final SftpClient client, final SshPath path, final String attribute, final Object value,
            final LinkOption... options) throws IOException {
        String view;
        String attr;
        int i = attribute.indexOf(':');
        if (i == -1) {
            view = "basic";
            attr = attribute;
        } else {
            view = attribute.substring(0, i++);
            attr = attribute.substring(i);
        }

        setAttribute(client, path, view, attr, value, options);
        return null;
    }

    private static void setAttribute(final SftpClient client, final SshPath path, final String view, final String attr,
            final Object value, @SuppressWarnings("unused") final LinkOption... options) throws IOException {
        @SuppressWarnings("resource")
        Collection<String> views = path.getFileSystem().supportedFileAttributeViews();
        if (GenericUtils.isEmpty(views) || (!views.contains(view))) {
            throw new UnsupportedOperationException("setAttribute(" + path + ")[" + view + ":" + attr + "=" + value
                    + "] view " + view + " not supported: " + views);
        }

        SftpClient.Attributes attributes = new SftpClient.Attributes();
        switch (attr) {
        case "lastModifiedTime":
            attributes.modifyTime((int) ((FileTime) value).to(TimeUnit.SECONDS));
            break;
        case "lastAccessTime":
            attributes.accessTime((int) ((FileTime) value).to(TimeUnit.SECONDS));
            break;
        case "creationTime":
            attributes.createTime((int) ((FileTime) value).to(TimeUnit.SECONDS));
            break;
        case "size":
            attributes.size(((Number) value).longValue());
            break;
        case "permissions": {
            @SuppressWarnings("unchecked")
            Set<PosixFilePermission> attrSet = (Set<PosixFilePermission>) value;
            attributes.perms(attributesToPermissions(attrSet));
            break;
        }
        case "owner":
            attributes.owner(((UserPrincipal) value).getName());
            break;
        case "group":
            attributes.group(((GroupPrincipal) value).getName());
            break;
        case "acl": {
            ValidateUtils.checkTrue("acl".equalsIgnoreCase(view), "ACL cannot be set via view=%s", view);
            @SuppressWarnings("unchecked")
            List<AclEntry> acl = (List<AclEntry>) value;
            attributes.acl(acl);
            break;
        }
        case "isRegularFile":
        case "isDirectory":
        case "isSymbolicLink":
        case "isOther":
        case "fileKey":
            throw new UnsupportedOperationException(
                    "setAttribute(" + path + ")[" + view + ":" + attr + "=" + value + "] modification N/A");
        default:
        }

        client.setStat(path.toSftpString(), attributes);
    }

    private static int attributesToPermissions(final Collection<PosixFilePermission> perms) {
        if (GenericUtils.isEmpty(perms)) {
            return 0;
        }

        int pf = 0;
        for (PosixFilePermission p : perms) {
            switch (p) {
            case OWNER_READ:
                pf |= SftpConstants.S_IRUSR;
                break;
            case OWNER_WRITE:
                pf |= SftpConstants.S_IWUSR;
                break;
            case OWNER_EXECUTE:
                pf |= SftpConstants.S_IXUSR;
                break;
            case GROUP_READ:
                pf |= SftpConstants.S_IRGRP;
                break;
            case GROUP_WRITE:
                pf |= SftpConstants.S_IWGRP;
                break;
            case GROUP_EXECUTE:
                pf |= SftpConstants.S_IXGRP;
                break;
            case OTHERS_READ:
                pf |= SftpConstants.S_IROTH;
                break;
            case OTHERS_WRITE:
                pf |= SftpConstants.S_IWOTH;
                break;
            case OTHERS_EXECUTE:
                pf |= SftpConstants.S_IXOTH;
                break;
            default:
            }
        }

        return pf;
    }
}
