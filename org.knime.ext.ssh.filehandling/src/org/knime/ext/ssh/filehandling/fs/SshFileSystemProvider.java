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
package org.knime.ext.ssh.filehandling.fs;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.AttributeView;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.sshd.sftp.client.SftpClient;
import org.knime.filehandling.core.connections.base.BaseFileSystemProvider;
import org.knime.filehandling.core.connections.base.attributes.BaseFileAttributes;
import org.knime.filehandling.core.defaultnodesettings.ExceptionUtil;

/**
 * File system provider for {@link SshFileSystem}.
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
public class SshFileSystemProvider extends BaseFileSystemProvider<SshPath, SshFileSystem> {
    private final ConnectionResourcePool m_resources;
    private final Map<Closeable, ConnectionResourceHolder> m_closeables = new ConcurrentHashMap<>();

    private final ThreadLocal<ConnectionResourceHolder> m_resourceRef
        = new ThreadLocal<>();

    /**
     * @param config
     *            SSH connection configuration.
     * @throws IOException
     */
    public SshFileSystemProvider(final SshFSConnectionConfig config) throws IOException {
        m_resources = new ConnectionResourcePool(config);
        m_resources.start();
    }

    @Override
    protected SeekableByteChannel newByteChannelInternal(final SshPath path, final Set<? extends OpenOption> options,
            final FileAttribute<?>... attrs) throws IOException {
        return invokeWithResource(false,
                resource -> NativeSftpProviderUtils.newByteChannelInternal(resource, path, options, attrs));
    }

    @Override
    protected void moveInternal(final SshPath source, final SshPath target, final CopyOption... options)
            throws IOException {

        final var optionSet = Arrays.stream(options).collect(Collectors.toSet());

        final BasicFileAttributes sourceAttrs;
        if (optionSet.contains(LinkOption.NOFOLLOW_LINKS)) {
            sourceAttrs = readAttributes(source, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        } else {
            sourceAttrs = readAttributes(source, BasicFileAttributes.class);
        }

        if (sourceAttrs.isSymbolicLink()) {
            throw new IOException("Moving of symbolic links not supported");
        }

        final BasicFileAttributes targetAttrs = readAttributesIfExists(target);

        invokeWithClient(true,
                client -> NativeSftpProviderUtils.moveInternal(client, source, target, targetAttrs, optionSet));
    }

    private BasicFileAttributes readAttributesIfExists(final SshPath target) throws IOException {
        try {
            return readAttributes(target, BasicFileAttributes.class);
        } catch (NoSuchFileException e) { // NOSONAR expected behavior
            return null;
        }
    }

    @Override
    protected void copyInternal(final SshPath source, final SshPath target, final CopyOption... options)
            throws IOException {
        // attributes of source file

        final var optionSet = Arrays.stream(options).collect(Collectors.toSet());
        final BasicFileAttributes sourceAttrs;

        if (optionSet.contains(LinkOption.NOFOLLOW_LINKS)) {
            sourceAttrs = readAttributes(source, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        } else {
            sourceAttrs = readAttributes(source, BasicFileAttributes.class);
        }

        if (sourceAttrs.isSymbolicLink()) {
            throw new IOException("Copying of symbolic links not supported");
        }

        invokeWithClient(true,
                client -> NativeSftpProviderUtils.copyInternal(client, source, target, sourceAttrs, optionSet));
    }

    @Override
    protected InputStream newInputStreamInternal(final SshPath path, final OpenOption... options) throws IOException {
        return invokeWithResource(false,
                resource -> NativeSftpProviderUtils.newInputStreamInternalImpl(resource, path, options));
    }

    @SuppressWarnings("resource")
    @Override
    protected OutputStream newOutputStreamInternal(final SshPath path, final OpenOption... options) throws IOException {
        final Set<OpenOption> opts = new HashSet<>(Arrays.asList(options));
        return Channels.newOutputStream(newByteChannelInternal(path, opts));
    }

    @Override
    protected Iterator<SshPath> createPathIterator(final SshPath dir, final Filter<? super Path> filter)
            throws IOException {
        return invokeWithResource(
                true,
                resource -> NativeSftpProviderUtils.createPathIteratorImpl(resource, dir, filter));
    }

    @Override
    protected void createDirectoryInternal(final SshPath dir, final FileAttribute<?>... attrs)
            throws IOException {
        invokeWithClient(true, client -> NativeSftpProviderUtils.createDirectoryInternal(client, dir, attrs));
    }

    @SuppressWarnings("resource")
    @Override
    public boolean exists(final SshPath path) throws IOException {
        if (path.isAbsolute() && path.getNameCount() == 0) {
            return true;
        }

        try {
            final BaseFileAttributes attrs = invokeWithClient(true,
                    client -> NativeSftpProviderUtils.readRemoteAttributes(client, path));
            getFileSystemInternal().addToAttributeCache(path, attrs);
            return true;
        } catch (final NoSuchFileException e) { // NOSONAR by contract this exception is not rethrown
            return false;
        }
    }

    @Override
    protected BaseFileAttributes fetchAttributesInternal(final SshPath path, final Class<?> type) throws IOException {
        if (!type.isAssignableFrom(PosixFileAttributes.class)) {
            throw new UnsupportedOperationException("readAttributes(" + path + ")[" + type.getSimpleName() + "] N/A");
        }

        return invokeWithClient(true, client -> NativeSftpProviderUtils.readRemoteAttributes(client, path));
    }

    @Override
    protected void checkAccessInternal(final SshPath path, final AccessMode... modes) throws IOException {
        // not checked only existence of attributes
        // because check access mode for different file system is very complex deal.
    }

    @Override
    protected void deleteInternal(final SshPath path) throws IOException {
        final BasicFileAttributes attrs = readAttributes(path, BasicFileAttributes.class);
        invokeWithClient(true, client -> NativeSftpProviderUtils.delete(client, path, attrs));
    }


    @SuppressWarnings("resource")
    @Override
    public void setAttribute(final Path path, final String name, final Object value,
            final LinkOption... options)
            throws IOException {
        invokeWithClient(true,
                client -> NativeSftpProviderUtils.setAttribute(client, (SshPath) path, name, value, options));
        getFileSystemInternal().removeFromAttributeCache(path);
    }

    /**
     * @param path
     *            path to write attributes.
     * @param attrs
     *            attributes.
     * @throws IOException
     */
    @SuppressWarnings("resource")
    void writeRemoteAttributes(final Path path, final SftpClient.Attributes attrs) throws IOException {
        invokeWithClient(true,
                client -> NativeSftpProviderUtils.writeAttributes(client, (SshPath) path, attrs));
        getFileSystemInternal().removeFromAttributeCache(path);
    }

    void prepareClose() {
        m_resources.stop();
    }

    /**
     * @param releaseResource
     *            whether or not should release resource just after invoke method.
     * @param func
     *            function to invoke with resource.
     * @return invocation result.
     */
    private <R> R invokeWithResource(final boolean releaseResource, final WithResourceInvocable<R> func)
            throws IOException {

        final ConnectionResource resource = m_resources.take();
        try {
            final R result = func.invoke(resource);
            if (releaseResource) {
                m_resources.release(resource);
            } else {
                // if not released immediately then should be listen
                // for release it later.
                m_resourceRef.get().setResource(resource);
            }
            return result;
        } catch (Exception e) { // NOSONAR prevent resource leakage caused by non-IOEs being thrown
            m_resources.release(resource);
            throw ExceptionUtil.wrapAsIOException(e);
        }
    }

    /**
     * @param command
     *            the command to execute
     * @param encoding
     *            the encoding in which to send the command
     * @param timeOut
     *            the maximum time out to wait for the resource to be created and
     *            the connection to be established. A duration of {@code null} means
     *            no time out
     * @param func
     *            function to invoke with channel.
     * @return invocation result.
     * @throws IOException
     *             if any exception occurs
     */
    @SuppressWarnings("resource")
    public <R> R invokeWithExecChannel(final String command, final Charset encoding, final Duration timeOut,
            final WithExecChannelInvocable<R> func) throws IOException {

        final var chan = m_resources.takeExecChannel(command, encoding, timeOut);
        try {
            return func.invoke(chan);
        } catch (Exception e) { // NOSONAR prevent resource leakage caused by non-IOEs being thrown
            throw ExceptionUtil.wrapAsIOException(e);
        } finally {
            m_resources.release(chan);
        }
    }

    /**
     * @param releaseResource
     *            whether or not should release resource just after invoke method.
     * @param func
     *            function to invoke with client.
     * @return invocation result.
     */
    private <R> R invokeWithClient(final boolean releaseResource, final WithClientInvocable<R> func)
            throws IOException {
        return invokeWithResource(releaseResource, resource -> func.invoke(resource.getClient()));
    }

    void unregisterCloseable(final Closeable closeable) {
        final ConnectionResourceHolder holder = m_closeables.remove(closeable);
        if (holder != null) {
            m_resources.release(holder.getResource());
        }
    }

    private void startCatchResource() {
        if (m_resourceRef.get() != null) {
            throw new IllegalStateException("Already waiting resource");
        }
        m_resourceRef.set(new ConnectionResourceHolder());
    }

    /**
     * @param closeable
     *            closeable.
     */
    private void finishCatchResource(final Closeable closeable) {
        final ConnectionResourceHolder holder = m_resourceRef.get();
        m_resourceRef.remove();
        if (closeable != null && holder != null) {
            m_closeables.put(closeable, holder);
        }
    }

    @Override
    public SeekableByteChannel newByteChannel(final Path path, final Set<? extends OpenOption> options, final FileAttribute<?>... attrs)
            throws IOException {
        startCatchResource();
        SeekableByteChannel channel = null;
        try {
            channel = super.newByteChannel(path, options, attrs);
        } finally {
            finishCatchResource(channel);
        }
        return channel;
    }

    @Override
    public InputStream newInputStream(final Path path, final OpenOption... options) throws IOException {
        startCatchResource();
        InputStream in = null;
        try {
            in = super.newInputStream(path, options);
        } finally {
            finishCatchResource(in);
        }
        return in;
    }

    @Override
    public OutputStream newOutputStream(final Path path, final OpenOption... options) throws IOException {
        startCatchResource();
        OutputStream out = null;
        try {
            out = super.newOutputStream(path, options);
        } finally {
            finishCatchResource(out);
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected <V extends FileAttributeView> V getFileAttributeViewInternal(final SshPath path, final Class<V> type,
            final LinkOption... options) {

        if (type == AttributeView.class || type == BasicFileAttributeView.class
                || type == PosixFileAttributeView.class) {
            return (V) new SshFileAttributeView(path, options);
        } else {
            return null;
        }
    }

    @Override
    protected boolean isHiddenInternal(final SshPath path) throws IOException {
        return path != null && !path.isRoot() && path.getFileName().toString().startsWith(".");
    }
}
