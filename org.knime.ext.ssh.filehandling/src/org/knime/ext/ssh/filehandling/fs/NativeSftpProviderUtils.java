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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.sshd.client.subsystem.sftp.SftpClient;
import org.apache.sshd.client.subsystem.sftp.SftpClient.Attributes;
import org.apache.sshd.client.subsystem.sftp.SftpClient.OpenMode;
import org.apache.sshd.client.subsystem.sftp.extensions.CopyFileExtension;
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
    static InputStream newInputStreamInternalImpl(final ConnectionResource resource, final SshPath path,
            final OpenOption... options) throws IOException {
        final SftpClient sftpClient = resource.getClient();
        Collection<OpenMode> modes = OpenMode.fromOpenOptions(Arrays.asList(options));
        if (modes.isEmpty()) {
            modes = EnumSet.of(OpenMode.Read);
        }

        return sftpClient.read(path.toSftpString(), modes);
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

        if (attrs.length > 0) {
            SftpClient.Attributes attributes = new SftpClient.Attributes();
            for (FileAttribute<?> attr : attrs) {
                addToAttributres(dir, attributes, attr.name(), attr.value());
            }
            sftp.setStat(dir.toSftpString(), attributes);
        }

        return null;
    }

    static Void copyInternal(final SftpClient sftpClient, final SshPath source,
            final SshPath target,
            final CopyOption... options) throws IOException {
        @SuppressWarnings("resource")
        SshFileSystemProvider provider = source.getFileSystem().provider();

        boolean replaceExisting = false;
        boolean copyAttributes = false;
        boolean noFollowLinks = false;
        for (CopyOption opt : options) {
            replaceExisting |= opt == StandardCopyOption.REPLACE_EXISTING;
            copyAttributes |= opt == StandardCopyOption.COPY_ATTRIBUTES;
            noFollowLinks |= opt == LinkOption.NOFOLLOW_LINKS;
        }
        LinkOption[] linkOptions = IoUtils.getLinkOptions(!noFollowLinks);

        // attributes of source file
        BasicFileAttributes attrs = provider.readAttributes(source, BasicFileAttributes.class, linkOptions);
        if (attrs.isSymbolicLink()) {
            throw new IOException("Copying of symbolic links not supported");
        }

        if (replaceExisting) {
            provider.deleteIfExists(target);
        }

        // create directory or copy file
        if (attrs.isDirectory()) {
            createDirectoryInternal(sftpClient, target);
        } else {
            CopyFileExtension copyFile = sftpClient.getExtension(CopyFileExtension.class);
            if (copyFile.isSupported()) {
                copyFile.copyFile(source.toString(), target.toString(), false);
            } else {
                try (InputStream in = provider.newInputStream(source);
                        OutputStream os = provider.newOutputStream(target)) {
                    IoUtils.copy(in, os);
                }
            }
        }

        // copy basic attributes to target
        if (copyAttributes) {
            setAttributes(sftpClient, attrs, target, linkOptions);
        }
        return null;
    }

    static Void setAttributes(
            final SftpClient sftpClient,
            final BasicFileAttributes attrs,
            final SshPath target,
            @SuppressWarnings("unused") final LinkOption... linkOptions) throws IOException {
        SftpClient.Attributes attributes = new SftpClient.Attributes();

        addToAttributres(target, attributes, "lastModifiedTime", attrs.lastModifiedTime());
        addToAttributres(target, attributes, "lastAccessTime", attrs.lastAccessTime());
        addToAttributres(target, attributes, "creationTime", attrs.creationTime());

        sftpClient.setStat(target.toSftpString(), attributes);
        return null;
    }

    static Void moveInternal(final SftpClient sftpClient, final SshPath source,
            final SshPath target,
            final CopyOption... options) throws IOException {
        boolean replaceExisting = false;
        boolean noFollowLinks = false;
        for (CopyOption opt : options) {
            replaceExisting |= opt == StandardCopyOption.REPLACE_EXISTING;
            // atomic move can't be supported by given SFTP implementation
            // because not supported CopyMode
            // atomicMove |= opt == StandardCopyOption.ATOMIC_MOVE;
            noFollowLinks |= opt == LinkOption.NOFOLLOW_LINKS;
        }
        LinkOption[] linkOptions = IoUtils.getLinkOptions(noFollowLinks);
        @SuppressWarnings("resource")
        SshFileSystemProvider provider = target.getFileSystem().provider();

        // attributes of source file
        BasicFileAttributes attrs = provider.readAttributes(source, BasicFileAttributes.class, linkOptions);
        if (attrs.isSymbolicLink()) {
            throw new IOException("Moving of source symbolic link (" + source + ") to " + target + " not supported");
        }

        // delete target if it exists and REPLACE_EXISTING is specified
        if (replaceExisting) {
            provider.deleteIfExists(target);
        }

        sftpClient.rename(source.toSftpString(), target.toSftpString());
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
            @SuppressWarnings("unused") final LinkOption... options) throws IOException {
        SftpClient.Attributes attributes = new SftpClient.Attributes();
        addToAttributres(path, attributes, attribute, value);
        client.setStat(path.toSftpString(), attributes);
        return null;
    }

    static Void writeAttributes(final SftpClient client, final SshPath path, final Attributes attrs)
            throws IOException {
        client.setStat(path.toSftpString(), attrs);
        return null;
    }

    private static void addToAttributres(final SshPath path, final SftpClient.Attributes attributes,
            final String attribute, final Object value) {
        if (value == null) {
            return;
        }

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

        @SuppressWarnings("resource")
        Collection<String> views = path.getFileSystem().supportedFileAttributeViews();
        if (GenericUtils.isEmpty(views) || (!views.contains(view))) {
            throw new UnsupportedOperationException("setAttribute(" + path + ")[" + view + ":" + attr + "=" + value
                    + "] view " + view + " not supported: " + views);
        }

        switch (attr) {
        // this Mine SFTP implementation ignores the changes only one
        // lastModifiedTime or lastAccessTime attribute
        // should be changed both
        case "lastModifiedTime":
        case "lastAccessTime":
            attributes.modifyTime((int) ((FileTime) value).to(TimeUnit.SECONDS));
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
