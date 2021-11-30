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
import java.nio.file.AccessDeniedException;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.sshd.common.SshException;
import org.apache.sshd.common.util.GenericUtils;
import org.apache.sshd.common.util.ValidateUtils;
import org.apache.sshd.common.util.io.IoUtils;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClient.Attributes;
import org.apache.sshd.sftp.client.SftpClient.DirEntry;
import org.apache.sshd.sftp.client.SftpClient.OpenMode;
import org.apache.sshd.sftp.client.extensions.CopyFileExtension;
import org.apache.sshd.sftp.client.fs.SftpFileSystem;
import org.apache.sshd.sftp.client.fs.SftpFileSystemProvider;
import org.apache.sshd.sftp.client.impl.SftpRemotePathChannel;
import org.apache.sshd.sftp.common.SftpConstants;
import org.apache.sshd.sftp.common.SftpException;
import org.knime.filehandling.core.connections.base.attributes.BaseFileAttributes;

/**
 * This class contains methods for access to native NAME SFTP implementation.
 * Most of code has copied from {@link SftpFileSystemProvider}
 *
 * @author @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
@SuppressWarnings("restriction")
public final class NativeSftpProviderUtils {
    private NativeSftpProviderUtils() {
    }

    @SuppressWarnings({ "resource" })
    static SeekableByteChannel newByteChannelInternal(final ConnectionResource resource, final SshPath path,
            final Set<? extends OpenOption> options, @SuppressWarnings("unused") final FileAttribute<?>... attrs)
            throws IOException {

        Collection<OpenMode> modes = OpenMode.fromOpenOptions(options);
        if (modes.isEmpty()) {
            modes = EnumSet.of(OpenMode.Read, OpenMode.Write);
        }

        try {
            return new SshSeekableByteChannel(
                    new SftpRemotePathChannel(path.toSftpString(), resource.getClient(), false, modes));
        } catch (SftpException ex) {
            throw convertAndRethrow(ex, path);
        }
    }

    @SuppressWarnings("resource")
    static InputStream newInputStreamInternalImpl(final ConnectionResource resource, final SshPath path,
            final OpenOption... options) throws IOException {
        final SftpClient sftpClient = resource.getClient();
        Collection<OpenMode> modes = OpenMode.fromOpenOptions(Arrays.asList(options));
        if (modes.isEmpty()) {
            modes = EnumSet.of(OpenMode.Read);
        }

        try {
            return sftpClient.read(path.toSftpString(), modes);
        } catch (SftpException ex) {
            throw convertAndRethrow(ex, path);
        }
    }

    @SuppressWarnings("resource")
    static Iterator<SshPath> createPathIteratorImpl(final ConnectionResource resource, final SshPath dir,
            final Filter<? super Path> filter) throws IOException {
        final SftpClient sftpClient = resource.getClient();

        try {
            final List<SshPath> files = new LinkedList<>();
            final Iterator<DirEntry> iter = sftpClient.readDir(dir.toSftpString()).iterator();

            while (iter.hasNext()) {
                final DirEntry entry = iter.next();
                String fileName = entry.getFilename();

                // ignore current and parent directory
                if (".".equals(fileName) || "..".equals(fileName)) {
                    continue;
                }

                final SshPath path = (SshPath) dir.resolve(fileName);

                final BaseFileAttributes attrs = toBaseFileAttributes(path, entry.getAttributes());
                if (!attrs.isSymbolicLink()) {
                    // sftpClient.readDir() does not follow symbolic links.
                    // We should avoid caching file attributes for symbolic links as the cache
                    // entries usually create in methods that follow symlinks, which leads to
                    // inconsistent behavior.
                    dir.getFileSystem().addToAttributeCache(path, attrs);
                }

                try {
                    if (filter == null || filter.accept(path)) {
                        files.add(path);
                    }
                } catch (final IOException e) {
                    throw new DirectoryIteratorException(e);
                }
            }

            return files.iterator();
        } catch (RuntimeException die) {
            if (die.getCause() instanceof SftpException) {
                throw convertAndRethrow((SftpException) die.getCause(), dir);
            }
            throw die;
        } catch (SftpException ex) {
            throw convertAndRethrow(ex, dir);
        }
    }

    static Void createDirectoryInternal(final SftpClient sftp, final SshPath dir, final FileAttribute<?>... attrs)
            throws IOException {

        try {
            sftp.mkdir(dir.toString());
        } catch (SftpException e) {
            int sftpStatus = e.getStatus();
            if ((sftp.getVersion() == SftpConstants.SFTP_V3) && (sftpStatus == SftpConstants.SSH_FX_FAILURE)) {
                try {
                    if (sftp.stat(dir.toString()) != null) {
                        FileAlreadyExistsException faeEx = new FileAlreadyExistsException(dir.toString());
                        faeEx.setStackTrace(e.getStackTrace());
                        throw faeEx;
                    }
                } catch (SshException e2) {
                    e.addSuppressed(e2);
                }
            }
            throw convertAndRethrow(e, dir);
        }

        if (attrs.length > 0) {
            SftpClient.Attributes attributes = new SftpClient.Attributes();
            for (FileAttribute<?> attr : attrs) {
                addToAttributres(dir, attributes, attr.name(), attr.value());
            }
            writeAttributes(sftp, dir, attributes);
        }

        return null;
    }

    static Void delete(final SftpClient sftp, final SshPath path, final BasicFileAttributes attrs) throws IOException {

        try {
            if (attrs.isDirectory()) {
                sftp.rmdir(path.toString());
            } else {
                sftp.remove(path.toString());
            }

            return null;
        } catch (SftpException ex) {
            throw convertAndRethrow(ex, path);
        }
    }

    static Void copyInternal(final SftpClient sftpClient, //
            final SshPath source, //
            final SshPath target, //
            final BasicFileAttributes sourceAttrs, //
            final Set<CopyOption> optionSet) throws IOException {

        // create directory or copy file
        if (sourceAttrs.isDirectory()) {
            createDirectoryInternal(sftpClient, target);
        } else {
            CopyFileExtension copyFile = sftpClient.getExtension(CopyFileExtension.class);
            if (copyFile.isSupported()) {
                try {
                    copyFile.copyFile(source.toString(), //
                            target.toString(), //
                            optionSet.contains(StandardCopyOption.REPLACE_EXISTING));
                } catch (SftpException ex) {
                    throw convertAndRethrow(ex, target);
                }
            } else {
                copyUsingTempFile(sftpClient, source, target);
            }
        }

        // copy basic attributes to target
        if (optionSet.contains(StandardCopyOption.COPY_ATTRIBUTES)) {
            final LinkOption[] linkOptions = IoUtils.getLinkOptions(!optionSet.contains(LinkOption.NOFOLLOW_LINKS));
            setAttributes(sftpClient, sourceAttrs, target, linkOptions);
        }
        return null;
    }

    private static void copyUsingTempFile(final SftpClient sftpClient, final SshPath source, final SshPath target)
            throws IOException {

        final Path localTmpFile = Files.createTempFile("ssh", ".tmp");

        try {
            try (final InputStream in = sftpClient.read(source.toSftpString())) {
                Files.copy(in, localTmpFile, StandardCopyOption.REPLACE_EXISTING);
            } catch (SftpException ex) {
                throw convertAndRethrow(ex, source);
            }

            try (final OutputStream out = sftpClient.write(target.toSftpString(), OpenMode.Write, OpenMode.Create,
                    OpenMode.Truncate)) {

                Files.copy(localTmpFile, out);
            } catch (SftpException ex) {
                throw convertAndRethrow(ex, target);
            }
        } finally {
            Files.delete(localTmpFile);
        }
    }

    static Void setAttributes(final SftpClient sftpClient, final BasicFileAttributes attrs, final SshPath target,
            @SuppressWarnings("unused") final LinkOption... linkOptions) throws IOException {
        SftpClient.Attributes attributes = new SftpClient.Attributes();

        addToAttributres(target, attributes, "lastModifiedTime", attrs.lastModifiedTime());
        addToAttributres(target, attributes, "lastAccessTime", attrs.lastAccessTime());
        addToAttributres(target, attributes, "creationTime", attrs.creationTime());

        writeAttributes(sftpClient, target, attributes);
        return null;
    }

    static Void moveInternal(final SftpClient client, final SshPath source, final SshPath target,
            final BasicFileAttributes targetAttrs, final Set<CopyOption> optionSet) throws IOException {

        try {
            if (optionSet.contains(StandardCopyOption.REPLACE_EXISTING)) {
                if (targetAttrs != null) {
                    // The SFTP version supported by OpenSSH does not support rename with
                    // the Overwrite option, hence we delete the target first
                    delete(client, target, targetAttrs);
                }

                client.rename(source.toSftpString(), //
                        target.toSftpString());
            } else {
                client.rename(source.toSftpString(), //
                        target.toSftpString());
            }

        } catch (SftpException ex) {
            throw convertAndRethrow(ex, target);
        }
        return null;
    }

    static BaseFileAttributes readRemoteAttributes(final SftpClient client, final SshPath path,
            final LinkOption... options) throws IOException {
        try {
            SftpClient.Attributes attrs;
            if (IoUtils.followLinks(options)) {
                attrs = client.stat(path.toString());
            } else {
                attrs = client.lstat(path.toString());
            }
            return toBaseFileAttributes(path, attrs);
        } catch (SftpException e) {
            throw convertAndRethrow(e, path);
        }
    }

    static Void setAttribute(final SftpClient client, final SshPath path, final String attribute, final Object value,
            @SuppressWarnings("unused") final LinkOption... options) throws IOException {
        SftpClient.Attributes attributes = new SftpClient.Attributes();
        addToAttributres(path, attributes, attribute, value);
        writeAttributes(client, path, attributes);
        return null;
    }

    static Void writeAttributes(final SftpClient client, final SshPath path, final Attributes attrs)
            throws IOException {
        try {
            client.setStat(path.toSftpString(), attrs);
        } catch (SftpException ex) {
            throw convertAndRethrow(ex, path);
        }
        return null;
    }

    private static IOException convertAndRethrow(final SftpException e, final SshPath file) throws IOException {
        final String path = file.toString();
        final int status = e.getStatus();

        IOException converted = null;
        if (status == SftpConstants.SSH_FX_NO_SUCH_FILE) {
            converted = new NoSuchFileException(path);
        } else if (status == SftpConstants.SSH_FX_PERMISSION_DENIED) {
            converted = new AccessDeniedException(path);
        } else if (status == SftpConstants.SSH_FX_FILE_ALREADY_EXISTS) {
            converted = new FileAlreadyExistsException(file.toSftpString());
        }

        if (converted != null) {
            converted.setStackTrace(e.getStackTrace());
            throw converted;
        }
        throw e;
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

    private static BaseFileAttributes toBaseFileAttributes(final SshPath path, final Attributes attrs) {
        // SFTP v3 does not provide ctime file attributes, see
        // https://datatracker.ietf.org/doc/html/draft-ietf-secsh-filexfer-02#section-5
        // SFTP v3 is by far the most widely used SFTP version (e.g. OpenSSH only
        // supports v3)
        final FileTime creationTime = attrs.getCreateTime() != null ? attrs.getCreateTime() : attrs.getModifyTime();

        final String owner = StringUtils.isBlank(attrs.getOwner()) ? Integer.toString(attrs.getUserId())
                : attrs.getOwner();
        final String group = StringUtils.isBlank(attrs.getGroup()) ? Integer.toString(attrs.getGroupId())
                : attrs.getGroup();

        return new BaseFileAttributes(attrs.isRegularFile(), //
                path, //
                attrs.getModifyTime(), //
                attrs.getAccessTime(), //
                creationTime, //
                attrs.getSize(), //
                attrs.isSymbolicLink(), //
                attrs.isOther(), //
                new SftpFileSystem.DefaultUserPrincipal(owner), //
                new SftpFileSystem.DefaultGroupPrincipal(group), //
                SftpFileSystemProvider.permissionsToAttributes(attrs.getPermissions()));
    }
}
